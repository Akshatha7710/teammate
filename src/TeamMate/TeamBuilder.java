package TeamMate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * TeamBuilder: Implements a single-pass, optimized greedy algorithm to maximize the number
 * of teams, strictly enforce 1 Leader / 1-2 Thinkers / Rest Balanced, and maintain skill balance
 * based on a **Binary Skill Polarization** (High > 5, Low <= 5) strategy.
 */
public class TeamBuilder {

    // --- CONFIGURATION: MINIMUM ACCEPTABLE SIZE ---
    // The size check is now performed using 'teamSize' (N) in validateTeams.
    private static final int MINIMUM_ACCEPTABLE_SIZE = 3;
    // ----------------------------------------------

    private final ExecutorService executor;

    public TeamBuilder() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Data class to hold the result of the formation: formed teams and participants who remain unformed.
     */
    public static class TeamFormationResult {
        public final List<Team> formedTeams;
        public final List<Participant> unformedParticipants;

        public TeamFormationResult(List<Team> formedTeams, List<Participant> unformedParticipants) {
            this.formedTeams = formedTeams;
            this.unformedParticipants = unformedParticipants;
        }
    }

    /**
     * Builds teams asynchronously, validates quality, and separates unformed participants.
     */
    public TeamFormationResult buildTeamsAndValidate(List<Participant> participants, int teamSize)
            throws InterruptedException, ExecutionException {

        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be null or empty.");
        }
        if (teamSize <= 1) {
            throw new IllegalArgumentException("Team size must be greater than 1.");
        }

        Callable<TeamFormationResult> formationJob = () -> formTeamsGreedyAlgorithm(participants, teamSize);

        Future<TeamFormationResult> future = executor.submit(formationJob);

        TeamFormationResult result = future.get();
        // Pass the required teamSize to validateTeams to enforce the exact size requirement
        return validateTeams(result.formedTeams, result.unformedParticipants, teamSize);
    }

    /** The core concurrent, greedy team formation logic, optimized for personality caps and size N. */
    private TeamFormationResult formTeamsGreedyAlgorithm(List<Participant> participants, int teamSize) {
        List<Team> teams = new ArrayList<>();
        List<Participant> sortedParticipants = new ArrayList<>(participants);
        // Sort participants by skill descending to allow high-skill members to seed teams first
        sortedParticipants.sort(Comparator.comparingInt(Participant::getSkillLevel).reversed());

        for (Participant p : sortedParticipants) {
            Team bestTeam = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            // 1. Try to fit into an existing, incomplete team
            for (Team t : teams) {
                if (t.size() < teamSize) { // Uses teamSize (N) as the cap
                    double score = evaluateFit(t, p, teamSize);
                    if (score > bestScore) {
                        bestScore = score;
                        bestTeam = t;
                    }
                }
            }

            // 2. If no good fit or if teams list is empty, create a new team
            // Start a new team if no good existing fit, and the participant is a Leader (highest priority for new team)
            if (bestTeam == null || (bestScore < 0.0 && p.getPersonalityType() == PersonalityType.LEADER)) {
                bestTeam = new Team();
                teams.add(bestTeam);
            }

            // Only add the participant if the fit is acceptable.
            // The score threshold is low, but personality weights must ensure key members are accepted.
            if (bestTeam != null && bestScore > -10.0) {
                bestTeam.addMember(p);
            }
        }

        // Collect all participants who didn't get placed in any team during the loop
        Set<Participant> formedParticipants = teams.stream()
                .flatMap(t -> t.getMembers().stream())
                .collect(Collectors.toSet());

        List<Participant> unformedParticipants = participants.stream()
                .filter(p -> !formedParticipants.contains(p))
                .collect(Collectors.toList());

        return new TeamFormationResult(teams, unformedParticipants);
    }

    /** * Validates teams against EXACT SIZE (requiredSize) and strict personality requirements.
     */
    private TeamFormationResult validateTeams(List<Team> formedTeams, List<Participant> initialUnformed, int requiredSize) {
        List<Team> validatedTeams = new ArrayList<>();
        List<Participant> unformed = new ArrayList<>(initialUnformed);

        for (Team t : formedTeams) {
            // New Logic: Must be the required size AND meet quality metrics
            if (t.size() == requiredSize && isTeamQualityMet(t)) {
                validatedTeams.add(t);
            } else {
                // If team fails validation (wrong size OR fails quality), all members are added to the unformed list
                unformed.addAll(t.getMembers());
            }
        }
        return new TeamFormationResult(validatedTeams, unformed);
    }

    /** Checks if team meets exactly 1 Leader, 1-2 Thinkers, and at least 1 Balanced member. */
    private boolean isTeamQualityMet(Team t) {

        long leaders = t.getMembers().stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.LEADER)
                .count();

        long thinkers = t.getMembers().stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                .count();

        long balanced = t.getMembers().stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.BALANCED)
                .count();

        // Enforce exactly 1 Leader
        if (leaders != 1) return false;

        // Enforce 1 to 2 Thinkers
        if (thinkers < 1 || thinkers > 2) return false;

        // Enforce at least 1 Balanced
        if (balanced < 1) return false;

        return true;
    }

    /** Helper function to classify skill: true if High (> 5), false if Low (<= 5). */
    private boolean isHighSkill(int skillLevel) {
        return skillLevel > 5;
    }


    /** Calculates a weighted fit score for adding participant 'p' to team 't'. */
    private double evaluateFit(Team t, Participant p, int teamSize) {
        double score = 0.0;

        // --- 1. Personality Scoring (ENHANCED WEIGHTS TO DOMINATE SKILL SCORES) ---
        long currentLeaders = t.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.LEADER).count();
        long currentThinkers = t.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.THINKER).count();
        PersonalityType incomingType = p.getPersonalityType();


        if (incomingType == PersonalityType.LEADER) {
            if (currentLeaders == 0) {
                // INCREASED REWARD: Ensure the first Leader is prioritized over any skill penalty
                score += 50.0;
            } else {
                score -= 1000.0; // MASSIVE PENALTY: NO second Leader (Hard Cap)
            }
        }
        else if (incomingType == PersonalityType.THINKER) {
            if (currentThinkers == 0) {
                // INCREASED REWARD: Ensure the first Thinker is prioritized over any skill penalty
                score += 50.0;
            } else if (currentThinkers == 1) {
                score += 5.0; // Minor reward for 2nd Thinker (allowed filler)
            } else {
                score -= 1000.0; // MASSIVE PENALTY: NO third Thinker (Hard Cap)
            }
        }
        else if (incomingType == PersonalityType.BALANCED) {
            // High reward for Balanced as they are the preferred filler type
            score += 30.0;
        }
        else { // UNCLASSIFIED
            // Heavy penalty for last resort, but now relative to the high rewards
            score -= 15.0;
        }


        // --- 2. Skill Balancing (High Priority - ENFORCE BINARY POLARIZATION) ---
        boolean newMemberIsHigh = isHighSkill(p.getSkillLevel());

        // Count existing members by skill class
        long highCount = t.getMembers().stream()
                .filter(m -> isHighSkill(m.getSkillLevel()))
                .count();
        long lowCount = t.size() - highCount;

        if (t.size() == 0) {
            // No action on empty team.
        }
        else {
            // *** ADJUSTED PENALTY: 40.0 is greater than BALANCED reward (30.0), forcing polarization ***
            final double MONOCULTURE_PENALTY = 40.0;
            final double BALANCING_REWARD = 20.0;

            // Scenario A: Monoculture Check (All existing members are currently the same skill class)
            if (highCount == t.size() || lowCount == t.size()) {

                boolean isMonocultureHigh = highCount == t.size();

                // If adding the opposite skill class (breaking monoculture)
                if ((isMonocultureHigh && !newMemberIsHigh) || (!isMonocultureHigh && newMemberIsHigh)) {
                    score += BALANCING_REWARD;
                }
                // If reinforcing the monoculture (all High + another High, or all Low + another Low)
                else {
                    score -= MONOCULTURE_PENALTY; // Strong penalty to force mixing
                }
            }
            // Scenario B: Already Mixed (Aim to keep High and Low counts roughly equal)
            else {
                // Reward adding the underrepresented skill type
                if (newMemberIsHigh && highCount < lowCount) {
                    score += 5.0;
                } else if (!newMemberIsHigh && lowCount < highCount) {
                    score += 5.0;
                }
                // Penalize adding to the majority
                else {
                    score -= 5.0;
                }
            }
        }


        // --- 3. Diversity and Variety Scoring ---
        // A. Game/Sport Variety (Cap per game: Max 2)
        long sameInterestCount = t.getMembers().stream()
                .filter(m -> m.getInterest().equals(p.getInterest()))
                .count();

        if (sameInterestCount >= 2) {
            score -= 10.0; // Significant penalty to avoid a third member with the same interest
        } else if (sameInterestCount == 0) {
            score += 1.5; // Minor reward for introducing a new interest (diversity)
        }

        // B. Role Diversity (Ensure at least 3 unique roles)
        Set<Role> currentRoles = t.getMembers().stream().map(Participant::getPreferredRole).collect(Collectors.toSet());

        // Strong reward for a new role when under the minimum diversity (3 unique roles)
        if (!currentRoles.contains(p.getPreferredRole())) {
            if (currentRoles.size() < 3) {
                score += 2.0;
            } else {
                score += 0.5;
            }
        }

        // --- 4. Team Filling (Minor biases and Randomization) ---

        // Minor reward for filling a larger team, ensuring smaller teams aren't always prioritized
        score += (double) t.size() / teamSize * 0.1;

        // RANDOMIZATION WITHIN CONSTRAINTS: Introduce a small random factor to break ties
        score += Math.random() * 0.05; // Add a random float between 0.0 and 0.05

        return score;
    }

    /** Shuts down the internal executor service. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
package TeamMate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TeamBuilder {

    private static final int MINIMUM_ACCEPTABLE_SIZE = 3;
    private static final int MAX_SAME_GAME_PER_TEAM = 2;

    // THREAD POOL FOR SCORING
    private final ExecutorService executor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static class TeamFormationResult {
        public final List<Team> formedTeams;
        public final List<Participant> unformedParticipants;

        public TeamFormationResult(List<Team> formedTeams, List<Participant> unformedParticipants) {
            this.formedTeams = formedTeams;
            this.unformedParticipants = unformedParticipants;
        }
    }

    // OPTION 7 → STRICT VALIDATION FROM ALL PARTICIPANTS
    public TeamFormationResult buildTeamsAndValidate(List<Participant> participants, int teamSize)
            throws TeamMateException {

        if (teamSize < MINIMUM_ACCEPTABLE_SIZE)
            throw new TeamMateException("Team size must be at least " + MINIMUM_ACCEPTABLE_SIZE);

        // SPECIAL HANDLING FOR SIZE 3
        if (teamSize == 3) {
            long leaders = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.LEADER).count();
            long thinkers = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.THINKER).count();
            long balanced = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.BALANCED).count();

            if (leaders < 1 || thinkers < 1 || balanced < 1)
                throw new TeamMateException("Cannot form team of size 3: need 1 LEADER, 1 THINKER, 1 BALANCED.");
        }

        List<Participant> pool = new ArrayList<>(participants);
        Collections.shuffle(pool, new Random());
        List<Team> formed = new ArrayList<>();

        Map<PersonalityType, List<Participant>> byPersonality = pool.stream()
                .collect(Collectors.groupingBy(Participant::getPersonalityType, Collectors.toCollection(ArrayList::new)));

        // STRICT TEAM FORMATION LOOP
        while (pool.size() >= teamSize) {
            Team team = new Team();
            List<Participant> selected = new ArrayList<>();

            // SIZE 3 EXACT COMPOSITION
            if (teamSize == 3) {
                Participant leader = pickAndRemove(byPersonality, pool, PersonalityType.LEADER);
                Participant thinker = pickAndRemove(byPersonality, pool, PersonalityType.THINKER);
                Participant balanced = pickAndRemove(byPersonality, pool, PersonalityType.BALANCED);

                team.addMember(leader);
                team.addMember(thinker);
                team.addMember(balanced);
                formed.add(team);
                continue;
            }

            // PICK LEADER
            Participant leader = pickAndRemove(byPersonality, pool, PersonalityType.LEADER);
            if (leader == null) break;
            team.addMember(leader);
            selected.add(leader);

            // PICK THINKER
            Participant thinker = pickAndRemove(byPersonality, pool, PersonalityType.THINKER);
            if (thinker == null) {
                undoRollback(selected, pool, byPersonality);
                break;
            }
            team.addMember(thinker);
            selected.add(thinker);

            // FILL REMAINING SLOTS using multithreaded ranking
            int slotsToFill = teamSize - selected.size();
            List<Participant> candidates = rankCandidatesParallel(pool, team, teamSize);

            for (Participant cand : new ArrayList<>(candidates)) {
                if (slotsToFill <= 0) break;

                if (!canAddToTeam(cand, team)) continue;

                long leadersCount = countPersonality(team, PersonalityType.LEADER);
                long thinkersCount = countPersonality(team, PersonalityType.THINKER);

                if (cand.getPersonalityType() == PersonalityType.LEADER && leadersCount >= 1) continue;
                if (cand.getPersonalityType() == PersonalityType.THINKER && thinkersCount >= 2) continue;

                // ACCEPT
                team.addMember(cand);
                selected.add(cand);
                pool.remove(cand);
                byPersonality.get(cand.getPersonalityType()).remove(cand);
                slotsToFill--;
            }

            if (validateTeamStrict(team, teamSize)) {
                formed.add(team);
                AppLogger.info("Formed team " + team.getId() + " size=" + team.size());
            } else {
                AppLogger.warning("Dissolving candidate team " + team.getId() + " (constraints not met)");
                undoRollback(selected, pool, byPersonality);
                break;
            }
        }

        List<Participant> unformed = sortedUnformed(pool);
        return new TeamFormationResult(formed, unformed);
    }

    // OPTION 6 → RELAXED VALIDATION (ONLY UNFORMED)
    public TeamFormationResult buildTeamsFromUnformed(List<Participant> participants, int teamSize)
            throws TeamMateException {

        if (teamSize < MINIMUM_ACCEPTABLE_SIZE)
            throw new TeamMateException("Team size must be at least " + MINIMUM_ACCEPTABLE_SIZE);

        // SPECIAL HANDLING FOR SIZE 3
        if (teamSize == 3) {
            long leaders = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.LEADER).count();
            long thinkers = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.THINKER).count();
            long balanced = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.BALANCED).count();

            if (leaders < 1 || thinkers < 1 || balanced < 1)
                throw new TeamMateException("Cannot form team of size 3 from unformed participants.");
        }

        List<Participant> pool = new ArrayList<>(participants);
        Collections.shuffle(pool);
        List<Team> formed = new ArrayList<>();

        Map<PersonalityType, List<Participant>> byPersonality =
                pool.stream().collect(Collectors.groupingBy(Participant::getPersonalityType,
                        Collectors.toCollection(ArrayList::new)));

        while (pool.size() >= teamSize) {
            Team team = new Team();
            List<Participant> selected = new ArrayList<>();

            // EXACT COMBO FOR SIZE 3
            if (teamSize == 3) {
                team.addMember(pickAndRemove(byPersonality, pool, PersonalityType.LEADER));
                team.addMember(pickAndRemove(byPersonality, pool, PersonalityType.THINKER));
                team.addMember(pickAndRemove(byPersonality, pool, PersonalityType.BALANCED));
                formed.add(team);
                continue;
            }

            Participant leader = pickAndRemove(byPersonality, pool, PersonalityType.LEADER);
            if (leader == null) break;
            team.addMember(leader);
            selected.add(leader);

            Participant thinker = pickAndRemove(byPersonality, pool, PersonalityType.THINKER);
            if (thinker == null) {
                undoRollback(selected, pool, byPersonality);
                break;
            }
            team.addMember(thinker);
            selected.add(thinker);

            int slotsToFill = teamSize - selected.size();
            List<Participant> candidates = rankCandidatesParallel(pool, team, teamSize);

            for (Participant cand : candidates) {
                if (slotsToFill <= 0) break;
                if (!canAddToTeam(cand, team)) continue;

                long leadersCount = countPersonality(team, PersonalityType.LEADER);
                long thinkersCount = countPersonality(team, PersonalityType.THINKER);

                if (cand.getPersonalityType() == PersonalityType.LEADER && leadersCount >= 1) continue;
                if (cand.getPersonalityType() == PersonalityType.THINKER && thinkersCount >= 2) continue;

                team.addMember(cand);
                selected.add(cand);
                pool.remove(cand);
                byPersonality.get(cand.getPersonalityType()).remove(cand);
                slotsToFill--;
            }

            if (team.size() == teamSize && validateTeamFinalSize(team)) {
                formed.add(team);
                AppLogger.info("Formed unformed team " + team.getId() + " size=" + team.size());
            } else {
                AppLogger.warning("Dissolving unformed team " + team.getId());
                undoRollback(selected, pool, byPersonality);
                break;
            }
        }

        List<Participant> unformed = sortedUnformed(pool);
        return new TeamFormationResult(formed, unformed);
    }

    // MULTITHREADED SCORING (FAST)
    private List<Participant> rankCandidatesParallel(List<Participant> pool, Team team, int teamSize) {
        List<Callable<Map.Entry<Participant, Double>>> tasks = new ArrayList<>();

        for (Participant p : pool) {
            tasks.add(() -> Map.entry(p, calculateScore(p, team, teamSize)));
        }

        try {
            List<Future<Map.Entry<Participant, Double>>> futures = executor.invokeAll(tasks);

            List<Map.Entry<Participant, Double>> scored = new ArrayList<>();
            for (Future<Map.Entry<Participant, Double>> f : futures) {
                scored.add(f.get());
            }

            return scored.stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Error in threaded scoring", e);
        }
    }

    // HELPER METHODS
    private void undoRollback(List<Participant> selected, List<Participant> pool,
                              Map<PersonalityType, List<Participant>> byPersonality) {
        for (Participant p : selected) {
            pool.add(p);
            byPersonality.computeIfAbsent(p.getPersonalityType(), k -> new ArrayList<>()).add(p);
        }
    }

    private List<Participant> sortedUnformed(List<Participant> pool) {
        return pool.stream().sorted(Comparator.comparing(Participant::getId))
                .collect(Collectors.toList());
    }

    private long countPersonality(Team t, PersonalityType type) {
        return t.getMembers().stream()
                .filter(m -> m.getPersonalityType() == type).count();
    }

    private Participant pickAndRemove(Map<PersonalityType, List<Participant>> byPersonality,
                                      List<Participant> pool, PersonalityType type) {

        List<Participant> list = byPersonality.get(type);
        if (list == null || list.isEmpty()) return null;

        Participant p = list.remove(0);
        pool.remove(p);
        return p;
    }

    private boolean canAddToTeam(Participant p, Team t) {
        long sameGameCount = t.getMembers().stream()
                .filter(m -> m.getInterest().equalsIgnoreCase(p.getInterest()))
                .count();
        return sameGameCount < MAX_SAME_GAME_PER_TEAM;
    }

    // making balancing teams based on leader,thinker and balanced
    private boolean validateTeamStrict(Team t, int teamSize) {
        if (t.size() != teamSize) return false;

        long leaders = countPersonality(t, PersonalityType.LEADER);
        long thinkers = countPersonality(t, PersonalityType.THINKER);
        long balanced = countPersonality(t, PersonalityType.BALANCED);

        if (leaders != 1) return false;
        if (thinkers < 1 || thinkers > 2) return false;
        if (leaders + thinkers + balanced != teamSize) return false;

        if (t.rolesPresent().size() < 3) return false;

        Map<String, Long> byGame = t.getMembers().stream()
                .collect(Collectors.groupingBy(Participant::getInterest, Collectors.counting()));

        for (Long c : byGame.values()) if (c > MAX_SAME_GAME_PER_TEAM) return false;

        return true;
    }

    //checking for conditions
    private boolean validateTeamFinalSize(Team t) {
        if (t.size() < MINIMUM_ACCEPTABLE_SIZE) return false;

        long leaders = countPersonality(t, PersonalityType.LEADER);
        long thinkers = countPersonality(t, PersonalityType.THINKER);

        if (leaders == 0 || thinkers == 0) return false;

        Map<String, Long> byGame = t.getMembers().stream()
                .collect(Collectors.groupingBy(Participant::getInterest, Collectors.counting()));

        for (Long c : byGame.values()) if (c > MAX_SAME_GAME_PER_TEAM) return false;

        return true;
    }

    // calculate score to determine leader,thinker or balanced
    private double calculateScore(Participant p, Team team, int teamSize) {
        double score = 0.0;

        if (!team.rolesPresent().contains(p.getPreferredRole()))
            score += 2.0;

        long sameGame = team.getMembers().stream()
                .filter(m -> m.getInterest().equalsIgnoreCase(p.getInterest())).count();

        if (sameGame == 0) score += 1.0;
        else if (sameGame == 1) score += 0.2;

        double avg = team.averageSkill();
        double projected = ((avg * team.size()) + p.getSkillLevel()) / (team.size() + 1);
        score += 2.0 * (1.0 - Math.abs(projected - 50.0) / 50.0);

        long thinkers = countPersonality(team, PersonalityType.THINKER);
        if (p.getPersonalityType() == PersonalityType.THINKER && thinkers < 2)
            score += 1.0;

        long leaders = countPersonality(team, PersonalityType.LEADER);
        if (p.getPersonalityType() == PersonalityType.LEADER && leaders == 0)
            score += 5.0;

        score += Math.random() * 0.01;
        return score;
    }

    // OPTIONAL SHUTDOWN
    public void shutdown() {
        executor.shutdown();
    }
}
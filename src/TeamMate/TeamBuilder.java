package TeamMate;

import java.util.*;
import java.util.stream.Collectors;

public class TeamBuilder {

    private static final int MINIMUM_ACCEPTABLE_SIZE = 3;
    private static final int MAX_SAME_GAME_PER_TEAM = 2;

    public static class TeamFormationResult {
        public final List<Team> formedTeams;
        public final List<Participant> unformedParticipants;

        public TeamFormationResult(List<Team> formedTeams, List<Participant> unformedParticipants) {
            this.formedTeams = formedTeams;
            this.unformedParticipants = unformedParticipants;
        }
    }

    /**
     * Option 7: Forms teams of the exact 'teamSize' using STRICT validation from the entire participant list.
     * Stops and returns remaining participants immediately upon failure.
     */
    public TeamFormationResult buildTeamsAndValidate(List<Participant> participants, int teamSize) throws TeamMateException {
        if (teamSize < MINIMUM_ACCEPTABLE_SIZE)
            throw new TeamMateException("Team size must be at least " + MINIMUM_ACCEPTABLE_SIZE);

        // --- SPECIAL HANDLING FOR TEAM SIZE 3 ---
        if (teamSize == 3) {
            long leaders = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.LEADER).count();
            long thinkers = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.THINKER).count();
            long balanced = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.BALANCED).count();
            if (leaders < 1 || thinkers < 1 || balanced < 1) {
                throw new TeamMateException("Cannot form team of size 3: need 1 LEADER, 1 THINKER, 1 BALANCED.");
            }
        }

        List<Participant> pool = new ArrayList<>(participants);
        Collections.shuffle(pool, new Random());
        List<Team> formed = new ArrayList<>();

        Map<PersonalityType, List<Participant>> byPersonality = pool.stream()
                .collect(Collectors.groupingBy(Participant::getPersonalityType, Collectors.toCollection(ArrayList::new)));

        // PHASE 1: STRICT TEAM FORMATION
        while (pool.size() >= teamSize) {
            Team team = new Team();
            List<Participant> selected = new ArrayList<>();

            // TEAM SIZE 3: pick exactly 1 LEADER, 1 THINKER, 1 BALANCED
            if (teamSize == 3) {
                Participant leader = pickAndRemove(byPersonality, pool, PersonalityType.LEADER);
                Participant thinker = pickAndRemove(byPersonality, pool, PersonalityType.THINKER);
                Participant balanced = pickAndRemove(byPersonality, pool, PersonalityType.BALANCED);
                team.addMember(leader);
                team.addMember(thinker);
                team.addMember(balanced);
                formed.add(team);
                continue; // skip normal scoring logic
            }

            // NORMAL TEAM FORMATION FOR SIZE > 3
            Participant leader = pickAndRemove(byPersonality, pool, PersonalityType.LEADER);
            if (leader == null) break;
            team.addMember(leader); selected.add(leader);

            Participant thinker = pickAndRemove(byPersonality, pool, PersonalityType.THINKER);
            if (thinker == null) {
                pool.addAll(selected);
                for (Participant p : selected)
                    byPersonality.computeIfAbsent(p.getPersonalityType(), k -> new ArrayList<>()).add(p);
                break;
            }
            team.addMember(thinker); selected.add(thinker);

            int slotsToFill = teamSize - selected.size();
            List<Participant> candidates = new ArrayList<>(pool);
            candidates.sort((a, b) -> Double.compare(calculateScore(b, team, teamSize), calculateScore(a, team, teamSize)));

            for (Participant cand : new ArrayList<>(candidates)) {
                if (slotsToFill <= 0) break;
                if (!canAddToTeam(cand, team)) continue;
                long leadersCount = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.LEADER).count();
                long thinkersCount = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.THINKER).count();
                if (cand.getPersonalityType() == PersonalityType.LEADER && leadersCount >= 1) continue;
                if (cand.getPersonalityType() == PersonalityType.THINKER && thinkersCount >= 2) continue;
                team.addMember(cand);
                selected.add(cand);
                pool.remove(cand);
                byPersonality.getOrDefault(cand.getPersonalityType(), new ArrayList<>()).remove(cand);
                slotsToFill--;
            }

            if (validateTeamStrict(team, teamSize)) {
                formed.add(team);
                AppLogger.info("Formed team " + team.getId() + " size=" + team.size());
            } else {
                AppLogger.warning("Dissolving candidate team " + team.getId() + " (constraints not met)");
                for (Participant p : selected) {
                    pool.add(p);
                    byPersonality.computeIfAbsent(p.getPersonalityType(), k -> new ArrayList<>()).add(p);
                }
                break;
            }
        }

        List<Participant> unformed = new ArrayList<>(pool);
        unformed.sort(Comparator.comparing(Participant::getId));
        return new TeamFormationResult(formed, unformed);
    }

    //-------------------------------------------------------------------------

    /**
     * Option 6: Dedicated method for the user to try and form teams of the exact 'teamSize'
     * from the unformed pool using relaxed constraints.
     */
    public TeamFormationResult buildTeamsFromUnformed(List<Participant> participants, int teamSize) throws TeamMateException {
        if (teamSize < MINIMUM_ACCEPTABLE_SIZE)
            throw new TeamMateException("Team size must be at least " + MINIMUM_ACCEPTABLE_SIZE);

        // --- SPECIAL HANDLING FOR TEAM SIZE 3 ---
        if (teamSize == 3) {
            long leaders = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.LEADER).count();
            long thinkers = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.THINKER).count();
            long balanced = participants.stream().filter(p -> p.getPersonalityType() == PersonalityType.BALANCED).count();
            if (leaders < 1 || thinkers < 1 || balanced < 1) {
                throw new TeamMateException("Cannot form team of size 3 from unformed participants: need 1 LEADER, 1 THINKER, 1 BALANCED.");
            }
        }

        List<Participant> pool = new ArrayList<>(participants);
        Collections.shuffle(pool, new Random());
        List<Team> formed = new ArrayList<>();
        Map<PersonalityType, List<Participant>> byPersonality = pool.stream()
                .collect(Collectors.groupingBy(Participant::getPersonalityType, Collectors.toCollection(ArrayList::new)));

        while (pool.size() >= teamSize) {
            Team team = new Team();
            List<Participant> selected = new ArrayList<>();

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

            Participant leader = pickAndRemove(byPersonality, pool, PersonalityType.LEADER);
            if (leader == null) break;
            team.addMember(leader); selected.add(leader);

            Participant thinker = pickAndRemove(byPersonality, pool, PersonalityType.THINKER);
            if (thinker == null) {
                pool.addAll(selected);
                for (Participant p : selected)
                    byPersonality.computeIfAbsent(p.getPersonalityType(), k -> new ArrayList<>()).add(p);
                break;
            }
            team.addMember(thinker); selected.add(thinker);

            int slotsToFill = teamSize - selected.size();
            List<Participant> candidates = new ArrayList<>(pool);
            candidates.sort((a, b) -> Double.compare(calculateScore(b, team, teamSize), calculateScore(a, team, teamSize)));

            for (Participant cand : new ArrayList<>(candidates)) {
                if (slotsToFill <= 0) break;
                if (!canAddToTeam(cand, team)) continue;
                long leadersCount = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.LEADER).count();
                long thinkersCount = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.THINKER).count();
                if (cand.getPersonalityType() == PersonalityType.LEADER && leadersCount >= 1) continue;
                if (cand.getPersonalityType() == PersonalityType.THINKER && thinkersCount >= 2) continue;
                team.addMember(cand);
                selected.add(cand);
                pool.remove(cand);
                byPersonality.getOrDefault(cand.getPersonalityType(), new ArrayList<>()).remove(cand);
                slotsToFill--;
            }

            if (team.size() == teamSize && validateTeamFinalSize(team)) {
                formed.add(team);
                AppLogger.info("Formed unformed team " + team.getId() + " size=" + team.size() + " (Final Size Mode)");
            } else {
                AppLogger.warning("Dissolving candidate unformed team " + team.getId() + " (size " + team.size() + " failed final-size constraints)");
                for (Participant p : selected) {
                    pool.add(p);
                    byPersonality.computeIfAbsent(p.getPersonalityType(), k -> new ArrayList<>()).add(p);
                }
                break;
            }
        }

        List<Participant> unformed = new ArrayList<>(pool);
        unformed.sort(Comparator.comparing(Participant::getId));
        return new TeamFormationResult(formed, unformed);
    }

    //-------------------------------------------------------------------------

    private Participant pickAndRemove(Map<PersonalityType, List<Participant>> byPersonality, List<Participant> pool, PersonalityType type) {
        List<Participant> list = byPersonality.get(type);
        if (list == null || list.isEmpty()) return null;
        Participant p = list.remove(0);
        pool.remove(p);
        return p;
    }

    private boolean validateTeamStrict(Team team, int teamSize) {
        if (team.size() != teamSize) return false;
        long leaders = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.LEADER).count();
        long thinkers = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.THINKER).count();
        long balanced = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.BALANCED).count();
        if (leaders != 1) return false;
        if (thinkers < 1 || thinkers > 2) return false;
        if (leaders + thinkers + balanced != team.size()) return false;
        if (team.rolesPresent().size() < 3) return false;
        Map<String, Long> byGame = team.getMembers().stream().collect(Collectors.groupingBy(Participant::getInterest, Collectors.counting()));
        for (Long cnt : byGame.values()) if (cnt > MAX_SAME_GAME_PER_TEAM) return false;
        return true;
    }

    private boolean validateTeamFinalSize(Team t) {
        if (t.size() < MINIMUM_ACCEPTABLE_SIZE) return false;
        long leaders = t.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.LEADER).count();
        if (leaders == 0) return false;
        long thinkers = t.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.THINKER).count();
        if (thinkers == 0) return false;
        Map<String, Long> byGame = t.getMembers().stream()
                .collect(Collectors.groupingBy(Participant::getInterest, Collectors.counting()));
        for (Long cnt : byGame.values()) if (cnt > MAX_SAME_GAME_PER_TEAM) return false;
        return true;
    }

    private boolean canAddToTeam(Participant p, Team t) {
        long sameGame = t.getMembers().stream().filter(m -> m.getInterest().equalsIgnoreCase(p.getInterest())).count();
        return sameGame < MAX_SAME_GAME_PER_TEAM;
    }

    private double calculateScore(Participant p, Team team, int teamSize) {
        double score = 0.0;
        if (!team.rolesPresent().contains(p.getPreferredRole())) score += 2.0;
        long sameGame = team.getMembers().stream().filter(m -> m.getInterest().equalsIgnoreCase(p.getInterest())).count();
        if (sameGame == 0) score += 1.0; else if (sameGame == 1) score += 0.2;
        double curAvg = team.averageSkill();
        double projected = ((curAvg * team.size()) + p.getSkillLevel()) / (team.size() + 1);
        score += 2.0 * (1.0 - Math.abs(projected - 50.0) / 50.0);
        long thinkers = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.THINKER).count();
        if (p.getPersonalityType() == PersonalityType.THINKER && thinkers < 2) score += 1.0;
        long leaders = team.getMembers().stream().filter(m -> m.getPersonalityType() == PersonalityType.LEADER).count();
        if (p.getPersonalityType() == PersonalityType.LEADER && leaders == 0) score += 5.0;
        score += Math.random() * 0.01;
        return score;
    }
}
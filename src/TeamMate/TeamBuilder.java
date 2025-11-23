package TeamMate;

import java.util.*;
import java.util.concurrent.*;

/**
 * Simplified TeamBuilder:
 * - Forms mixed teams (interest/role/personality diversity + skill balance)
 * - Executes formation in a worker thread to demonstrate concurrency
 * - Easy to justify in UML and viva
 */
public class TeamBuilder {

    private final ExecutorService executor;

    public TeamBuilder() {
        this.executor = Executors.newSingleThreadExecutor(); // single worker thread for formation
    }

    /**
     * Build teams of given teamSize. This method submits the formation job to a worker
     * and waits for the result (demonstrates concurrency).
     */
    public List<Team> buildTeams(List<Participant> participants, int teamSize) throws InterruptedException {
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("Participants cannot be null/empty");
        if (teamSize <= 1) throw new IllegalArgumentException("teamSize must be > 1");

        Callable<List<Team>> job = () -> formTeamsGreedy(participants, teamSize);

        Future<List<Team>> future = executor.submit(job);
        try {
            return future.get(); // wait for formation to complete
        } catch (ExecutionException e) {
            throw new RuntimeException("Error during team formation", e.getCause());
        }
    }

    /**
     * Shut down internal executor. Call once when builder is no longer required.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Greedy formation algorithm:
     * - Sort participants by skill descending
     * - For each participant, place in team with best fit (diversity + skill balance)
     */
    private List<Team> formTeamsGreedy(List<Participant> participants, int teamSize) {
        Team.resetCounter();
        int total = participants.size();
        int numTeams = Math.max(1, (total + teamSize - 1) / teamSize);

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) teams.add(new Team());

        List<Participant> sorted = new ArrayList<>(participants);
        sorted.sort(Comparator.comparingInt(Participant::getSkillLevel).reversed());

        for (Participant p : sorted) {
            Team best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (Team t : teams) {
                if (t.size() >= teamSize) continue;
                double score = evaluateFit(t, p, teamSize);
                if (score > bestScore) {
                    bestScore = score;
                    best = t;
                }
            }
            if (best == null) {
                Team extra = new Team();
                extra.addMember(p);
                teams.add(extra);
            } else {
                best.addMember(p);
            }
        }

        return teams;
    }

    private double evaluateFit(Team t, Participant p, int teamSize) {
        double score = 0.0;

        // interest diversity
        Set<String> interests = new HashSet<>();
        for (Participant m : t.getMembers()) interests.add(m.getInterest());
        if (!interests.contains(p.getInterest())) score += 1.0;

        // role variety
        Set<Role> roles = new HashSet<>();
        for (Participant m : t.getMembers()) roles.add(m.getPreferredRole());
        if (!roles.contains(p.getPreferredRole())) score += 1.0;

        // personality mix
        Set<PersonalityType> types = new HashSet<>();
        for (Participant m : t.getMembers()) types.add(m.getPersonalityType());
        if (!types.contains(p.getPersonalityType())) score += 0.8;

        // skill balancing
        double teamAvg = t.getMembers().isEmpty() ? p.getSkillLevel() : t.getMembers().stream().mapToInt(Participant::getSkillLevel).average().orElse(p.getSkillLevel());
        double skillDiff = Math.abs(teamAvg - p.getSkillLevel());
        score -= skillDiff / 100.0;

        // small bias for filling
        score += (teamSize - t.size()) * 0.05;

        return score;
    }
}
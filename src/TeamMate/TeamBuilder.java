package TeamMate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**

 TeamBuilder class - Forms balanced teams from a given list of participants.

 Teams are balanced by grouping by PersonalityType and then

 pairing high-skill players with low-skill players within those groups.

 Uses a fixed-size thread pool for efficient concurrent processing.
 */
public class TeamBuilder {

    private final ExecutorService executor;

    public TeamBuilder() {
// Use available processors for thread count for efficient concurrency
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**

     Builds teams from the given participant list using multiple threads.

     @param participants The full list of participants.

     @param teamSize The required size for each team.

     @return The list of formed teams.

     @throws InterruptedException If the execution is interrupted.
     */
    public List<Team> buildTeams(List<Participant> participants, int teamSize) throws InterruptedException {
        if (participants == null || participants.isEmpty() || teamSize <= 1) {
            throw new IllegalArgumentException("Participant list cannot be null/empty, and team size must be > 1.");
        }

// --- 1. Preparation: Group by Personality Type ---
        Map<PersonalityType, List<Participant>> grouped = participants.stream()
                .collect(Collectors.groupingBy(Participant::getPersonalityType));

// Create tasks for each personality group
        List<Callable<List<Team>>> tasks = new ArrayList<>();
        for (Map.Entry<PersonalityType, List<Participant>> entry : grouped.entrySet()) {
// Each task runs formTeams on its subset
            tasks.add(() -> formTeams(entry.getValue(), teamSize));
        }

// --- 2. Concurrent Team Formation ---
        List<Team> teams = Collections.synchronizedList(new ArrayList<>());

        try {
            List<Future<List<Team>>> futures = executor.invokeAll(tasks);
            for (Future<List<Team>> future : futures) {
// Future.get() retrieves the result of the callable
                teams.addAll(future.get());
            }

        } catch (ExecutionException e) {
            System.err.println("An error occurred during concurrent team formation: " + e.getMessage());
// Re-throw as runtime exception to halt the program execution
            throw new RuntimeException("Error during concurrent team building", e);
        } finally {
// Shutdown the executor gracefully
            executor.shutdown();
        }

// --- 3. Handle and Balance Leftovers (Partial Teams) ---
        List<Team> fullTeams = new ArrayList<>();
        List<Participant> leftovers = new ArrayList<>();

// Separate full teams from partial teams, consolidating all partial members into 'leftovers'
        for (Team t : teams) {
            if (t.size() == teamSize) {
                fullTeams.add(t);
            } else if (t.size() > 0) {
// Collect members from partial teams
                leftovers.addAll(t.getMembers());
            }
        }

// Final redistribution of leftovers to balance the final list
        balanceLeftovers(fullTeams, leftovers, teamSize);

// Reset the counter so team IDs start from T1 for future runs
        Team.resetCounter();

        return fullTeams;
    }

    /**

     Forms teams from a subset of participants (all of the same PersonalityType)

     by pairing the highest-skill player with the lowest-skill players for balance.

     @param subset Participants of a single personality type.

     @param teamSize The target size of the team.

     @return A list of formed teams, possibly including one partial (leftover) team.
     */
    private List<Team> formTeams(List<Participant> subset, int teamSize) {
// PriorityQueue sorts descending by skill because Participant implements Comparable
        PriorityQueue<Participant> pool = new PriorityQueue<>(subset);
        List<Team> localTeams = new ArrayList<>();

        while (pool.size() >= teamSize) {
            Team newTeam = new Team();

            // 1. Take the highest skill player (Leader)
            Participant leader = pool.poll();
            newTeam.addMember(leader);

            // 2. Build the rest of the team from the lowest skill available
            List<Participant> remainingInPool = new ArrayList<>();

            // FIX: Use poll() in a loop to transfer elements to a List, resolving the drainTo error.
            // Temporarily empty the pool and transfer members to a list
            while (!pool.isEmpty()) {
                remainingInPool.add(pool.poll());
            }

            // Sort ascending (lowest skill first) to grab the lowest skilled participants
            remainingInPool.sort(Comparator.comparingInt(Participant::getSkillLevel));

            int membersNeeded = teamSize - 1;

            // Take the lowest skilled players for balance
            for (int i = 0; i < membersNeeded; i++) {
                // If there are enough remaining players
                if (!remainingInPool.isEmpty()) {
                    newTeam.addMember(remainingInPool.remove(0));
                }
            }

            // Put unselected players back into the PriorityQueue
            pool.addAll(remainingInPool);

            // The team is guaranteed to be full here due to the while (pool.size() >= teamSize) check
            localTeams.add(newTeam);


        }

// Add any remaining participants as a partial team (leftovers)
        if (pool.size() > 0) {
            Team leftovers = new Team();

            // FIX: Use poll() in a loop to transfer remaining elements, resolving the drainTo error.
            while (!pool.isEmpty()) {
                leftovers.addMember(pool.poll());
            }
            localTeams.add(leftovers);


        }

        return localTeams;
    }

    /**

     Distributes leftover players evenly among existing full teams, then forms a new final team if necessary.

     @param fullTeams The list of full teams (will be modified).

     @param leftovers The list of remaining participants.

     @param teamSize The target team size.
     */
    private void balanceLeftovers(List<Team> fullTeams, List<Participant> leftovers, int teamSize) {
// Sort leftovers by skill level (descending) so the best remaining players are distributed first
        leftovers.sort(Comparator.comparingInt(Participant::getSkillLevel).reversed());

        int numLeftovers = leftovers.size();
        int currentLeftoverIndex = 0;
        int teamIndex = 0;

// Pass 1: Distribute leftovers one by one across all full teams
        while (currentLeftoverIndex < numLeftovers && !fullTeams.isEmpty()) {
            Team t = fullTeams.get(teamIndex % fullTeams.size()); // Cycle through teams

            // Add the next best remaining player
            t.addMember(leftovers.get(currentLeftoverIndex++));

            // Advance to the next team
            teamIndex++;


        }

// Pass 2: Create a final team with any truly remaining players
        if (currentLeftoverIndex < numLeftovers) {
            Team finalTeam = new Team();
            while (currentLeftoverIndex < numLeftovers) {
                finalTeam.addMember(leftovers.get(currentLeftoverIndex++));
            }
            fullTeams.add(finalTeam);
        }
    }
}
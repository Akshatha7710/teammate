package TeamMate;

import java.util.List;
import java.util.concurrent.*;

public class TeamFormation {

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static List<Team> formTeams(List<Participant> participants, int teamSize) {
        Callable<List<Team>> job = () -> {
            TeamBuilder builder = new TeamBuilder();
            return builder.buildTeams(participants, teamSize);
        };

        Future<List<Team>> future = executor.submit(job);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Team formation interrupted.");
            return List.of();
        } catch (ExecutionException e) {
            throw new RuntimeException("Error forming teams", e.getCause());
        }
    }

    public static void shutdown() {
        executor.shutdown();
    }
}

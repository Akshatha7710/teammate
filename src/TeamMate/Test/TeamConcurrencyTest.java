package TeamMate.Test;

import TeamMate.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TeamConcurrencyTest {

    @Test
    void testTeamIdUniquenessUnderConcurrency() throws InterruptedException {
        int numThreads = 10;
        int teamsPerThread = 100;
        int totalTeams = numThreads * teamsPerThread;

        // 1. Reset the static counter for consistent testing
        Team.resetCounter();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        // Synchronized Set is required to safely collect IDs from multiple threads
        Set<String> generatedIds = Collections.synchronizedSet(new HashSet<>());
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                for (int j = 0; j < teamsPerThread; j++) {
                    Team team = new Team();
                    generatedIds.add(team.getId());
                }
                return null;
            });
        }

        // 2. Run all tasks concurrently and wait for them to finish
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 3. Assert: If the number of unique IDs equals total teams, the AtomicInteger worked correctly.
        assertEquals(totalTeams, generatedIds.size(),
                "All concurrent team creations must result in the expected number of unique IDs.");
    }

    @Test
    void testLoggerThreadSafety() throws InterruptedException {
        int numThreads = 5;
        int logsPerThread = 20;
        int totalLogs = numThreads * logsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            int threadId = i;
            tasks.add(() -> {
                for (int j = 0; j < logsPerThread; j++) {
                    // AppLogger uses a synchronized block internally, testing its safety under load
                    AppLogger.info("Thread " + threadId + " logging entry " + j);
                }
                return null;
            });
        }

        // Wait for all logging tasks to complete
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assert: Ensure the logger didn't crash and captured the logs.
        assertTrue(AppLogger.getRecentLogs().size() >= totalLogs * 0.9,
                "Logger should have captured most of the concurrent logs without crashing.");
    }
}
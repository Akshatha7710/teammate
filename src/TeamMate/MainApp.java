package TeamMate;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class MainApp {

    private static final String INPUT_FILE = "participants_sample.csv";
    private static final String OUTPUT_FILE = "formed_teams.csv";

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    // Variable to store unformed participants in memory from the last run
    private static List<Participant> unformedParticipantsCache = new CopyOnWriteArrayList<>();

    // NEW: Store the last used team size (N) for use with unformed participants
    private static int lastTeamSize = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        FileService fileService = new FileService();

        List<Participant> participants = new CopyOnWriteArrayList<>();
        try {
            participants = fileService.loadParticipants(INPUT_FILE);
            System.out.printf("%d total participants loaded (including 0 unformed).%n", participants.size());
        } catch (IOException e) {
            System.err.println("CSV load failed. Starting with empty participant list: " + e.getMessage());
        }

        List<Team> teams = null;

        boolean exit = false;
        while (!exit) {
            System.out.println("\nSelect role:");
            System.out.println("1. Participant ");
            System.out.println("2. Organizer ");
            System.out.println("3. Exit ");
            System.out.print("Choice: ");
            int choice = getInt(scanner, 1, 3);

            System.out.println("------------");

            switch (choice) {
                case 1 -> handleParticipant(scanner, participants, fileService, teams);
                case 2 -> teams = handleOrganizer(scanner, participants, fileService, teams);
                case 3 -> exit = true;
            }
        }

        executor.shutdown();
        scanner.close();
        System.out.println("--- Application Closed ---\n");
    }

    private static void handleParticipant(Scanner scanner, List<Participant> participants,
                                          FileService fileService, List<Team> teams) {
        System.out.println("1. Fill Survey ");
        System.out.println("2. Check Team Assignment ");
        System.out.println("3. Back ");
        System.out.print("Choice: ");
        int choice = getInt(scanner, 1, 3);

        switch (choice) {
            case 1 -> {
                // PASS unformedParticipantsCache to SurveyProcessor
                SurveyProcessor surveyJob = new SurveyProcessor(scanner, participants, fileService, unformedParticipantsCache);
                Future<Participant> future = executor.submit(surveyJob);
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Survey failed: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            case 2 -> {
                if (teams == null || teams.isEmpty()) {
                    System.out.println("No teams have been formed yet.");
                    break;
                }

                System.out.print("Enter your Participant ID to check assignment: ");
                String pid = scanner.nextLine().trim();

                Team assignedTeam = findTeamByParticipantId(teams, pid);

                if (assignedTeam != null) {
                    System.out.println("\n--- Your Team Assignment ---");
                    System.out.println("You are assigned to Team ID: " + assignedTeam.getId());
                    System.out.println("Other members in your team:");

                    assignedTeam.getMembers().stream()
                            .filter(p -> !p.getId().equalsIgnoreCase(pid))
                            .forEach(p -> System.out.println("  - " + p.getName() + " (Role: " + p.getPreferredRole() + ")"));
                    System.out.println("---------------------------\n");
                } else {
                    System.out.println("You are not currently assigned to a formed team.");
                }
            }
            case 3 -> {}
        }
    }

    private static List<Team> handleOrganizer(Scanner scanner, List<Participant> participants,
                                              FileService fileService, List<Team> teams) {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Organizer Menu ---");
            System.out.println("1. Edit Participant ");
            System.out.println("2. Remove Participant ");
            System.out.println("3. Form Teams ");
            System.out.println("4. Show All Participants ");
            System.out.println("5. View Unformed Participants (In-Memory)");
            System.out.println("6. Form Team with Unformed Participants");
            System.out.println("7. Back ");
            System.out.print("Choice: ");
            int choice = getInt(scanner, 1, 7);

            switch (choice) {
                case 1:
                    ParticipantEditor.editParticipant(participants, scanner);
                    try {
                        fileService.saveParticipants(participants, INPUT_FILE);
                        System.out.println("Participant list updated and saved to " + INPUT_FILE);
                    } catch (IOException e) {
                        System.err.println("Error saving changes to CSV: " + e.getMessage());
                    }
                    break;

                case 2:
                    ParticipantEditor.removeParticipant(participants, scanner);
                    try {
                        fileService.saveParticipants(participants, INPUT_FILE);
                        System.out.println("Participant removed and list saved to " + INPUT_FILE);
                    } catch (IOException e) {
                        System.err.println("Error saving changes to CSV: " + e.getMessage());
                    }
                    break;

                case 3: // Form & Validate Teams (Large Batch)
                    if (participants.isEmpty()) {
                        System.out.println("Cannot form teams: No participants loaded.");
                        break;
                    }
                    System.out.print("Enter desired team size (This will be the exact size of all formed teams): ");
                    int size = getInt(scanner, 3, participants.size());

                    TeamBuilder builder = new TeamBuilder();
                    try {
                        TeamBuilder.TeamFormationResult result = builder.buildTeamsAndValidate(participants, size);
                        teams = result.formedTeams;
                        unformedParticipantsCache = result.unformedParticipants;

                        // Store the size N
                        lastTeamSize = size;

                        System.out.printf("\nSuccessfully Formed Teams: %d (All size %d)%n", teams.size(), size);
                        System.out.printf("Participants in Waiting List: %d (Stored in memory for this session)%n",
                                unformedParticipantsCache.size());

                        fileService.saveTeams(teams, OUTPUT_FILE);
                        System.out.println("Teams saved to " + OUTPUT_FILE);

                    } catch (Exception e) {
                        System.err.println("Error during team formation: " + e.getMessage());
                    } finally {
                        builder.shutdown();
                    }
                    break;

                case 4: // Show All Participants (Organizer only)
                    participants.forEach(System.out::println);
                    break;

                case 5: // View Unformed Participants (IN-MEMORY ONLY)
                    if (unformedParticipantsCache.isEmpty()) {
                        System.out.println("No participants in the waiting list (in-memory).");
                    } else {
                        System.out.println("\n--- Unformed Participants (In-Memory) ---");
                        unformedParticipantsCache.forEach(System.out::println);
                    }
                    break;

                case 6: // Form Team with Unformed Participants (Now respects size N)
                    // Use the last size, or ask if it was never set (default to 3 as minimum)
                    int requiredSize = (lastTeamSize > 0) ? lastTeamSize : getInt(scanner, 3, 10);

                    System.out.printf("Attempting to form one team of size %d from the waiting list...%n", requiredSize);
                    List<Team> newTeam = attemptFormTeamFromUnformed(fileService, teams, requiredSize);

                    if (newTeam != null) {
                        if (teams == null) teams = new CopyOnWriteArrayList<>();
                        teams.addAll(newTeam);
                        System.out.printf("\nSUCCESS: Team %s formed from the waiting list and added to formed teams.%n", newTeam.getFirst().getId());

                        try {
                            fileService.saveTeams(teams, OUTPUT_FILE);
                            System.out.println("All formed teams saved to " + OUTPUT_FILE);
                        } catch (IOException e) {
                            System.err.println("Error saving formed teams to CSV: " + e.getMessage());
                        }
                    }
                    break;

                case 7:
                    back = true;
                    break;
            }
        }
        return teams;
    }

    /**
     * Attempts to form a single team of EXACT requiredSize from the unformed cache
     * based on personality criteria (1L, 1-2T, rest Balanced/Other).
     * @param requiredSize The exact size N of the team to form.
     */
    private static List<Team> attemptFormTeamFromUnformed(FileService fileService, List<Team> formedTeams, int requiredSize) {
        if (unformedParticipantsCache.size() < requiredSize) {
            System.out.println("ERROR: Cannot form a team of " + requiredSize + ". Only " + unformedParticipantsCache.size() + " participants in the waiting list.");
            return null;
        }

        // 1. Separate by required personality types (Use mutable lists)
        // These lists will contain copies of the references, allowing safe removal from the cache later.
        List<Participant> leaders = unformedParticipantsCache.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.LEADER)
                .collect(Collectors.toCollection(ArrayList::new));

        List<Participant> thinkers = unformedParticipantsCache.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                .collect(Collectors.toCollection(ArrayList::new));

        List<Participant> others = unformedParticipantsCache.stream()
                .filter(p -> p.getPersonalityType() != PersonalityType.LEADER && p.getPersonalityType() != PersonalityType.THINKER)
                .collect(Collectors.toCollection(ArrayList::new));

        // 2. Check for minimum requirements: 1 Leader, 1 Thinker
        if (leaders.isEmpty() || thinkers.isEmpty()) {
            System.out.println("ERROR: Cannot form a team. Waiting list requires at least one LEADER and one THINKER.");
            return null;
        }

        int requiredOthers = requiredSize - 2; // spots remaining after 1L and 1T

        // Check if we can satisfy the 1-2 Thinker rule with the remaining slots
        if (requiredOthers < 0) {
            System.out.println("ERROR: Required size is too small to accommodate 1L, 1T. Min size is 3.");
            return null;
        }

        // This check implicitly ensures we have at least 1 BALANCED or UNCLASSIFIED member
        if (others.size() < requiredOthers) {
            System.out.printf("ERROR: Cannot form a team of size %d. Waiting list needs %d other members (Balanced/Unclassified) but only found %d.%n",
                    requiredSize, requiredOthers, others.size());
            return null;
        }

        // 3. Select and form the team
        Team newTeam = new Team();

        // Select 1 Leader and 1 Thinker
        // FIX: Using get(0) instead of getFirst() for Java version compatibility
        Participant leader = leaders.get(0);
        Participant thinker = thinkers.get(0);

        List<Participant> selectedMembers = new ArrayList<>();
        selectedMembers.add(leader);
        selectedMembers.add(thinker);

        // Select the required number of 'other' members (0 to N-2)
        selectedMembers.addAll(others.subList(0, requiredOthers));

        // Add all selected members to the team
        for (Participant p : selectedMembers) {
            newTeam.addMember(p);
        }

        // 4. Update the in-memory cache by removing the members used
        unformedParticipantsCache.remove(leader);
        unformedParticipantsCache.remove(thinker);

        // Remove the 'others' that were used from the cache.
        unformedParticipantsCache.removeAll(selectedMembers.subList(2, requiredSize));

        return List.of(newTeam);
    }

    // Helper method to find a participant's team (remains the same)
    private static Team findTeamByParticipantId(List<Team> teams, String participantId) {
        if (teams == null) return null;
        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                if (member.getId().equalsIgnoreCase(participantId)) {
                    return team;
                }
            }
        }
        return null;
    }

    // Utility: read int input within range (remains the same)
    private static int getInt(Scanner scanner, int min, int max) {
        while (true) {
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val < min || val > max) throw new NumberFormatException();
                return val;
            } catch (NumberFormatException e) {
                System.out.printf("Enter a number between %d and %d: ", min, max);
            }
        }
    }
}
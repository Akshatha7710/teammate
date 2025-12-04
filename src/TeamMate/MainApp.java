package TeamMate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class MainApp {

    private static final FileService fileService = new FileService();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final TeamMateDB teamMateDB = new TeamMateDB(); // INSTANTIATES & LOADS DB
    private static List<Participant> unformedParticipantsCache = new CopyOnWriteArrayList<>();
    private static List<Team> teams = new CopyOnWriteArrayList<>();
    private static List<Participant> participants = new CopyOnWriteArrayList<>();
    private static int lastTeamSize = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AppLogger.info("Application starting up...");

        // ----------------------------------------------------
        // SHUTDOWN HOOK: This ensures data is saved when the app closes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            AppLogger.info("Application received shutdown signal. Saving data...");
            try {
                teamMateDB.saveToDisk();
                AppLogger.info("Database saved successfully.");
            } catch (TeamMateDBException e) {
                AppLogger.error("Failed to save database on shutdown.", e);
            }
        }));
        // ----------------------------------------------------

        // Step 1: Initialize participants list from CSV
        try {
            List<Participant> csvParticipants = fileService.loadParticipants(FileService.INPUT_FILE);
            AppLogger.info("Loaded " + csvParticipants.size() + " participants from CSV.");

            // Add all CSV participants to the in-memory participant list
            participants.addAll(csvParticipants);

            // Save participants to DB stub (this effectively updates/imports data from CSV into the persistent DB)
            for (Participant p : participants) {
                try {
                    teamMateDB.saveParticipant(p);
                } catch (TeamMateDBException e) {
                    AppLogger.error("Failed to save participant " + p.getId() + " to DB on startup", e);
                }
            }
        } catch (IOException e) {
            AppLogger.error("Failed loading participants", e);
        }

        // Step 2: Initialize teams list from persistent DB (or CSV as fallback/initial load)
        try {
            // Load teams from the DB and update the in-memory teams list
            teams.addAll(teamMateDB.findAllTeams());
            if (teams.isEmpty()) {
                // FALLBACK: If DB was empty or failed to load, try to load teams from CSV
                teams.addAll(fileService.loadTeams(FileService.OUTPUT_FILE, participants));
                for (Team t : teams) {
                    try {
                        teamMateDB.saveTeam(t);
                    } catch (TeamMateDBException e) {
                        AppLogger.error("Failed to save imported team " + t.getId() + " to DB", e);
                    }
                }
            }

            Team.initializeCounter(teams);
            AppLogger.info("Initialized with " + teams.size() + " teams.");

        } catch (IOException e) {
            AppLogger.warning("No formed teams loaded from CSV or DB.");
            Team.resetCounter();
        }

        boolean exit = false;
        while (!exit) {
            showMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": participantMenu(scanner); break;
                case "2": organizerMenu(scanner); break;
                case "3":
                    exit = true;
                    executor.shutdown(); // Shutdown the executor service
                    break;
                default: System.out.println("Invalid choice. Enter a number between 1-3"); break;
            }
        }
    }

    private static void showMainMenu() {
        System.out.println("\n1) Participant");
        System.out.println("2) Organizer");
        System.out.println("3) Exit");
        System.out.print("Choice: ");
    }

    // Participant submenu
    private static void participantMenu(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Participant Menu ---");
            System.out.println("1) Fill Survey");
            System.out.println("2) View My Team");
            System.out.println("3) Back");
            System.out.print("Choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1":
                    Future<Participant> f = executor.submit(
                            new SurveyProcessor(scanner, participants, fileService, unformedParticipantsCache)
                    );
                    try {
                        Participant p = f.get();
                        if (p != null) {
                            try {
                                teamMateDB.saveParticipant(p);
                            } catch (TeamMateDBException e) {
                                AppLogger.error("Failed to save updated participant to DB", e);
                            }
                        }
                    } catch (Exception e) {
                        AppLogger.warning("Survey interrupted.");
                    }
                    break;
                case "2":
                    viewMyTeam(scanner);
                    break;
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1-3.");
            }
        }
    }

    private static void viewMyTeam(Scanner scanner) {
        if (participants.isEmpty()) {
            System.out.println("No participants available yet.");
            return;
        }

        System.out.print("Enter your Participant ID: ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty() || input.matches("\\D+")) {
            System.out.println("Invalid Participant ID.");
            return;
        }

        Participant found = participants.stream()
                .filter(p -> p.getId().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);

        if (found == null) {
            System.out.println("Participant ID is not available in the list.");
            return;
        }

        Team t = findTeamByParticipantId(teams, found.getId());
        if (t == null) {
            System.out.println("You are not yet assigned to a team.");
            return;
        }

        System.out.println("Your Team ID: " + t.getId());
        System.out.println("Members:");
        for (Participant m : t.getMembers()) {
            System.out.println(" - " + m);
        }
    }

    // Organizer submenu
    private static void organizerMenu(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Organizer Menu ---");
            System.out.println("1) Edit Participant");
            System.out.println("2) Remove Participant");
            System.out.println("3) View Unformed Participants");
            System.out.println("4) View Logger Data");
            System.out.println("5) View All Participants");
            System.out.println("6) Make Team With Unformed Participants");
            System.out.println("7) Form Teams (From ALL participants)");
            System.out.println("8) Back");
            System.out.print("Choice: ");
            String c = scanner.nextLine().trim();
            try {
                switch (c) {
                    case "1":
                        ParticipantEditor.editParticipant(participants, scanner);
                        fileService.saveParticipants(participants, FileService.INPUT_FILE);
                        // Save all participants to DB
                        for (Participant p : participants) teamMateDB.saveParticipant(p);
                        break;
                    case "2":
                        ParticipantEditor.removeParticipant(participants, teams, unformedParticipantsCache, scanner);
                        fileService.saveParticipants(participants, FileService.INPUT_FILE);
                        fileService.saveTeams(teams, FileService.OUTPUT_FILE);
                        // Save all participants/teams to DB
                        for (Participant p : participants) teamMateDB.saveParticipant(p);
                        for (Team t : teams) teamMateDB.saveTeam(t);
                        break;
                    case "3":
                        if (unformedParticipantsCache.isEmpty()) System.out.println("No unformed participants.");
                        else unformedParticipantsCache.forEach(System.out::println);
                        break;
                    case "4":
                        List<String> logs = AppLogger.getRecentLogs();
                        logs.forEach(System.out::println);
                        break;
                    case "5":
                        participants.stream().sorted(Comparator.comparing(Participant::getId))
                                .forEach(System.out::println);
                        break;
                    case "6":
                        makeTeamFromUnformed(scanner);
                        break;
                    case "7":
                        formTeamsFromAll(scanner);
                        break;
                    case "8":
                        back = true; break;
                    default:
                        System.out.println("Invalid option. Choose a number between 1 and 8.");
                }
            } catch (IOException e) {
                AppLogger.error("File I/O error", e);
            } catch (TeamMateException e) {
                AppLogger.error("TeamMate error: " + e.getMessage(), e);
            } catch (TeamMateDBException e) { // Handle DB errors here
                AppLogger.error("Database error: " + e.getMessage(), e);
            }
        }
    }

    // Form teams using ONLY unformedParticipantsCache (relaxed mode)
    private static void makeTeamFromUnformed(Scanner scanner) throws TeamMateException, IOException, TeamMateDBException {
        if (unformedParticipantsCache.size() < 3) {
            System.out.println("Not enough participants in the waiting list.");
            return;
        }

        System.out.print("Enter desired team size (must be >= 3): ");
        int teamSize;
        try { teamSize = Integer.parseInt(scanner.nextLine().trim()); } catch (NumberFormatException e) {
            System.out.println("Invalid number, try again."); return;
        }

        if (teamSize < 3) {
            System.out.println("Team size must be at least 3."); return;
        }

        lastTeamSize = teamSize;

        TeamBuilder tb = new TeamBuilder();
        TeamBuilder.TeamFormationResult res = tb.buildTeamsFromUnformed(
                new ArrayList<>(unformedParticipantsCache), teamSize
        );

        if (res.formedTeams.isEmpty()) {
            System.out.println("No valid team can be formed with the remaining unformed participants.");
            return;
        }

        for (Team t : res.formedTeams) {
            System.out.println(t);
            teamMateDB.saveTeam(t);
        }

        unformedParticipantsCache = res.unformedParticipants;
        teams.addAll(res.formedTeams);
        fileService.saveTeams(teams, FileService.OUTPUT_FILE);

        AppLogger.info("Formed " + res.formedTeams.size() + " team(s) from unformed cache.");
        System.out.println("Formed " + res.formedTeams.size() + " team(s).");
    }

    // Form teams using ALL participants (strict mode)
    private static void formTeamsFromAll(Scanner scanner) throws TeamMateException, IOException, TeamMateDBException {
        System.out.print("Enter desired team size (must be >= 3): ");
        int teamSize;
        try {
            teamSize = Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Invalid.");
            return;
        }

        lastTeamSize = teamSize;
        TeamBuilder tb = new TeamBuilder();
        // 1. Declare 'res' outside the try-block
        TeamBuilder.TeamFormationResult res;

        // 2. Wrap the call in a try-catch block for validation handling
        try {
            res = tb.buildTeamsAndValidate(new ArrayList<>(participants), teamSize);
        } catch (TeamMateException e) {
            // This block executes if teamSize < 3
            System.out.println(e.getMessage()); // Prints: "Team size must be at least 3"
            AppLogger.warning("Team formation failed: " + e.getMessage());
            return; // Stops execution here
        }

        // Execution only reaches here if validation in TeamBuilder passed.

        if (res.formedTeams.isEmpty()) {
            System.out.println("No valid teams could be formed from all participants.");
            return;
        }

        // Save all formed teams and participants to DB
        for (Team t : res.formedTeams) teamMateDB.saveTeam(t);
        for (Participant p : participants) teamMateDB.saveParticipant(p);

        teams.addAll(res.formedTeams);
        unformedParticipantsCache.clear();
        unformedParticipantsCache.addAll(res.unformedParticipants);

        fileService.saveTeams(teams, FileService.OUTPUT_FILE);
        fileService.saveParticipants(participants, FileService.INPUT_FILE);

        AppLogger.info("Formed " + res.formedTeams.size() + " team(s) from all participants.");
        System.out.println("Formed " + res.formedTeams.size() + " team(s). Unformed participants: " + res.unformedParticipants.size());
    }

    private static Team findTeamByParticipantId(List<Team> teams, String participantId) {
        for (Team t : teams) {
            for (Participant m : t.getMembers()) {
                if (m.getId().equalsIgnoreCase(participantId)) return t;
            }
        }
        return null;
    }
}
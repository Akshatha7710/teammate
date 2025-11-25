package TeamMate;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Console entrypoint for TeamMate with runtime menu options
 */
public class MainApp {

    private static final int DEFAULT_TEAM_SIZE = 4;
    private static final String INPUT_FILE = "participants_sample.csv";
    private static final String OUTPUT_FILE = "formed_teams.csv";

    public static void main(String[] args) {
        System.out.println("--- TeamMate Application Initiated ---");
        FileService fileService = new FileService();
        Scanner scanner = new Scanner(System.in);

        List<Participant> participants;
        try {
            participants = fileService.loadFromCsv(INPUT_FILE);
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return;
        }

        if (participants.isEmpty()) {
            System.err.println("No participants loaded. Exiting.");
            return;
        }

        boolean exit = false;
        List<Team> formedTeams = null;

        while (!exit) {
            System.out.println("\nSelect role:");
            System.out.println("1. Participant");
            System.out.println("2. Organizer");
            System.out.println("3. Exit");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1 -> handleParticipant(participants, formedTeams, scanner);
                case 2 -> formedTeams = handleOrganizer(participants, scanner, fileService);
                case 3 -> exit = true;
                default -> System.out.println("Invalid choice.");
            }
        }

        scanner.close();
        System.out.println("--- TeamMate Application Complete ---");
    }

    private static void handleParticipant(List<Participant> participants, List<Team> teams, Scanner scanner) {
        System.out.print("Enter your Participant ID: ");
        String pid = scanner.nextLine().trim();

        Participant p = participants.stream().filter(x -> x.getId().equalsIgnoreCase(pid)).findFirst().orElse(null);
        if (p == null) {
            System.out.println("Participant ID not found.");
            return;
        }

        if (teams == null) {
            System.out.println("Teams not yet formed. Please wait for the organizer.");
            return;
        }

        Team assigned = teams.stream().filter(t -> t.getMembers().contains(p)).findFirst().orElse(null);
        if (assigned != null) {
            System.out.printf("You are assigned to team %s%n", assigned.getId());
        } else {
            System.out.println("You have not been assigned to any team.");
        }
    }

    private static List<Team> handleOrganizer(List<Participant> participants, Scanner scanner, FileService fileService) {
        List<Team> teams = null;
        boolean back = false;

        while (!back) {
            System.out.println("\nOrganizer Menu:");
            System.out.println("1. Add/Edit Participant");
            System.out.println("2. Remove Participant");
            System.out.println("3. Form Teams");
            System.out.println("4. Show All Participants");
            System.out.println("5. Back");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> ParticipantEditor.editParticipant(participants, scanner);
                case 2 -> ParticipantEditor.removeParticipant(participants, scanner);
                case 3 -> {
                    System.out.print("Enter team size: ");
                    int size = scanner.nextInt();
                    scanner.nextLine();
                    teams = TeamFormation.formTeams(participants, size);
                    try {
                        fileService.saveTeams(teams, OUTPUT_FILE);
                        System.out.printf("Teams saved to %s%n", OUTPUT_FILE);
                    } catch (IOException e) {
                        System.err.println("Error saving teams: " + e.getMessage());
                    }
                }
                case 4 -> participants.forEach(System.out::println);
                case 5 -> back = true;
                default -> System.out.println("Invalid choice.");
            }
        }
        return teams;
    }
}

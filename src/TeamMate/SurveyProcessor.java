package TeamMate;

import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class SurveyProcessor implements Callable<Participant> {

    private static final Set<String> ALLOWED_GAMES = Set.of(
            "CHESS", "FIFA", "BASKETBALL", "VALORANT", "CS:GO", "DOTA 2"
    );
    private static final Set<String> ALLOWED_ROLES = Arrays.stream(Role.values())
            .map(Enum::name)
            .collect(java.util.stream.Collectors.toSet());

    private final Scanner scanner;
    private final List<Participant> participants;
    private final FileService fileService;
    private final List<Participant> unformedCache;

    public SurveyProcessor(Scanner scanner, List<Participant> participants, FileService fileService, List<Participant> unformedCache) {
        this.scanner = scanner;
        this.participants = participants;
        this.fileService = fileService;
        this.unformedCache = unformedCache;
    }

    @Override
    public Participant call() {
        System.out.println("---- Fill Survey ----");

        // 1. ID Input and Validation (Format PXXX, e.g., P001)
        String id;
        while (true) {
            System.out.print("ID (e.g., P101): ");
            id = scanner.nextLine().trim();
            if (id.isEmpty()) {
                System.out.println("❌ Error: Participant ID is required.");
            } else if (!id.toUpperCase().matches("P\\d+")) {
                System.out.println("❌ Error: Invalid ID format. Must start with 'P' followed by digits (e.g., P015).");
            } else {
                String finalId = id;
                if (participants.stream().anyMatch(p -> p.getId().equalsIgnoreCase(finalId))) {
                    System.out.println("❌ Error: A participant with this ID already exists.");
                } else {
                    break;
                }
            }
        }

        // 2. Name Generation
        String name = generateParticipantName(id);
        System.out.println("Name (Auto-generated): " + name);

        // 3. Email Auto-generation and Validation
        String email;
        String defaultEmail = generateDefaultEmail(id);
        while (true) {
            // Prompt user with the generated email as the default
            System.out.print("Email (Default: " + defaultEmail + " - press Enter to accept): ");
            String emailInput = scanner.nextLine().trim();

            String currentEmail = emailInput.isEmpty() ? defaultEmail : emailInput;

            if (currentEmail.isEmpty() || !currentEmail.contains("@") || !currentEmail.contains(".")) {
                System.out.println("❌ Error: Valid email address is required (e.g., user@university.edu).");
            } else {
                email = currentEmail;
                break;
            }
        }

        // 4. Preferred Game Input and Validation
        String game;
        String gameList = String.join(", ", ALLOWED_GAMES);
        while (true) {
            System.out.print("Preferred Game (" + gameList + "): ");
            game = scanner.nextLine().trim();
            if (game.isEmpty()) {
                System.out.println("❌ Error: Preferred game is required.");
            } else if (!ALLOWED_GAMES.contains(game.toUpperCase())) {
                System.out.println("❌ Error: Invalid game. Must be one of: " + gameList);
            } else {
                game = game.toUpperCase(); // Normalize game name
                break;
            }
        }

        // 5. Role Input and Validation
        Role role;
        String roleList = String.join("/", ALLOWED_ROLES);
        while (true) {
            System.out.print("Role (" + roleList + "): ");
            String roleInput = scanner.nextLine().trim().toUpperCase();
            try {
                role = Role.valueOf(roleInput);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("❌ Error: Invalid role. Must be one of: " + roleList);
            }
        }

        // 6. Skill Input (0-10 validation, 0-100 storage)
        int skillInput;
        int skillLevel;
        while (true) {
            System.out.print("Skill (0-10): ");
            try {
                skillInput = Integer.parseInt(scanner.nextLine().trim());
                if (skillInput >= 0 && skillInput <= 10) {
                    skillLevel = skillInput * 10; // Scale 0-10 to 0-100
                    break;
                } else {
                    System.out.println("❌ Error: Skill must be a number between 0 and 10.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Error: Invalid input. Skill must be a number.");
            }
        }

        // 7. Personality Survey and Calculation
        System.out.println("\n---- 5-Question Personality Survey (Rate 1-5) ----");
        int q1 = promptQuestion(scanner, "Q1: I enjoy taking the lead and guiding others during group activities.");
        int q2 = promptQuestion(scanner, "Q2: I prefer analyzing situations and coming up with strategic solutions.");
        int q3 = promptQuestion(scanner, "Q3: I work well with others and enjoy collaborative teamwork.");
        int q4 = promptQuestion(scanner, "Q4: I am calm under pressure and can help maintain team morale.");
        int q5 = promptQuestion(scanner, "Q5: I like making quick decisions and adapting in dynamic situations.");

        int rawScore = q1 + q2 + q3 + q4 + q5;

        // Calculate and scale personality score
        int personalityScore = rawScore * 4; // Range 20-100
        PersonalityType pt = calculatePersonalityType(personalityScore);

        System.out.printf("Personality Score: %d (Raw: %d). Type: %s\n", personalityScore, rawScore, pt.name());

        // Create and Save Participant
        Participant p = new Participant(id, name, email, game, role, skillLevel, personalityScore, pt);
        participants.add(p);
        unformedCache.add(p);

        AppLogger.info("New survey added: " + id);
        try {
            fileService.saveParticipants(participants, FileService.INPUT_FILE);
        } catch (Exception e) {
            AppLogger.warning("Failed to save participants: " + e.getMessage());
        }
        System.out.println("Survey recorded. Thank you!");
        return p;
    }

    // --- Helper Methods ---

    /**
     * Auto-generates the email based on the participant ID number.
     * Example: P101 -> user101@university.edu
     */
    private String generateDefaultEmail(String id) {
        // Extracts digits from ID, e.g., P101 -> 101
        String idNumber = id.replaceAll("[^0-9]", "");
        if (idNumber.isEmpty()) {
            return "user@university.edu"; // Fallback
        }
        return "user" + idNumber + "@university.edu";
    }

    /**
     * Auto-generates the name based on the participant ID.
     */
    private String generateParticipantName(String id) {
        // Example: P015 -> Participant_15
        if (id.toUpperCase().startsWith("P")) {
            return "Participant_" + id.substring(1);
        }
        return "Participant_" + id;
    }

    /**
     * Prompts for a survey question and ensures the input is between 1 and 5.
     */
    private int promptQuestion(Scanner scanner, String question) {
        int rating;
        while (true) {
            System.out.print(question + " (1-5): ");
            try {
                rating = Integer.parseInt(scanner.nextLine().trim());
                if (rating >= 1 && rating <= 5) {
                    return rating;
                }
                System.out.println("❌ Error: Rating must be a number between 1 and 5.");
            } catch (NumberFormatException e) {
                System.out.println("❌ Error: Invalid input. Please enter a number.");
            }
        }
    }

    /**
     * Calculates the PersonalityType based on the scaled score (20-100).
     */
    private PersonalityType calculatePersonalityType(int scaledScore) {
        if (scaledScore >= 90) {
            return PersonalityType.LEADER;
        } else if (scaledScore >= 70) {
            return PersonalityType.BALANCED;
        } else if (scaledScore >= 50) {
            return PersonalityType.THINKER;
        } else {
            return PersonalityType.UNCLASSIFIED;
        }
    }
}
package TeamMate;

/**
 * Data Model for a single Participant, demonstrating Encapsulation.
 * Implements Comparable for sorting by skill level (descending).
 */
public class Participant implements Comparable<Participant> {
    private final String id;
    private final String name;
    private final String email;
    private final String interest;
    private final Role preferredRole;
    private final int skillLevel;
    private final int personalityScore;
    private final PersonalityType personalityType;

    /**
     * The primary constructor for creating a participant.
     */
    public Participant(String id, String name, String email, String interest,
                       Role preferredRole, int skillLevel, int personalityScore, PersonalityType personalityType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.interest = interest;
        this.preferredRole = preferredRole;
        this.skillLevel = skillLevel;
        this.personalityScore = personalityScore;
        // Classification logic is delegated to the utility class
        this.personalityType = PersonalityClassifier.classify(personalityScore);
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getInterest() { return interest; }
    public Role getPreferredRole() { return preferredRole; }
    public int getSkillLevel() { return skillLevel; }
    public PersonalityType getPersonalityType() { return personalityType; }
    public int getPersonalityScore() { return personalityScore; }

    /**
     * Defines the natural ordering for Participants, used for sorting by skill level.
     * Higher skill level comes first (descending order).
     * @param other The participant to compare against.
     */
    @Override
    public int compareTo(Participant other) {
        return Integer.compare(other.skillLevel, this.skillLevel);
    }

    @Override
    public String toString() {
        return String.format("%s (Skill:%d, Role:%s, Type:%s)",
                name, skillLevel, preferredRole, personalityType);
    }
}
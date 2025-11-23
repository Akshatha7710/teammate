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
     * Primary constructor.
     *
     * @param id               participant id
     * @param name             participant name
     * @param email            participant email
     * @param interest         preferred game/interest
     * @param preferredRole    preferred role enum
     * @param skillLevel       integer 0-100
     * @param personalityScore integer 0-100
     * @param personalityType  optional PersonalityType; if null, classifier is used
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

        // Use provided personalityType if valid, otherwise classify from score
        if (personalityType != null && personalityType != PersonalityType.UNCLASSIFIED) {
            this.personalityType = personalityType;
        } else {
            this.personalityType = PersonalityClassifier.classify(personalityScore);
        }
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
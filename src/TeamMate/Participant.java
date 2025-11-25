package TeamMate;

import java.util.Objects;

/**
 * Represents a participant in the TeamMate system.
 */
public class Participant {

    private String id;
    private String name;
    private String email;
    private String interest;       // Preferred game or activity
    private int skillLevel;        // 0-100
    private Role preferredRole;
    private int personalityScore;  // 0-100
    private PersonalityType personalityType;

    // Constructor without PersonalityType (classifies automatically)
    public Participant(String id, String name, String email, String interest,
                       int skillLevel, Role preferredRole, int personalityScore) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.interest = interest;
        this.skillLevel = skillLevel;
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = PersonalityClassifier.classify(personalityScore);
    }

    // Constructor with optional PersonalityType from CSV
    public Participant(String id, String name, String email, String interest,
                       Role preferredRole, int skillLevel, int personalityScore, PersonalityType personalityType) {
        this(id, name, email, interest, skillLevel, preferredRole, personalityScore);
        if (personalityType != null) this.personalityType = personalityType;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getInterest() { return interest; }
    public int getSkillLevel() { return skillLevel; }
    public Role getPreferredRole() { return preferredRole; }
    public int getPersonalityScore() { return personalityScore; }
    public PersonalityType getPersonalityType() { return personalityType; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setInterest(String interest) { this.interest = interest; }
    public void setSkillLevel(int skillLevel) { this.skillLevel = skillLevel; }
    public void setPreferredRole(Role preferredRole) { this.preferredRole = preferredRole; }
    public void setPersonalityScore(int personalityScore) {
        this.personalityScore = personalityScore;
        this.personalityType = PersonalityClassifier.classify(personalityScore);
    }

    @Override
    public String toString() {
        return String.format("Participant[ID=%s, Name=%s, Email=%s, Interest=%s, Skill=%d, Role=%s, Score=%d, Personality=%s]",
                id, name, email, interest, skillLevel, preferredRole, personalityScore, personalityType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participant)) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

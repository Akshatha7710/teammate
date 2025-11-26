package TeamMate;

import java.util.Objects;

public class Participant {

    private final String id;
    private String name;
    private String email;
    private String interest;       // Preferred game or activity
    private int skillLevel;        // Standardized 1-10
    private Role preferredRole;
    private int personalityScore;  // 0-100
    private PersonalityType personalityType;

    // --- Merged Classification Logic (Project Rule: 90/70/50) ---
    private static PersonalityType classify(int score) {
        if (score >= 90) return PersonalityType.LEADER;
        else if (score >= 70) return PersonalityType.BALANCED;
        else if (score >= 50) return PersonalityType.THINKER;
        return PersonalityType.UNCLASSIFIED;
    }

    // 7-arg Constructor (New participant, auto-classifies)
    public Participant(String id, String name, String email, String interest,
                       int skillLevel, Role preferredRole, int personalityScore) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.interest = interest;
        this.skillLevel = Math.max(1, Math.min(10, skillLevel)); // Enforce 1-10 range
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = classify(personalityScore);
    }

    // 8-arg Constructor (CSV load, preserves explicit type if provided)
    public Participant(String id, String name, String email, String interest,
                       Role preferredRole, int skillLevel, int personalityScore, PersonalityType personalityType) {
        this(id, name, email, interest, skillLevel, preferredRole, personalityScore);
        if (personalityType != null) {
            this.personalityType = personalityType;
        }
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
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setInterest(String interest) { this.interest = interest; }

    // Enforce 1-10 range on skill level setting
    public void setSkillLevel(int skillLevel) {
        this.skillLevel = Math.max(1, Math.min(10, skillLevel));
    }

    public void setPreferredRole(Role preferredRole) { this.preferredRole = preferredRole; }

    public void setPersonalityScore(int personalityScore) {
        this.personalityScore = personalityScore;
        this.personalityType = classify(personalityScore);
    }

    @Override
    public String toString() {
        return String.format("Participant[ID=%s, Name=%s, Interest=%s, Skill=%d, Role=%s, Personality=%s]",
                id, name, interest, skillLevel, preferredRole, personalityType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participant that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
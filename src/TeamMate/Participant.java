package TeamMate;

import java.util.Objects;

public class Participant {
    private String id;
    private String name;
    private String email;
    private String interest;
    private Role preferredRole;
    private int skillLevel;
    private int personalityScore;
    private PersonalityType personalityType;

    public Participant(String id, String name, String email, String interest,
                       Role preferredRole, int skillLevel, int personalityScore) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.interest = interest;
        this.preferredRole = preferredRole;
        this.skillLevel = skillLevel;
        this.personalityScore = personalityScore;
        this.personalityType = PersonalityClassifier.classify(personalityScore);
    }

    public Participant(String datum, String datum1, String datum2, String datum3, int i, String datum4, int personalityScore, String datum5) {
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getInterest() { return interest; }
    public Role getPreferredRole() { return preferredRole; }
    public int getSkillLevel() { return skillLevel; }
    public PersonalityType getPersonalityType() { return personalityType; }

    @Override
    public String toString() {
        return String.format("%s (%s, %s, %s)", name, interest, preferredRole, personalityType);
    }
}
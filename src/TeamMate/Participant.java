package TeamMate;

public class Participant {
    private final String id;
    private String name;
    private String email;
    private String interest;
    private Role preferredRole;
    private int skillLevel;
    private int personalityScore;
    private PersonalityType personalityType;

    public Participant(String id, String name, String email, String interest,
                       Role preferredRole, int skillLevel, int personalityScore,
                       PersonalityType personalityType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.interest = interest;
        this.preferredRole = preferredRole;
        this.skillLevel = skillLevel;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
    }

    public Participant(String id, String name, String email, String interest,
                       Role preferredRole, int skillLevel, PersonalityType personalityType) {
        this(id, name, email, interest, preferredRole, skillLevel, 0, personalityType);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getInterest() { return interest; }
    public Role getPreferredRole() { return preferredRole; }
    public int getSkillLevel() { return skillLevel; }
    public int getPersonalityScore() { return personalityScore; }
    public PersonalityType getPersonalityType() { return personalityType; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setInterest(String interest) { this.interest = interest; }
    public void setPreferredRole(Role preferredRole) { this.preferredRole = preferredRole; }
    public void setSkillLevel(int skillLevel) { this.skillLevel = skillLevel; }
    public void setPersonalityScore(int personalityScore) { this.personalityScore = personalityScore; }
    public void setPersonalityType(PersonalityType personalityType) { this.personalityType = personalityType; }

    @Override
    public String toString() {
        return String.format("%s - %s / %s / %s / skill=%d / %s",
                id, name, interest, preferredRole, skillLevel, personalityType);
    }
}

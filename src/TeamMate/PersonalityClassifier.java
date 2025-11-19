package TeamMate;

/**
 * Utility class to classify personality based on a score.
 */
public class PersonalityClassifier {
    /**
     * Classifies a participant's personality type based on their aggregated score.
     * @param score The total personality score.
     * @return The corresponding PersonalityType enum.
     */
    public static PersonalityType classify(int score) {
        if (score >= 90) return PersonalityType.LEADER;
        if (score >= 70) return PersonalityType.BALANCED;
        if (score >= 50) return PersonalityType.THINKER;
        return PersonalityType.UNCLASSIFIED;
    }

    /**
     * Calculates the total personality score from individual question scores.
     * @param q Variable arguments for question scores (Q1, Q2, Q3, Q4, Q5).
     * @return The total score.
     */
    public static int calculateScore(int... q) {
        int total = 0;
        for (int val : q) total += val;
        return total;
    }
}
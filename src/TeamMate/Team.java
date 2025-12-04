package TeamMate;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Team implements Serializable {

    private static final AtomicInteger COUNTER = new AtomicInteger(1); // auto ID generator
    private final String id;                                           // team ID (T1, T2, ...)
    private final List<Participant> members = new ArrayList<>();       // team members

    public Team() {
        this.id = "T" + COUNTER.getAndIncrement();
    }

    // Reset ID counter (used when no teams exist)
    public static void resetCounter() { COUNTER.set(1); }

    // Set counter based on existing team IDs
    public static void initializeCounter(List<Team> existing) {
        int max = existing.stream()
                .map(Team::getId)
                .map(id -> id.replaceAll("\\D+", ""))  // keep only digits
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        COUNTER.set(max + 1);
    }

    // Basic getters
    public String getId() { return id; }
    public List<Participant> getMembers() { return Collections.unmodifiableList(members); }
    public int size() { return members.size(); }

    // Modify team
    public void addMember(Participant p) { members.add(p); }
    public void removeMember(Participant p) { members.remove(p); }

    // Simple stats
    public double averageSkill() {
        return members.stream().mapToInt(Participant::getSkillLevel).average().orElse(0.0);
    }

    public Set<Role> rolesPresent() {
        return members.stream().map(Participant::getPreferredRole).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return String.format("%s size=%d avgSkill=%.1f members=%s",
                id, size(), averageSkill(),
                members.stream().map(Participant::getId).toList());
    }
}

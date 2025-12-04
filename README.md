TeamMate is a Java console application designed to form balanced, skill-optimized teams from a list of participants. It uses a concurrent algorithm to ensure fairness and efficiency.

Key Features & Rules
Algorithm-Based Formation: Teams are formed using a calculated score that balances skill, role, and personality.

Concurrency: The TeamBuilder uses an ExecutorService for faster, parallel team evaluation, and the AppLogger is thread-safe to ensure data integrity.

Strict Validation: The system enforces critical business rules:

Minimum team size is 3.

Teams of exactly size 3 must include at least one LEADER and one THINKER personality type.

Robust Error Handling: Custom exceptions provide the Organizer with specific feedback (e.g., "Team size must be at least 3") when rules are violated.

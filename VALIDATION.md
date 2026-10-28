# Validation status

## Passed in this workspace
- Dependency-free SQLite smoke check (`python3 check_sqlite.py`) executes SQL extracted from Store.java. Covers database reopen, fixed timestamps, duplicate claims, different helpers, old-player timestamps, and failed-claim blocking.
- All shipped YAML files parsed successfully.

## Not yet run
- Java compilation and Gradle/JUnit: this workspace has Java 17 runtime only, no javac or Gradle. Paper 26.x development needs JDK 25.
- Live Paper server tests and economy integration.
- GitHub Actions: configured, but no remote repository exists yet.

## Required live acceptance check
1. Build with JDK 25 and Gradle 9.2.1; require successful JUnit and bundled jar generation.
2. Start Paper 26.x with the economy plugin; configure its console money-grant command.
3. As admin, `/arcanew test join`: private hologram appears. Another observer must not see it.
4. Type `Welcome ArcaneTest`; compare balance before/after for exactly the configured reward.
5. Repeat with lowercase `welcome ArcaneTest`: private duplicate message and unchanged balance.
6. Test normal newcomer with two real players: 30-second delay, 5-minute window, case-insensitive chat, no self-reward, offline target rejection, vanilla username Tab completion.
7. Restart during the delay and during the window. Verify timestamps survive and repeat helpers cannot receive another reward.
8. Reload config; verify prior real claims still block and new settings apply to future arrivals. Invalid config must retain previous settings.
9. Stop test/quit/restart; check hologram cleanup. Test a broken reward command and inspect failure logging/claim retention.
10. Try test/reload commands without admin permission; verify rejection.

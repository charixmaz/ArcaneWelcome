# Validation status

## Passed in this workspace
- Dependency-free SQLite smoke check (`python3 check_sqlite.py`) executes SQL extracted from Store.java. Covers database reopen, fixed timestamps, duplicate claims, different helpers, old-player timestamps, and failed-claim blocking.
- All shipped YAML files parsed successfully.

## Not yet run
- Maven compilation/JUnit: requires JDK 25. Previous Gradle CI passed; Maven migration requires a fresh CI run.
- Live Paper server tests and economy integration.
- GitHub Actions: Maven build and artifact upload configured.

## Required live acceptance check
1. Run `mvn clean verify` with JDK 25 and Maven 3.9+; require successful JUnit and bundled jar generation.
2. Start Paper 26.x with the economy plugin; configure its console money-grant command.
3. As admin, `/arcanew test join`: private hologram appears. Another observer must not see it.
4. Type `Welcome ArcaneTest`; compare balance before/after for exactly the configured reward.
5. Repeat with lowercase `welcome ArcaneTest`: private duplicate message and unchanged balance.
6. Test normal newcomer with two real players: 30-second delay, 5-minute window, case-insensitive chat, no self-reward, offline target rejection, vanilla username Tab completion.
7. Restart during the delay and during the window. Verify timestamps survive and repeat helpers cannot receive another reward.
8. Reload config; verify prior real claims still block and new settings apply to future arrivals. Invalid config must retain previous settings.
9. Stop test/quit/restart; check hologram cleanup. Test a broken reward command and inspect failure logging/claim retention.
10. Try test/reload commands without admin permission; verify rejection.

## Quiz and personalized title acceptance checks
- Build and run JUnit (including three simultaneous correct answers yielding one winner, expired/cancelled rounds, exact accepted answers, generated arithmetic, and config validation).
- `/arcanew test hologram`: your own name appears in the screen welcome.
- `/arcanew test quiz`: question appears; one correct player gets one reward command. Three players answering together must still produce one reward.
- For the blue whale question, “whale” must not win. Test accepted Washington spellings.
- Allow timeout: reveal answer; no money. Start a new round and confirm prior answers are not carried across.
- Reload malformed quizzes.yml: existing settings remain; reload valid file: current round cancels without reward.
- Test six-minute/ten-minute intervals, minimum players, disabled categories, optional cooldown and difficulty rewards.
- Test server restart, broken economy command, and non-admin command rejection.

## Chat battles / unscramble acceptance checks
- JUnit: one immutable ballot per UUID, simultaneous duplicate ballots, late/invalid votes, cancellation, counts/percentages, no-vote results, shared gate, unscramble letter conservation and non-identity shuffle, config rejection, and chat-card formatting.
- With two clients, `/arcanew test battle`: vote with 1/2 and names; reconnect and try to vote twice. Exactly one ballot per UUID must count. Verify win, tie, no-vote, stop, skip and early end displays.
- Start a quiz during a battle (and vice versa): must refuse without interrupting the active event. Automatic events must leave a 15-second gap.
- `/arcanew test unscramble`: inspect readability, solve the word, verify exactly one real reward and a complete winner card.
- Validate private confirmations, event-card wrapping at normal chat width, and tab completion in a live Paper client. No in-game visual verification is claimed by automated tests.
- Reload a broken battles.yml or quizzes.yml during an active event: prior runtime settings and event must remain. Valid reload cancels the event and resets schedules. Restart must not replay votes or rewards.

## Expanded pack checks
- Require 240 trivia entries (80 in each bundled category), 240 unique battle question/option combinations, and 240 unique valid words.
- Every shipped trivia answer is one English word, and question text is unique.
- Test one-time additive migration, owner edits/settings preservation, word deduplication, unchanged legacy multiword demo removal, and no re-adding deleted content after migration.

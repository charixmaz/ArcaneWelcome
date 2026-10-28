# ArcaneWelcome

Paper 26.x chat welcome rewards. Java 25 required. Live server verification is still required.

## Build and install
Open `pom.xml` in IntelliJ as a Maven project. Set Project SDK and Maven Runner JRE to JDK 25. In the Maven panel, run Lifecycle → package.
Alternatively, install JDK 25 and Maven 3.9+, then run `mvn clean verify`.
Copy `target/ArcaneWelcome-0.1.0.jar` into the server's `plugins` folder and restart.
GitHub Actions also builds and uploads the bundled jar when this project is pushed.
No /welcome command exists.

## Owner configuration
Edit `plugins/ArcaneWelcome/config.yml` and `messages.yml`, then `/arcanew reload`.
The default reward command `eco give {player} 50` requires a compatible economy plugin.
Use your economy's console grant command, without a leading slash. ArcaneWelcome does not manage balances itself.

New players wait 30 seconds, then receive a private screen title and a public welcome announcement. The default reward window lasts 300 seconds. Type `Welcome PlayerName` or `welcome PlayerName`; an optional @ is accepted. Matching ignores case and requires the whole username. Normal chat remains visible. Players must be online; self-welcomes are blocked.
Vanilla chat already completes online usernames with Tab. Custom context-sensitive newcomer-only completion and @ completion are not implemented or promised; normal `welcome Name` uses vanilla completion.

## Solo test
As an operator or a player with `arcanew.admin`:
1. Run `/arcanew test join`. A private screen title appears immediately by default.
2. Type `welcome ArcaneTest` in normal chat. Your configured REAL economy command runs.
3. Check your balance with your economy plugin.
4. Repeat `welcome ArcaneTest`: duplicate notification, no second command.
5. `/arcanew test hologram` previews only your private screen title immediately, even when normal welcome titles are disabled. It creates no newcomer or payment claim.
6. `/arcanew test stop` ends the test. A new test join creates a fresh test session.
Test sessions are private, temporary, and separate from genuine newcomer records. Setting `test.pay-rewards: false` performs a dry run. Test names take precedence over real players with that name for the tester only.

## Persistence and crash behavior
`welcome.db` is a SQLite database. UUID-based player records, fixed eligibility timestamps, and unique sender/newcomer claims persist through restart and plugin configuration reload. Offline time counts toward both delay and expiry. Rejoining never renews the window. Existing players seen on first install are not treated as newcomers.
Database work uses a single background worker except initial startup opening. Claims are committed before dispatching a console command. Reserved, dispatched, and failed claims all block duplicates. This favors preventing duplicate payments: a crash between reservation and dispatch can leave an unpaid claim requiring manual review. Generic console commands cannot provide transactional or exactly-once economy payments, nor can their return value prove a deposit occurred. Do not blindly retry uncertain claims. Back up the database with the server stopped, or use SQLite's backup API; copying just the .db file during WAL activity is unsafe.
Use `/arcanew reload` for settings; avoid Bukkit `/reload`. Welcome titles fade out automatically and are cleared on test stop or clean shutdown. Active newcomer windows resume for online players after plugin enable, using saved timestamps.

## Checks
JUnit covers exact chat matching, persistence after reopening, returning-player exclusion, uncertain-claim blocking, and concurrent duplicate protection. Live Paper validation is still required for API compatibility, vanilla Tab behavior, title visibility/timing, permissions, chat-plugin interaction, and actual economy balances.

## Repository
https://github.com/charixmaz/ArcaneWelcome

## Styling
`messages.yml` controls the bold yellow `[Arcane]` prefix and white chat text. `{amount}` uses `reward.display-amount` (default `$50`), styled bold green; keep that label in sync with your console reward command. Command dispatch does not verify actual payment.
`title.text` defaults to `&f&lWelcome` and `title.subtitle` to `&bto the server!`. Timing uses ticks: `title.fade-in-ticks: 10`, `title.stay-ticks: 40`, `title.fade-out-ticks: 10` gives a 3-second screen welcome. Use `/arcanew reload`, then `/arcanew test hologram` to preview. The old command name remains available; no floating entities are spawned.
On startup, unchanged original message/hologram defaults are upgraded to the new style; customized values are preserved. Messages printed by your economy plugin use that plugin’s own configuration.

Existing installations receive missing `title` settings automatically on startup. Old `hologram` settings are unused; the old enabled setting is carried over on first migration. Customize the new `title` section for text and timing.

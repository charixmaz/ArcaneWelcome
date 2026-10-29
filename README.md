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

## Automatic chat quizzes and word games
`quizzes.yml` contains all quiz settings and questions. The default is a quiz 10 minutes after startup and 10 minutes after each round ends, with 30 seconds to answer. Set `quiz.interval-seconds: 360` for six minutes. Only the first correct answer accepted by the server wins; other correct answers receive no reward. A round closes before its reward reservation/payment begins, including when several players answer at nearly the same time.

Players answer in normal chat. Answers ignore capitalization and repeated spaces but otherwise must match one item in `answers`. “Whale” does not match “Blue whale”. Timeout reveals the first listed answer. Trivia and generated math mix randomly; division uses whole-number answers. Immediate repeats are avoided where the available question pool/range permits it.

Minimal owner question:
```yaml
questions:
  biggest-animal:
    question: 'What is the largest animal in the world?'
    answers:
      - 'Blue whale'
```

Reward defaults to `eco give {player} 100`; `quiz.reward-display` is only its chat label. The economy command must exist and support console execution. As with welcomes, dispatch cannot prove payment, and a crash after saving a reservation can leave an unpaid winner for manual review. Failed/uncertain payments are never automatically retried. Active rounds are cancelled on reload/restart; no prior answers are replayed.

Optional settings: minimum online players, category toggles, per-difficulty rewards, and winner cooldown measured in following rounds (disabled by default). Admins can start tests even when automatic quizzes are disabled or the minimum online count is unmet. Manual tests pay the REAL configured reward.

Admin commands (permission `arcanew.admin`):
- `/arcanew test quiz` or `/arcanew quiz start`: start one round now; an existing round is not replaced.
- `/arcanew quiz stop`: stop the active round. Automatic scheduling resumes after the interval.
- `/arcanew quiz skip`: stop the active round and immediately show another question.
- `/arcanew reload`: validate all config files; on success cancel the current quiz and reset its interval. Invalid config keeps previous runtime settings.

Quiz announcements use configurable `quiz-*` entries in `messages.yml`, with the bold yellow Arcane prefix, white text, and bold green money. New message keys are added on startup without overwriting customized text.

The screen welcome now supports `{player}` in both `title.text` and `title.subtitle`. The default is `&f&lWelcome {player}` with `&bto the server!` beneath it. Existing unchanged defaults migrate automatically; customized titles remain intact.

## Feature 2: Unscramble rounds and polished chat cards
Quizzes now mix trivia, generated arithmetic, and unscramble words. Configure `unscramble` in `quizzes.yml`: `enabled`, `chance-percent`, and a simple `words` list. The unscramble percentage applies to non-math rounds: the defaults produce approximately 40% math, 30% words, 30% trivia. If a source is unavailable, the remaining enabled sources are used. To run only words, disable math and remove/disable trivia categories. Only the first correct full word wins, using the quiz reward and cooldown protections already in place.

Words must use 3–24 English letters, with at least two different letters. The displayed shuffle never equals the solution; repeated letters are retained correctly. Immediate repeats are avoided when another word exists.

Preview a word round with `/arcanew test unscramble` or `/arcanew quiz unscramble`. This pays the REAL quiz reward to the first winner.

Event messages in `messages.yml` support either one string or a list of lines. The first line receives the bold yellow Arcane prefix; detail lines are indented. Defaults use cyan headings, white questions, yellow choices/letters, muted instructions, and bold green money. Quiz winner cards include the answer and elapsed seconds. Original shipped quiz messages are upgraded automatically; owner-customized messages are preserved.

## Feature 3: Chat voting battles
`battles.yml` controls automatic two-choice chat battles. Defaults: every 15 minutes, 45 seconds to vote, at least two online players. Players type `1`, `2`, or the full option name in normal chat. Capitalization and extra spaces are ignored. Only exact option names count, so ordinary conversation is unaffected. Each UUID gets one vote; votes cannot be changed, and reconnecting cannot grant another vote. A private confirmation is sent when counted. The public result shows counts and percentages, including proper tie/no-vote results. Battles have no money rewards.

Minimal topic:
```yaml
topics:
  pets:
    question: 'Cats or dogs?'
    options: ['Cats', 'Dogs']
```

`/arcanew test battle` or `/arcanew battle start` starts a battle immediately. `/arcanew battle end` closes voting and shows results early. `/arcanew battle stop` discards votes; `/arcanew battle skip` moves to another topic. Admin commands tab-complete and all require `arcanew.admin`. Manual tests bypass minimum player count and automatic enable switches but never interrupt another live event.

Quizzes and battles share an event gate: one at a time, with a 15-second gap between automatic events. The oldest due event goes first when both are waiting. Welcome messages still work throughout. Admin tests can bypass the gap but cannot overlap an active event. Reload or restart cancels active rounds; votes are temporary and never replayed. A valid reload resets both schedules, while invalid files retain current runtime settings. Configure all `battle-*` and `quiz-*` chat cards in `messages.yml`.

On upgrade, `battles.yml` is created and the default unscramble section is added to existing `quizzes.yml` without replacing trivia questions. If you previously customized quiz message strings, edit them yourself to adopt the new multiline layout.

## Expanded ready-to-play content pack
The bundled files include **240 one-word trivia questions** (80 general knowledge, 80 geography, 80 Minecraft), **240 different two-option battles**, and **240 unique unscramble words**. Generated math rounds remain unlimited. Trivia uses short questions with a single-word answer; no multiword city names or whale-species names are needed in the bundled pack. Owners may still add multiword answers if desired.

Example: `What is the capital of Slovakia?` → `Bratislava`. The Slovak animal clue accepts `Bear`. All word puzzles use 3–24 letters with at least two distinct letters. Battle choices are opinions, with no correct answer and no money reward.

Existing installations receive the pack once at startup. New question/topic IDs are added without replacing owner-customized IDs; words are merged case-insensitively. Original unchanged multiword demo questions are removed. Custom questions, categories, intervals, rewards, and battle topics remain intact. Before a file is merged, its previous contents are saved as `quizzes.yml.pre-pack-1.bak` or `battles.yml.pre-pack-1.bak`. `content-pack-version: 1` records completion, so later owner deletions are not undone on restart. After the upgrade, edit files normally and use `/arcanew reload`.

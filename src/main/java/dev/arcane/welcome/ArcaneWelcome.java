package dev.arcane.welcome;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class ArcaneWelcome extends JavaPlugin implements Listener, CommandExecutor {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Map<UUID, Store.Arrival> arrivals = new HashMap<>();
    private final Map<UUID, Trial> trials = new HashMap<>();
    private final Set<UUID> welcomeTitles = new HashSet<>();
    private final Set<UUID> busy = new HashSet<>();
    private final Set<String> announced = new HashSet<>();
    private volatile boolean stopping;
    private Store store;
    private Settings settings;
    private final QuizManager quizzes = new QuizManager(this);
    private YamlConfiguration messages;
    private record Trial(String id, String name, long starts, long ends) {}
    private record Settings(long delay, long duration, String word, String command,
                            boolean titleEnabled, long fadeIn, long stay, long fadeOut, String titleText, String subtitleText,
                            String testName, long testDelay, boolean testPay, String rewardDisplay) {}

    @Override public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);
        if (!new File(getDataFolder(), "quizzes.yml").exists()) saveResource("quizzes.yml", false);
        try { upgradeDefaultStyle(); loadSettings(); }
        catch (Exception e) { getLogger().severe("Invalid configuration: " + e.getMessage()); getServer().getPluginManager().disablePlugin(this); return; }
        // Initial open happens once before accepting events; all subsequent I/O uses the worker.
        try { store = new Store(getDataFolder().toPath().resolve("welcome.db")); }
        catch (Exception e) { getLogger().severe("Database unavailable: " + e); getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("arcanew")).setExecutor(this);
        for (Player p : Bukkit.getOnlinePlayers()) register(p, false);
        Bukkit.getScheduler().runTaskTimer(this,this::tick,20,20);
    }

    // Replace only shipped legacy defaults; preserve owner-customized text.
    private void upgradeDefaultStyle() throws Exception {
        File file=new File(getDataFolder(),"messages.yml");
        var current=YamlConfiguration.loadConfiguration(file);
        var bundled=new YamlConfiguration();
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(getResource("messages.yml")),java.nio.charset.StandardCharsets.UTF_8)) {bundled.load(reader);}
        Map<String,String> legacy=Map.ofEntries(
            Map.entry("usage", "&f/arcanew reload | /arcanew test join | /arcanew test hologram | /arcanew test stop"),
            Map.entry("hologram-preview","&fShowing your private welcome hologram."),
            Map.entry("announcement","&d{newcomer} is new! &fType welcome {newcomer} to welcome them."),
            Map.entry("reward","&aWelcome reward command sent. Thanks for welcoming {newcomer}!"),
            Map.entry("already","&eYou already welcomed this player."),
            Map.entry("self","&eYou cannot welcome yourself."),
            Map.entry("closed","&eThat player is not currently accepting welcomes."),
            Map.entry("failed","&cReward could not be dispatched. Contact an administrator."),
            Map.entry("busy","&eYour previous welcome is still being processed."),
            Map.entry("reloaded","&aArcaneWelcome configuration reloaded."),
            Map.entry("invalid-config","&cInvalid configuration; previous settings remain active. Check the console."),
            Map.entry("test-started","&dTest started. Type welcome {newcomer}. Real rewards: {paid}."),
            Map.entry("test-stopped","&eYour test session ended."),
            Map.entry("no-permission","&cYou do not have permission."));
        boolean changed=false;
        for(String key:bundled.getKeys(false)) {
            if(!current.contains(key) || Objects.equals(current.getString(key),legacy.get(key))) {
                current.set(key,bundled.getString(key));changed=true;
            }
        }
        if(changed)current.save(file);
        File configFile=new File(getDataFolder(),"config.yml");
        var config=YamlConfiguration.loadConfiguration(configFile);
        boolean configChanged=false;
        if("&dWelcome to the server!".equals(config.getString("hologram.text"))) {
            config.set("hologram.text","&f&lWelcome&r &bto the server!");configChanged=true;
        }
        if(!config.contains("reward.display-amount")) {config.set("reward.display-amount","$50");configChanged=true;}
        var defaults=new YamlConfiguration();
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(getResource("config.yml")),java.nio.charset.StandardCharsets.UTF_8)) {defaults.load(reader);}
        for(String key:defaults.getKeys(true)) {
            if(key.startsWith("title.") && !defaults.isConfigurationSection(key) && !config.contains(key)) {
                config.set(key,key.equals("title.enabled")?config.getBoolean("hologram.enabled",true):defaults.get(key));
                configChanged=true;
            }
        }
        if("&f&lWelcome".equals(config.getString("title.text"))) {config.set("title.text","&f&lWelcome {player}");configChanged=true;}
        if(configChanged)config.save(configFile);
    }
    private long seconds(YamlConfiguration c,String path,long min,long max) {
        long v=c.getLong(path); if(v<min || v>max) throw new IllegalArgumentException(path+" must be "+min+".."+max); return v;
    }
    private void loadSettings() throws Exception {
        var c=new YamlConfiguration(); c.load(new File(getDataFolder(),"config.yml"));
        var m=new YamlConfiguration(); m.load(new File(getDataFolder(),"messages.yml"));
        String word=c.getString("welcome.word","welcome");
        String command=c.getString("reward.command","").strip();
        String test=c.getString("test.name","ArcaneTest");
        if(!word.matches("[A-Za-z]+") || !test.matches("[A-Za-z0-9_]{1,16}") || command.isBlank()
                || command.startsWith("/") || command.contains("\n") || command.contains("\r")) throw new IllegalArgumentException("Check welcome.word, test.name, reward.command");
        Settings candidate=new Settings(seconds(c,"welcome.delay-seconds",0,86400),seconds(c,"welcome.duration-seconds",1,86400),word,command,
                c.getBoolean("title.enabled",true),seconds(c,"title.fade-in-ticks",0,1200),seconds(c,"title.stay-ticks",1,1200),seconds(c,"title.fade-out-ticks",0,1200),c.getString("title.text","&f&lWelcome {player}"),c.getString("title.subtitle","&bto the server!"),
                test,seconds(c,"test.delay-seconds",0,86400),c.getBoolean("test.pay-rewards",true),c.getString("reward.display-amount","$50"));
        // Parse all files before committing any runtime changes; bad reload keeps prior settings.
        QuizSettings quizCandidate=QuizSettings.load(new File(getDataFolder(),"quizzes.yml"));
        var bundled=new YamlConfiguration();
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(getResource("messages.yml")),java.nio.charset.StandardCharsets.UTF_8)) {bundled.load(reader);}
        m.setDefaults(bundled);
        settings=candidate; messages=m;
        quizzes.configure(quizCandidate);
    }
    private static long now() { return System.currentTimeMillis(); }
    @SuppressWarnings("deprecation") private String color(String value) { return ChatColor.translateAlternateColorCodes('&',value); }
    private String msg(String key,String name) { return color((messages.getString("prefix","&e&l[Arcane]&r &f")+messages.getString(key,key)).replace("{amount}",settings.rewardDisplay).replace("{newcomer}",name).replace("{paid}",String.valueOf(settings.testPay))); }
    private void send(Player p,String key,String name) { p.sendMessage(msg(key,name)); }
    private <T> void io(Callable<T> job,Consumer<T> done) {
        if(stopping) return;
        worker.submit(() -> {
            try { T result=job.call(); main(() -> done.accept(result)); }
            catch(Exception e) { getLogger().severe("Database operation failed: "+e); }
        });
    }
    private void main(Runnable action) { if(!stopping) Bukkit.getScheduler().runTask(this,() -> {if(!stopping) action.run();}); }
    @EventHandler public void join(PlayerJoinEvent e) { register(e.getPlayer(), !e.getPlayer().hasPlayedBefore()); }
    private void register(Player p,boolean first) {
        UUID id=p.getUniqueId(); String name=p.getName(); long starts=now()+settings.delay*1000, ends=starts+settings.duration*1000;
        io(() -> store.join(id.toString(),name,first,starts,ends),a -> {
            if(p.isOnline()) arrivals.put(id,a);
        });
    }
    @EventHandler public void quit(PlayerQuitEvent e) {
        UUID id=e.getPlayer().getUniqueId(); arrivals.remove(id); trials.remove(id); removeWelcomeTitle(id);
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void chat(AsyncChatEvent e) {
        String text=PlainTextComponentSerializer.plainText().serialize(e.message());
        UUID sender=e.getPlayer().getUniqueId();
        // Preserve ordinary chat, including formatting and normal broadcast.
        QuizRound round=quizzes.snapshot();
        long receivedAt=now();
        main(() -> {
            Player player=Bukkit.getPlayer(sender);
            if(player!=null)quizzes.answer(player,text,round,receivedAt);
            WelcomeText.target(text,settings.word).ifPresent(name -> welcome(sender,name));
        });
    }
    private void tick() {
        long time=now();
        quizzes.tick(time);
        for(var entry:arrivals.entrySet()) {
            var a=entry.getValue(); Player p=Bukkit.getPlayer(entry.getKey());
            if(p!=null && time>=a.starts() && time<a.ends() && announced.add(a.id())) {
                Bukkit.broadcastMessage(msg("announcement",a.name())); showWelcomeTitle(p);
            }
        }
        for(var entry:trials.entrySet()) {
            Trial t=entry.getValue(); Player p=Bukkit.getPlayer(entry.getKey());
            if(p!=null && time>=t.starts && time<t.ends && announced.add(t.id)) {
                send(p,"announcement",t.name); showWelcomeTitle(p);
            }
        }
    }
    private void welcome(UUID id,String name) {
        Player p=Bukkit.getPlayer(id); if(p==null) return;
        Trial t=trials.get(id);
        if(t!=null && t.name.equalsIgnoreCase(name)) {
            if(now()<t.starts || now()>=t.ends) {send(p,"closed",name);return;}
            claim(p,t.id,t.name,!settings.testPay); return;
        }
        var target=arrivals.values().stream().filter(a -> a.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if(target==null || now()<target.starts() || now()>=target.ends()) {send(p,"closed",name);return;}
        if(target.id().equals(id.toString())) {send(p,"self",name);return;}
        claim(p,target.id(),target.name(),false);
    }
    private void claim(Player p,String target,String name,boolean dry) {
        UUID id=p.getUniqueId(); if(!busy.add(id)) {send(p,"busy",name);return;}
        String command=settings.command.replace("{player}",p.getName());
        // Persist reservation BEFORE dispatch: prevents double payments after crash/reload.
        worker.submit(() -> {
            try {
                boolean reserved=store.reserve(id.toString(),target);
                main(() -> {
                    busy.remove(id);
                    if(!reserved) {if(p.isOnline())send(p,"already",name);return;}
                    boolean ok=false;
                    try { ok=dry || Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command); }
                    catch(Exception e) {getLogger().severe("Reward command failed: "+e);}
                    final String status=ok?"dispatched":"failed";
                    io(() -> {store.status(id.toString(),target,status);return true;},ignored -> {});
                    if(p.isOnline())send(p,ok?"reward":"failed",name);
                    if(!ok)getLogger().warning("Claim retained for manual review: "+id+" -> "+target);
                });
            } catch(Exception e) {
                getLogger().severe("Claim failed: "+e);
                main(() -> {busy.remove(id);if(p.isOnline())send(p,"failed",name);});
            }
        });
    }
    void quizSend(CommandSender sender,String key,Map<String,String> values) {
        String text=messages.getString("prefix","&e&l[Arcane]&r &f")+messages.getString(key,key);
        for(var entry:values.entrySet())text=text.replace("{"+entry.getKey()+"}",entry.getValue());
        sender.sendMessage(color(text));
    }
    void quizBroadcast(String key,Map<String,String> values) {
        for(Player p:Bukkit.getOnlinePlayers())quizSend(p,key,values);
    }
    void rewardQuiz(Player player,QuizRound round) {
        String sender=player.getUniqueId().toString();
        String command=round.command.replace("{player}",player.getName());
        // One persistent reservation per round. No retry after uncertain command outcomes.
        worker.submit(() -> {
            try {
                if(!store.reserve("quiz-winner",round.id))return;
                main(() -> {
                    boolean ok=false;
                    try {ok=Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command);}
                    catch(Exception e) {getLogger().severe("Quiz reward command failed: "+e);}
                    final String status=(ok?"dispatched:":"failed:")+sender;
                    io(() -> {store.status("quiz-winner",round.id,status);return true;},ignored -> {});
                    if(!ok) {
                        getLogger().warning("Quiz reward retained for review: "+round.id+" winner="+sender);
                        if(player.isOnline())quizSend(player,"quiz-reward-failed",Map.of());
                    }
                });
            } catch(Exception e) {
                getLogger().severe("Quiz reward reservation failed: "+e);
                main(() -> {if(player.isOnline())quizSend(player,"quiz-reward-failed",Map.of());});
            }
        });
    }
    private void removeWelcomeTitle(UUID id) {
        if(welcomeTitles.remove(id)) {
            Player p=Bukkit.getPlayer(id);if(p!=null)p.clearTitle();
        }
    }
    private void showWelcomeTitle(Player p) { showWelcomeTitle(p,false); }
    private void showWelcomeTitle(Player p,boolean preview) {
        if(!preview && !settings.titleEnabled)return;
        var serializer=LegacyComponentSerializer.legacyAmpersand();
        var times=net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(settings.fadeIn*50),
                java.time.Duration.ofMillis(settings.stay*50),
                java.time.Duration.ofMillis(settings.fadeOut*50));
        p.showTitle(net.kyori.adventure.title.Title.title(
                serializer.deserialize(settings.titleText.replace("{player}",p.getName())),
                serializer.deserialize(settings.subtitleText.replace("{player}",p.getName())),times));
        welcomeTitles.add(p.getUniqueId());
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.hasPermission("arcanew.admin")) {sender.sendMessage(msg("no-permission",""));return true;}
        if(args.length==2 && args[0].equalsIgnoreCase("quiz") && quizzes.command(sender,args[1]))return true;
        if(args.length==2 && args[0].equalsIgnoreCase("test") && args[1].equalsIgnoreCase("quiz")) {quizzes.command(sender,"test");return true;}
        if(args.length==1 && args[0].equalsIgnoreCase("reload")) {
            try {loadSettings();sender.sendMessage(msg("reloaded",""));}
            catch(Exception e) {getLogger().warning(e.getMessage());sender.sendMessage(msg("invalid-config",""));}
            return true;
        }
        if(sender instanceof Player p && args.length==2 && args[0].equalsIgnoreCase("test")) {
            if(args[1].equalsIgnoreCase("hologram")) {showWelcomeTitle(p,true);send(p,"hologram-preview","");return true;}
            if(args[1].equalsIgnoreCase("stop")) {trials.remove(p.getUniqueId());removeWelcomeTitle(p.getUniqueId());send(p,"test-stopped","");return true;}
            if(args[1].equalsIgnoreCase("join")) {
                long starts=now()+settings.testDelay*1000;
                trials.put(p.getUniqueId(),new Trial("test:"+UUID.randomUUID(),settings.testName,starts,starts+settings.duration*1000));
                send(p,"test-started",settings.testName);tick();return true;
            }
        }
        sender.sendMessage(msg("usage",""));return true;
    }
    @Override public void onDisable() {
        stopping=true;
        quizzes.stop(false);
        for(UUID id:new HashSet<>(welcomeTitles))removeWelcomeTitle(id);
        // Close is queued behind all already-submitted database writes.
        worker.submit(() -> {if(store!=null)try{store.close();}catch(Exception e){getLogger().severe(e.toString());}});
        worker.shutdown();
        try { if(!worker.awaitTermination(10,TimeUnit.SECONDS))getLogger().warning("Database worker still draining; do not remove database files."); }
        catch(InterruptedException e) {Thread.currentThread().interrupt();}
    }
}

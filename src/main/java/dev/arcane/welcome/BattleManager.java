package dev.arcane.welcome;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

final class BattleManager {
    private final ArcaneWelcome plugin;
    private final ChatEventGate gate;
    private BattleSettings settings;
    private volatile BattleRound active;
    private long nextStart;
    private String previous="";
    BattleManager(ArcaneWelcome plugin,ChatEventGate gate) {this.plugin=plugin;this.gate=gate;}
    BattleRound snapshot() {return active;}
    long nextDue() {return settings!=null && settings.enabled()?nextStart:Long.MAX_VALUE;}
    void configure(BattleSettings candidate) {stop(true);settings=candidate;nextStart=System.currentTimeMillis()+settings.interval();}
    void tick(long now) {
        BattleRound round=active;
        if(round!=null) {
            var result=round.finish(now,false);
            if(result!=null)results(round,result,now);
        }
        if(active==null && settings.enabled() && now>=nextStart && Bukkit.getOnlinePlayers().size()>=settings.minimum())start(false);
    }
    private boolean start(boolean manual) {
        if(active!=null || settings.topics().isEmpty() || !gate.acquire("battle",System.currentTimeMillis(),manual))return false;
        var choices=settings.topics().stream().filter(t->!t.id().equals(previous)).toList();
        if(choices.isEmpty())choices=settings.topics();
        var topic=choices.get(ThreadLocalRandom.current().nextInt(choices.size()));previous=topic.id();
        active=new BattleRound(topic.question(),topic.first(),topic.second(),System.currentTimeMillis()+settings.duration());
        plugin.quizBroadcast("battle-question",Map.of("question",topic.question(),"option1",topic.first(),"option2",topic.second(),"seconds",String.valueOf(settings.duration()/1000)));
        return true;
    }
    void vote(Player player,String answer,BattleRound submitted,long receivedAt) {
        BattleRound round=active;
        if(round==null || round!=submitted)return;
        switch(round.vote(player.getUniqueId(),answer,Math.max(receivedAt,System.currentTimeMillis()))) {
            case ACCEPTED -> plugin.quizSend(player,"battle-voted",Map.of());
            case DUPLICATE -> {if(round.notifyDuplicate(player.getUniqueId()))plugin.quizSend(player,"battle-already",Map.of());}
            default -> {}
        }
    }
    private void results(BattleRound round,BattleRound.Result result,long now) {
        active=null;gate.release("battle",now);nextStart=now+settings.interval();
        String key=result.total()==0?"battle-empty":result.first()==result.second()?"battle-tie":"battle-result";
        plugin.quizBroadcast(key,Map.of("question",round.question,"option1",round.first,"option2",round.second,
                "votes1",String.valueOf(result.first()),"votes2",String.valueOf(result.second()),
                "percent1",String.valueOf(result.percent(result.first())),"percent2",String.valueOf(result.secondPercent()),
                "total",String.valueOf(result.total()),"winner",result.first()>result.second()?round.first:round.second));
    }
    void stop(boolean announce) {
        if(active!=null) {active.cancel();active=null;gate.release("battle",System.currentTimeMillis());if(announce)plugin.quizBroadcast("battle-stopped",Map.of());}
        if(settings!=null)nextStart=System.currentTimeMillis()+settings.interval();
    }
    boolean command(CommandSender sender,String action) {
        switch(action.toLowerCase(Locale.ROOT)) {
            case "start", "test" -> {if(!start(true))plugin.quizSend(sender,"event-busy",Map.of());}
            case "stop" -> stop(true);
            case "skip" -> {stop(true);if(!start(true))plugin.quizSend(sender,"event-busy",Map.of());}
            case "end" -> {if(active!=null) {var result=active.finish(System.currentTimeMillis(),true);if(result!=null)results(active,result,System.currentTimeMillis());}}
            default -> {return false;}
        }
        return true;
    }
}

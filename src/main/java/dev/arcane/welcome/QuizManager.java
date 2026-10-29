package dev.arcane.welcome;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** All controller operations run on the server thread. Chat only reads snapshot(). */
final class QuizManager {
    private final ArcaneWelcome plugin;
    private QuizSettings settings;
    private volatile QuizRound active;
    private long nextStart;
    private String previous="";
    private final Map<UUID,Integer> cooldowns=new HashMap<>();
    QuizManager(ArcaneWelcome plugin) {this.plugin=plugin;}
    QuizRound snapshot() {return active;}
    void configure(QuizSettings candidate) {
        if(active!=null)stop(true);
        settings=candidate;nextStart=System.currentTimeMillis()+settings.interval();
    }
    void tick(long now) {
        QuizRound round=active;
        if(round!=null && round.expire(now)) {
            active=null;nextStart=now+settings.interval();
            plugin.quizBroadcast("quiz-timeout",Map.of("answer",round.answer));
        }
        if(active==null && settings.enabled() && now>=nextStart) {
            nextStart=now+settings.interval();
            if(Bukkit.getOnlinePlayers().size()>=settings.minimumPlayers())start();
        }
    }
    private boolean start() {
        if(active!=null)return false;
        var random=ThreadLocalRandom.current();
        QuizSettings.Question selected=null;
        boolean math=random.nextInt(100)<settings.mathChance();
        if(!math && !settings.questions().isEmpty()) {
            var options=settings.questions().stream().filter(q -> !q.id().equals(previous)).toList();
            if(options.isEmpty())options=settings.questions();
            selected=options.get(random.nextInt(options.size()));
        }
        String identity;
        if(selected==null) {
            String question="",answer="";
            // Avoid immediate repeated equations too, where the configured range permits it.
            for(int attempt=0;attempt<20;attempt++) {
                var generated=QuizMath.generate(settings.min(),settings.max(),settings.operations(),random);
                question=generated.question();answer=generated.answer();
                if(!question.equals(previous))break;
            }
            identity=question;
            selected=new QuizSettings.Question(identity,question,List.of(answer),settings.command(),settings.reward());
        } else identity=selected.id();
        previous=identity;
        cooldowns.replaceAll((id,left)->left-1);cooldowns.values().removeIf(left->left<=0);
        active=new QuizRound(selected.text(),selected.answers(),System.currentTimeMillis()+settings.timeout(),selected.command(),selected.reward());
        plugin.quizBroadcast("quiz-question",Map.of("question",active.question,"seconds",String.valueOf(settings.timeout()/1000),"amount",active.reward));
        return true;
    }
    void answer(Player player,String text,QuizRound submittedRound,long receivedAt) {
        QuizRound round=active;
        if(round==null || round!=submittedRound || cooldowns.containsKey(player.getUniqueId()))return;
        // Deadline is checked at processing time as well; an expired round can never pay.
        if(!round.tryWin(text,Math.max(receivedAt,System.currentTimeMillis())))return;
        active=null;nextStart=System.currentTimeMillis()+settings.interval();
        if(settings.cooldown()>0)cooldowns.put(player.getUniqueId(),settings.cooldown()+1);
        plugin.quizBroadcast("quiz-winner",Map.of("player",player.getName(),"amount",round.reward));
        plugin.rewardQuiz(player,round);
    }
    void stop(boolean announce) {
        QuizRound round=active;active=null;
        if(round!=null) {round.close();if(announce)plugin.quizBroadcast("quiz-stopped",Map.of());}
        if(settings!=null)nextStart=System.currentTimeMillis()+settings.interval();
    }
    boolean command(CommandSender sender,String action) {
        switch(action.toLowerCase(Locale.ROOT)) {
            case "start", "test" -> {if(!start())plugin.quizSend(sender,"quiz-running",Map.of());}
            case "stop" -> stop(true);
            case "skip" -> {stop(true);start();}
            default -> {return false;}
        }
        return true;
    }
}

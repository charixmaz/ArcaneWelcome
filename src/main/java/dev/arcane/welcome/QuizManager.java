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
    private final ChatEventGate gate;
    private volatile QuizRound active;
    private long nextStart;
    private String previous="";
    private final Map<UUID,Integer> cooldowns=new HashMap<>();
    QuizManager(ArcaneWelcome plugin,ChatEventGate gate) {this.plugin=plugin;this.gate=gate;}
    long nextDue() {return settings!=null && settings.enabled()?nextStart:Long.MAX_VALUE;}
    QuizRound snapshot() {return active;}
    void configure(QuizSettings candidate) {
        if(active!=null)stop(true);
        settings=candidate;nextStart=System.currentTimeMillis()+settings.interval();
    }
    void tick(long now) {
        QuizRound round=active;
        if(round!=null && round.expire(now)) {
            active=null;gate.release("quiz",now);nextStart=now+settings.interval();
            plugin.quizBroadcast("quiz-timeout",Map.of("answer",round.answer));
        }
    }
    boolean eligible() {return settings.enabled() && Bukkit.getOnlinePlayers().size()>=settings.minimumPlayers();}
    boolean automatic() {return start(false,false);}

    private boolean start(boolean manual,boolean forceUnscramble) {
        if(active!=null)return false;
        var random=ThreadLocalRandom.current();
        String type=forceUnscramble?(settings.words().isEmpty()?"none":"unscramble"):settings.pickType(random);
        if(type.equals("none") || !gate.acquire("quiz",System.currentTimeMillis(),manual))return false;
        QuizSettings.Question selected=null;
        if(type.equals("trivia")) {
            String id=plugin.rotate("trivia",settings.questions().stream().map(QuizSettings.Question::id).toList());
            selected=settings.questions().stream().filter(q->q.id().equals(id)).findFirst().orElseThrow();
        }
        String identity;
        if(type.equals("unscramble")) {
            String word=plugin.rotate("words",settings.words());
            identity="word:"+word;
            selected=new QuizSettings.Question(identity,Unscramble.display(Unscramble.shuffle(word,random)),List.of(word),settings.command(),settings.reward());
        } else if(selected==null) {
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
        active.type=switch(type) {case "math"->"Math";case "unscramble"->"Unscramble";default->"Trivia";};
        plugin.quizBroadcast(type.equals("unscramble")?"quiz-unscramble":"quiz-question",Map.of("question",active.question,"type",active.type,"seconds",String.valueOf(settings.timeout()/1000),"amount",active.reward));
        return true;
    }
    void answer(Player player,String text,QuizRound submittedRound,long receivedAt) {
        QuizRound round=active;
        if(round==null || round!=submittedRound || cooldowns.containsKey(player.getUniqueId()))return;
        // Deadline is checked at processing time as well; an expired round can never pay.
        if(!round.tryWin(text,Math.max(receivedAt,System.currentTimeMillis())))return;
        active=null;gate.release("quiz",System.currentTimeMillis());nextStart=System.currentTimeMillis()+settings.interval();
        if(settings.cooldown()>0)cooldowns.put(player.getUniqueId(),settings.cooldown()+1);
        plugin.quizBroadcast("quiz-winner",Map.of("player",player.getName(),"amount",round.reward,"answer",round.answer,"elapsed",String.format(Locale.ROOT,"%.1f",(System.currentTimeMillis()-round.startedAt)/1000.0)));
        plugin.rewardQuiz(player,round);
    }
    void stop(boolean announce) {
        QuizRound round=active;active=null;
        if(round!=null) {round.close();gate.release("quiz",System.currentTimeMillis());if(announce)plugin.quizBroadcast("quiz-stopped",Map.of());}
        if(settings!=null)nextStart=System.currentTimeMillis()+settings.interval();
    }
    boolean command(CommandSender sender,String action) {
        switch(action.toLowerCase(Locale.ROOT)) {
            case "start", "test" -> {if(!start(true,false))plugin.quizSend(sender,"event-busy",Map.of());}
            case "unscramble" -> {if(!start(true,true))plugin.quizSend(sender,"event-busy",Map.of());}
            case "stop" -> stop(true);
            case "skip" -> {stop(true);if(!start(true,false))plugin.quizSend(sender,"event-busy",Map.of());}
            default -> {return false;}
        }
        return true;
    }
}

package dev.arcane.welcome;

import java.util.*;

/** UUID ballots, one immutable vote per player, no money or reward command. */
final class BattleRound {
    enum Vote { ACCEPTED, DUPLICATE, INVALID, CLOSED }
    record Result(int first,int second) {
        int total() {return first+second;}
        int percent(int count) {return total()==0?0:(int)Math.round(count*100.0/total());}
        int secondPercent() {return total()==0?0:100-percent(first);}
    }
    final String question,first,second;
    final long deadline;
    private final Map<UUID,Integer> votes=new HashMap<>();
    private final Set<UUID> duplicateNotified=new HashSet<>();
    private boolean closed;
    BattleRound(String question,String first,String second,long deadline) {
        this.question=question;this.first=first;this.second=second;this.deadline=deadline;
    }
    synchronized Vote vote(UUID player,String answer,long now) {
        if(closed || now>=deadline)return Vote.CLOSED;
        String text=QuizRound.normalize(answer);
        int choice=text.equals("1") || text.equals(QuizRound.normalize(first))?1:
                text.equals("2") || text.equals(QuizRound.normalize(second))?2:0;
        if(choice==0)return Vote.INVALID;
        if(votes.containsKey(player))return Vote.DUPLICATE;
        votes.put(player,choice);return Vote.ACCEPTED;
    }
    synchronized boolean notifyDuplicate(UUID id) {return duplicateNotified.add(id);}
    synchronized Result finish(long now,boolean force) {
        if(closed || (!force && now<deadline))return null;
        closed=true;int firstCount=(int)votes.values().stream().filter(v->v==1).count();
        return new Result(firstCount,votes.size()-firstCount);
    }
    synchronized void cancel() {closed=true;}
}

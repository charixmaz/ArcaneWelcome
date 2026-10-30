package dev.arcane.welcome;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BattleTest {
    private BattleRound round() {return new BattleRound("Cats or dogs?","Cats","Dogs",1000);}
    @Test void oneVotePerUuidAcrossNamesAndNumbers() {
        var b=round();UUID id=UUID.randomUUID();
        assertEquals(BattleRound.Vote.ACCEPTED,b.vote(id,"1",100));
        assertEquals(BattleRound.Vote.DUPLICATE,b.vote(id,"  DOGS  ",200));
        assertEquals(BattleRound.Vote.ACCEPTED,b.vote(UUID.randomUUID(),"dogs",200));
        var result=b.finish(1000,false);
        assertEquals(1,result.first());assertEquals(1,result.second());assertEquals(50,result.percent(1));
        assertNull(b.finish(1001,false));
    }
    @Test void ignoresOrdinaryChatAndLateVotes() {
        var b=round();assertEquals(BattleRound.Vote.INVALID,b.vote(UUID.randomUUID(),"I like cats",100));
        assertEquals(BattleRound.Vote.CLOSED,b.vote(UUID.randomUUID(),"1",1000));
        assertEquals(0,b.finish(1000,false).total());
    }
    @Test void stoppedRoundCannotAcceptVotesOrPublishResults() {
        var b=round();b.cancel();assertNull(b.finish(1001,false));
        assertEquals(BattleRound.Vote.CLOSED,b.vote(UUID.randomUUID(),"2",100));
    }
    @Test void concurrentDuplicateBallotsCountOnce() throws Exception {
        var b=round();var pool=Executors.newFixedThreadPool(3);UUID id=UUID.randomUUID();
        try {
            var futures=pool.invokeAll(List.of(()->b.vote(id,"1",100),()->b.vote(id,"2",100),()->b.vote(id,"Cats",100)));
            assertEquals(1,futures.stream().filter(f->{try{return f.get()==BattleRound.Vote.ACCEPTED;}catch(Exception e){throw new RuntimeException(e);}}).count());
            assertEquals(1,b.finish(1000,false).total());
        } finally {pool.shutdownNow();}
    }
    @Test void displayedPercentagesAlwaysSumToOneHundred() {
        var result=new BattleRound.Result(1,7);
        assertEquals(100,result.percent(result.first())+result.secondPercent());
        assertEquals(0,new BattleRound.Result(0,0).secondPercent());
    }
    @Test void eventGateUsesOneTwentyFiveMinuteCadence() {
        var gate=new ChatEventGate();gate.configure(1500000,100);
        assertFalse(gate.acquire("quiz",1499999,false));
        assertTrue(gate.acquire("quiz",1500100,false));
        assertFalse(gate.acquire("battle",1500101,true));
        gate.release("quiz",1530100);
        assertFalse(gate.acquire("battle",1530101,false));
        assertFalse(gate.acquire("battle",3000099,false));
        assertTrue(gate.acquire("battle",3000100,false));
    }
    @Test void reloadDoesNotAllowAnotherEarlyEvent() {
        var gate=new ChatEventGate();gate.configure(1500000,0);
        assertTrue(gate.acquire("quiz",1500000,false));gate.release("quiz",1530000);
        gate.configure(1500000,1530010);assertFalse(gate.due(1530011));
        assertTrue(gate.due(3000000));
    }
}

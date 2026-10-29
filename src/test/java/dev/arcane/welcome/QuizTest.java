package dev.arcane.welcome;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class QuizTest {
    private QuizRound round() {return new QuizRound("Largest animal?",List.of("Blue whale"),1000,"eco give {player} 100","$100");}
    @Test void onlyExplicitAnswersAndOneWinner() {
        var q=round();assertFalse(q.tryWin("whale",100));
        assertTrue(q.tryWin("  BLUE   whale  ",100));
        assertFalse(q.tryWin("Blue whale",101));assertFalse(q.tryWin("Blue whale",102));
    }
    @Test void simultaneousCorrectAnswersHaveExactlyOneWinner() throws Exception {
        var q=round();var pool=Executors.newFixedThreadPool(3);var gate=new CountDownLatch(1);
        try {
            var tasks=new ArrayList<Future<Boolean>>();
            for(int i=0;i<3;i++)tasks.add(pool.submit(() -> {gate.await();return q.tryWin("Blue whale",100);}));
            gate.countDown();int winners=0;
            for(var task:tasks)if(task.get())winners++;
            assertEquals(1,winners);
        } finally {pool.shutdownNow();}
    }
    @Test void expiredAndCancelledRoundsNeverWin() {
        var q=round();assertFalse(q.tryWin("Blue whale",1000));assertTrue(q.expire(1000));assertFalse(q.expire(1001));
        var cancelled=round();cancelled.close();assertFalse(cancelled.tryWin("Blue whale",100));
    }
    @Test void timeoutDoesNotRevealAfterWinner() {
        var q=round();assertTrue(q.tryWin("Blue whale",999));assertFalse(q.expire(1001));
    }
}

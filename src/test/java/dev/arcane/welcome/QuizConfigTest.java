package dev.arcane.welcome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class QuizConfigTest {
    @TempDir Path dir;
    private QuizSettings load(String content) throws Exception {
        Path file=dir.resolve("quizzes.yml");Files.writeString(file,content);return QuizSettings.load(file.toFile());
    }
    @Test void defaultConfigAndExplicitWhaleAnswer() throws Exception {
        var c=QuizSettings.load(Path.of("src/main/resources/quizzes.yml").toFile());
        assertEquals(600000,c.interval());assertEquals(30000,c.timeout());assertEquals(0,c.cooldown());
        var whale=c.questions().stream().filter(q->q.id().equals("biggest-animal")).findFirst().orElseThrow();
        assertEquals(List.of("Blue whale"),whale.answers());assertEquals("$100",whale.reward());
    }
    @Test void rejectsInvalidTimersAnswersAndUnsafeCommands() {
        assertThrows(IllegalArgumentException.class,()->load("quiz:\n  interval-seconds: 0\n"));
        assertThrows(IllegalArgumentException.class,()->load("questions:\n  bad:\n    question: 'Bad?'\n    answers: []\n"));
        assertThrows(IllegalArgumentException.class,()->load("quiz:\n  reward-command: '/eco give {player} 100'\n"));
        assertThrows(IllegalArgumentException.class,()->load("math:\n  operations: [power]\n"));
    }
    @Test void categoryFilteringAndDifficultyReward() throws Exception {
        var c=load("""
            categories:
              geography: false
            difficulty-rewards:
              hard:
                command: 'eco give {player} 200'
                display: '$200'
            questions:
              hidden:
                category: geography
                question: 'Hidden?'
                answers: ['Yes']
              hard:
                question: 'Hard?'
                difficulty: hard
                answers: ['Yes']
            """);
        assertEquals(1,c.questions().size());assertEquals("$200",c.questions().getFirst().reward());
    }
    @Test void unscrambleOnlyPoolAndInvalidWords() throws Exception {
        var c=load("math:\n  enabled: false\nunscramble:\n  enabled: true\n  words: [diamond, creeper]\n");
        assertEquals("unscramble",c.pickType(new Random(42)));
        assertThrows(IllegalArgumentException.class,()->load("unscramble:\n  words: [aaaa]\n"));
        assertThrows(IllegalArgumentException.class,()->load("math:\n  enabled: false\nunscramble:\n  enabled: false\n"));
    }
    @Test void generatedEquationsHaveCorrectWholeNumberAnswers() {
        var random=new Random(42);
        for(String operation:List.of("addition","subtraction","multiplication","division")) {
            for(int i=0;i<100;i++) {
                var e=QuizMath.generate(0,50,List.of(operation),random);
                var parts=e.question().split(" ");int a=Integer.parseInt(parts[0]),b=Integer.parseInt(parts[2]);
                int expected=switch(operation) {case "addition"->a+b;case "subtraction"->a-b;case "multiplication"->a*b;default->{assertNotEquals(0,b);assertEquals(0,a%b);yield a/b;}};
                assertEquals(String.valueOf(expected),e.answer());
            }
        }
    }
}

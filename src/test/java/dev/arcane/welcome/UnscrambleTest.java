package dev.arcane.welcome;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UnscrambleTest {
    @Test void shufflePreservesLettersAndNeverRevealsWord() {
        var random=new Random(42);
        for(String word:List.of("diamond","banana","aab","creeper"))for(int i=0;i<100;i++) {
            String shuffled=Unscramble.shuffle(word,random);
            assertNotEquals(word.toUpperCase(Locale.ROOT),shuffled);
            assertArrayEquals(word.toUpperCase(Locale.ROOT).chars().sorted().toArray(),shuffled.chars().sorted().toArray());
        }
    }
    @Test void rejectsWordsThatCannotMakeAUsefulPuzzle() {
        for(String word:List.of("aaa","a","ice cream","12",""))assertFalse(Unscramble.valid(word));
        assertTrue(Unscramble.valid("Obsidian"));
    }
    @Test void cardsPrefixOnlyHeaderAndSubstituteWithoutRecursing() {
        var lines=ChatMessages.render("[Arcane] ",List.of("QUIZ {type}","{question}","Reward {amount}"),
                Map.of("type","Trivia","question","What is {amount}?","amount","$100"));
        assertEquals(List.of("[Arcane] QUIZ Trivia","  What is {amount}?","  Reward $100"),lines);
    }
}

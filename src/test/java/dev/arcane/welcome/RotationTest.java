package dev.arcane.welcome;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RotationTest {
    @Test void exhaustsPoolAndSurvivesRestore() {
        var bag=new ShuffleRotation();var pool=List.of("a","b","c","d");var random=new Random(42);
        var drawn=new HashSet<String>();
        drawn.add(bag.draw("questions",pool,random));drawn.add(bag.draw("questions",pool,random));
        var restored=new ShuffleRotation();bag.snapshot().forEach(restored::restore);
        drawn.add(restored.draw("questions",pool,random));String last=restored.draw("questions",pool,random);drawn.add(last);
        assertEquals(4,drawn.size());assertNotEquals(last,restored.draw("questions",pool,random));
    }
    @Test void independentPoolsAndRemovedEntriesWork() {
        var bag=new ShuffleRotation();var r=new Random(1);
        assertEquals("only",bag.draw("words",List.of("only"),r));
        assertEquals("only",bag.draw("words",List.of("only"),r));
        assertEquals("new",bag.draw("words",List.of("new"),r));
        String first=bag.draw("events",List.of("quiz","battle"),r);
        assertNotEquals(first,bag.draw("events",List.of("quiz","battle"),r));
    }
}

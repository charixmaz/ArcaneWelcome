package dev.arcane.welcome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BattleConfigTest {
    @TempDir Path dir;
    private BattleSettings load(String text) throws Exception {
        Path f=dir.resolve("battles.yml");Files.writeString(f,text);return BattleSettings.load(f.toFile());
    }
    @Test void bundledTopicsAndUiLoad() throws Exception {
        var b=BattleSettings.load(Path.of("src/main/resources/battles.yml").toFile());
        assertEquals(4,b.topics().size());assertEquals(45000,b.duration());assertEquals(2,b.minimum());
        var m=new org.bukkit.configuration.file.YamlConfiguration();m.load(Path.of("src/main/resources/messages.yml").toFile());
        for(String key:List.of("quiz-question","quiz-unscramble","quiz-winner","battle-question","battle-result","battle-tie","battle-empty"))
            assertTrue(m.getStringList(key).size()>=2,key);
    }
    @Test void rejectsAmbiguousOptionsAndInvalidDuration() {
        assertThrows(IllegalArgumentException.class,()->load("topics:\n  t:\n    question: 'Pick?'\n    options: [Cats, cats]\n"));
        assertThrows(IllegalArgumentException.class,()->load("topics:\n  t:\n    question: 'Pick?'\n    options: ['2', Cats]\n"));
        assertThrows(IllegalArgumentException.class,()->load("battle:\n  duration-seconds: -1\n"));
    }
}

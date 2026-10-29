package dev.arcane.welcome;

import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContentPackTest {
    @Test void packHasNoDuplicateQuestionsWordsOrBattlePairs() throws Exception {
        var quiz=new YamlConfiguration();quiz.load(Path.of("src/main/resources/quizzes.yml").toFile());
        var questions=quiz.getConfigurationSection("questions");assertNotNull(questions);
        var seen=new HashSet<String>();var counts=new HashMap<String,Integer>();
        for(String id:questions.getKeys(false)) {
            String text=quiz.getString("questions."+id+".question");assertTrue(seen.add(text.toLowerCase(Locale.ROOT)),text);
            counts.merge(quiz.getString("questions."+id+".category"),1,Integer::sum);
        }
        assertEquals(Map.of("general",80,"geography",80,"minecraft",80),counts);
        var words=quiz.getStringList("unscramble.words");assertEquals(240,new HashSet<>(words).size());
        for(String word:words)assertTrue(Unscramble.valid(word),word);
        var battles=new YamlConfiguration();battles.load(Path.of("src/main/resources/battles.yml").toFile());
        var topics=battles.getConfigurationSection("topics");assertNotNull(topics);seen.clear();
        for(String id:topics.getKeys(false)) {
            var options=new ArrayList<>(battles.getStringList("topics."+id+".options"));Collections.sort(options);
            String signature=battles.getString("topics."+id+".question")+options;
            assertTrue(seen.add(signature),signature);
        }
        assertEquals(240,seen.size());
    }
    @Test void migrationPreservesOwnerSettingsAndEditsAndRunsOnlyOnce() throws Exception {
        var bundled=new YamlConfiguration();bundled.load(Path.of("src/main/resources/quizzes.yml").toFile());
        var owner=new YamlConfiguration();owner.set("quiz.reward-command","custom pay {player} 7");
        owner.set("quiz.interval-seconds",360);owner.set("categories.geography",false);
        owner.set("questions.pack-general-001.question","Owner version?");owner.set("questions.pack-general-001.answers",List.of("Owner"));
        owner.set("questions.custom.question","Custom?");owner.set("questions.custom.answers",List.of("Custom"));
        owner.set("unscramble.words",List.of("DIAMOND","customword"));
        assertTrue(ContentPack.merge(owner,bundled,"questions",true));
        assertEquals(360,owner.getInt("quiz.interval-seconds"));assertFalse(owner.getBoolean("categories.geography"));
        assertEquals("custom pay {player} 7",owner.getString("quiz.reward-command"));
        assertEquals("Owner version?",owner.getString("questions.pack-general-001.question"));
        assertEquals("Custom?",owner.getString("questions.custom.question"));
        assertTrue(owner.getStringList("unscramble.words").contains("customword"));
        assertEquals(241,owner.getStringList("unscramble.words").size());
        owner.set("questions.pack-general-002",null);
        assertFalse(ContentPack.merge(owner,bundled,"questions",true));
        assertFalse(owner.contains("questions.pack-general-002"));
    }
    @Test void ownerDifficultyOnDemoIsPreserved() throws Exception {
        var bundled=new YamlConfiguration();bundled.load(Path.of("src/main/resources/quizzes.yml").toFile());
        var owner=new YamlConfiguration();owner.set("questions.biggest-animal.question","What is the largest animal in the world?");
        owner.set("questions.biggest-animal.answers",List.of("Blue whale"));owner.set("questions.biggest-animal.category","general");
        owner.set("questions.biggest-animal.difficulty","hard");ContentPack.merge(owner,bundled,"questions",true);
        assertEquals("hard",owner.getString("questions.biggest-animal.difficulty"));
    }
    @Test void migrationRemovesOnlyUnchangedMultiwordDemoQuestions() throws Exception {
        var bundled=new YamlConfiguration();bundled.load(Path.of("src/main/resources/quizzes.yml").toFile());
        var owner=new YamlConfiguration();owner.set("questions.biggest-animal.question","What is the largest animal in the world?");
        owner.set("questions.biggest-animal.answers",List.of("Blue whale"));owner.set("questions.biggest-animal.category","general");
        owner.set("questions.usa-capital.question","Owner's capital question?");owner.set("questions.usa-capital.answers",List.of("Washington"));
        ContentPack.merge(owner,bundled,"questions",true);
        assertFalse(owner.contains("questions.biggest-animal"));assertTrue(owner.contains("questions.usa-capital"));
    }
}

package dev.arcane.welcome;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** One-time additive upgrade. Owner content and reward/schedule settings win. */
final class ContentPack {
    static boolean merge(YamlConfiguration current,YamlConfiguration bundled,String section,boolean words) {
        int version=bundled.getInt("content-pack-version",0);
        if(version<=current.getInt("content-pack-version",0))return false;
        var entries=bundled.getConfigurationSection(section);
        if(entries!=null)for(String key:entries.getKeys(false)) {
            String path=section+"."+key;
            if(!current.contains(path))current.set(path,Objects.requireNonNull(entries.getConfigurationSection(key)).getValues(false));
        }
        if(words) {
            var merged=new LinkedHashMap<String,String>();
            for(String word:current.getStringList("unscramble.words"))merged.putIfAbsent(word.toLowerCase(Locale.ROOT),word);
            for(String word:bundled.getStringList("unscramble.words"))merged.putIfAbsent(word.toLowerCase(Locale.ROOT),word);
            current.set("unscramble.words",new ArrayList<>(merged.values()));
            // Remove only unchanged old demo questions that require multi-word answers.
            if("What is the capital of the USA?".equals(current.getString("questions.usa-capital.question")) &&
                    current.getStringList("questions.usa-capital.answers").equals(List.of("Washington DC","Washington D.C.","Washington, D.C.")))current.set("questions.usa-capital",null);
            if("What is the largest animal in the world?".equals(current.getString("questions.biggest-animal.question")) &&
                    current.getStringList("questions.biggest-animal.answers").equals(List.of("Blue whale")))current.set("questions.biggest-animal",null);
        }
        current.set("content-pack-version",version);return true;
    }
}

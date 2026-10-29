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
            removeUnchangedDemo(current,"usa-capital","geography","What is the capital of the USA?",List.of("Washington DC","Washington D.C.","Washington, D.C."));
            removeUnchangedDemo(current,"biggest-animal","general","What is the largest animal in the world?",List.of("Blue whale"));
        }
        current.set("content-pack-version",version);return true;
    }
    private static void removeUnchangedDemo(YamlConfiguration current,String id,String category,String question,List<String> answers) {
        String path="questions."+id;
        var entry=current.getConfigurationSection(path);
        if(entry!=null && entry.getValues(false).equals(Map.of("question",question,"category",category,"answers",answers)))current.set(path,null);
    }
}

package dev.arcane.welcome;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

record BattleSettings(boolean enabled,long interval,long duration,int minimum,List<Topic> topics) {
    record Topic(String id,String question,String first,String second) {}
    static BattleSettings load(File file) throws Exception {
        var c=new YamlConfiguration();c.load(file);
        long interval=QuizSettings.number(c,"battle.interval-seconds",900,1,86400)*1000;
        long duration=QuizSettings.number(c,"battle.duration-seconds",45,5,3600)*1000;
        int minimum=(int)QuizSettings.number(c,"battle.minimum-online-players",2,1,10000);
        var topics=new ArrayList<Topic>();
        var section=c.getConfigurationSection("topics");
        if(section!=null)for(String id:section.getKeys(false)) {
            String path="topics."+id;
            String question=c.getString(path+".question","");
            List<String> options=c.getStringList(path+".options");
            if(question.isBlank() || options.size()!=2 || options.stream().anyMatch(String::isBlank))
                throw new IllegalArgumentException(path+" requires a question and exactly two non-empty options");
            String first=options.get(0).strip(),second=options.get(1).strip();
            if(QuizRound.normalize(first).equals(QuizRound.normalize(second)) ||
                    first.matches("[12]") || second.matches("[12]"))
                throw new IllegalArgumentException(path+" options must differ and cannot be named 1 or 2");
            topics.add(new Topic(id,question,first,second));
        }
        if(c.getBoolean("battle.enabled",true) && topics.isEmpty())throw new IllegalArgumentException("Add at least one battle topic");
        return new BattleSettings(c.getBoolean("battle.enabled",true),interval,duration,minimum,List.copyOf(topics));
    }
}

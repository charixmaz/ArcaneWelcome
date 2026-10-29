package dev.arcane.welcome;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import java.util.*;

record QuizSettings(boolean enabled,long interval,long timeout,int minimumPlayers,int cooldown,
                    int mathChance,int min,int max,List<String> operations,List<Question> questions,
                    String command,String reward) {
    record Question(String id,String text,List<String> answers,String command,String reward) {}
    static QuizSettings load(File file) throws Exception {
        var c=new YamlConfiguration();c.load(file);
        long interval=number(c,"quiz.interval-seconds",600,1,86400);
        long timeout=number(c,"quiz.answer-time-seconds",30,1,3600);
        int minimum=(int)number(c,"quiz.minimum-online-players",1,1,10000);
        int cooldown=(int)number(c,"quiz.winner-cooldown-rounds",0,0,100);
        int chance=(int)number(c,"math.chance-percent",40,0,100);
        int min=(int)number(c,"math.minimum-number",1,0,10000);
        int max=(int)number(c,"math.maximum-number",50,1,10000);
        if(min>max)throw new IllegalArgumentException("math.minimum-number must not exceed maximum-number");
        String command=validCommand(c.getString("quiz.reward-command","eco give {player} 100"));
        String reward=c.getString("quiz.reward-display","$100");
        List<String> operations=c.getStringList("math.operations");
        if(operations.isEmpty() || !Set.of("addition","subtraction","multiplication","division").containsAll(operations))
            throw new IllegalArgumentException("math.operations must list addition, subtraction, multiplication or division");
        List<Question> questions=new ArrayList<>();
        ConfigurationSection section=c.getConfigurationSection("questions");
        if(section!=null)for(String id:section.getKeys(false)) {
            String path="questions."+id;
            String text=c.getString(path+".question","");
            List<String> answers=c.getStringList(path+".answers");
            if(text.isBlank() || answers.isEmpty() || answers.stream().anyMatch(String::isBlank))
                throw new IllegalArgumentException(path+" needs a question and non-empty answers list");
            String category=c.getString(path+".category","general");
            if(!c.getBoolean("categories."+category,true))continue;
            String difficulty=c.getString(path+".difficulty","default");
            String cmd=validCommand(c.getString("difficulty-rewards."+difficulty+".command",command));
            String label=c.getString("difficulty-rewards."+difficulty+".display",reward);
            questions.add(new Question(id,text,List.copyOf(answers),cmd,label));
        }
        boolean math=c.getBoolean("math.enabled",true);
        if(!math)chance=0;
        if(questions.isEmpty() && !math && c.getBoolean("quiz.enabled",true))
            throw new IllegalArgumentException("Enable math or supply at least one enabled trivia question");
        return new QuizSettings(c.getBoolean("quiz.enabled",true),interval*1000,timeout*1000,minimum,cooldown,
                questions.isEmpty() && math?100:chance,min,max,List.copyOf(operations),List.copyOf(questions),command,reward);
    }
    static String validCommand(String command) {
        if(command==null || command.isBlank() || command.startsWith("/") || command.contains("\n") || command.contains("\r"))
            throw new IllegalArgumentException("Quiz reward commands must be console commands without a leading slash");
        return command;
    }
    private static long number(YamlConfiguration c,String path,long fallback,long min,long max) {
        if(c.contains(path) && (!c.isInt(path) && !c.isLong(path)))throw new IllegalArgumentException(path+" must be an integer");
        long value=c.getLong(path,fallback);
        if(value<min || value>max)throw new IllegalArgumentException(path+" must be "+min+".."+max);
        return value;
    }
}

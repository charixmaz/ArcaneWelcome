package dev.arcane.welcome;

import java.util.*;
import java.util.regex.*;

/** Prefix only the header; indented detail lines keep event cards compact. */
final class ChatMessages {
    private static final Pattern PLACEHOLDER=Pattern.compile("\\{([a-zA-Z0-9]+)\\}");
    static List<String> render(String prefix,List<String> lines,Map<String,String> values) {
        List<String> output=new ArrayList<>();
        for(int i=0;i<lines.size();i++) {
            Matcher matcher=PLACEHOLDER.matcher(lines.get(i));StringBuilder text=new StringBuilder();
            while(matcher.find())matcher.appendReplacement(text,Matcher.quoteReplacement(values.getOrDefault(matcher.group(1),matcher.group())));
            matcher.appendTail(text);
            output.add((i==0?prefix:"  ")+text);
        }
        return List.copyOf(output);
    }
}

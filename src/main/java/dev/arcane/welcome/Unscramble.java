package dev.arcane.welcome;

import java.util.*;
import java.util.random.RandomGenerator;

final class Unscramble {
    static boolean valid(String word) {
        return word.matches("[A-Za-z]{3,24}") && word.toLowerCase(Locale.ROOT).chars().distinct().count()>1;
    }
    static String shuffle(String word,RandomGenerator random) {
        if(!valid(word))throw new IllegalArgumentException("Unscramble words need 3–24 letters with at least two different letters");
        String source=word.toUpperCase(Locale.ROOT);
        char[] letters=source.toCharArray();
        for(int attempt=0;attempt<20;attempt++) {
            for(int i=letters.length-1;i>0;i--) {
                int j=random.nextInt(i+1);char t=letters[i];letters[i]=letters[j];letters[j]=t;
            }
            if(!new String(letters).equals(source))return new String(letters);
        }
        // Guaranteed non-identity fallback even with an unlucky/random stub generator.
        for(int i=1;i<letters.length;i++)if(letters[i]!=letters[0]) {
            char t=letters[0];letters[0]=letters[i];letters[i]=t;break;
        }
        return new String(letters);
    }
    static String display(String shuffled) {return String.join(" ",shuffled.split(""));}
}

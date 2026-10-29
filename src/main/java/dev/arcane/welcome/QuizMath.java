package dev.arcane.welcome;

import java.util.List;
import java.util.random.RandomGenerator;

final class QuizMath {
    record Equation(String question,String answer) {}
    static Equation generate(int min,int max,List<String> operations,RandomGenerator random) {
        int a=random.nextInt(min,max+1),b=random.nextInt(min,max+1);
        return switch(operations.get(random.nextInt(operations.size()))) {
            case "addition" -> new Equation(a+" + "+b+" = ?",String.valueOf(a+b));
            case "subtraction" -> new Equation(Math.max(a,b)+" - "+Math.min(a,b)+" = ?",String.valueOf(Math.abs(a-b)));
            case "multiplication" -> new Equation(a+" × "+b+" = ?",String.valueOf(a*b));
            case "division" -> {b=Math.max(1,b);yield new Equation((a*b)+" ÷ "+b+" = ?",String.valueOf(a));}
            default -> throw new IllegalArgumentException("Unknown math operation");
        };
    }
}

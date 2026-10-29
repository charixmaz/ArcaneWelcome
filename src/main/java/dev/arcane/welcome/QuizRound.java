package dev.arcane.welcome;

import java.util.*;

/** A round can transition to a winner only once, even with simultaneous answers. */
final class QuizRound {
    final String id = "quiz:" + UUID.randomUUID();
    final String question, answer, command, reward;
    String type="Trivia";
    final long startedAt=System.currentTimeMillis();
    final long deadline;
    private final Set<String> answers;
    private boolean closed;

    QuizRound(String question, List<String> answers, long deadline, String command, String reward) {
        this.question=question; this.answer=answers.getFirst(); this.deadline=deadline;
        this.command=command; this.reward=reward;
        this.answers=new HashSet<>(); answers.forEach(a -> this.answers.add(normalize(a)));
    }
    static String normalize(String value) { return value.strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT); }
    synchronized boolean tryWin(String value,long time) {
        if(closed || time>=deadline || !answers.contains(normalize(value)))return false;
        closed=true;return true;
    }
    synchronized boolean expire(long time) {
        if(closed || time<deadline)return false;
        closed=true;return true;
    }
    synchronized void close() { closed=true; }
}

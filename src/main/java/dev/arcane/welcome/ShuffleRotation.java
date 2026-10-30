package dev.arcane.welcome;
import java.util.*;

/** Persistent seen sets: exhaust a pool before another cycle, with no boundary repeat. */
final class ShuffleRotation {
    private final Map<String,List<String>> seen=new HashMap<>();
    void restore(String key,List<String> values) {seen.put(key,new ArrayList<>(values));}
    Map<String,List<String>> snapshot() {return Map.copyOf(seen);}
    String draw(String key,List<String> pool,Random random) {
        if(pool.isEmpty())throw new IllegalArgumentException("Empty rotation pool");
        var used=seen.computeIfAbsent(key,k->new ArrayList<>());
        used.removeIf(id->!pool.contains(id));
        var choices=new ArrayList<>(pool.stream().distinct().filter(id->!used.contains(id)).toList());
        if(choices.isEmpty()) {
            String last=used.isEmpty()?null:used.getLast();used.clear();choices.addAll(new LinkedHashSet<>(pool));
            if(choices.size()>1)choices.remove(last);
        }
        String result=choices.get(random.nextInt(choices.size()));used.add(result);return result;
    }
}

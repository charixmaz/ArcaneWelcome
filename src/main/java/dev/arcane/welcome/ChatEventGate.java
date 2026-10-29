package dev.arcane.welcome;

/** Shared by quiz and battle controllers; no overlapping chat events. */
final class ChatEventGate {
    private String owner;
    private long availableAt;
    boolean acquire(String candidate,long now,boolean manual) {
        if(owner!=null || (!manual && now<availableAt))return false;
        owner=candidate;return true;
    }
    void release(String candidate,long now) {
        if(candidate.equals(owner)) {owner=null;availableAt=now+15000;}
    }
    boolean busy() {return owner!=null;}
}

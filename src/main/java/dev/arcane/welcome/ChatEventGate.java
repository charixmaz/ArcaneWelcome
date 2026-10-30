package dev.arcane.welcome;

/** One shared automatic cadence, measured from event start. Admin tests can bypass it. */
final class ChatEventGate {
    private String owner;
    private long availableAt;
    private long interval=1500000;
    void configure(long interval,long now) {this.interval=interval;if(availableAt==0)availableAt=now+interval;}
    boolean due(long now) {return owner==null && now>=availableAt;}
    boolean acquire(String candidate,long now,boolean manual) {
        if(owner!=null || (!manual && now<availableAt))return false;
        owner=candidate;availableAt=now+interval;return true;
    }
    void release(String candidate,long now) {if(candidate.equals(owner))owner=null;}
    boolean busy() {return owner!=null;}
}

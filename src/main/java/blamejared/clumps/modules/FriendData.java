package blamejared.clumps.modules;

public class FriendData {

    private final String name;
    private boolean attackable;
    private boolean showTracers;
    private boolean showEsp;

    public FriendData(String name) {
        this(name, false, true, true);
    }

    public FriendData(String name, boolean attackable, boolean showTracers, boolean showEsp) {
        this.name = name;
        this.attackable = attackable;
        this.showTracers = showTracers;
        this.showEsp = showEsp;
    }

    public String getName() { return name; }

    public boolean isAttackable() { return attackable; }
    public void setAttackable(boolean attackable) { this.attackable = attackable; }

    public boolean showTracers() { return showTracers; }
    public void setShowTracers(boolean showTracers) { this.showTracers = showTracers; }

    public boolean showEsp() { return showEsp; }
    public void setShowEsp(boolean showEsp) { this.showEsp = showEsp; }
}

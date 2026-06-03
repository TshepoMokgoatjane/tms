package za.co.tms.domain;

public enum Title {

    MR("Mr"),
    MRS("Mrs"),
    MS("Ms"),
    MISS("Miss"),
    DR("Dr"),
    PROF("Prof"),
    REV("Rev");

    private final String displayName;

    Title(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}

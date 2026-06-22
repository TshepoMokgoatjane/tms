package za.co.tms.domain;

import lombok.Getter;

@Getter
public enum Title {

    MR("Mr."),
    MRS("Mrs."),
    MS("Ms."),
    MISS("Miss"),
    DR("Dr."),
    PROF("Prof."),
    REV("Rev"),
    SIR("Sir."),
    LORD("Lord."),
    PRINCE("Prince."),
    CHAIRPERSON("Chairperson."),
    EXCELLENCY("Excellency."),
    POPE("Pope."),
    QUEEN("Queen."),
    KING("King.");

    private final String displayName;

    Title(String displayName) {
        this.displayName = displayName;
    }
}
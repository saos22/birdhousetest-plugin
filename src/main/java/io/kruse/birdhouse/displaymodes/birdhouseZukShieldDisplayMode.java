package io.kruse.birdhouse.displaymodes;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
public enum birdhouseZukShieldDisplayMode
{
    OFF("Off"),
    LIVE("Live (follow shield)"),
    PREDICT("Predict (NOT WORKING YET)");

    final private String name;

    birdhouseZukShieldDisplayMode(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
package io.kruse.birdhouse.displaymodes;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
public enum birdhouseWaveDisplayMode
{
    CURRENT("Current wave"),
    NEXT("Next wave"),
    BOTH("Both"),
    NONE("None");

    private final String name;

    birdhouseWaveDisplayMode(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
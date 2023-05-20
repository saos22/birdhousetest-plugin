package io.kruse.birdhouse.displaymodes;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
public enum birdhouseSafespotDisplayMode
{
    OFF("Off"),
    INDIVIDUAL_TILES("Individual tiles"),
    AREA("Area (lower fps)");

    final private String name;

    birdhouseSafespotDisplayMode(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
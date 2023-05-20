package io.kruse.birdhouse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import io.kruse.birdhouse.displaymodes.birdhouseWaveDisplayMode;
import lombok.AccessLevel;
import lombok.Setter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;

import static io.kruse.birdhouse.birdhouseWaveMappings.addWaveComponent;

@Singleton
public class birdhouseWaveOverlay extends Overlay
{
    private final birdhousePlugin plugin;
    private final PanelComponent panelComponent;

    @Setter(AccessLevel.PACKAGE)
    private Color waveHeaderColor;

    @Setter(AccessLevel.PACKAGE)
    private Color waveTextColor;

    @Setter(AccessLevel.PACKAGE)
    private birdhouseWaveDisplayMode displayMode;

    @Inject
    birdhouseWaveOverlay(final birdhousePlugin plugin)
    {
        this.panelComponent = new PanelComponent();
        this.setPosition(OverlayPosition.TOP_RIGHT);
        this.setPriority(OverlayPriority.HIGH);
        this.plugin = plugin;

        panelComponent.setPreferredSize(new Dimension(160, 0));
    }

    public Dimension render(final Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        if (displayMode == birdhouseWaveDisplayMode.CURRENT ||
                displayMode == birdhouseWaveDisplayMode.BOTH)
        {
            addWaveComponent(
                    plugin,
                    panelComponent,
                    "Current Wave (Wave " + plugin.getCurrentWaveNumber() + ")",
                    plugin.getCurrentWaveNumber(),
                    waveHeaderColor,
                    waveTextColor
            );
        }

        if (displayMode == birdhouseWaveDisplayMode.NEXT ||
                displayMode == birdhouseWaveDisplayMode.BOTH)
        {
            addWaveComponent(
                    plugin,
                    panelComponent,
                    "Next Wave (Wave " + plugin.getNextWaveNumber() + ")",
                    plugin.getNextWaveNumber(),
                    waveHeaderColor,
                    waveTextColor
            );
        }

        return panelComponent.render(graphics);
    }
}
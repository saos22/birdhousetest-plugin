/*
 * Copyright (c) 2019, Jacky <liangj97@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.kruse.birdhouse;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import io.kruse.birdhouse.displaymodes.birdhouseNamingDisplayMode;
import io.kruse.birdhouse.displaymodes.birdhousePrayerDisplayMode;
import io.kruse.birdhouse.displaymodes.birdhouseSafespotDisplayMode;
import io.kruse.birdhouse.displaymodes.birdhouseWaveDisplayMode;
import io.kruse.birdhouse.displaymodes.birdhouseZukShieldDisplayMode;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
		name = "Epic Birdhouse Runs",
		description = "Makes birdhouse runs epic",
		tags = {"birdhouse","epic"}
)
@Slf4j
@Singleton
public class birdhousePlugin extends Plugin
{
	private static final int birdhouse_REGION = 9043;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private birdhouseOverlay birdhouseOverlay;

	@Inject
	private birdhouseWaveOverlay waveOverlay;

	@Inject
	private birdhouseInfoBoxOverlay jadOverlay;

	@Inject
	private birdhouseOverlay prayerOverlay;

	@Inject
	private birdhouseConfig config;

	@Getter(AccessLevel.PACKAGE)
	private birdhouseConfig.FontStyle fontStyle = birdhouseConfig.FontStyle.BOLD;
	@Getter(AccessLevel.PACKAGE)
	private int textSize = 32;

	private WorldPoint lastLocation = new WorldPoint(0, 0, 0);

	@Getter(AccessLevel.PACKAGE)
	private int currentWaveNumber;

	@Getter(AccessLevel.PACKAGE)
	private final List<birdhouseNPC> birdhouseNpcs = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, Map<birdhouseNPC.Attack, Integer>> upcomingAttacks = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private birdhouseNPC.Attack closestAttack = null;

	@Getter(AccessLevel.PACKAGE)
	private final List<WorldPoint> obstacles = new ArrayList<>();

	@Getter(AccessLevel.PACKAGE)
	private boolean finalPhase = false;
	@Getter(AccessLevel.PACKAGE)
	private NPC zukShield = null;
	private WorldPoint zukShieldLastPosition = null;
	private WorldPoint zukShieldBase = null;
	private int zukShieldCornerTicks = -2;

	@Getter(AccessLevel.PACKAGE)
	private birdhouseNPC centralNibbler = null;

	// 0 = total safespot
	// 1 = pray melee
	// 2 = pray range
	// 3 = pray magic
	// 4 = pray melee, range
	// 5 = pray melee, magic
	// 6 = pray range, magic
	// 7 = pray all
	@Getter(AccessLevel.PACKAGE)
	private final Map<WorldPoint, Integer> safeSpotMap = new HashMap<>();
	@Getter(AccessLevel.PACKAGE)
	private final Map<Integer, List<WorldPoint>> safeSpotAreas = new HashMap<>();

	@Getter(AccessLevel.PACKAGE)
	private long lastTick;

	@Getter(AccessLevel.PACKAGE)
	private birdhousePrayerDisplayMode prayerDisplayMode;
	@Getter(AccessLevel.PACKAGE)
	private boolean descendingBoxes;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateNonPriorityDescendingBoxes;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateBlobDetectionTick;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateWhenPrayingCorrectly;

	private birdhouseWaveDisplayMode waveDisplay;
	@Getter(AccessLevel.PACKAGE)
	private birdhouseNamingDisplayMode npcNaming;
	@Getter(AccessLevel.PACKAGE)
	private boolean npcLevels;
	private Color getWaveOverlayHeaderColor;
	private Color getWaveTextColor;

	@Getter(AccessLevel.PACKAGE)
	private birdhouseSafespotDisplayMode safespotDisplayMode;
	@Getter(AccessLevel.PACKAGE)
	private int safespotsCheckSize;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateNonSafespotted;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateTemporarySafespotted;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateSafespotted;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateObstacles;

	@Getter(AccessLevel.PACKAGE)
	private boolean indicateNibblers;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateCentralNibbler;

	@Getter(AccessLevel.PACKAGE)
	private boolean indicateActiveHealersJad;
	@Getter(AccessLevel.PACKAGE)
	private boolean indicateActiveHealersZuk;

	private boolean indicateNpcPositionBat;
	private boolean indicateNpcPositionBlob;
	private boolean indicateNpcPositionMeleer;
	private boolean indicateNpcPositionRanger;
	private boolean indicateNpcPositionMage;

	private boolean ticksOnNpcBat;
	private boolean ticksOnNpcBlob;
	private boolean ticksOnNpcMeleer;
	private boolean ticksOnNpcRanger;
	private boolean ticksOnNpcMage;
	private boolean ticksOnNpcHealerJad;
	private boolean ticksOnNpcJad;
	private boolean ticksOnNpcZuk;

	@Getter(AccessLevel.PACKAGE)
	private boolean ticksOnNpcZukShield;

	private boolean safespotsBat;
	private boolean safespotsBlob;
	private boolean safespotsMeleer;
	private boolean safespotsRanger;
	private boolean safespotsMage;
	private boolean safespotsHealerJad;
	private boolean safespotsJad;
	private birdhouseZukShieldDisplayMode safespotsZukShieldBeforeHealers;
	private birdhouseZukShieldDisplayMode safespotsZukShieldAfterHealers;

	private boolean prayerBat;
	private boolean prayerBlob;
	private boolean prayerMeleer;
	private boolean prayerRanger;
	private boolean prayerMage;
	private boolean prayerHealerJad;
	private boolean prayerJad;

	private boolean hideNibblerDeath;
	private boolean hideBatDeath;
	private boolean hideBlobDeath;
	private boolean hideBlobSmallRangedDeath;
	private boolean hideBlobSmallMagicDeath;
	private boolean hideBlobSmallMeleeDeath;
	private boolean hideMeleerDeath;
	private boolean hideRangerDeath;
	private boolean hideMagerDeath;
	private boolean hideJadDeath;
	private boolean hideHealerJadDeath;
	private boolean hideHealerZukDeath;
	private boolean hideZukDeath;

	@Provides
	birdhouseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(birdhouseConfig.class);
	}

	@Override
	protected void startUp()
	{
		updateConfig();

		waveOverlay.setDisplayMode(this.waveDisplay);
		waveOverlay.setWaveHeaderColor(this.getWaveOverlayHeaderColor);
		waveOverlay.setWaveTextColor(this.getWaveTextColor);

		if (isInbirdhouse())
		{
			overlayManager.add(birdhouseOverlay);

			if (this.waveDisplay != birdhouseWaveDisplayMode.NONE)
			{
				overlayManager.add(waveOverlay);
			}

			overlayManager.add(jadOverlay);
			overlayManager.add(prayerOverlay);
			//hideNpcDeaths();
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(birdhouseOverlay);
		overlayManager.remove(waveOverlay);
		overlayManager.remove(jadOverlay);
		overlayManager.remove(prayerOverlay);

		currentWaveNumber = -1;

		//showNpcDeaths();
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!"birdhouse".equals(event.getGroup()))
		{
			return;
		}

		updateConfig();
		//hideNpcDeaths();
		//showNpcDeaths();

		if (event.getKey().endsWith("color"))
		{
			waveOverlay.setWaveHeaderColor(this.getWaveOverlayHeaderColor);
			waveOverlay.setWaveTextColor(this.getWaveTextColor);
		}
		else if ("waveDisplay".equals(event.getKey()))
		{
			overlayManager.remove(waveOverlay);

			waveOverlay.setDisplayMode(this.waveDisplay);

			if (isInbirdhouse() && this.waveDisplay != birdhouseWaveDisplayMode.NONE)
			{
				overlayManager.add(waveOverlay);
			}
		}
	}

	@Subscribe
	private void onGameTick(GameTick GameTickEvent)
	{
		if (!isInbirdhouse())
		{
			return;
		}

		lastTick = System.currentTimeMillis();

		upcomingAttacks.clear();
		calculateUpcomingAttacks();

		closestAttack = null;
		calculateClosestAttack();

		safeSpotMap.clear();
		calculateSafespots();

		safeSpotAreas.clear();
		calculateSafespotAreas();

		obstacles.clear();
		calculateObstacles();

		centralNibbler = null;
		calculateCentralNibbler();
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (!isInbirdhouse())
		{
			return;
		}

		if (event.getNpc().getId() == NpcID.ANCESTRAL_GLYPH)
		{
			zukShield = event.getNpc();
		}

		final birdhouseNPC.Type birdhouseNPCType = birdhouseNPC.Type.typeFromId(event.getNpc().getId());

		if (birdhouseNPCType == null)
		{
			return;
		}

		if (birdhouseNPCType == birdhouseNPC.Type.ZUK)
		{
			finalPhase = false;
			zukShieldCornerTicks = -2;
			zukShieldLastPosition = null;
			zukShieldBase = null;
		}
		if (birdhouseNPCType == birdhouseNPC.Type.HEALER_ZUK)
		{
			finalPhase = true;
		}

		// Blobs need to be added to the end of the list because the prayer for their detection tick will be based
		// on the upcoming attacks of other NPC's
		if (birdhouseNPCType == birdhouseNPC.Type.BLOB)
		{
			birdhouseNpcs.add(new birdhouseNPC(event.getNpc()));
		}
		else
		{
			birdhouseNpcs.add(0, new birdhouseNPC(event.getNpc()));
		}
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		if (!isInbirdhouse())
		{
			return;
		}

		if (event.getNpc().getId() == NpcID.ANCESTRAL_GLYPH)
		{
			zukShield = null;
		}

		birdhouseNpcs.removeIf(birdhouseNPC -> birdhouseNPC.getNpc() == event.getNpc());
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event)
	{
		if (!isInbirdhouse())
		{
			return;
		}

		if (event.getActor() instanceof NPC)
		{
			final NPC npc = (NPC) event.getActor();

			if (ArrayUtils.contains(birdhouseNPC.Type.NIBBLER.getNpcIds(), npc.getId())
					&& npc.getAnimation() == 7576)
			{
				birdhouseNpcs.removeIf(birdhouseNPC -> birdhouseNPC.getNpc() == npc);
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (!isInbirdhouse())
		{
			birdhouseNpcs.clear();

			currentWaveNumber = -1;

			overlayManager.remove(birdhouseOverlay);
			overlayManager.remove(waveOverlay);
			overlayManager.remove(jadOverlay);
			overlayManager.remove(prayerOverlay);
		}
		else if (currentWaveNumber == -1)
		{
			birdhouseNpcs.clear();

			currentWaveNumber = 1;

			overlayManager.add(birdhouseOverlay);
			overlayManager.add(jadOverlay);
			overlayManager.add(prayerOverlay);

			if (this.waveDisplay != birdhouseWaveDisplayMode.NONE)
			{
				overlayManager.add(waveOverlay);
			}
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (!isInbirdhouse() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage();

		if (event.getMessage().contains("Wave:"))
		{
			message = message.substring(message.indexOf(": ") + 2);
			currentWaveNumber = Integer.parseInt(message.substring(0, message.indexOf('<')));
		}
	}

	private boolean isInbirdhouse()
	{
		return ArrayUtils.contains(client.getMapRegions(), birdhouse_REGION);
	}

	int getNextWaveNumber()
	{
		return currentWaveNumber == -1 || currentWaveNumber == 69 ? -1 : currentWaveNumber + 1;
	}

	private void calculateUpcomingAttacks()
	{
		for (birdhouseNPC birdhouseNPC : birdhouseNpcs)
		{
			birdhouseNPC.gameTick(client, lastLocation, finalPhase);

			if (birdhouseNPC.getType() == io.kruse.birdhouse.birdhouseNPC.Type.ZUK && zukShieldCornerTicks == -1)
			{
				birdhouseNPC.updateNextAttack(io.kruse.birdhouse.birdhouseNPC.Attack.UNKNOWN, 12); // TODO: Could be 10 or 11. Test!
				zukShieldCornerTicks = 0;
			}

			// Map all upcoming attacks and their priority + determine which NPC is about to attack next
			if (birdhouseNPC.getTicksTillNextAttack() > 0 && isPrayerHelper(birdhouseNPC)
					&& (birdhouseNPC.getNextAttack() != io.kruse.birdhouse.birdhouseNPC.Attack.UNKNOWN
					|| (indicateBlobDetectionTick && birdhouseNPC.getType() == io.kruse.birdhouse.birdhouseNPC.Type.BLOB
					&& birdhouseNPC.getTicksTillNextAttack() >= 4)))
			{
				upcomingAttacks.computeIfAbsent(birdhouseNPC.getTicksTillNextAttack(), k -> new HashMap<>());

				if (indicateBlobDetectionTick && birdhouseNPC.getType() == io.kruse.birdhouse.birdhouseNPC.Type.BLOB
						&& birdhouseNPC.getTicksTillNextAttack() >= 4)
				{
					upcomingAttacks.computeIfAbsent(birdhouseNPC.getTicksTillNextAttack() - 3, k -> new HashMap<>());
					upcomingAttacks.computeIfAbsent(birdhouseNPC.getTicksTillNextAttack() - 4, k -> new HashMap<>());

					// If there's already a magic attack on the detection tick, group them
					if (upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC))
					{
						if (upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).get(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC) > io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).put(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC, io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority());
						}
					}
					// If there's already a ranged attack on the detection tick, group them
					else if (upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED))
					{
						if (upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).get(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED) > io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).put(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED, io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority());
						}
					}
					// If there's going to be a magic attack on the blob attack tick, pray range on the detect tick so magic is prayed on the attack tick
					else if (upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack()).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC)
							|| upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 4).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC))
					{
						if (!upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED)
								|| upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).get(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED) > io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).put(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED, io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority());
						}
					}
					// If there's going to be a ranged attack on the blob attack tick, pray magic on the detect tick so range is prayed on the attack tick
					else if (upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack()).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED)
							|| upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 4).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.RANGED))
					{
						if (!upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).containsKey(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC)
								|| upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).get(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC) > io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority())
						{
							upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).put(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC, io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority());
						}
					}
					// If there's no magic or ranged attack on the detection tick, create a magic pray blob
					else
					{
						upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack() - 3).put(io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC, io.kruse.birdhouse.birdhouseNPC.Type.BLOB.getPriority());
					}
				}
				else
				{
					final birdhouseNPC.Attack attack = birdhouseNPC.getNextAttack();
					final int priority = birdhouseNPC.getType().getPriority();

					if (!upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack()).containsKey(attack)
							|| upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack()).get(attack) > priority)
					{
						upcomingAttacks.get(birdhouseNPC.getTicksTillNextAttack()).put(attack, priority);
					}
				}
			}
		}
	}

	private void calculateClosestAttack()
	{
		if (prayerDisplayMode == birdhousePrayerDisplayMode.PRAYER_TAB
				|| prayerDisplayMode == birdhousePrayerDisplayMode.BOTH)
		{
			int closestTick = 999;
			int closestPriority = 999;

			for (Integer tick : upcomingAttacks.keySet())
			{
				final Map<birdhouseNPC.Attack, Integer> attackPriority = upcomingAttacks.get(tick);

				for (birdhouseNPC.Attack currentAttack : attackPriority.keySet())
				{
					final int currentPriority = attackPriority.get(currentAttack);
					if (tick < closestTick || (tick == closestTick && currentPriority < closestPriority))
					{
						closestAttack = currentAttack;
						closestPriority = currentPriority;
						closestTick = tick;
					}
				}
			}
		}
	}

	private void calculateSafespots()
	{
		if (currentWaveNumber < 69)
		{
			if (safespotDisplayMode != birdhouseSafespotDisplayMode.OFF)
			{
				int checkSize = (int) Math.floor(safespotsCheckSize / 2.0);

				for (int x = -checkSize; x <= checkSize; x++)
				{
					for (int y = -checkSize; y <= checkSize; y++)
					{
						final WorldPoint checkLoc = client.getLocalPlayer().getWorldLocation().dx(x).dy(y);

						if (obstacles.contains(checkLoc))
						{
							continue;
						}

						for (birdhouseNPC birdhouseNPC : birdhouseNpcs)
						{
							if (!isNormalSafespots(birdhouseNPC))
							{
								continue;
							}

							if (!safeSpotMap.containsKey(checkLoc))
							{
								safeSpotMap.put(checkLoc, 0);
							}

							if (birdhouseNPC.canAttack(client, checkLoc)
									|| birdhouseNPC.canMoveToAttack(client, checkLoc, obstacles))
							{
								if (birdhouseNPC.getType().getDefaultAttack() == io.kruse.birdhouse.birdhouseNPC.Attack.MELEE)
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 1);
									}
									else if (safeSpotMap.get(checkLoc) == 2)
									{
										safeSpotMap.put(checkLoc, 4);
									}
									else if (safeSpotMap.get(checkLoc) == 3)
									{
										safeSpotMap.put(checkLoc, 5);
									}
									else if (safeSpotMap.get(checkLoc) == 6)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}

								if (birdhouseNPC.getType().getDefaultAttack() == io.kruse.birdhouse.birdhouseNPC.Attack.MAGIC
										|| (birdhouseNPC.getType() == io.kruse.birdhouse.birdhouseNPC.Type.BLOB
										&& safeSpotMap.get(checkLoc) != 2 && safeSpotMap.get(checkLoc) != 4))
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 3);
									}
									else if (safeSpotMap.get(checkLoc) == 1)
									{
										safeSpotMap.put(checkLoc, 5);
									}
									else if (safeSpotMap.get(checkLoc) == 2)
									{
										safeSpotMap.put(checkLoc, 6);
									}
									else if (safeSpotMap.get(checkLoc) == 5)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}

								if (birdhouseNPC.getType().getDefaultAttack() == io.kruse.birdhouse.birdhouseNPC.Attack.RANGED
										|| (birdhouseNPC.getType() == io.kruse.birdhouse.birdhouseNPC.Type.BLOB
										&& safeSpotMap.get(checkLoc) != 3 && safeSpotMap.get(checkLoc) != 5))
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 2);
									}
									else if (safeSpotMap.get(checkLoc) == 1)
									{
										safeSpotMap.put(checkLoc, 4);
									}
									else if (safeSpotMap.get(checkLoc) == 3)
									{
										safeSpotMap.put(checkLoc, 6);
									}
									else if (safeSpotMap.get(checkLoc) == 4)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}

								if (birdhouseNPC.getType() == io.kruse.birdhouse.birdhouseNPC.Type.JAD
										&& birdhouseNPC.getNpc().getWorldArea().isInMeleeDistance(checkLoc))
								{
									if (safeSpotMap.get(checkLoc) == 0)
									{
										safeSpotMap.put(checkLoc, 1);
									}
									else if (safeSpotMap.get(checkLoc) == 2)
									{
										safeSpotMap.put(checkLoc, 4);
									}
									else if (safeSpotMap.get(checkLoc) == 3)
									{
										safeSpotMap.put(checkLoc, 5);
									}
									else if (safeSpotMap.get(checkLoc) == 6)
									{
										safeSpotMap.put(checkLoc, 7);
									}
								}
							}
						}
					}
				}
			}
		}
		else if (currentWaveNumber == 69 && zukShield != null)
		{
			final WorldPoint zukShieldCurrentPosition = zukShield.getWorldLocation();

			if (zukShieldLastPosition != null && zukShieldLastPosition.getX() != zukShieldCurrentPosition.getX()
					&& zukShieldCornerTicks == -2)
			{
				zukShieldBase = zukShieldLastPosition;
				zukShieldCornerTicks = -1;
			}

			zukShieldLastPosition = zukShield.getWorldLocation();

			if (safespotDisplayMode != birdhouseSafespotDisplayMode.OFF)
			{
				if ((finalPhase && safespotsZukShieldAfterHealers == birdhouseZukShieldDisplayMode.LIVE)
						|| (!finalPhase && safespotsZukShieldBeforeHealers == birdhouseZukShieldDisplayMode.LIVE))
				{
					for (int x = zukShield.getWorldLocation().getX() - 1; x <= zukShield.getWorldLocation().getX() + 3; x++)
					{
						for (int y = zukShield.getWorldLocation().getY() - 4; y <= zukShield.getWorldLocation().getY() - 2; y++)
						{
							safeSpotMap.put(new WorldPoint(x, y, client.getPlane()), 0);
						}
					}
				}
				else if ((finalPhase && safespotsZukShieldAfterHealers == birdhouseZukShieldDisplayMode.PREDICT)
						|| (!finalPhase && safespotsZukShieldBeforeHealers == birdhouseZukShieldDisplayMode.PREDICT))
				{
					if (zukShieldCornerTicks >= 0)
					{
						// TODO: Predict zuk shield safespots
						// Calculate distance from zukShieldCurrentPosition to zukShieldBase.
						// - If shield is not in corner: calculate next position in current direction (use
						//   difference between current and last position to get direction)
						// - If shield is in corner: increment zukShieldCornerTicks and predict next shield
						//   position based on how many ticks the shield has been in the corner.
					}
				}
			}
		}
	}

	private void calculateSafespotAreas()
	{
		if (safespotDisplayMode == birdhouseSafespotDisplayMode.AREA)
		{
			for (WorldPoint worldPoint : safeSpotMap.keySet())
			{
				if (!safeSpotAreas.containsKey(safeSpotMap.get(worldPoint)))
				{
					safeSpotAreas.put(safeSpotMap.get(worldPoint), new ArrayList<>());
				}

				safeSpotAreas.get(safeSpotMap.get(worldPoint)).add(worldPoint);
			}
		}

		lastLocation = client.getLocalPlayer().getWorldLocation();
	}

	private void calculateObstacles()
	{
		for (NPC npc : client.getNpcs())
		{
			obstacles.addAll(npc.getWorldArea().toWorldPointList());
		}
	}

	private void calculateCentralNibbler()
	{
		birdhouseNPC bestNibbler = null;
		int bestAmountInArea = 0;
		int bestDistanceToPlayer = 999;

		for (birdhouseNPC birdhouseNPC : birdhouseNpcs)
		{
			if (birdhouseNPC.getType() != io.kruse.birdhouse.birdhouseNPC.Type.NIBBLER)
			{
				continue;
			}

			int amountInArea = 0;
			final int distanceToPlayer = birdhouseNPC.getNpc().getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation());

			for (birdhouseNPC checkNpc : birdhouseNpcs)
			{
				if (checkNpc.getType() != io.kruse.birdhouse.birdhouseNPC.Type.NIBBLER
						|| checkNpc.getNpc().getWorldArea().distanceTo(birdhouseNPC.getNpc().getWorldArea()) > 1)
				{
					continue;
				}

				amountInArea++;
			}

			if (amountInArea > bestAmountInArea
					|| (amountInArea == bestAmountInArea && distanceToPlayer < bestDistanceToPlayer))
			{
				bestNibbler = birdhouseNPC;
			}
		}

		if (bestNibbler != null)
		{
			centralNibbler = bestNibbler;
		}
	}

	private boolean isPrayerHelper(birdhouseNPC birdhouseNPC)
	{
		switch (birdhouseNPC.getType())
		{
			case BAT:
				return prayerBat;
			case BLOB:
				return prayerBlob;
			case MELEE:
				return prayerMeleer;
			case RANGER:
				return prayerRanger;
			case MAGE:
				return prayerMage;
			case HEALER_JAD:
				return prayerHealerJad;
			case JAD:
				return prayerJad;
			default:
				return false;
		}
	}

	boolean isTicksOnNpc(birdhouseNPC birdhouseNPC)
	{
		switch (birdhouseNPC.getType())
		{
			case BAT:
				return ticksOnNpcBat;
			case BLOB:
				return ticksOnNpcBlob;
			case MELEE:
				return ticksOnNpcMeleer;
			case RANGER:
				return ticksOnNpcRanger;
			case MAGE:
				return ticksOnNpcMage;
			case HEALER_JAD:
				return ticksOnNpcHealerJad;
			case JAD:
				return ticksOnNpcJad;
			case ZUK:
				return ticksOnNpcZuk;
			default:
				return false;
		}
	}

	boolean isNormalSafespots(birdhouseNPC birdhouseNPC)
	{
		switch (birdhouseNPC.getType())
		{
			case BAT:
				return safespotsBat;
			case BLOB:
				return safespotsBlob;
			case MELEE:
				return safespotsMeleer;
			case RANGER:
				return safespotsRanger;
			case MAGE:
				return safespotsMage;
			case HEALER_JAD:
				return safespotsHealerJad;
			case JAD:
				return safespotsJad;
			default:
				return false;
		}
	}

	boolean isIndicateNpcPosition(birdhouseNPC birdhouseNPC)
	{
		switch (birdhouseNPC.getType())
		{
			case BAT:
				return indicateNpcPositionBat;
			case BLOB:
				return indicateNpcPositionBlob;
			case MELEE:
				return indicateNpcPositionMeleer;
			case RANGER:
				return indicateNpcPositionRanger;
			case MAGE:
				return indicateNpcPositionMage;
			default:
				return false;
		}
	}

//	private void hideNpcDeaths()
//	{
//
//		if (this.hideNibblerDeath)
//		{
//			client.addHiddenNpcDeath("Jal-Nib");
//		}
//		if (this.hideBatDeath)
//		{
//			client.addHiddenNpcDeath("Jal-MejRah");
//		}
//		if (this.hideBlobDeath)
//		{
//			client.addHiddenNpcDeath("Jal-Ak");
//		}
//		if (this.hideBlobSmallMeleeDeath)
//		{
//			client.addHiddenNpcDeath("Jal-AkRek-Ket");
//		}
//		if (this.hideBlobSmallMagicDeath)
//		{
//			client.addHiddenNpcDeath("Jal-AkRek-Mej");
//		}
//		if (this.hideBlobSmallRangedDeath)
//		{
//			client.addHiddenNpcDeath("Jal-AkRek-Xil");
//		}
//		if (this.hideMeleerDeath)
//		{
//			client.addHiddenNpcDeath("Jal-ImKot");
//		}
//		if (this.hideRangerDeath)
//		{
//			client.addHiddenNpcDeath("Jal-Xil");
//		}
//		if (this.hideMagerDeath)
//		{
//			client.addHiddenNpcDeath("Jal-Zek");
//		}
//		if (this.hideHealerJadDeath && isInbirdhouse())
//		{
//			client.addHiddenNpcDeath("Yt-HurKot");
//		}
//		if (this.hideJadDeath)
//		{
//			client.addHiddenNpcDeath("JalTok-Jad");
//		}
//		if (this.hideHealerZukDeath)
//		{
//			client.addHiddenNpcDeath("Jal-MejJak");
//		}
//		if (this.hideZukDeath)
//		{
//			client.addHiddenNpcDeath("TzKal-Zuk");
//		}
//	}

//	private void showNpcDeaths()
//	{
//		if (!this.hideNibblerDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-Nib");
//		}
//		if (!this.hideBatDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-MejRah");
//		}
//		if (!this.hideBlobDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-Ak");
//		}
//		if (!this.hideBlobSmallMeleeDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-AkRek-Ket");
//		}
//		if (!this.hideBlobSmallMagicDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-AkRek-Mej");
//		}
//		if (!this.hideBlobSmallRangedDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-AkRek-Xil");
//		}
//		if (!this.hideMeleerDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-ImKot");
//		}
//		if (!this.hideRangerDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-Xil");
//		}
//		if (!this.hideMagerDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-Zek");
//		}
//		if (!this.hideHealerJadDeath)
//		{
//			client.removeHiddenNpcDeath("Yt-HurKot");
//		}
//		if (!this.hideJadDeath)
//		{
//			client.removeHiddenNpcDeath("JalTok-Jad");
//		}
//		if (!this.hideHealerZukDeath)
//		{
//			client.removeHiddenNpcDeath("Jal-MejJak");
//		}
//		if (!this.hideZukDeath)
//		{
//			client.removeHiddenNpcDeath("TzKal-Zuk");
//		}
//	}

	private void updateConfig()
	{
		this.prayerDisplayMode = config.prayerDisplayMode();
		this.descendingBoxes = config.descendingBoxes();
		this.indicateWhenPrayingCorrectly = config.indicateWhenPrayingCorrectly();
		this.indicateNonPriorityDescendingBoxes = config.indicateNonPriorityDescendingBoxes();
		this.indicateBlobDetectionTick = config.indicateBlobDetectionTick();

		this.waveDisplay = config.waveDisplay();
		this.npcNaming = config.npcNaming();
		this.npcLevels = config.npcLevels();
		this.getWaveOverlayHeaderColor = config.getWaveOverlayHeaderColor();
		this.getWaveTextColor = config.getWaveTextColor();

		this.safespotDisplayMode = config.safespotDisplayMode();
		this.safespotsCheckSize = config.safespotsCheckSize();
		this.indicateNonSafespotted = config.indicateNonSafespotted();
		this.indicateTemporarySafespotted = config.indicateTemporarySafespotted();
		this.indicateSafespotted = config.indicateSafespotted();
		this.indicateObstacles = config.indicateObstacles();
		this.safespotsZukShieldBeforeHealers = config.safespotsZukShieldBeforeHealers();

		this.indicateNibblers = config.indicateNibblers();
		this.indicateCentralNibbler = config.indicateCentralNibbler();

		this.indicateActiveHealersJad = config.indicateActiveHealerJad();
		this.indicateActiveHealersZuk = config.indicateActiveHealerZuk();

		this.indicateNpcPositionBat = config.indicateNpcPositionBat();
		this.indicateNpcPositionBlob = config.indicateNpcPositionBlob();
		this.indicateNpcPositionMeleer = config.indicateNpcPositionMeleer();
		this.indicateNpcPositionRanger = config.indicateNpcPositionRanger();
		this.indicateNpcPositionMage = config.indicateNpcPositionMage();

		this.ticksOnNpcBat = config.ticksOnNpcBat();
		this.ticksOnNpcBlob = config.ticksOnNpcBlob();
		this.ticksOnNpcMeleer = config.ticksOnNpcMeleer();
		this.ticksOnNpcRanger = config.ticksOnNpcRanger();
		this.ticksOnNpcMage = config.ticksOnNpcMage();
		this.ticksOnNpcHealerJad = config.ticksOnNpcHealerJad();
		this.ticksOnNpcJad = config.ticksOnNpcJad();
		this.ticksOnNpcZuk = config.ticksOnNpcZuk();
		this.ticksOnNpcZukShield = config.ticksOnNpcZukShield();

		this.safespotsBat = config.safespotsBat();
		this.safespotsBlob = config.safespotsBlob();
		this.safespotsMeleer = config.safespotsMeleer();
		this.safespotsRanger = config.safespotsRanger();
		this.safespotsMage = config.safespotsMage();
		this.safespotsHealerJad = config.safespotsHealerJad();
		this.safespotsJad = config.safespotsJad();
		this.safespotsZukShieldBeforeHealers = config.safespotsZukShieldBeforeHealers();
		this.safespotsZukShieldAfterHealers = config.safespotsZukShieldAfterHealers();

		this.prayerBat = config.prayerBat();
		this.prayerBlob = config.prayerBlob();
		this.prayerMeleer = config.prayerMeleer();
		this.prayerRanger = config.prayerRanger();
		this.prayerMage = config.prayerMage();
		this.prayerHealerJad = config.prayerHealerJad();
		this.prayerJad = config.prayerJad();

		this.hideNibblerDeath = config.hideNibblerDeath();
		this.hideBatDeath = config.hideBatDeath();
		this.hideBlobDeath = config.hideBlobDeath();
		this.hideBlobSmallRangedDeath = config.hideBlobSmallRangedDeath();
		this.hideBlobSmallMagicDeath = config.hideBlobSmallMagicDeath();
		this.hideBlobSmallMeleeDeath = config.hideBlobSmallMeleeDeath();
		this.hideMeleerDeath = config.hideMeleerDeath();
		this.hideRangerDeath = config.hideRangerDeath();
		this.hideMagerDeath = config.hideMagerDeath();
		this.hideHealerJadDeath = config.hideHealerJadDeath();
		this.hideJadDeath = config.hideJadDeath();
		this.hideHealerZukDeath = config.hideHealerZukDeath();
		this.hideZukDeath = config.hideZukDeath();
	}

	public Iterable<? extends birdhouseNPC> getbirdhouseNpcs() {
		return birdhouseNpcs;
	}
}
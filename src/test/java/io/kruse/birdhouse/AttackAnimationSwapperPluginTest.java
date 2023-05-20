package io.kruse.birdhouse;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AttackAnimationSwapperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(birdhousePlugin.class);
		RuneLite.main(args);
	}
}
package net.simpvp.Misc;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class WitherSkulls implements Listener {
	private static final int ACTIVATION_MINIMUM = 32 * 32;
	private static final long CLEANUP_TIME = 60 * 20;

	/**
	 * Prevent explosion damage from blue wither skulls if no player is nearby.
	 */
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!(event.getEntity() instanceof WitherSkull))
			return;

		WitherSkull skull = (WitherSkull) event.getEntity();
		if (!skull.isCharged())
			return; // Not blue skull

		Location loc = event.getLocation();
		boolean hasNearby = loc.getWorld().getPlayers().stream()
				.anyMatch(p -> p.getLocation().distanceSquared(loc) < ACTIVATION_MINIMUM);

		if (!hasNearby)
			event.setCancelled(true);
	}

	public WitherSkulls() {
		// Periodically remove wither skulls that have built up in lazy chunks or
		// above/below the height limit.
		Bukkit.getScheduler().runTaskTimer(Misc.instance, () -> {
			Bukkit.getWorlds().stream()
					.flatMap(w -> w.getEntitiesByClass(WitherSkull.class).stream())
					.filter(WitherSkulls::shouldDelete)
					.forEach(e -> e.remove());
		}, 0L, CLEANUP_TIME);
	}

	public static boolean shouldDelete(WitherSkull skull) {
		World world = skull.getWorld();
		Location loc = skull.getLocation();

		// Avoid potentially loading a chunk with Entity#getChunk()
		if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
			return true;

		// Prevent skulls from building up in lazy chunks on the edge of a loaded area
		if (skull.getChunk().getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING)
			return true;

		return (loc.getY() < world.getMinHeight() - 100 || loc.getY() > world.getMaxHeight() + 100);
	}

}

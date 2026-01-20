package net.simpvp.Misc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;

public class GlobalSoundFix {
	/**
	 * For every global WORLD_EVENT packet, set the position to [0,0] preventing
	 * any coordinate tracking exploits.
	 * See: https://minecraft.wiki/w/Java_Edition_protocol/Packets#World_Event
	 *
	 * Currently (1.21) fixes:
	 * Wither spawn
	 * Ender Dragon death
	 * End portal opening
	 *
	 * In the past it was necessary to randomize Thunder sounds in a separate
	 * packet to fix a similar exploit, but this has since been fixed by the game.
	 */
	public static void add_protocol_listener() {
		Misc.instance.getLogger().info("Enabling global sound fix.");

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
				Misc.instance,
				ListenerPriority.NORMAL,
				PacketType.Play.Server.WORLD_EVENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer p = event.getPacket();

				boolean global = p.getBooleans().read(0);
				if (global == false) {
					return;
				}

				BlockPosition position = new BlockPosition(0, 100, 0);
				p.getBlockPositionModifier().writeSafely(0, position);
			}
		});
	}
}

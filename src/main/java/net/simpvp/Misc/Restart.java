package net.simpvp.Misc;

import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Restart implements CommandExecutor {
	/* Lets player use the /requestrestart and /cancelrestart commands */

	private static JavaPlugin plugin = Misc.instance;

	private static boolean cancelled = true;
	private static UUID requester = null;
	private static long last_request = System.currentTimeMillis();

	private static final Instant server_start = Instant.now();
	private static final Duration minimum_uptime = Duration.ofHours(plugin.getConfig().getLong("minimum-uptime-hours", 24));

	// This is a list of players who can't use /cancelrestart and /requestrestart
	List<?> cancel = Misc.instance.getConfig().getStringList("disableCancelRestart").stream().map(UUID::fromString).collect(java.util.stream.Collectors.toList());
	
	public boolean onCommand(CommandSender sender,
			Command cmd,
			String label,
			String[] args) {

		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;

			int played_ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
			int played_hours = played_ticks / (20 * 60 * 60);

			if (played_hours < 500) {
				player.sendMessage(ChatColor.RED + "You need to have played for at least 500 hours on this server to use this command.");
				return true;
			}
		}

		if (cancel.contains(player.getUniqueId())) {
			sender.sendMessage(ChatColor.RED + "You can no longer use this command.");
			return true;
		}

		if (cmd.getName().equals("requestrestart")) {
			if (player == null) {
				sender.sendMessage("Only players can use this command");
				return true;
			}

			Duration uptime = Duration.between(server_start, Instant.now());
			boolean high_uptime = uptime.compareTo(minimum_uptime) >= 0;

			double[] tps = plugin.getServer().getTPS();
			boolean low_tps = (tps[0] <= 17.0) || (tps[1] <= 17.0);

			// Allow request if uptime is high or TPS is low
			if (!high_uptime && !low_tps) {
				player.sendMessage(ChatColor.RED + "A restart does not appear to be necessary. TPS must be below 17 or uptime over " + minimum_uptime.toHours() + " hours.");
				return true;
			}

			/* 5 minute waiting period */
			if (System.currentTimeMillis() <
					(5 * 60 * 1000) + last_request) {
				player.sendMessage(ChatColor.RED
						+ "A restart has been requested too recently. Please wait a bit longer before requesting a restart.");
				return true;
			}

			/* All checks completed */


			String msg = "[Announcement] " + player.getName() + " has requested a server restart. If you disagree with this request, type /cancelrestart. If it is not cancelled, the server will restart in 30 seconds.";
			for (Player p : plugin.getServer().getOnlinePlayers()) {
				p.sendMessage(ChatColor.AQUA + msg);
			}
			plugin.getLogger().info(msg);

			last_request = System.currentTimeMillis();
			cancelled = false;
			requester = player.getUniqueId();

			/* What we do now is a create an asynchronous task that
			 * waits 30 seconds and then creates a synchronous task
			 * that calls sync_effect_restart */
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						return;
					}
					new BukkitRunnable() {
						@Override
						public void run() {
							sync_effect_restart();
						}
					}.runTaskLater(plugin, 0);
				}
			}.runTaskAsynchronously(plugin);

		} else if (cmd.getName().equals("cancelrestart")) {
			if (cancelled == true) {
				sender.sendMessage(ChatColor.RED + "There is no restart for you to cancel.");
			} else if (requester != null && player != null &&
					requester.equals(player.getUniqueId())) {
				sender.sendMessage(ChatColor.RED + "You cannot cancel a restart you requested.");
			} else {
				cancelled = true;
				String msg = "[Announcement] " + sender.getName() + " has cancelled the requested server restart. Tip: Ask in chat if anybody minds a restart before initiating one.";
				for (Player p : plugin.getServer().getOnlinePlayers()) {
					p.sendMessage(ChatColor.AQUA + msg);
				}
				plugin.getLogger().info(msg);
			}
		}

		return true;
	}

	/** Effect the restart, i.e. check whether the restart has been
	 * cancelled, and if it hasn't then actually restart.
	 *
	 * This should be called synchronously so it can use Bukkit */
	private void sync_effect_restart() {
		if (cancelled == true) {
			return;
		}

		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
				"stop");
	}

}


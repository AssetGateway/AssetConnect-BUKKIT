package asset.connect.bukkit;

import java.net.InetSocketAddress;
import java.util.Map;

import net.minecraft.server.NetworkManager;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import asset.connect.bukkit.util.ReflectionUtils;

import com.google.common.collect.MapMaker;

public class ConnectPluginListener implements Listener {

	private ConnectPlugin connectPlugin;
	private Map<Player, InetSocketAddress> playersToAddresses = new MapMaker().weakKeys().makeMap();
	
	public ConnectPluginListener(ConnectPlugin connectPlugin) {
		this.connectPlugin = connectPlugin;
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent playerLoginEvent) {
		String[] playerData = playerLoginEvent.getHostname().split("\\:")[0].split("\\;");
		if(playerData.length != 3) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Authentication Failed");
			return;
		}
		if(!playerData[0].equals(this.connectPlugin.getAuthenticationKey())) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Authentication Failed");
			return;
		}
		InetSocketAddress playerAddress = new InetSocketAddress(playerData[1], Integer.parseInt(playerData[2]));
		ReflectionUtils.setFinalField(PlayerLoginEvent.class, playerLoginEvent, "address", playerAddress.getAddress());
		this.playersToAddresses.put(playerLoginEvent.getPlayer(), playerAddress);
		if(playerLoginEvent.getResult() == Result.KICK_BANNED
				&& playerLoginEvent.getKickMessage().startsWith("Your IP address is banned from this server!\nReason: ")) {
			if(this.connectPlugin.getServer().getIPBans().contains(playerData[1])) {
				playerLoginEvent.disallow(Result.KICK_BANNED, "Your IP address is banned from this server!");
			} else if(this.connectPlugin.getServer().getOnlinePlayers().length >= this.connectPlugin.getServer().getMaxPlayers()) {
				playerLoginEvent.disallow(Result.KICK_FULL, "The server is full!");
			} else {
				playerLoginEvent.allow();
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
		Player player = playerJoinEvent.getPlayer();
		ReflectionUtils.setFinalField(NetworkManager.class, (NetworkManager) ((CraftPlayer) player).getHandle().netServerHandler.networkManager, "j", this.playersToAddresses.remove(player));
	}
	
}

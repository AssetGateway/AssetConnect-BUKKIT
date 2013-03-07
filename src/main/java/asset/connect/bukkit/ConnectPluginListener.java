package asset.connect.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;

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
		// verify integrity
		String[] playerData = playerLoginEvent.getHostname().split("\\:")[0].split("\\;");
		if(playerData.length != 3) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Authentication Failed");
			return;
		}
		if(!playerData[0].equals(this.connectPlugin.getAuthenticationKey())) {
			playerLoginEvent.disallow(Result.KICK_OTHER, "Authentication Failed");
			return;
		}
		
		// store IP address
		InetSocketAddress playerAddress = new InetSocketAddress(playerData[1], Integer.parseInt(playerData[2]));
		ReflectionUtils.setFinalField(PlayerLoginEvent.class, playerLoginEvent, "address", playerAddress.getAddress());
		this.playersToAddresses.put(playerLoginEvent.getPlayer(), playerAddress);
		
		// emulate a normal login procedure with the IP address
		if(playerLoginEvent.getResult() == Result.KICK_BANNED && playerLoginEvent.getKickMessage().startsWith("Your IP address is banned from this server!\nReason: ")) {
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
		
		// store IP address
		try {
			Method getHandleMethod = player.getClass().getMethod("getHandle");
			Object entityPlayer = getHandleMethod.invoke(player);
			
			// old MC support
			String playerConnectionFieldName = "playerConnection";
			for (Field field : entityPlayer.getClass().getFields()){
				if (!field.getName().equals("netServerHandler")){
					continue;
				}
				playerConnectionFieldName = "netServerHandler";
				break;
			}
			
			Field playerConnectionField = entityPlayer.getClass().getField(playerConnectionFieldName);
			Object playerConnection = playerConnectionField.get(entityPlayer);
			
			Field networkManagerField = playerConnection.getClass().getField("networkManager");
			Object networkManager = networkManagerField.get(playerConnection);
			
			// spigot support
			String socketAddressFieldName = "j";
			for(Field field : networkManager.getClass().getClass().getFields()) {
				if (!field.getName().equals("address")){
					continue;
				}
				socketAddressFieldName = "address";
				break;
			}
			
			ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, socketAddressFieldName, this.playersToAddresses.remove(player));
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
	
	
}

package asset.connect.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import asset.connect.api.Connect;
import asset.connect.bukkit.util.ReflectionUtils;
import asset.connect.lib.ConnectImpl;

public class ConnectPlugin extends JavaPlugin {

	private ExecutorService executorService;
	private Connect connect;
	private ConnectThread connectThread;
	
	private String authenticationKey;
	
	@Override
	public void onEnable() {
		try {
			this.executorService = Executors.newFixedThreadPool(4);
			this.connect = new ConnectImpl(this.executorService, new ConnectSettingsImpl(this.getConfig()), this.getInboundAddress().getAddress().getHostAddress());
			this.connectThread = new ConnectThread(this);
			
			this.getServer().getServicesManager().register(Connect.class, this.connect, this, ServicePriority.Normal);
			this.getServer().getPluginManager().registerEvents(new ConnectPluginListener(this), this);
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					ConnectPlugin.this.connectThread.start();
				}
			});
			
			// Grab CraftServer
			Object craftServer = super.getServer();
			
			// Get "protected final MinecraftServer console"
			Field console_field = craftServer.getClass().getDeclaredField("console");
			console_field.setAccessible(true);
			Object console = console_field.get(craftServer);
			
			for (Method m : console.getClass().getMethods()){
				if (m.getName().equals("setOnlineMode")){
					System.out.println(m.getParameterTypes().toString());
				}
			}
			
			// Set offline mode
			Method setOnlineMode = console.getClass().getMethod("setOnlineMode", boolean.class);
			setOnlineMode.invoke(console, Boolean.FALSE);
			
			// Connection throttle
			YamlConfiguration configuration = ReflectionUtils.getPrivateField(craftServer, YamlConfiguration.class, "configuration");
			configuration.set("settings.connection-throttle", 0);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public void onDisable() {
		try {
			if(this.executorService != null) {
				this.executorService.shutdownNow();
			}
			if(this.connectThread != null) {
				this.connectThread.stop();
			}
			if(this.connect != null) {
				this.connect.close();
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		} finally {
			this.executorService = null;
			this.connect = null;
			this.connectThread = null;
		}
	}
	
	public InetSocketAddress getInboundAddress() {
		return new InetSocketAddress(this.getServer().getIp().isEmpty() ? "0.0.0.0" : this.getServer().getIp(), this.getServer().getPort());
	}
	
	public Connect getConnect() {
		return this.connect;
	}
	
	public String getAuthenticationKey() {
		return this.authenticationKey;
	}
	
	public void setAuthenticationKey(String authenticationKey) {
		this.authenticationKey = authenticationKey;
	}
	
}

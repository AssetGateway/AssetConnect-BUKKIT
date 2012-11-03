package asset.connect.bukkit;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import asset.connect.api.Connect;
import asset.connect.lib.ConnectImpl;

public class ConnectPlugin extends JavaPlugin {

	private ExecutorService executorService;
	private Connect connect;
	private ConnectTask connectTask;
	
	private String authenticationKey;
	
	@Override
	public void onEnable() {
		try {
			this.executorService = Executors.newFixedThreadPool(4);
			this.connect = new ConnectImpl(this.executorService, new ConnectSettingsImpl(this.getConfig()), this.getInboundAddress().getHostName());
			this.connectTask = new ConnectTask(this);
			
			this.connectTask.setId(this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, this.connectTask, 20L, 20L));
			this.getServer().getServicesManager().register(Connect.class, this.connect, this, ServicePriority.Normal);
			this.getServer().getPluginManager().registerEvents(new ConnectPluginListener(this), this);
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
			if(this.connect != null) {
				this.connect.close();
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		} finally {
			this.executorService = null;
			this.connect = null;
			this.connectTask = null;
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

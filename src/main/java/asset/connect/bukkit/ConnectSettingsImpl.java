package asset.connect.bukkit;

import java.net.InetSocketAddress;

import org.bukkit.configuration.file.FileConfiguration;

import asset.connect.api.ConnectSettings;

public class ConnectSettingsImpl implements ConnectSettings {

	private String outboundIp;
	private int outboundPort;
	private String username;
	private String password;
	
	public ConnectSettingsImpl(FileConfiguration fileConfiguration) {
		this.outboundIp = fileConfiguration.getString("settings.ip");
		this.outboundPort = fileConfiguration.getInt("settings.port");
		this.username = fileConfiguration.getString("settings.auth.user");
		this.password = fileConfiguration.getString("settings.auth.pass");
	}
	
	public InetSocketAddress getOutboundAddress() {
		return new InetSocketAddress(this.outboundIp, this.outboundPort);
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

}

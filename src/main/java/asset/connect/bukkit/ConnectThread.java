package asset.connect.bukkit;

import asset.connect.api.Connect;
import asset.connect.api.ConnectSettings;
import asset.connect.api.request.impl.AnnounceRequest;
import asset.connect.api.request.impl.AuthenticateRequest;
import asset.connect.api.request.impl.KeyRequest;
import asset.connect.api.result.impl.AnnounceResult;
import asset.connect.api.result.impl.AuthenticateResult;
import asset.connect.api.result.impl.KeyResult;

public class ConnectThread implements Runnable {

	private ConnectPlugin connectPlugin;
	private Thread thread;

	public ConnectThread(ConnectPlugin connectPlugin) {
		this.connectPlugin = connectPlugin;
	}

	public void start() {
		if(this.thread != null) {
			return;
		}
		this.thread = new Thread(this);
		this.thread.setName("connect-thread");
		this.thread.start();
	}

	public void stop() {
		if(this.thread == null) {
			return;
		}
		this.thread.interrupt();
		this.thread = null;
	}

	public void run() {
		Connect connect = this.connectPlugin.getConnect();
		ConnectSettings settings = connect.getSettings();
		try {
			while(!connect.isClosed()) {
				// connect
				try {
					connect.connect();
				} catch(Exception exception) {
					connect.disconnect();
					System.out.println("[Connect] Couldn't connect to remote host: \"" + exception.getMessage() + "\", retrying");
					Thread.sleep(1000L);
					continue;
				}
				
				// key
				KeyResult keyResult = connect.request(new KeyRequest()).await(1000L);
				if(keyResult == null) {
					connect.disconnect();
					System.out.println("[Connect] Connection timed out while keying, retrying");
					Thread.sleep(1000L);
					continue;
				}
				
				// authenticate
				AuthenticateResult authenticationResult = this.connectPlugin.getConnect().request(new AuthenticateRequest(settings.getUsername(), settings.getPassword(), keyResult.getKey())).await(1000L);
				if(authenticationResult == null) {
					connect.disconnect();
					System.out.println("[Connect] Connection timed out while authenticating, retrying");
					Thread.sleep(1000L);
					continue;
				}
				switch(authenticationResult.getStatusCode()) {
				case SUCCESS:
					break;
				case INVALID_CREDENTIALS:
					connect.disconnect();
					System.out.println("[Connect] Invalid username or password, retrying");
					Thread.sleep(1000L);
					continue;
				default:
					connect.disconnect();
					System.out.println("[Connect] Unknown error while authenticating: \"" + authenticationResult.getStatusCode() + "\", retrying");
					Thread.sleep(1000L);
					continue;
				}
				
				// announce
				AnnounceResult announceResult = this.connectPlugin.getConnect().request(new AnnounceRequest(this.connectPlugin.getInboundAddress().getPort())).await(1000L);
				if(announceResult == null) {
					connect.disconnect();
					System.out.println("[Connect] Connection timed out while announcing, retrying");
					Thread.sleep(1000L);
					continue;
				}
				switch(announceResult.getStatusCode()) {
				case SUCCESS:
					break;
				default:
					connect.disconnect();
					System.out.println("[Connect] Unknown error while announcing: \"" + announceResult.getStatusCode() + "\", retrying");
					Thread.sleep(1000L);
					continue;
				}
				
				// pause
				System.out.println("[Connect] Connected to the cloud");
				this.connectPlugin.setAuthenticationKey(keyResult.getKey());
				while(connect.isConnected()) {
					Thread.sleep(1000L);
				}
				this.connectPlugin.setAuthenticationKey(null);
				System.out.println("[Connect] Lost connection to the cloud, reconnecting");
			}
		} catch(InterruptedException exception) {
			//ignore
		} catch(Exception exception) {
			System.out.println("-=== FATAL ====- Please report this error to AssetGateway:");
			exception.printStackTrace();
		}
	}

}

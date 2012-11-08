package asset.connect.bukkit;

import asset.connect.api.Connect;
import asset.connect.api.ConnectSettings;
import asset.connect.api.request.impl.AnnounceRequest;
import asset.connect.api.request.impl.AuthenticateRequest;
import asset.connect.api.request.impl.KeyRequest;
import asset.connect.api.result.impl.AnnounceResult;
import asset.connect.api.result.impl.AuthenticateResult;

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
				try {
					connect.connect();
				} catch(Exception exception) {
					connect.disconnect();
					System.out.println("[Connect] Couldn't connect to remote host: " + exception.getMessage());
					Thread.sleep(1000L);
					continue;
				}
				
				String authenticationKey = connect.request(new KeyRequest()).await().getKey();
				AuthenticateResult authenticationResult = this.connectPlugin.getConnect().request(new AuthenticateRequest(settings.getUsername(), settings.getPassword(), this.connectPlugin.getAuthenticationKey())).await();
				switch(authenticationResult.getStatusCode()) {
				case SUCCESS:
					break;
				case INVALID_CREDENTIALS:
					connect.disconnect();
					System.out.println("[Connect] Invalid username or password");
					Thread.sleep(1000L);
					continue;
				default:
					connect.disconnect();
					System.out.println("[Connect] Unknown error while authenticating: " + authenticationResult.getStatusCode());
					Thread.sleep(1000L);
					continue;
				}
				
				AnnounceResult announceResult = this.connectPlugin.getConnect().request(new AnnounceRequest(this.connectPlugin.getInboundAddress().getPort())).await();
				switch(announceResult.getStatusCode()) {
				case SUCCESS:
					break;
				default:
					connect.disconnect();
					System.out.println("[Connect] Unknown error while announcing: " + announceResult.getStatusCode());
					Thread.sleep(1000L);
					continue;
				}
				
				System.out.println("[Connect] Connected to the cloud");
				this.connectPlugin.setAuthenticationKey(authenticationKey);
				while(connect.isConnected()) {
					Thread.sleep(1000L);
				}
				this.connectPlugin.setAuthenticationKey(null);
			}
		} catch(InterruptedException exception) {
			//ignore
		} catch(Exception exception) {
			System.out.println("==== FATAL ==== Please report this error to AssetGateway:");
			exception.printStackTrace();
		}
	}

}

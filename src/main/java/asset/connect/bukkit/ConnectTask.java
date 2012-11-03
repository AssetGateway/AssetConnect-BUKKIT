package asset.connect.bukkit;

import java.util.concurrent.locks.ReentrantLock;

import asset.connect.api.ConnectSettings;
import asset.connect.api.request.impl.AnnounceRequest;
import asset.connect.api.request.impl.AuthenticateRequest;
import asset.connect.api.request.impl.KeyRequest;
import asset.connect.api.result.impl.AnnounceResult;
import asset.connect.api.result.impl.AuthenticateResult;

public class ConnectTask implements Runnable {

	private ConnectPlugin connectPlugin;
	private int id;
	private boolean authenticated;

	private ReentrantLock lock = new ReentrantLock();

	public ConnectTask(ConnectPlugin connectPlugin) {
		this.connectPlugin = connectPlugin;
	}

	public void run() {
		if(!this.lock.tryLock()) {
			return;
		}
		try {
			if(this.connectPlugin.getConnect().isClosed()) {
				this.connectPlugin.getServer().getScheduler().cancelTask(this.id);
				return;
			}
			this.connect();
			this.auth();
		} finally {
			this.lock.unlock();
		}
	}

	private void connect() {
		if(this.connectPlugin.getConnect().isConnected()) {
			return;
		}
		if(this.connectPlugin.getConnect().connect() == false) {
			System.out.println("[Connect] Couldn't connect to remote host");
			return;
		}
		this.connectPlugin.setAuthenticationKey(null);
		this.authenticated = false;
		System.out.println("[Connect] Connected to remote host");
	}

	private void auth() {
		if(!this.connectPlugin.getConnect().isConnected()) {
			return;
		}
		if(this.authenticated) {
			return;
		}
		try {
			this.connectPlugin.setAuthenticationKey(this.connectPlugin.getConnect().request(new KeyRequest()).await().getKey());

			ConnectSettings settings = this.connectPlugin.getConnect().getSettings();
			AuthenticateResult authResult = this.connectPlugin.getConnect().request(new AuthenticateRequest(settings.getUsername(), settings.getPassword(), this.connectPlugin.getAuthenticationKey())).await();
			switch(authResult.getStatusCode()) {
			case SUCCESS:
				this.authenticated = true;
				System.out.println("[Connect] Authenticated to remote host");
				break;
			case INVALID_CREDENTIALS:
				this.connectPlugin.getConnect().disconnect();
				System.out.println("[Connect] Invalid username or password");
				return;
			default:
				this.connectPlugin.getConnect().disconnect();
				System.out.println("[Connect] Unknown error while authenticating: " + authResult.getStatusCode());
				return;
			}

			AnnounceResult announceResult = this.connectPlugin.getConnect().request(new AnnounceRequest(this.connectPlugin.getInboundAddress().getPort())).await();
			switch(announceResult.getStatusCode()) {
			case SUCCESS:
				System.out.println("[Connect] Announced to remote host");
				break;
			default:
				this.connectPlugin.getConnect().disconnect();
				System.out.println("[Connect] Unknown error while announcing: " + announceResult.getStatusCode());
				return;
			}
		} catch(Exception exception) {
			//ignore
		}
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

}

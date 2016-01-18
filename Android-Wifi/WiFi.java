import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.text.format.Formatter;
import android.util.Log;

public class WiFi {

    public String SSID;
    public String Pass;
    public Context c;
    public WifiManager wifi;
    public List<ScanResult> scanResults;
    public List<WifiConfiguration> storedNetworks;
    public boolean isConnecting = false;
    public int IP = -1;
    public String IPs = "";
    public Thread connect;
    public boolean scanConnect;

	public WiFi(Context c, String SSID, String Pass){
		this.c = c;
		this.SSID = SSID;
		this.Pass = Pass;
		wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
		if ((connect == null) || (!connect.isAlive())){
			connect = new connectThread(true);
			connect.start();
	 	}
	}

    public WiFi(Context c){
        this.c = c;
        wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        if ((connect == null) || (!connect.isAlive())){
            connect = new connectThread(false);
            connect.start();
        }
    }

    public void setWiFiCredentials(String SSID, String Pass){
        this.SSID = SSID;
        this.Pass = Pass;
    }
	
	public void restart(){
        try{
            c.unregisterReceiver(wifiscan);
        }catch(Exception e){
            e.printStackTrace();
        }
		if ((connect == null) || (!connect.isAlive())){
			connect = new connectThread(true);
			connect.start();
            Log.v("ConnectionMonitor", "new wifi connect thread");
        }
	}
	
	public void scanAvailable(){
        try{
            c.unregisterReceiver(wifiscan);
        }catch(Exception e){
            e.printStackTrace();
        }
		if ((connect == null) || (!connect.isAlive())){
			connect = new connectThread(false);
			connect.start();
		}
	}
	
	public boolean isWifiConnected(){
        try{
            ConnectivityManager connectionManager = (ConnectivityManager) c.getSystemService(c.CONNECTIVITY_SERVICE);
            NetworkInfo wlan = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wlan.isConnected()) {
                return true;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
	}
	
	public int getRouterIP(){
		WifiInfo info = wifi.getConnectionInfo();
		IP = info.getIpAddress();
		return IP;
	}
	
	public String getFormattedRouterIP(){
		final DhcpInfo dhcp = wifi.getDhcpInfo();
		IPs = Formatter.formatIpAddress(dhcp.gateway);
		return IPs;
	}
	
	public class connectThread extends Thread{
		
		public connectThread(Boolean connect){
			scanConnect = connect;
		}
		
		@Override
		public void run() {
			isConnecting = true;
			if (enableWifi()){
				if (scanConnect){
					wifiScanConfig();
				}
				wifiScan();
			}
			isConnecting = false;
		}
	}

	public boolean enableWifi(){
		boolean wifiEnabled = false;
		if (!wifi.isWifiEnabled()){
			for (int i = 0; i < 10; i++){
				if (wifiEnabled = wifi.setWifiEnabled(true)){
					break;
				}else{
					try{
						Thread.sleep(500 * (i + 1));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}else{
			wifiEnabled = true;
		}
		return wifiEnabled;
	}
	
	public void wifiScan(){
		c.registerReceiver(wifiscan, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		wifi.startScan();
	}
	
	public void wifiScanConfig(){
		storedNetworks = wifi.getConfiguredNetworks();
	}
	
	public void wifiConnect(){
		isConnecting = true;
        try{
            for (WifiConfiguration potential : storedNetworks){
                if (potential.SSID.contains(this.SSID)){
                    wifi.disconnect();
                    wifi.enableNetwork(potential.networkId,	true);
                    isConnecting = false;
                    return;
                }
            }
            for (ScanResult potential : scanResults){
                if (potential.SSID.contains(this.SSID)){
                    wifi.disconnect();
                    connectWPA();
                    wifi.reconnect();
                    isConnecting = false;
                    return;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
		isConnecting = false;
	}

    public void wifiDisconnect(){
        try{
            wifi.disconnect();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void useCellular(AndroidUtility util){
        if (util != null){
            wifiDisconnect();
            util.setMobileDataEnabled(c, true);
        }
    }
	
	private BroadcastReceiver wifiscan = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent intent) {
			scanResults = wifi.getScanResults();
			if (scanConnect){
				wifiConnect();
			}
			c.unregisterReceiver(wifiscan);
		}
	};

    //this removes all previous network configurations in case of tablet reprovisioning
    public void clearNetworks(){
        storedNetworks = wifi.getConfiguredNetworks();
        for (WifiConfiguration network : storedNetworks){
            wifi.removeNetwork(network.networkId);
            wifi.saveConfiguration();
        }
    }

    public void clearNetwork(String SSID){
        storedNetworks = wifi.getConfiguredNetworks();
        for (WifiConfiguration network : storedNetworks){
            if (network.SSID.contains(SSID) || network.SSID.equals(SSID) || SSID.contains(network.SSID)){
                wifi.removeNetwork(network.networkId);
                wifi.saveConfiguration();
                break;
            }
        }
    }
	
	public void connectWPA(){
		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = "\"" + this.SSID + "\"";
		wc.preSharedKey  = "\"" + this.Pass + "\"";
		wc.status = WifiConfiguration.Status.ENABLED;        
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
	    wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		int res = wifi.addNetwork(wc);
		boolean b = wifi.enableNetwork(res, true);     
	    return b;
	}
	
}

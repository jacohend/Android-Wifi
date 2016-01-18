import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by jacob on 1/29/14
 * createAP and setAP were taken from a test hotspot app written by Jacob Henderson, Dec 2013
 *
 */
public class WiFiAP {

    public String SSID;
    public String Pass;
    public Context c;
    public WifiManager wifi;
    public List<ScanResult> scanResults;
    public  List<WifiConfiguration> storedNetworks;
    public boolean isConnecting = false;
    public int IP = -1;
    public String IPs = "";
    public Thread createAP;
    public boolean scanConnect;

    public WiFiAP(Context c, String SSID, String Pass){
        this.c = c;
        this.SSID = SSID;
        this.Pass = Pass;
        wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
        if ((createAP == null) || (!createAP.isAlive())){
            createAP = new createAPThread();
            createAP.start();
        }
    }

    public class createAPThread extends Thread{

        @Override
        public void run() {
            isConnecting = true;
            if (disableWifi()){
                createAP();
            }
            isConnecting = false;
        }
    }

    public boolean createAP(){
        try{
            WifiConfiguration netConfig = new WifiConfiguration();
            netConfig.SSID = "\""+this.SSID+"\"";
            //netConfig.hiddenSSID = true;
            netConfig.preSharedKey = "\"" + this.Pass + "\"";
            netConfig.status = WifiConfiguration.Status.ENABLED;
            netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            try{
                boolean APstatus = setAP(netConfig, true);
                Method isWifiApEnabledmethod = wifi.getClass().getMethod("isWifiApEnabled");
                while(!(Boolean)isWifiApEnabledmethod.invoke(wifi)){};
                Method getWifiApStateMethod = wifi.getClass().getMethod("getWifiApState");
                int APstate=(Integer)getWifiApStateMethod.invoke(wifi);
                return APstatus;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public boolean destroyAP(){
        return setAP(null, false);
    }

    public boolean setAP(WifiConfiguration netConfig, boolean enabled){
        Boolean result = false;
        try{
            result = (Boolean) wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(wifi, netConfig, enabled);
        }catch(Exception e){
            e.printStackTrace();
            return result;
        }
        return result;
    }

    public boolean disableWifi(){
        boolean wifiDisabled = false;
        if (wifi.isWifiEnabled()){
            for (int i = 0; i < 10; i++){
                if (wifiDisabled = wifi.setWifiEnabled(false)){
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
            wifiDisabled = true;
        }
        return wifiDisabled;
    }

    public boolean isAPEnabled(){
        try{
            Method isWifiApEnabledmethod = wifi.getClass().getMethod("isWifiApEnabled");
            return (Boolean)isWifiApEnabledmethod.invoke(wifi);
        }catch (Exception e){
            return false;
        }
    }
}

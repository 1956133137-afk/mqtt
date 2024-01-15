package com.yannuo.dgcanteen.networkstate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;


import com.yannuo.dgcanteen.common.MyApplication;
import com.yannuo.dgcanteen.util.LogUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import androidx.annotation.RequiresApi;

/**
 *
 * author:Almighty
 * date: 2021/9/1 14:33.
 */
public class NetworkStateManager {

    private String TAG = "NetworkStateManager";
    private static NetworkStateManager instance;

    //wifi连接状态
    private boolean wifiConnect ;
    //以太网连接状态
    private boolean ethernetConnect ;
    //其他网络传输方式
    private boolean otherConnect = false;

    private String WIFI = "wifi";
    private String ETHERNET = "ethernet";

    //选择网络传输方式ethernet、wifi
    private String selectConnect = "wifi";


    //网络Ip地址
    private String ip = "";
    //网络Mac地址
    private String mac ="";

    private List<NetWorkListener> observers;
    



    private NetworkStateManager(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            observers = new ArrayList<>();
            MyNetworkCallback myNetworkCallback = new MyNetworkCallback();
            ConnectivityManager connectivityManager = MyApplication.applicationContext.
                    getSystemService(ConnectivityManager.class);
            connectivityManager.registerDefaultNetworkCallback(myNetworkCallback);
            initStatue(connectivityManager);
        }
    }

   private void initStatue(ConnectivityManager connectivityManager){
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           Network currentNetwork = connectivityManager.getActiveNetwork();
           NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);
           if (caps == null)return;
           ethernetConnect = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
           wifiConnect = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
           updateIpAndMacAddress();
       }
   }

    public static NetworkStateManager getInstance(){
        if (instance == null){
            synchronized (NetworkStateManager.class){
                instance = new NetworkStateManager();
            }
        }
        return instance;
    }



    private void clearNetWorkConnectFlag(){
        wifiConnect = false;
        ethernetConnect = false;
        otherConnect = false;
        ip = "";
        mac ="";
    }

    /**
     * 添加观察者
     * @param listener
     */
    public void registerObserver(NetWorkListener listener){
        if (listener != null && !observers.contains(listener)){
            observers.add(listener);
        }
    }

    /**
     * 取消观察
     * @param listener
     */
    public void unRegisterObserver(NetWorkListener listener){
        if (listener != null && observers.contains(listener)){
            observers.remove(listener);
        }
    }


    /**
     * 更新IP和Mac地址
     */
    private void updateIpAndMacAddress(){
        ip = getNetworkIPAddress(MyApplication.applicationContext);
        mac = getMacAddressFromIp(MyApplication.applicationContext);
    }


    public NetworkStateManager setSelectConnect(String selectConnect) {
        this.selectConnect = selectConnect;
        return this;
    }


    public String getIp() {
        return ip;
    }

    public String getMac() {
        return mac;
    }


    public boolean isWifiConnect() {
        return wifiConnect & selectConnect.equals(WIFI);
    }

    public void setWifiConnect(boolean wifiConnect) {
        this.wifiConnect = wifiConnect ;
    }

    public boolean isEthernetConnect() {
        return ethernetConnect & selectConnect.equals(ETHERNET);
    }

    private void setEthernetConnect(boolean ethernetConnect) {
        this.ethernetConnect = ethernetConnect;
    }

    public boolean isOtherConnect() {
        return otherConnect;
    }

    private void setOtherConnect(boolean otherConnect) {
        this.otherConnect = otherConnect;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    class MyNetworkCallback extends ConnectivityManager.NetworkCallback {
           @Override
           public void onAvailable(Network network) {
//               Log.e(TAG, "The default network is now: " + network);
//               Toast.makeText(MyApplication.applicationContext, "网络已连接", Toast.LENGTH_SHORT).show();
               updateIpAndMacAddress();
               for (NetWorkListener listener : observers){
                   listener.netWorkStatus("0");
               }
           }

           @Override
           public void onLost(Network network) {
//               Log.e(TAG, "The application no longer has a default network. The last default network was " + network);
//               Toast.makeText(MyApplication.applicationContext, "网络中断", Toast.LENGTH_SHORT).show();
               clearNetWorkConnectFlag();
               for (NetWorkListener listener : observers){
                   listener.netWorkStatus("1");
               }
           }

           /*
           @Override
           public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
               Log.e(TAG, "The default network changed capabilities: " + networkCapabilities);
               if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                   updateIpAndMacAddress();
                   if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                       setWifiConnect(true);
                       Log.d(TAG, "当前网络为 Wifi ,IP: "+ip+"  Mac: "+mac);
                       Toast.makeText(MyApplication.applicationContext, "当前网络为 Wifi IP: "+ip+"  Mac: "+mac, Toast.LENGTH_SHORT).show();
                   } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                       setEthernetConnect(true);
                       Toast.makeText(MyApplication.applicationContext, "当前网络为 以太网 IP: "+ip+"  Mac: "+mac, Toast.LENGTH_SHORT).show();
                       Log.d(TAG, "当前网络为 以太网 ,IP: "+ip+"  Mac: "+mac);
                   }else {
                       Log.d(TAG, "当前网络为 其他类型 ");
                   }
               }else {
                   setOtherConnect(true);
                   Log.d(TAG, "网络具有强制门户，访问网络受限... ");
               }
           }*/

//           @Override
//           public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
//               Toast.makeText(MyApplication.applicationContext, "onLinkPropertiesChanged", Toast.LENGTH_SHORT).show();
//           }
       }



    /**
     *
     * @param context
     * @return
     */
    public String getNetworkIPAddress(Context context){

        ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean state = false;
        String ipAddress = "0.0.0.0";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
            if (activeNetworkInfo!= null && activeNetworkInfo.isConnected()){
                state = true;
                int type = activeNetworkInfo.getType();

                switch (type ){
                    case ConnectivityManager.TYPE_WIFI :

                        //  wifi网络
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        ipAddress = intIP2StringIP(wifiInfo.getIpAddress());
//                        Log.d("MacAddressUtil", " connected: TYPE_WIFI  ip--> " +ipAddress);
                        break;

//                    case ConnectivityManager.TYPE_MOBILE :
//                        Log.d("MacAddressUtil", " connected: TYPE_MOBILE" );
//                        break;
                    case ConnectivityManager.TYPE_ETHERNET :

                        ipAddress = getLocalIp();
//                        Log.d("MacAddressUtil", " connected: TYPE_ETHERNET ip--> "+ipAddress );
                        break;

//                    default:
//                        Log.d("MacAddressUtil", " connected is other" );
                }
            }
        }
        return ipAddress;
    }

    /**
     * 检查是否联网
     * @param context
     * @return
     */
    public boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    /**
     *     获取有限网IP
     */
    private String getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return "0.0.0.0";

    }

    public boolean Ping(String str){
        boolean pass = true;
        try{
            Process p =  Runtime.getRuntime().exec("ping -c 2 -w 100 " + str);
            int status = p.waitFor();
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuilder builder = new StringBuilder();
            String line = "";
            while ((line = in.readLine())!= null){
                builder.append(line);
            }
            if (status == 0){
                pass = true;
            }else if (status != 0 ) {
                pass = false;
            }
        }catch (Exception e){
            LogUtil.e(TAG,""+e);
        }
        return pass;
    }

    /**
     *  //获取网络Mac地址
     * @param context
     * @return
     */
    public String getMacAddressFromIp(Context context) {
        String mac_s= "";
        StringBuilder buf = new StringBuilder();
        try {
            byte[] mac;
            NetworkInterface ne=NetworkInterface.getByInetAddress(InetAddress.getByName(getNetworkIPAddress(context)));
            mac = ne.getHardwareAddress();
            for (byte b : mac) {
                buf.append(String.format("%02X:", b));
//                Log.d("IpAndMac", "buf: "+buf.toString());
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            mac_s = buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mac_s;
    }

    public interface NetWorkListener {

        /**
         *
         * @param statue 0--有网 ，1无网络
         */
        void netWorkStatus(String statue);
    }

}

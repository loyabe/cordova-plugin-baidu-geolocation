package com.eteng.geolocation.baidu;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.Poi;
import com.eteng.geolocation.w3.PositionOptions;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.util.SparseArray;
import android.Manifest;
import android.content.pm.PackageManager;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GeolocationPlugin extends CordovaPlugin {

  private static final String TAG = "GeolocationPlugin";

  private static final int GET_CURRENT_POSITION = 0;
  private static final int WATCH_POSITION = 1;
  private static final int CLEAR_WATCH = 2;
  BDGeolocation geolocation;
//  private SparseArray<BDGeolocation> store = new SparseArray<BDGeolocation>();
  private String [] permissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

  private JSONArray requestArgs;
  private CallbackContext context;

    private boolean isNative = false;
    private String postURL = "";
    private String token = "";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    Log.i(TAG, "插件调用");
    JSONObject options = new JSONObject();

    requestArgs = args;
    context = callbackContext;

    if (action.equals("getCurrentPosition")) {
      getPermission(GET_CURRENT_POSITION);
      try {
        options = args.getJSONObject(0);
      } catch (JSONException e) {
        Log.v(TAG, "options 未传入");
      }
      return getCurrentPosition(options, callbackContext);
    } else if (action.equals("watchPosition")) {
      getPermission(WATCH_POSITION);
      try {
        options = args.getJSONObject(0);
         isNative = options.getBoolean("isNative");
          postURL = options.getString("postURL");
          token = options.getString("token");
      } catch (JSONException e) {
        Log.v(TAG, "options 未传入");
      }
      int watchId = args.getInt(1);
      return watchPosition(options, watchId, callbackContext);
    } else if (action.equals("clearWatch")) {
      getPermission(CLEAR_WATCH);
      int watchId = args.getInt(0);
      return clearWatch(watchId, callbackContext);
    }
    return false;
  }

  private boolean clearWatch(int watchId, CallbackContext callback) {
    Log.i(TAG, "停止监听");
//    BDGeolocation geolocation = store.get(watchId);
//    store.remove(watchId);
    if (geolocation != null)
        geolocation.clearWatch();
    callback.success();
    return true;
  }

  private boolean watchPosition(JSONObject options, int watchId, final CallbackContext callback) {
    Log.i(TAG, "监听位置变化");
    Context ctx = cordova.getActivity().getApplicationContext();
    PositionOptions positionOpts = new PositionOptions(options);
    if (geolocation == null) {
      geolocation = new BDGeolocation(ctx);
    }
//    store.put(watchId, geolocation);
    return geolocation.watchPosition(positionOpts, new BDLocationListener() {
      @Override
      public void onReceiveLocation(BDLocation location) {

        if (null != location && location.getLocType() != BDLocation.TypeServerError) {
          StringBuffer sb = new StringBuffer(256);
          sb.append("time : ");
          /**
           * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
           * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
           */
          sb.append(location.getTime());
          sb.append("\nerror code : ");
          sb.append(location.getLocType());
          sb.append("\nlatitude : ");
          sb.append(location.getLatitude());
          sb.append("\nlontitude : ");
          sb.append(location.getLongitude());
          sb.append("\nradius : ");
          sb.append(location.getRadius());
          sb.append("\nCountryCode : ");
          sb.append(location.getCountryCode());
          sb.append("\nCountry : ");
          sb.append(location.getCountry());
          sb.append("\ncitycode : ");
          sb.append(location.getCityCode());
          sb.append("\ncity : ");
          sb.append(location.getCity());
          sb.append("\nDistrict : ");
          sb.append(location.getDistrict());
          sb.append("\nStreet : ");
          sb.append(location.getStreet());
          sb.append("\naddr : ");
          sb.append(location.getAddrStr());
          sb.append("\nDescribe: ");
          sb.append(location.getLocationDescribe());
          sb.append("\nDirection(not all devices have value): ");
          sb.append(location.getDirection());
          sb.append("\nPoi: ");
          if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
            for (int i = 0; i < location.getPoiList().size(); i++) {
              Poi poi = (Poi) location.getPoiList().get(i);
              sb.append(poi.getName() + ";");
            }
          }
          if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
            sb.append("\nspeed : ");
            sb.append(location.getSpeed());// 单位：km/h
            sb.append("\nsatellite : ");
            sb.append(location.getSatelliteNumber());
            sb.append("\nheight : ");
            sb.append(location.getAltitude());// 单位：米
            sb.append("\ndescribe : ");
            sb.append("gps定位成功");
          } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
            // 运营商信息
            sb.append("\noperationers : ");
            sb.append(location.getOperators());
            sb.append("\ndescribe : ");
            sb.append("网络定位成功");
          } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
            sb.append("\ndescribe : ");
            sb.append("离线定位成功，离线定位结果也是有效的");
          } else if (location.getLocType() == BDLocation.TypeServerError) {
            sb.append("\ndescribe : ");
            sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
          } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
            sb.append("\ndescribe : ");
            sb.append("网络不同导致定位失败，请检查网络是否通畅");
          } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
            sb.append("\ndescribe : ");
            sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
          }
          Log.e("baidu", sb.toString());
        }

        JSONArray message = new MessageBuilder(location).build();

          if (isNative){
              postLBSDetails (postURL, token, message);
          }else {
              PluginResult result = new PluginResult(PluginResult.Status.OK, message);
              result.setKeepCallback(true);
              callback.sendPluginResult(result);
          }



      }
    });
  }

  private boolean getCurrentPosition(JSONObject options, final CallbackContext callback) {
    Log.i(TAG, "请求当前地理位置");
    Context ctx = cordova.getActivity().getApplicationContext();
    PositionOptions positionOpts = new PositionOptions(options);
    if (geolocation == null) {
      geolocation = new BDGeolocation(ctx);
    }
    return geolocation.getCurrentPosition(positionOpts, new BDLocationListener() {
      @Override
      public void onReceiveLocation(BDLocation location) {
        JSONArray message = new MessageBuilder(location).build();
        callback.success(message);
      }
    });
  }

  /**
   * 获取对应权限
   * int requestCode Action代码
   */
  public void getPermission(int requestCode){
    if(!hasPermisssion()){
      PermissionHelper.requestPermissions(this, requestCode, permissions);
    }
  }

  /**
   * 权限请求结果处理函数
   * int requestCode Action代码
   * String[] permissions 权限集合
   * int[] grantResults 授权结果集合
   */
  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                         int[] grantResults) throws JSONException
   {
       PluginResult result;
       //This is important if we're using Cordova without using Cordova, but we have the geolocation plugin installed
       if(context != null) {
           for (int r : grantResults) {
               if (r == PackageManager.PERMISSION_DENIED) {
                   Log.d(TAG, "Permission Denied!");
                   result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                   context.sendPluginResult(result);
                   return;
               }

           }
           switch(requestCode)
           {
               case GET_CURRENT_POSITION:
                   getCurrentPosition(this.requestArgs.getJSONObject(0), this.context);
                   break;
               case WATCH_POSITION:
                   watchPosition(this.requestArgs.getJSONObject(0), this.requestArgs.getInt(1), this.context);
                   break;
               case CLEAR_WATCH:
                   clearWatch(this.requestArgs.getInt(0), this.context);
                   break;
           }
       }
   }

   /**
    * 判断是否有对应权限
    */
   public boolean hasPermisssion() {
       for(String p : permissions)
       {
           if(!PermissionHelper.hasPermission(this, p))
           {
               return false;
           }
       }
       return true;
   }

   /*
    * We override this so that we can access the permissions variable, which no longer exists in
    * the parent class, since we can't initialize it reliably in the constructor!
    */

   public void requestPermissions(int requestCode)
   {
       PermissionHelper.requestPermissions(this, requestCode, permissions);
   }


    private boolean postLBSDetails(String serverURL, final String token, JSONArray message) {


        try {
            JSONObject lbs = message.getJSONObject(0);
            JSONObject coords = lbs.getJSONObject("coords");
            JSONObject json = new JSONObject();
            json.put("lng", coords.getDouble("longitude"));
            json.put("lat", coords.getDouble("latitude"));
            json.put("addr", lbs.getString("address"));
            json.put("status", 0);
            json.put("describe",  lbs.getString("describe"));
            String params = json.toString();
            Log.e(TAG, "post content :" + params);
            byte[] postData = params.getBytes("UTF-8");
            int postDataLength = postData.length;

            URL url = new URL(serverURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Charset", "utf-8");
            conn.setRequestProperty("token", token);
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write( postData );

            InputStream in = new BufferedInputStream(conn.getInputStream());
            String result = readStream(in);

            JSONObject jsonResponse = new JSONObject(result);


            Log.e(TAG, jsonResponse.toString());
        } catch (JSONException e) {

        } catch (MalformedURLException e) {

        } catch (IOException e) {

        }

        return true;
    }
    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

}

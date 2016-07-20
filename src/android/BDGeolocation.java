package com.eteng.geolocation.baidu;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.eteng.geolocation.w3.PositionOptions;

import android.content.Context;

public class BDGeolocation {

  private String TAG = "BDGeolocation";
  private LocationClient client;
  private Object  objLock = new Object();
  public static final String COORD_BD09LL = "bd09ll";
  public static final String COORD_BD09 = "bd09";
  public static final String COORD_GCJ02 = "gcj02";

  private BDLocationListener listener;

  BDGeolocation(Context context) {
    client = new LocationClient(context);
  }

  private void setOptions(PositionOptions options) {
    // set default coorType
    String coorType = options.getCoorType();
    if (coorType == null || coorType.trim().isEmpty()) {
      coorType = COORD_GCJ02;
    }

    // set default locationMode
    LocationMode locationMode = LocationMode.Battery_Saving;
    if (options.isEnableHighAccuracy()) {
      locationMode = LocationMode.Hight_Accuracy;
    }

    long distanceFilter =  options.getDistanceFilter();

    if (distanceFilter <= 0){
      distanceFilter = 100;
    }

    LocationClientOption bdoptions = new LocationClientOption();
    bdoptions.setLocationMode(locationMode);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
    bdoptions.setCoorType(coorType);//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
    bdoptions.setScanSpan(3000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
    bdoptions.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
    bdoptions.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
    bdoptions.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
    bdoptions.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
    bdoptions.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
    bdoptions.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
    bdoptions.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
    bdoptions.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
//    bdoptions.setOpenAutoNotifyMode(10000 ,  (int)distanceFilter, LocationClientOption.LOC_SENSITIVITY_HIGHT);
    client.setLocOption(bdoptions);
  }

  public boolean getCurrentPosition(PositionOptions options, final BDLocationListener callback) {
    listener = new BDLocationListener() {
      @Override
      public void onReceiveLocation(BDLocation location) {
        callback.onReceiveLocation(location);
        clearWatch();
      }
    };
    if (client.isStarted()){
      client.stop();
    }
    setOptions(options);
    client.registerLocationListener(listener);
    client.start();
    return true;
  }

  public boolean watchPosition(PositionOptions options, BDLocationListener callback) {
    listener = callback;
    if (client.isStarted()){
      client.stop();
    }
    setOptions(options);
    client.registerLocationListener(listener);
    start();
    return true;
  }

  public boolean clearWatch() {

    stop();
    client.unRegisterLocationListener(listener);
    listener = null;
    return true;
  }

  public void start(){
    synchronized (objLock) {
      if(client != null && !client.isStarted()){
        client.start();
      }
    }
  }
  public void stop(){
    synchronized (objLock) {
      if(client != null && client.isStarted()){
        client.stop();
      }
    }
  }






}

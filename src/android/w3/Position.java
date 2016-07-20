package com.eteng.geolocation.w3;

import com.baidu.location.BDLocation;

import org.json.JSONException;
import org.json.JSONObject;

public class Position {

  Coordinates coords;
  long timestamp;
  String address;


  public String getDescribe() {
    return describe;
  }

  public void setDescribe(String describe) {
    this.describe = describe;
  }

  String describe;

  public int getLocType() {
    return locType;
  }

  public Position setLocType(int locType) {
    this.locType = locType;
    return this;
  }

  int locType;

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();

    try {
      json.put("timestamp", timestamp);
      json.put("coords", coords.toJSON());
      json.put("address", address);
      json.put("loctype", locType);
      if (getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
        json.put("describe", "gps定位成功");
      } else if (getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
        json.put("describe", "网络定位成功");
      } else if (getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
        json.put("describe", "离线定位成功，离线定位结果也是有效的");
      } else if (getLocType() == BDLocation.TypeServerError) {
        json.put("describe", "服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
      } else if (getLocType() == BDLocation.TypeNetWorkException) {
        json.put("describe", "网络不同导致定位失败，请检查网络是否通畅");
      } else if (getLocType() == BDLocation.TypeCriteriaException) {
        json.put("describe", "无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
      }
      return json;
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return null;
  }

  public Coordinates getCoords() {
    return coords;
  }
  public Position setCoords(Coordinates coords) {
    this.coords = coords;
    return this;
  }
  public long getTimestamp() {
    return timestamp;
  }
  public Position setTimestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public String getAddress() {
    return address;
  }
  public Position setAddress(String address) {
    this.address = address;
    return this;
  }

}

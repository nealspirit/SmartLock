package com.smartlock.android.com.smartlock.android.util;

import android.util.Log;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.smartlock.android.R;
import com.smartlock.android.domain.LockInfo;
import com.smartlock.android.domain.UserInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {

    public static void sendOkHttpRequest(String address, okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }

    public static List<LockInfo> parseJSONWithJSONObjectTolockAddress(String jsonData) {
        List<LockInfo> lockInfoList = new ArrayList<>();

        try{
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++){
                LockInfo lock = new LockInfo();

                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String id = jsonObject.getString("id");
                double longitude = Double.parseDouble(jsonObject.getString("longitude"));
                double latitude = Double.parseDouble(jsonObject.getString("latitude"));

                lock.setId(Integer.parseInt(id));
                lock.setLongitude(longitude);
                lock.setLatitude(latitude);
                lockInfoList.add(lock);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return lockInfoList;
    }

    public static UserInfo parseJSONWithJSONObjectToUserInfo(String jsonData){
        UserInfo user = new UserInfo();

        try{
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                user.setUserName(jsonObject.getString("name"));
                user.setPhoneNumber(jsonObject.getString("phone"));
                user.setIdentity(jsonObject.getString("identity"));
                user.setAddress(jsonObject.getString("address"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return user;
    }
}

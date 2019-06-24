package com.smartlock.android.com.smartlock.android.util;

import android.util.Log;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.smartlock.android.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {

    public static void sendOkHttpRequest(String address, okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }

    public static List<OverlayOptions> parseJSONWithJSONObject(String jsonData) {
        List<OverlayOptions> options = new ArrayList<OverlayOptions>();

        try{
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String id = jsonObject.getString("id");
                double longitude = Double.parseDouble(jsonObject.getString("longitude"));
                double latitude = Double.parseDouble(jsonObject.getString("latitude"));

                //构建Marker图标
                BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.position);
                //创建点
                LatLng point = new LatLng(latitude,longitude);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option = new MarkerOptions()
                        .position(point)
                        .icon(bitmap)
                        .animateType(MarkerOptions.MarkerAnimateType.grow);

                //将OverlayOptions添加到list
                options.add(option);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return options;
    }
}

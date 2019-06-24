package com.smartlock.android;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.bigkoo.pickerview.builder.OptionsPickerBuilder;
import com.bigkoo.pickerview.listener.OnOptionsSelectListener;
import com.bigkoo.pickerview.view.OptionsPickerView;
import com.smartlock.android.com.smartlock.android.util.HttpUtil;
import com.smartlock.android.com.smartlock.android.util.TimeUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    MapStatusUpdate update;

    private Button startTimeBtn;
    private Button stopTimeBtn;
    private ProgressDialog progressDialog;

    private double latitude;
    private double longitude;
    private boolean isFisrtLocate = true;

    private Date date;

    List<String> DateList = new ArrayList<>();
    List<List> TimeLists = new ArrayList<>();
    List<OverlayOptions> options = new ArrayList<OverlayOptions>();

    String [] times = {"0:00","0:30","1:00","1:30","2:00","2:30","3:00","3:30","4:00","4:30","5:00","5:30",
                       "6:00","6:30","7:00","7:30","8:00","8:30","9:00","9:30", "10:00","10:30","11:00","11:30",
                       "12:00","12:30","13:00","13:30","14:00","14:30","15:00","15:30","16:00","16:30","17:00","17:30",
                       "18:00","18:30","19:00","19:30","20:00","20:30","21:00","21:30","22:00","22:30","23:00","23:30"};

    private String startTime = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        SDKInitializer.initialize(getApplicationContext());

        setContentView(R.layout.activity_main);
        startTimeBtn = findViewById(R.id.btn_changeStartTime);
        stopTimeBtn = findViewById(R.id.btn_changeStopTime);
        date = new Date(System.currentTimeMillis());
        startTimeBtn.setText("今天 " + TimeUtil.getCurrentTime(date) + " (现在)");

        //设置点击事件
        startTimeBtn.setOnClickListener(this);
        stopTimeBtn.setOnClickListener(this);
        findViewById(R.id.fab).setOnClickListener(this);

        //获取地图控件引用
        mMapView = findViewById(R.id.bmapView);

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setTrafficEnabled(true);//开启交通图
        mBaiduMap.setMyLocationEnabled(true);//开启地图定位图层
        mMapView.showZoomControls(false);//关闭地图缩放按钮
        //调整地图显示比例尺
        update = MapStatusUpdateFactory.zoomTo(19f);
        mBaiduMap.animateMapStatus(update);

        //申请权限
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(this,permissions,1);
        }else {
            requestLocation();
        }

    }

    private void requestLocation() {
        initLocation();
        mLocationClient.start();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();

        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(5000);

        option.setIsNeedAddress(true);
        //可选，是否需要地址信息，默认为不需要，即参数为false
        //如果开发者需要获得当前点的地址信息，此处必须为true

        mLocationClient.setLocOption(option);
    }

    //设置位置监听接口
    private class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null){
                return;
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
            mBaiduMap.setMyLocationData(locData);

            //获取纬度信息
            latitude = location.getLatitude();
            //获取经度信息
            longitude = location.getLongitude();

            if (isFisrtLocate){
                //更新地图
                LatLng ll = new LatLng(latitude,longitude);
                update = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(update);

                isFisrtLocate = false;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_changeStartTime:
                initDataTimeList(true);

                OptionsPickerView pvOptions_startTime = new OptionsPickerBuilder(this, new OnOptionsSelectListener() {
                    @Override
                    public void onOptionsSelect(int options1, int option2, int options3 ,View v) {
                        //返回的分别是三个级别的选中位置
                        String tx = DateList.get(options1) + " "
                                + TimeLists.get(options1).get(option2);
                        startTimeBtn.setText(tx);
                        stopTimeBtn.setText("预定时间");
                    }
                })
                        .setDecorView((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content))
                        .build();
                pvOptions_startTime.setPicker(DateList,TimeLists);
                pvOptions_startTime.show();
                break;
            case R.id.btn_changeStopTime:
                initDataTimeList(false);

                OptionsPickerView pvOptions_stopTime = new OptionsPickerBuilder(this, new OnOptionsSelectListener() {
                    @Override
                    public void onOptionsSelect(int options1, int option2, int options3 ,View v) {
                        //返回的分别是三个级别的选中位置
                        String tx = DateList.get(options1) + " "
                                + TimeLists.get(options1).get(option2);
                        stopTimeBtn.setText(tx);

                        queryFromServer();

                    }
                })
                        .setDecorView((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content))
                        .build();
                pvOptions_stopTime.setPicker(DateList,TimeLists);
                pvOptions_stopTime.show();
                break;
            case R.id.fab:
                //更新地图
                LatLng ll = new LatLng(latitude,longitude);
                update = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(update);
                update = MapStatusUpdateFactory.zoomTo(19f);//调整地图显示比例尺
                mBaiduMap.animateMapStatus(update);
                break;
            default:
                break;
        }
    }

    private void setMarker(List<OverlayOptions> optionsList){
        //清除地图上的所有覆盖物
        mBaiduMap.clear();

        //在地图上批量添加
        mBaiduMap.addOverlays(options);

        //用来构造InfoWindow的TextView
        //TextView textView = new TextView(getApplicationContext());
        //textView.setText(String.valueOf(number));
        //textView.setTextSize(15);

        //构造InfoWindow
        //InfoWindow mInfoWindow = new InfoWindow(textView,point,-30);
        //使InfoWindow生效
        //mBaiduMap.showInfoWindow(mInfoWindow);

    }

    private void initDataTimeList(boolean isStartTime) {
        DateList.clear();
        TimeLists.clear();
        date = new Date(System.currentTimeMillis());

        List<String> TodayTimeList = new ArrayList<>();
        List<String> TomorrowTimeList = new ArrayList<>();

        int time_hour_string,time_min_string;

        if (isStartTime){
            DateList.add("今天");DateList.add("明天");

            if (isStartTime) {
                TodayTimeList.add(TimeUtil.getCurrentTime(date) + " (现在)");
            }

            time_hour_string = TimeUtil.getCurrentHour(date);
            time_min_string = TimeUtil.getCurrentMin(date);
        }else {
            startTime = startTimeBtn.getText().toString();
            String date_string = startTime.split(" ")[0];
            time_hour_string = Integer.parseInt(startTime.split(" ")[1].split(":")[0]);
            time_min_string = Integer.parseInt(startTime.split(" ")[1].split(":")[1]);

            if (date_string.equals("今天")){
                DateList.add("今天");DateList.add("明天");
            }else if (date_string.equals("明天")){
                DateList.add("明天");DateList.add("后天");
            }
        }

        //添加第一段的时间表
        int a = time_hour_string * 2;
        if (time_min_string >= 0 && time_min_string < 15){
            a = a + 1;
        }else if (time_min_string > 15 && time_min_string < 30){
            a = a + 2;
        }else if (time_min_string >= 30 && time_min_string < 45){
            a = a + 2;
        }else if (time_min_string >= 45 && time_min_string <60){
            a = a + 3;
        }
        for(int i = a ; i < 48 ; i++){
            TodayTimeList.add(times[i]);
        }

        //添加第二段的时间表
        for (int i = 0 ; i < a ; i++){
            TomorrowTimeList.add(times[i]);
        }

        TimeLists.add(TodayTimeList);
        TimeLists.add(TomorrowTimeList);
    }

    private void queryFromServer() {
        showProgressDialog();

        String first_date_string = startTimeBtn.getText().toString().split(" ")[0];
        String first_time_string = startTimeBtn.getText().toString().split(" ")[1];
        String second_date_string = stopTimeBtn.getText().toString().split(" ")[0];
        String second_time_string = stopTimeBtn.getText().toString().split(" ")[1];

        if (first_date_string.equals("今天")){
            first_date_string = "A:";
        }else if (first_date_string.equals("明天")){
            first_date_string = "B:";
        }

        if (second_date_string.equals("今天")){
            second_date_string = "A:";
        }else if (second_date_string.equals("明天")){
            second_date_string = "B:";
        }else if (second_date_string.equals("后天")){
            second_date_string = "C:";
        }

        String address = "http://192.168.1.104:8080/JavaWorkspace_war/FindLockController/findLockByTime?startTime=" + first_date_string + first_time_string + ":00&stopTime=" + second_date_string + second_time_string + ":00";
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        stopTimeBtn.setText("预定时间");
                        Toast.makeText(MainActivity.this,"加载失败，请检查网络后重试",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                if (!TextUtils.isEmpty(responseText)){
                    options = HttpUtil.parseJSONWithJSONObject(responseText);
                    setMarker(options);
                }
                closeProgressDialog();
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载中...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    for (int result : grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"权限未被授权",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else {
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
    }
}

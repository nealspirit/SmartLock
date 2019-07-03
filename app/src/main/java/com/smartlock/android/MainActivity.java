package com.smartlock.android;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.com.baidu.mapapi.ovelayutil.DrivingRouteOverlay;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.bigkoo.pickerview.builder.OptionsPickerBuilder;
import com.bigkoo.pickerview.listener.OnOptionsSelectListener;
import com.bigkoo.pickerview.view.OptionsPickerView;
import com.smartlock.android.com.smartlock.android.util.AnimUtil;
import com.smartlock.android.com.smartlock.android.util.HttpUtil;
import com.smartlock.android.com.smartlock.android.util.LockUtil;
import com.smartlock.android.com.smartlock.android.util.TimeUtil;
import com.smartlock.android.domain.LockInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener, BaiduMap.OnMarkerClickListener {
    public LocationClient mLocationClient = null;
    private MyLocationListener myListener = new MyLocationListener();
    private MapView mMapView = null;
    private BaiduMap mBaiduMap;
    private MapStatusUpdate update;
    private GeoCoder geoCoderSearch;
    private RoutePlanSearch routePlanSearch;

    private Button startTimeBtn;
    private Button stopTimeBtn;
    private ProgressDialog progressDialog;
    private TextView lockInfo;
    private CardView cardView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView nav_header_username;

    private boolean isFisrtLocate = true;

    private LatLng localPoint;
    private LatLng lockPoint;

    public static DrivingRouteOverlay overlay;

    private Date date;
    private LockInfo selectedLock;

    List<String> TimeList = new ArrayList<>();
    List<LockInfo> lockList = new ArrayList<>();
    List<OverlayOptions> options = new ArrayList<OverlayOptions>();

    String [] times = {"0:00","0:30","1:00","1:30","2:00","2:30","3:00","3:30","4:00","4:30","5:00","5:30",
                       "6:00","6:30","7:00","7:30","8:00","8:30","9:00","9:30", "10:00","10:30","11:00","11:30",
                       "12:00","12:30","13:00","13:30","14:00","14:30","15:00","15:30","16:00","16:30","17:00","17:30",
                       "18:00","18:30","19:00","19:30","20:00","20:30","21:00","21:30","22:00","22:30","23:00","23:30"};

    private String startTime = null;

    public static String ServerIP = "http://192.168.1.106:8080";//阿里云IP:http://47.101.35.145:8080
    private String username;
    private String password;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myListener);

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        SDKInitializer.initialize(getApplicationContext());
        //初始化控件
        setContentView(R.layout.activity_main);
        startTimeBtn = findViewById(R.id.btn_changeStartTime);
        stopTimeBtn = findViewById(R.id.btn_changeStopTime);
        date = new Date(System.currentTimeMillis());
        startTimeBtn.setText(TimeUtil.getCurrentTime(date) + " (现在)");
        cardView = findViewById(R.id.cardview);
        lockInfo = findViewById(R.id.tv_lockinfo);
        drawerLayout = findViewById(R.id.drawerlayout);
        navigationView = findViewById(R.id.user_info);
        nav_header_username = navigationView.getHeaderView(0).findViewById(R.id.username);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        //设置点击事件
        startTimeBtn.setOnClickListener(this);
        stopTimeBtn.setOnClickListener(this);
        findViewById(R.id.fab).setOnClickListener(this);
        findViewById(R.id.btn_navigation).setOnClickListener(this);
        findViewById(R.id.btn_openlight).setOnClickListener(this);
        findViewById(R.id.btn_book).setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);
        findViewById(R.id.nav_button).setOnClickListener(this);
        nav_header_username.setOnClickListener(this);

        //获取地图控件引用
        mMapView = findViewById(R.id.bmapView);

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setTrafficEnabled(true);//开启交通图
        mBaiduMap.setMyLocationEnabled(true);//开启地图定位图层
        mMapView.showZoomControls(false);//关闭地图缩放按钮
        //调整地图显示比例尺
        update = MapStatusUpdateFactory.zoomTo(19f);
        mBaiduMap.animateMapStatus(update);

        //设置地图标志点上的点击事件
        mBaiduMap.setOnMarkerClickListener(this);

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

            //获取纬度信息
            double latitude = location.getLatitude();
            //获取经度信息
            double longitude = location.getLongitude();
            //赋值点信息
            localPoint = new LatLng(latitude,longitude);

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection())
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            mBaiduMap.setMyLocationData(locData);

            if (isFisrtLocate){
                //更新地图
                update = MapStatusUpdateFactory.newLatLng(localPoint);
                mBaiduMap.animateMapStatus(update);

                isFisrtLocate = false;
            }
        }
    }

    //创建逆地理编码检索监听器
    OnGetGeoCoderResultListener geoCoderResultListener = new OnGetGeoCoderResultListener() {

        @Override
        public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
            if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                //没有找到检索结果
                return;
            } else {
                String distance_string;

                //车锁序号
                lockInfo.setText(selectedLock.getId() + "号车位\n");

                //详细地址
                lockInfo.append(reverseGeoCodeResult.getAddress() + "\n");

                //获取距离
                double distance = DistanceUtil.getDistance(localPoint,lockPoint);
                if (distance >= 1000){
                    distance_string = String.format("%.2f",distance / 1000);//保存小数点后2位
                    lockInfo.append("距离：" + distance_string + "KM");
                }else {
                    distance_string = String.valueOf((int) distance);
                    lockInfo.append("距离：" + distance_string + "M");
                }
            }
        }
    };

    //创建线路规划检索结果监听器
    OnGetRoutePlanResultListener routePlanResultListener = new OnGetRoutePlanResultListener() {

        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
            if (overlay != null) {
                overlay.removeFromMap();
            }
            overlay = new DrivingRouteOverlay(mBaiduMap);
            if (drivingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条路线为例）
                //为DrivingRouteOverlay实例设置数据
                overlay.setData(drivingRouteResult.getRouteLines().get(0));
                //在地图上绘制DrivingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

        }
    };

    //设置按钮点击事件
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_changeStartTime:
                initDataTimeList(true);

                OptionsPickerView pvOptions_startTime = new OptionsPickerBuilder(this, new OnOptionsSelectListener() {
                    @Override
                    public void onOptionsSelect(int options1, int option2, int options3 ,View v) {
                        //返回的分别是三个级别的选中位置
                        String tx = TimeList.get(options1);
                        startTimeBtn.setText(tx);
                        stopTimeBtn.setText("预定时间");
                    }
                })
                        .setDecorView((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content))
                        .build();
                pvOptions_startTime.setPicker(TimeList);
                pvOptions_startTime.show();
                break;
            case R.id.btn_changeStopTime:
                initDataTimeList(false);
                cardView.setVisibility(View.GONE);

                OptionsPickerView pvOptions_stopTime = new OptionsPickerBuilder(this, new OnOptionsSelectListener() {
                    @Override
                    public void onOptionsSelect(int options1, int option2, int options3 ,View v) {
                        //返回的分别是三个级别的选中位置
                        String tx = TimeList.get(options1);
                        stopTimeBtn.setText(tx);

                        queryFromServerToLockAddress();
                    }
                })
                        .setDecorView((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content))
                        .build();
                pvOptions_stopTime.setPicker(TimeList);
                pvOptions_stopTime.show();
                break;
            case R.id.fab:
                //更新地图
                update = MapStatusUpdateFactory.newLatLng(localPoint);
                mBaiduMap.animateMapStatus(update);
                update = MapStatusUpdateFactory.zoomTo(19f);//调整地图显示比例尺
                mBaiduMap.animateMapStatus(update);
                break;
            case R.id.btn_navigation:
                Toast.makeText(MainActivity.this,"导航",Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_openlight:
                Toast.makeText(MainActivity.this,"开灯",Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_book:
                Toast.makeText(MainActivity.this,"预订",Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_button:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.username:
                if (!TextUtils.isEmpty(username)){
                    Intent intent = new Intent(MainActivity.this,InfoActivity.class);
                    intent.putExtra("flag","个人信息");
                    startActivity(intent);
                }else {
                    Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                    startActivity(intent);
                }
                break;
            default:
                break;
        }
    }

    //设置菜单点击事件
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if (!TextUtils.isEmpty(username)) {
            Intent intent = new Intent(MainActivity.this,InfoActivity.class);

            switch (menuItem.getItemId()){
                case R.id.car:
                    intent.putExtra("flag","我的车辆");
                    break;
                case R.id.bookingLock:
                    intent.putExtra("flag","已预订车位");
                    break;
                case R.id.myLock:
                    intent.putExtra("flag","我的车锁");
                    break;
                default:
                    break;
            }
            Toast.makeText(MainActivity.this,"未完成的功能",Toast.LENGTH_SHORT).show();

            startActivity(intent);
        }else {
            Intent intent = new Intent(MainActivity.this,LoginActivity.class);
            startActivity(intent);
        }

        drawerLayout.closeDrawers();
        return true;
    }

    //设置地图标志物点击事件
    @Override
    public boolean onMarkerClick(Marker marker) {
        lockPoint = marker.getPosition();
        selectedLock = LockUtil.findLockWithlatitude(lockList,lockPoint.latitude,lockPoint.longitude);

        //获取地址
        geoCoderSearch = GeoCoder.newInstance();
        geoCoderSearch.setOnGetGeoCodeResultListener(geoCoderResultListener);
        geoCoderSearch.reverseGeoCode(new ReverseGeoCodeOption().location(lockPoint).radius(500));

        //规划线路
        routePlanSearch = RoutePlanSearch.newInstance();
        routePlanSearch.setOnGetRoutePlanResultListener(routePlanResultListener);
        PlanNode stNode = PlanNode.withLocation(localPoint);
        PlanNode enNode = PlanNode.withLocation(lockPoint);
        routePlanSearch.drivingSearch(new DrivingRoutePlanOption().from(stNode).to(enNode));

        showLockInfoWithAnim();

        return true;
    }

    private void setMarker(){
        //清除地图上的所有覆盖物
        mBaiduMap.clear();
        //清除点数据
        options.clear();

        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.mipmap.position);

        for(LockInfo lock : lockList) {
            //创建点
            LatLng point = new LatLng(lock.getLatitude(), lock.getLongitude());
            //构建MarkerOption，用于在地图上添加Marker
            OverlayOptions option = new MarkerOptions()
                    .position(point)
                    .icon(bitmap)
                    .animateType(MarkerOptions.MarkerAnimateType.grow);

            //将OverlayOptions添加到list
            options.add(option);
        }

        //在地图上批量添加
        mBaiduMap.addOverlays(options);
    }

    private void initDataTimeList(boolean isStartTime) {
        TimeList.clear();
        date = new Date(System.currentTimeMillis());

        int time_hour_string,time_min_string;

        if (isStartTime){
            TimeList.add(TimeUtil.getCurrentTime(date) + " (现在)");

            time_hour_string = TimeUtil.getCurrentHour(date);
            time_min_string = TimeUtil.getCurrentMin(date);
        }else {
            startTime = startTimeBtn.getText().toString();
            time_hour_string = Integer.parseInt(startTime.split(":")[0]);//获取预订起始时间小时数
            time_min_string = Integer.parseInt(startTime.split(":")[1].split(" ")[0]);//获取预订起始时间分钟数
        }

        //添加时间表
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
            TimeList.add(times[i]);
        }

    }

    //给服务器发送地址请求，获取使用使用时间内的车锁信息
    private void queryFromServerToLockAddress() {
        showProgressDialog();

        String first_time_string = startTimeBtn.getText().toString().split(" ")[0];
        String second_time_string = stopTimeBtn.getText().toString();

        String address = ServerIP + "/JavaWorkspace_war/FindLockController/findLockByTime?startTime=A:" +  first_time_string + ":00&stopTime=A:" +  second_time_string + ":00";

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
                    lockList = HttpUtil.parseJSONWithJSONObjectTolockAddress(responseText);
                    setMarker();
                }
                closeProgressDialog();
            }
        });
    }

    private void showLockInfoWithAnim() {
        cardView.setVisibility(View.VISIBLE);
        Animation animation = AnimUtil.getAnimation(this,AnimUtil.SLIDE_IN_BOTTOM);
        cardView.startAnimation(animation);
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
    protected void onStart() {
        super.onStart();
        username = pref.getString("username","");
        password = pref.getString("password","");
        if (!TextUtils.isEmpty(username)){
            nav_header_username.setText(username);
        }else {
            nav_header_username.setText("未登录");
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
        if (mLocationClient != null) {
            mLocationClient.stop();
        }
        if (mBaiduMap != null) {
            mBaiduMap.setMyLocationEnabled(false);
        }
        if (mMapView != null){
            mMapView.onDestroy();
        }
        if (geoCoderSearch != null){
            geoCoderSearch.destroy();
        }
        if (routePlanSearch != null){
            routePlanSearch.destroy();
        }
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
}

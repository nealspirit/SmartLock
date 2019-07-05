package com.smartlock.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.smartlock.android.com.smartlock.android.util.HttpUtil;
import com.smartlock.android.domain.LockInfo;
import com.smartlock.android.domain.UserInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class InfoActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView showInfo;
    private ProgressDialog progressDialog;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private UserInfo user;
    private List<LockInfo> lockInfoList = new ArrayList<>();

    private GeoCoder geoCoderSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Intent intent = getIntent();
        String flag = intent.getStringExtra("flag");

        toolbar = findViewById(R.id.toolbar_info);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setTitle(flag);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        showInfo = findViewById(R.id.infoActivity_textview);

        String userId = pref.getString("userId","");
        String username = pref.getString("username","");
        String password = pref.getString("password","");

        //设置注销按钮点击事件
        findViewById(R.id.button_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor = pref.edit();
                editor.clear();
                editor.apply();
                finish();
            }
        });

        switch (flag){
            case "个人信息":
                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                    queryFromServerToUserInfo(username,password,flag);
                }else {
                    editor = pref.edit();
                    editor.clear();
                    editor.apply();
                    finish();
                }
                break;
            case "我的车辆":
                showInfo.setText("null");
                break;
            case "已预订车位":
                queryFromServerToBookLock(userId);
                break;
            case "我的车锁":
                showInfo.setText("null");
                break;
        }

    }

    //给服务器发送请求，获取用户信息
    private void queryFromServerToUserInfo(final String userName, final String password , final String flag){
        String address = MainActivity.ServerIP + "/JavaWorkspace_war/UserController/signInUser?name=" + userName + "&password=" + password;
        showProgressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(InfoActivity.this,"连接超时",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();

                if (!TextUtils.isEmpty(responseText)){
                    responseText = "[" + responseText + "]";
                    user = HttpUtil.parseJSONWithJSONObjectToUserInfo(responseText);
                    user.setPassword(password);
                    closeProgressDialog();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showInfo.setText("手机号：" + "\n");
                            showInfo.append(user.getPhoneNumber() + "\n");
                            showInfo.append("地址：" + "\n");
                            showInfo.append(user.getAddress() + "\n");
                            showInfo.append("证件号：" + "\n");
                            showInfo.append(user.getIdentity() + "\n");
                        }
                    });
                }else {
                    editor.clear();
                    editor.apply();
                    closeProgressDialog();
                    finish();
                }
            }
        });
    }

    //给服务器发送请求，获取已预订车锁信息
    private void queryFromServerToBookLock(String userId) {
        String address = MainActivity.ServerIP + "/JavaWorkspace_war/LockController/findBookedLock?userId=" + userId;
        showProgressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(InfoActivity.this,"连接超时",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();

                if (!TextUtils.isEmpty(responseText)){
                    lockInfoList = HttpUtil.parseJSONWithJSONObjectTolockAddress(responseText);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showInfo.setText("");
                            for (LockInfo lock : lockInfoList) {
                                closeProgressDialog();
                                showInfo.append(lock.getId() + "号车位" + "\n");
                                showInfo.append("地址：" + "\n");
                                getLockAddressToTextView(lock);
                            }
                        }
                    });
                }else {
                    editor.clear();
                    editor.apply();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            finish();
                        }
                    });
                }
            }
        });
    }

    private void getLockAddressToTextView(LockInfo lock) {
        LatLng lockPoint = new LatLng(lock.getLatitude(),lock.getLongitude());

        //获取地址
        geoCoderSearch = GeoCoder.newInstance();
        geoCoderSearch.setOnGetGeoCodeResultListener(geoCoderResultListener);
        geoCoderSearch.reverseGeoCode(new ReverseGeoCodeOption().location(lockPoint).radius(500));
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
                //详细地址
                showInfo.append(reverseGeoCodeResult.getAddress() + "\n");
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geoCoderSearch != null){
            geoCoderSearch.destroy();
        }
    }
}

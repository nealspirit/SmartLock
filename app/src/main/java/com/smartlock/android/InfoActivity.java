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

import com.smartlock.android.com.smartlock.android.util.HttpUtil;
import com.smartlock.android.domain.UserInfo;

import java.io.IOException;

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

        findViewById(R.id.button_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor = pref.edit();
                editor.clear();
                editor.apply();
                finish();
            }
        });

        String username = pref.getString("username","");
        String password = pref.getString("password","");

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            queryFromServerToUserInfo(username,password,flag);
        }else {
            editor = pref.edit();
            editor.clear();
            editor.apply();
            finish();
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
                            if (flag.equals("个人信息")) {
                                showInfo.setText("手机号：" + "\n");
                                showInfo.append(user.getPhoneNumber() + "\n");
                                showInfo.append("地址：" + "\n");
                                showInfo.append(user.getAddress() + "\n");
                                showInfo.append("证件号：" + "\n");
                                showInfo.append(user.getIdentity() + "\n");
                            }else if (flag.equals("我的车辆")) {
                                showInfo.setText("null");
                            }else if (flag.equals("已预订车位")) {
                                showInfo.setText("null");
                            }else if (flag.equals("我的车辆")) {
                                showInfo.setText("null");
                            }
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

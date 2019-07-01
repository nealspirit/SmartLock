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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.smartlock.android.com.smartlock.android.util.HttpUtil;
import com.smartlock.android.domain.UserInfo;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText accountEdit;
    private EditText passwordEdit;
    private TextView wrongMessage;
    private ProgressDialog progressDialog;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private UserInfo user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar_login);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setTitle("登陆");

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        accountEdit = findViewById(R.id.et_account);
        passwordEdit = findViewById(R.id.et_password);
        wrongMessage = findViewById(R.id.wrong_message);

        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = accountEdit.getText().toString();
                String password = passwordEdit.getText().toString();
                queryFromServerToUserInfo(username,password);
            }
        });

        findViewById(R.id.btn_signin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this,SigninActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    //给服务器发送请求，获取用户信息
    private void queryFromServerToUserInfo(final String userName, final String password){
        String address = MainActivity.ServerIP + "/JavaWorkspace_war/UserController/signInUser?name=" + userName + "&password=" + password;
        showProgressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(LoginActivity.this,"连接超时",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                editor = pref.edit();

                if (!TextUtils.isEmpty(responseText)){
                    responseText = "[" + responseText + "]";
                    user = HttpUtil.parseJSONWithJSONObjectToUserInfo(responseText);
                    user.setPassword(password);
                    editor.putString("username",user.getUserName());
                    editor.putString("password",user.getPassword());
                    editor.apply();
                    closeProgressDialog();
                    finish();
                }else {
                    wrongMessage.setVisibility(View.VISIBLE);
                    passwordEdit.setText("");
                    editor.clear();
                    editor.apply();
                    closeProgressDialog();
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

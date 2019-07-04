package com.smartlock.android;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.mcxtzhang.captchalib.SwipeCaptchaView;
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

    private SwipeCaptchaView mSwipeCaptchaView;
    private SeekBar mSeekBar;
    private AlertDialog checkDialog;

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
                //隐藏软键盘
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

                String username = accountEdit.getText().toString();
                String password = passwordEdit.getText().toString();

                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                    checkIsHuman(username,password);
                }else {
                    Toast.makeText(LoginActivity.this,"账号或密码为空！",Toast.LENGTH_SHORT).show();
                    passwordEdit.setText("");
                }
            }
        });

        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void checkIsHuman(String username,String password) {
        final View checkview = getLayoutInflater().inflate(R.layout.check_layout,null);
        checkDialog = new AlertDialog.Builder(LoginActivity.this)
                .setView(checkview)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {

                    }
                })
                .show();

        initCheckView(checkview,username,password);
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
                    wrongMessage.setVisibility(View.GONE);

                    responseText = "[" + responseText + "]";
                    user = HttpUtil.parseJSONWithJSONObjectToUserInfo(responseText);
                    user.setPassword(password);

                    editor.putString("userId",user.getUserId());
                    editor.putString("username",user.getUserName());
                    editor.putString("password",user.getPassword());
                    editor.apply();

                    closeProgressDialog();
                    finish();
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            wrongMessage.setVisibility(View.VISIBLE);
                            passwordEdit.setText("");
                            editor.clear();
                            editor.apply();
                            closeProgressDialog();
                        }
                    });
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

    private void initCheckView(View view, final String username, final String password) {
        mSwipeCaptchaView = view.findViewById(R.id.swipeCaptchaView);
        mSeekBar = view.findViewById(R.id.dragBar);

        //设置匹配回调
        mSwipeCaptchaView.setOnCaptchaMatchCallback(new SwipeCaptchaView.OnCaptchaMatchCallback() {
            @Override
            public void matchSuccess(SwipeCaptchaView swipeCaptchaView) {
                mSeekBar.setEnabled(false);
                checkDialog.dismiss();
                queryFromServerToUserInfo(username,password);
            }

            @Override
            public void matchFailed(SwipeCaptchaView swipeCaptchaView) {
                Log.d("zxt", "matchFailed() called with: swipeCaptchaView = [" + swipeCaptchaView + "]");
                Toast.makeText(LoginActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
                mSwipeCaptchaView.createCaptcha();
                mSeekBar.setEnabled(true);
                mSeekBar.setProgress(0);
            }
        });

        //设置滑块监听器
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSwipeCaptchaView.setCurrentSwipeValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //随便放这里是因为控件
                mSeekBar.setMax(mSwipeCaptchaView.getMaxSwipeValue());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("zxt", "onStopTrackingTouch() called with: seekBar = [" + seekBar + "]");
                mSwipeCaptchaView.matchCaptcha();
            }
        });

        Bitmap resource = BitmapFactory.decodeResource(getResources(),R.drawable.pic11);
        mSwipeCaptchaView.setImageBitmap(resource);
    }
}

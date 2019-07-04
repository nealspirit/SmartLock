package com.smartlock.android;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.mcxtzhang.captchalib.SwipeCaptchaView;
import com.smartlock.android.com.smartlock.android.util.HttpUtil;
import com.smartlock.android.domain.UserInfo;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText accountEdit;
    private EditText passwordEdit;
    private EditText passwordConfirmEdit;
    private EditText phoneEdit;
    private EditText addressEdit;
    private EditText identityEdit;
    private TextView wrongUsernameText;
    private TextView wrongPasswordText;
    private ProgressDialog progressDialog;

    private SwipeCaptchaView mSwipeCaptchaView;
    private SeekBar mSeekBar;
    private AlertDialog checkDialog;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //初始化toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_register);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setTitle("注册");
        //初始化控件
        accountEdit = findViewById(R.id.et_account);
        passwordEdit = findViewById(R.id.et_password);
        passwordConfirmEdit = findViewById(R.id.et_password_reconfirm);
        phoneEdit = findViewById(R.id.et_phone);
        addressEdit = findViewById(R.id.et_address);
        identityEdit = findViewById(R.id.et_identity);
        wrongUsernameText = findViewById(R.id.wrong_username_message);
        wrongPasswordText = findViewById(R.id.wrong_password_message);

        //初始化preferences
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //隐藏软键盘
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

                String username = accountEdit.getText().toString();
                String password = passwordEdit.getText().toString();
                String phone = phoneEdit.getText().toString();
                String address = addressEdit.getText().toString();
                String identity = identityEdit.getText().toString();

                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(phone) && !TextUtils.isEmpty(address) && !TextUtils.isEmpty(identity)) {
                    if (passwordConfirmEdit.getText().toString().equals(password)){        //判断确认密码框与密码框中数据是否相同
                        wrongPasswordText.setVisibility(View.GONE);

                        UserInfo registerUser = new UserInfo();

                        registerUser.setUserName(username);
                        registerUser.setPassword(password);
                        registerUser.setPhoneNumber(phone);
                        registerUser.setAddress(address);
                        registerUser.setIdentity(identity);

                        checkIsHuman(registerUser);
                    }else {
                        wrongPasswordText.setVisibility(View.VISIBLE);
                        passwordEdit.setText("");
                        passwordConfirmEdit.setText("");
                    }

                }else {
                    Toast.makeText(RegisterActivity.this,"不能有空白项！",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void checkIsHuman(UserInfo registerUser) {
        final View checkview = getLayoutInflater().inflate(R.layout.check_layout,null);
        checkDialog = new AlertDialog.Builder(RegisterActivity.this)
                .setView(checkview)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {

                    }
                })
                .show();

        initCheckView(checkview,registerUser);
    }

    private void initCheckView(View view, final UserInfo registerUser) {
        mSwipeCaptchaView = view.findViewById(R.id.swipeCaptchaView);
        mSeekBar = view.findViewById(R.id.dragBar);

        //设置匹配回调
        mSwipeCaptchaView.setOnCaptchaMatchCallback(new SwipeCaptchaView.OnCaptchaMatchCallback() {
            @Override
            public void matchSuccess(SwipeCaptchaView swipeCaptchaView) {
                mSeekBar.setEnabled(false);
                checkDialog.dismiss();
                queryFromServerToRegisterUser(registerUser);
            }

            @Override
            public void matchFailed(SwipeCaptchaView swipeCaptchaView) {
                Log.d("zxt", "matchFailed() called with: swipeCaptchaView = [" + swipeCaptchaView + "]");
                Toast.makeText(RegisterActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
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

    private void queryFromServerToRegisterUser(final UserInfo registerUser) {
        String address = MainActivity.ServerIP + "/JavaWorkspace_war/UserController/insertUser?name=" + registerUser.getUserName() + "&password=" + registerUser.getPassword() + "&phone=" + registerUser.getPhoneNumber() + "&identity=" + registerUser.getIdentity() + "&address=" + registerUser.getAddress();
        showProgressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(RegisterActivity.this,"连接超时",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                editor = pref.edit();

                if (!TextUtils.isEmpty(responseText)){
                    if (responseText.equals("repeat")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                wrongUsernameText.setVisibility(View.VISIBLE);
                                closeProgressDialog();
                            }
                        });
                    }else {
                        wrongUsernameText.setVisibility(View.GONE);

                        //将用户数据存入缓存中
                        editor.putString("userId",responseText);
                        editor.putString("username",registerUser.getUserName());
                        editor.putString("password",registerUser.getPassword());
                        editor.apply();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RegisterActivity.this,"注册成功",Toast.LENGTH_SHORT).show();
                                closeProgressDialog();
                                finish();
                            }
                        });
                    }
                }else {
                    editor.clear();
                    editor.apply();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this,"服务器连接异常，请重试",Toast.LENGTH_SHORT).show();
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
}

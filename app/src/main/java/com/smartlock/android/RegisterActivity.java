package com.smartlock.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

public class RegisterActivity extends AppCompatActivity {
    private String address = MainActivity.ServerIP + "/JavaWorkspace_war/UserController/insertUser?name=**&phone=**&identity=**&address=**";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar_register);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setTitle("注册");
    }
}

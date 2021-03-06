package com.cooloongwu.coolchat.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cooloongwu.coolchat.R;
import com.cooloongwu.coolchat.base.AppConfig;
import com.cooloongwu.coolchat.base.MyService;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText edit_phone;
    private EditText edit_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initToolbar();
        initViews();
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("登录");
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initViews() {
        Button btn_login = (Button) findViewById(R.id.btn_login);
        btn_login.setOnClickListener(this);

        edit_phone = (EditText) findViewById(R.id.edit_phone);
        edit_password = (EditText) findViewById(R.id.edit_password);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                checkInput();
                break;
            default:
                break;
        }
    }

    private void checkInput() {
        String phone = edit_phone.getText().toString().trim();
        String password = edit_password.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "手机号或者密码不可为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("1".equals(phone)) {
            saveUserProfile("1");
        } else {
            saveUserProfile("2");
        }
    }

    private void saveUserProfile(String str) {
        if ("1".equals(str)) {
            AppConfig.setUserId(LoginActivity.this, 1);
            AppConfig.setUserName(LoginActivity.this, "CooLoongWu1");
            AppConfig.setUserAvatar(LoginActivity.this, "http://www.qqzhi.com/uploadpic/2015-01-11/130629886.jpg");
            AppConfig.setUserSex(LoginActivity.this, "男");
        } else {
            AppConfig.setUserId(LoginActivity.this, 2);
            AppConfig.setUserName(LoginActivity.this, "CooLoongWu2");
            AppConfig.setUserAvatar(LoginActivity.this, "http://img4.duitang.com/uploads/item/201402/26/20140226221725_XrJyt.thumb.200_0.jpeg");
            AppConfig.setUserSex(LoginActivity.this, "女");
        }
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();

        Intent intent = new Intent(LoginActivity.this, MyService.class);
        startService(intent);
    }
}

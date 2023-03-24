package com.yannuo.dgcanteen.activitys;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.util.ToastShowUtil;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etAddress, etMqttAddress, etMqttAccount, etMqttPassword;
    private CheckBox switchLine;
    private TextView tvVersion;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().setAttributes(params);
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
        if (Build.VERSION.SDK_INT >= 19) {
            uiFlags |= 0x00001000;  //SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide navigation bars - compatibility: building API level is lower thatn 19, use magic number directly for higher API target level
        } else {
            uiFlags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
        setContentView(R.layout.activity_setting);
        preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        init();
        reload();
    }

    private void reload() {
        etAddress.setText(preferences.getString("Address",""));
        switchLine.setChecked(preferences.getBoolean("Switch",false));
        etMqttAddress.setText(preferences.getString("MqttAddress",""));
        etMqttAccount.setText(preferences.getString("MqttAccount",""));
        etMqttPassword.setText(preferences.getString("MqttPassword",""));
        tvVersion.setText(preferences.getString("Version",""));
    }

    private void save(){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Address",etAddress.getText().toString());
        editor.putBoolean("Switch",switchLine.isChecked());
        editor.putString("MqttAddress",etMqttAddress.getText().toString());
        editor.putString("MqttAccount",etMqttAccount.getText().toString());
        editor.putString("MqttPassword",etMqttPassword.getText().toString());
        editor.putString("Version",tvVersion.getText().toString());
        editor.commit();
    }

    private void init(){
        findViewById(R.id.ibt_back).setOnClickListener(this);
        findViewById(R.id.btn_save).setOnClickListener(this);
        etAddress = findViewById(R.id.et_address);
        switchLine = findViewById(R.id.switch_line);
        etMqttAddress = findViewById(R.id.et_mqtt_address);
        etMqttAccount = findViewById(R.id.et_mqtt_account);
        etMqttPassword = findViewById(R.id.et_mqtt_password);
        findViewById(R.id.dish_menu).setOnClickListener(this);
        findViewById(R.id.syn_dishes).setOnClickListener(this);
        findViewById(R.id.btn_version).setOnClickListener(this);
        tvVersion = findViewById(R.id.tv_version);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.dish_menu:
                Intent intent = new Intent(this,DishManageActivity.class);
                startActivity(intent);
                break;
            case R.id.syn_dishes:

                break;
            case R.id.btn_version:
                try {
                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),0);
                    tvVersion.setText("当前版本：V" + packageInfo.versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
                break;
            case R.id.btn_save:
                save();
                ToastShowUtil.show(this,"保存成功");
                break;
            case R.id.ibt_back:
                finish();
                break;
            default:break;
        }
    }
}
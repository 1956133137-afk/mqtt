package com.yannuo.dgcanteen.activitys;

import android.content.Intent;
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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.download.CheckVersionWorker;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.nets.RetrofitClient;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.ToastShowUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etAddress, etMqttAddress, etMqttAccount, etMqttPassword;
    private CheckBox switchLine;
    private TextView tvVersion, tvFinalTime;
    private MMKV kv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();
        setContentView(R.layout.activity_setting);


        kv = MMKV.defaultMMKV();

        init();
        try {
            tvVersion.setText(getPackageManager().getPackageInfo(getPackageName(),0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        reload();
    }

    private void reload() {
        etAddress.setText(kv.decodeString(Constant.ADDRESS));
        switchLine.setChecked(kv.decodeBool(Constant.SWITCH,false));
        etMqttAddress.setText(kv.decodeString(Constant.MQTT_ADDRESS));
        etMqttAccount.setText(kv.decodeString(Constant.MQTT_ACCOUNT));
        etMqttPassword.setText(kv.decodeString(Constant.MQTT_PASSWORD));
//        tvVersion.setText(kv.decodeString("Version"));
        tvFinalTime.setText(kv.decodeString(Constant.FINAL_TIME));
    }

    private void save(){
        boolean change = true;
        change = kv.decodeString(Constant.ADDRESS).equals(etAddress.getText().toString());
        if (!change) {
            kv.encode(Constant.ADDRESS, etAddress.getText().toString());
            RetrofitClient.overLoad(); //更新服务器地址
        }

        kv.encode(Constant.SWITCH,switchLine.isChecked());

        change = true;
        if (!kv.decodeString(Constant.MQTT_ADDRESS).equals(etMqttAddress.getText().toString())) {
            change = false;
            kv.encode(Constant.MQTT_ADDRESS,etMqttAddress.getText().toString());
        }
        if (!kv.decodeString(Constant.MQTT_ACCOUNT).equals(etMqttAccount.getText().toString())) {
            change = false;
            kv.encode(Constant.MQTT_ACCOUNT,etMqttAccount.getText().toString());
        }

        if (!kv.decodeString(Constant.MQTT_PASSWORD).equals(etMqttPassword.getText().toString())) {
            change = false;
            kv.encode(Constant.MQTT_PASSWORD,etMqttPassword.getText().toString());
        }
        if (!change){
            //mqtt配置变更
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_NINTH,null));
        }

//        kv.encode("Version",tvVersion.getText().toString());
        ToastShowUtil.show(this,"保存成功:" + this.getFilesDir().getAbsolutePath() + "/mmkv");
    }

    private void init(){
        findViewById(R.id.ibt_back).setOnClickListener(this);
        findViewById(R.id.btn_save).setOnClickListener(this);
        etAddress = findViewById(R.id.et_address);
        switchLine = findViewById(R.id.switch_line);
        etMqttAddress = findViewById(R.id.et_mqtt_address);
        etMqttAccount = findViewById(R.id.et_mqtt_account);
        etMqttPassword = findViewById(R.id.et_mqtt_password);
        findViewById(R.id.btn_version).setOnClickListener(this);
        tvVersion = findViewById(R.id.tv_version);
        tvFinalTime = findViewById(R.id.tv_final_time);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_version:
                PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                        CheckVersionWorker.class,
                        15,
                        TimeUnit.MINUTES
                ).build();

                WorkManager.getInstance(this).enqueueUniquePeriodicWork(Constant.PERIODIC_WORK_KEY, ExistingPeriodicWorkPolicy.REPLACE,work);

                break;
            case R.id.btn_save:
                save();
                break;
            case R.id.ibt_back:
                finish();
                break;
            default:break;
        }
    }

    private void initScreen(){
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
    }
}
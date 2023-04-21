package com.yannuo.dgcanteen.activitys;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.databinding.ActivitySettingBinding;
import com.yannuo.dgcanteen.download.CheckVersionWorker;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.nets.RetrofitClient;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.ToastShowUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

public class SettingActivity extends AppCompatActivity {

    private ActivitySettingBinding binding;
    private MMKV kv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();
        initObject();
        initView();
        initEvent();
        initData();
    }

    private void initView(){
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initObject() {
        kv = MMKV.defaultMMKV();
    }

    private void initData(){
        try {
            binding.tvVersion.setText(getPackageManager().getPackageInfo(getPackageName(),0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        reload();
    }

    private void initEvent(){
        binding.btnVersion.setOnClickListener(view -> { //版本更新
            PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                    CheckVersionWorker.class,
                    15,
                    TimeUnit.MINUTES
            ).build();

            ToastShowUtil.show(this,"正在检查版本是否需要更新~");
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(Constant.PERIODIC_WORK_KEY, ExistingPeriodicWorkPolicy.REPLACE,work);
        });

        binding.btnSynPerson.setOnClickListener(view -> { //同步人员信息

        });

        binding.btnExitAlive.setOnClickListener(view -> { //退出保活

        });

        binding.btnSave.setOnClickListener(view -> { //保存信息
            save();
        });

        binding.ibtBack.setOnClickListener(view -> { //返回
            finish();
        });
    }

    private void reload() {
        binding.etAddress.setText(kv.decodeString(Constant.ADDRESS));
        binding.switchLine.setChecked(kv.decodeBool(Constant.SWITCH,false));
        binding.etMqttAddress.setText(kv.decodeString(Constant.MQTT_ADDRESS));
        binding.etMqttAccount.setText(kv.decodeString(Constant.MQTT_ACCOUNT));
        binding.etMqttPassword.setText(kv.decodeString(Constant.MQTT_PASSWORD));
        binding.tvFinalTime.setText(kv.decodeString(Constant.FINAL_TIME));
    }

    private void save(){
        boolean change = true;
        change = kv.decodeString(Constant.ADDRESS).equals(binding.etAddress.getText().toString());
        if (!change) {
            kv.encode(Constant.ADDRESS, binding.etAddress.getText().toString());
            RetrofitClient.overLoad(); //更新服务器地址
        }

        kv.encode(Constant.SWITCH,binding.switchLine.isChecked());

        change = true;
        if (!kv.decodeString(Constant.MQTT_ADDRESS).equals(binding.etMqttAddress.getText().toString())) {
            change = false;
            kv.encode(Constant.MQTT_ADDRESS,binding.etMqttAddress.getText().toString());
        }
        if (!kv.decodeString(Constant.MQTT_ACCOUNT).equals(binding.etMqttAccount.getText().toString())) {
            change = false;
            kv.encode(Constant.MQTT_ACCOUNT,binding.etMqttAccount.getText().toString());
        }

        if (!kv.decodeString(Constant.MQTT_PASSWORD).equals(binding.etMqttPassword.getText().toString())) {
            change = false;
            kv.encode(Constant.MQTT_PASSWORD,binding.etMqttPassword.getText().toString());
        }
        if (!change){
            //mqtt配置变更
            EventBus.getDefault().post(new MessageEvent(Constant.EVENT_NINTH,null));
        }
        ToastShowUtil.show(this,"保存成功:" + this.getFilesDir().getAbsolutePath() + "/mmkv");
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
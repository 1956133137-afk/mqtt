package com.yannuo.dgcanteen.service;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;


@SuppressLint("SpecifyJobSchedulerIdRange")
public class KeepAliveJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            // 如果当前设备大于 7.0 , 延迟 5 秒 , 再次执行一次
            startJob(this);
        }

        // 判定远程前台进程是否正在运行
        boolean isRemoteServiceRunning = false;
        try {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> list = null;
            if (activityManager != null) {
                list = activityManager.getRunningTasks(100);
            }
            if (list != null && list.size() > 0) {
                for (ActivityManager.RunningTaskInfo info : list) {
                    if (info.baseActivity.getPackageName().equals(getPackageName())) {
                        isRemoteServiceRunning = true;
                    }
                }
            }

            if (!isRemoteServiceRunning) {
                Intent lIntentt = getPackageManager().getLaunchIntentForPackage(getPackageName());
                lIntentt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(lIntentt);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i("KeepAliveJobService", "JobService onStopJob 关闭");
        return false;
    }

    public static void startJob(Context context){
        // 创建 JobScheduler
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // 第一个参数指定任务 ID
        // 第二个参数指定任务在哪个组件中执行
        // setPersisted 方法需要 android.permission.RECEIVE_BOOT_COMPLETED 权限
        // setPersisted 方法作用是设备重启后 , 依然执行 JobScheduler 定时任务
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(10,
                new ComponentName(context.getPackageName(), KeepAliveJobService.class.getName()))
                .setPersisted(true);
        jobInfoBuilder.setMinimumLatency(40_000);
//        }
        // 开启定时任务
        jobScheduler.schedule(jobInfoBuilder.build());

    }
}

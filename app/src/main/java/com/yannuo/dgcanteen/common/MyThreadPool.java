package com.yannuo.dgcanteen.common;




import com.yannuo.dgcanteen.util.LogUtil;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyThreadPool {
    private static MyThreadPool instance;
//    private static ExecutorService mExecutorService;
    private static ThreadPoolExecutor mExecutorService;
    private byte corePoolSize = 3;
    private byte  maxPoolSize = 6;
    private byte liveTime = 30;
    private byte capacity = 8;
    private String TAG = getClass().getSimpleName();

    private LinkedBlockingDeque<Runnable> mQueue ;

    private MyThreadPool(){
        mQueue = new LinkedBlockingDeque(capacity);
        mExecutorService = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                liveTime, TimeUnit.SECONDS, mQueue,(r, executor) -> {
            LogUtil.d(TAG, "MyThreadPool run onetime rejectedExecution");
            new Thread(r, "rejectedExecution Thread ").start();
        });
        LogUtil.d(TAG,"MyThreadPool 构造完成");
    }

    public static ThreadPoolExecutor getInstance(){
        if (instance == null){
            synchronized(MyThreadPool.class){
                instance = new MyThreadPool();
            }
        }
        LogUtil.d(instance.TAG, "当前线程活跃数: "+mExecutorService.getPoolSize()+"" +
                "\n队列任务数量:"+instance.mQueue.size());
        return mExecutorService;
    }
}

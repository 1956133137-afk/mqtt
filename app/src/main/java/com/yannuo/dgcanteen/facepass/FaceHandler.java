package com.yannuo.dgcanteen.facepass;

import android.content.Context;


public class FaceHandler {
    private static CameraManager mCameraManager = null;
    private static FaceHandler mFaceHandler = null;



    volatile private boolean lock = false;

    public  boolean isFaceInit() {
        return faceInit;
    }

    public  void setFaceInit(boolean faceInit) {
      this.faceInit = faceInit;
    }

    private volatile boolean faceInit = false ; //设备算法是否已初始化


    private FaceHandler(){}

    public static CameraManager getInstance(Context context) {
        if (mCameraManager == null) {
            synchronized (CameraManager.class) {
                mCameraManager = new CameraManager.Builder(context).build();
            }
        }
        return mCameraManager;
    }

    public static CameraManager getInstance() {
        return mCameraManager;
    }

    public static FaceHandler getFaceHInstance(){
        if (mFaceHandler == null) {
            synchronized (FaceHandler.class) {
                mFaceHandler = new FaceHandler();

            }
        }
        return mFaceHandler;
    }

    /**
     * 加锁防止二次调用
     * @param lock true:锁定，页面未关闭将忽略第二次之后发起
     */
    public void setLock(boolean lock){
        this.lock = lock;
    }

    public boolean getLock(){
        return this.lock;
    }

}
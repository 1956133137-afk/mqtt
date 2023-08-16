//package com.yannuo.dgcanteen.facepass;
//
//import android.content.Context;
//import android.graphics.ImageFormat;
//import android.graphics.PixelFormat;
//import android.graphics.Rect;
//import android.hardware.Camera;
//import android.os.AsyncTask;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//
//import com.proembed.service.MyService;
//import com.tencent.mmkv.MMKV;
//import com.yannuo.dgcanteen.util.Constant;
//import com.yannuo.dgcanteen.util.LogUtil;
//
//import java.security.InvalidParameterException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import mcv.facepass.FacePassHandler;
//
//
//public class CameraManager {
//    private int colorPreViewWidth ;   //预览宽度
//    private int colorPreViewHeight ;  //预览高度
//    private int blackWhitePreViewWidth ;   //黑白预览宽度
//    private int blackWhitePreViewHeight ;  //黑白预览高度
//
//    private int colorCameraId  ;     //彩色摄像头
//    private int colorPreRotation ;       //彩色预览旋转
//    private int colorCameraRotation ;       //彩色摄像头旋转
//    private int blackWhitePreRotation ;       //黑白预览旋转
//    private int blackWhiteCameraRotation ;       //黑白摄像头旋转
//    private boolean colorMirror ;         //彩色摄像头镜像
//    private boolean blackWhiteMirror ;         //黑白摄像头镜像
//    private Context mContext;
//
//
//    //    protected boolean front = false;
//    private String TAG = getClass().getSimpleName();
//    protected Camera cameraOne = null,cameraTwo = null;
//    //    protected Camera camera1 = null,camera2 = null;
//    protected short cameraId = -1;
//    protected SurfaceHolder surfaceHolder = null;
//    private CameraState state = CameraState.IDEL;
//    //    private short previewDegreen = 0;
//    //    private byte[] mPicBuffer;
//    private short angle;
//
//    private short wan_Height ;
//    private short wan_Width ;
//
//    private CameraDataStream cameraData;
//    private FacePass mFacePass;
//    //    private CameraPreview cameraPreview;
//    private SurfaceView cameraPreview;
//    protected Camera camera = null;
//
//    private CameraManager(Builder builder) {
//        mContext = builder.context;
//        this.colorPreViewWidth = builder.colorPreViewWidth;   //预览宽度
//        this.colorPreViewHeight =  builder.colorPreViewHeight;  //预览高度
//        this.blackWhitePreViewWidth = builder.blackWhitePreViewWidth;   //预览宽度
//        this.blackWhitePreViewHeight =  builder.blackWhitePreViewHeight;  //预览高度
//
//        this.colorCameraId = builder.colorCameraId;     //彩色摄像头
//        this.colorPreRotation = builder.colorPreRotation;       //彩色预览旋转
//        this.colorCameraRotation = builder.colorCameraRotation;       //彩色摄像头旋转
//        this.blackWhitePreRotation = builder.blackWhitePreRotation;       //黑白预览旋转
//        this.blackWhiteCameraRotation = builder.blackWhiteCameraRotation;       //黑白摄像头旋转
//        this.colorMirror = builder.colorMirror;         //彩色摄像头镜像
//        this.blackWhiteMirror = builder.blackWhiteMirror;         //黑白摄像头镜像
//
//
//    }
//
//    public void setCameraConfig(Builder builder){
//        this.colorPreViewWidth = builder.colorPreViewWidth;   //预览宽度
//        this.colorPreViewHeight =  builder.colorPreViewHeight;  //预览高度
//        this.blackWhitePreViewWidth = builder.blackWhitePreViewWidth;   //预览宽度
//        this.blackWhitePreViewHeight =  builder.blackWhitePreViewHeight;  //预览高度
//
//        this.colorCameraId = builder.colorCameraId;     //彩色摄像头
//        this.colorPreRotation = builder.colorPreRotation;       //彩色预览旋转
//        this.colorCameraRotation = builder.colorCameraRotation;       //彩色摄像头旋转
//        this.blackWhitePreRotation = builder.blackWhitePreRotation;       //黑白预览旋转
//        this.blackWhiteCameraRotation = builder.blackWhiteCameraRotation;       //黑白摄像头旋转
//        this.colorMirror = builder.colorMirror;         //彩色摄像头镜像
//        this.blackWhiteMirror = builder.blackWhiteMirror;         //黑白摄像头镜像
//    }
//
//
//    private boolean isSupportedPreviewSize(int width, int height, Camera.Parameters camPara) {
////        Camera.Parameters camPara = mCamera.getParameters();
//        List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
//        for (Camera.Size tmpSize : allSupportedSize) {
//            Log.i("metrics", "support height" + tmpSize.height + "width " + tmpSize.width);
//            if (tmpSize.height == height && tmpSize.width == width)
//                return true;
//        }
//        return false;
//    }
//
//
//    public CameraManager initAlgorithm( SDKInitResult callback ) {
//        MMKV mv = MMKV.defaultMMKV();
//        this.colorCameraRotation = mv.decodeInt(Constant.ROTATE_SET,Constant.ROTATE_SET_V);  //相机旋转
//        this.blackWhiteCameraRotation =  this.colorCameraRotation;
//        this.colorPreRotation =  mv.decodeInt(Constant.PRE_ANGLE_SET,Constant.PRE_ANGLE_SET_V);      //彩色预览旋转
//        this.blackWhitePreRotation = this.colorPreRotation;       //黑白预览旋转
//        this.colorMirror = mv.decodeBool(Constant.MIRROR_SET,Constant.MIRROR_SET_V);          //彩色摄像头镜像
//        this.blackWhiteMirror =  this.colorMirror;         //黑白摄像头镜像
//        cameraData = new CameraDataStream(mContext);
//        mFacePass = new FacePass(mContext);
//        cameraData.setListener(mFacePass);
//        mFacePass.config(callback);
//        return this;
//    }
//
//    /**
//     * 获取人脸操作句柄
//     * @return
//     */
//    public FacePassHandler getKSHandler(){
//        return mFacePass.getMFacePassHandler();
//    }
//
//
//    private Camera.Size getBestPreviewSize( Camera.Parameters camPara) {
//        List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
//        ArrayList<Camera.Size> widthLargerSize = new ArrayList<Camera.Size>();
//        int max = Integer.MIN_VALUE;
//        Camera.Size maxSize = null;
//        for (Camera.Size tmpSize : allSupportedSize) {
//            int multi = tmpSize.height * tmpSize.width;
//            if (multi > max) {
//                max = multi;
//                maxSize = tmpSize;
//            }
//            //选分辨率比较高的
//            if (tmpSize.width > tmpSize.height && (tmpSize.width > wan_Height / 2 || tmpSize.height > wan_Width / 2)) {
//                widthLargerSize.add(tmpSize);
//            }
//        }
//        if (widthLargerSize.isEmpty()) {
//            widthLargerSize.add(maxSize);
//        }
//
//        final float propotion = wan_Width >= wan_Height ? (float) wan_Width / (float) wan_Height : (float) wan_Height / (float) wan_Width;
//
//        Collections.sort(widthLargerSize, (lhs, rhs) -> {
//            //选预览比例跟屏幕比例比较接近的
//            float a = getPropotionDiff(lhs, propotion);
//            float b = getPropotionDiff(rhs, propotion);
//            return (int) ((a - b) * 10000);
//        });
//
//        float minPropotionDiff = getPropotionDiff(widthLargerSize.get(0), propotion);
//        ArrayList<Camera.Size> validSizes = new ArrayList<>();
//        for (int i = 0; i < widthLargerSize.size(); i++) {
//            Camera.Size size = widthLargerSize.get(i);
//            float propotionDiff = getPropotionDiff(size, propotion);
//            if (propotionDiff > minPropotionDiff) {
//                break;
//            }
//            validSizes.add(size);
//        }
//
//        Collections.sort(validSizes, (lhs, rhs) -> rhs.width * rhs.height - lhs.width * lhs.height);
//        return widthLargerSize.get(0);
//    }
//
//    public float getPropotionDiff(Camera.Size size, float standardPropotion) {
//        return Math.abs((float) size.width / (float) size.height - standardPropotion);
//    }
//
//
//    public int getCameraWidth() {
//        return colorPreViewWidth;
//    }
//    public int getCameraheight() {
//        return colorPreViewHeight;
//    }
//
//
//    public CameraManager setPreViewSize(int width ,int height ){
//        colorPreViewWidth = width;
//        colorPreViewHeight = height;
//        return this;
//    }
//
//
//    public void setPreviewDisplay(SurfaceView preview) {
//        this.cameraPreview = preview;
//    }
//
//
//    public void setControlParams(boolean alive  ,boolean voice){
//        mFacePass.setDectParams(alive,voice);
//    }
//
//    public CameraManager setMirror(boolean cMirror,boolean bMirror ){
//        colorMirror = cMirror;
//        blackWhiteMirror = bMirror;
//        return this;
//    }
//
//    public CameraManager setCameraRotation(boolean cMirror,boolean bMirror ){
//        colorMirror = cMirror;
//        blackWhiteMirror = bMirror;
//        return this;
//    }
//
//
//    public CameraManager setLiveness(boolean enable){
//        mFacePass.setLiveness(enable);
//        return this;
//    }
//
//    public CameraManager setFaceMinThreshold(int threshold){
//        mFacePass.setFaceMinThreshold(threshold);
//        return this;
//    }
//
//
//    public CameraManager setFacePose(float roll , float pitch  ,float yaw ){
//        mFacePass.setFacePose(roll  , pitch  ,yaw );
//        return this;
//    }
//
//    public CameraManager setFaceSearchThreshold(float searchThreshold ){
//        mFacePass.setFaceSearchThreshold(searchThreshold);
//        return this;
//    }
//
////    public CameraManager setFaceLivenessThreshold(float livenessThreshold){
////        mFacePass.setFaceLivenessThreshold(livenessThreshold );
////        return this;
////    }
//
//    public CameraManager setFaceBlurThreshold(float blurThreshold ){
//        mFacePass.setFaceBlurThreshold(blurThreshold);
//        return this;
//    }
//
//
//    public boolean open(Rect rect , RecognizeCallback listener) {
//        if (state != CameraState.OPENING) {
//            state = CameraState.OPENING;
////            closeCamera();
//            new AsyncTask<Object, Object, Object>() {
//                @Override
//                protected Object doInBackground(Object... params) {
//                    try {
//                        closeCamera();
////                        Thread.sleep(500);
//                        int count = Camera.getNumberOfCameras();
//                        if (count > 1) {
//                            cameraOne = Camera.open(0);
//                            cameraTwo = Camera.open(1);
//                        }else {
//                            LogUtil.e(TAG,"相机少于2个");
//                            throw new InvalidParameterException("相机少于2个");
//                        }
//                        if (cameraTwo != null && cameraOne!=null) {
//                            if (colorCameraId ==0){
//                                cameraOne.setDisplayOrientation(colorPreRotation);
//                                cameraTwo.setDisplayOrientation(blackWhitePreRotation);
//                            }else {
//                                cameraOne.setDisplayOrientation(blackWhitePreRotation );
//                                cameraTwo.setDisplayOrientation(colorPreRotation);
//                            }
//
//                            Camera.Parameters paramOne = cameraOne.getParameters();
//                            Camera.Parameters paramTwo = cameraTwo.getParameters();
//
//                            if (colorCameraId ==0) {
//                                if (colorPreViewWidth > 0 && colorPreViewHeight > 0 && isSupportedPreviewSize(colorPreViewWidth, colorPreViewHeight, paramOne)) {
//                                    paramOne.setPreviewSize(colorPreViewWidth, colorPreViewHeight);
//                                } else {
//                                    Camera.Size bestPreviewSize = getBestPreviewSize(paramOne);
//                                    LogUtil.i("paramOne", "best height is" + bestPreviewSize.height + "width is " + bestPreviewSize.width);
//                                    colorPreViewWidth = (short) bestPreviewSize.width;
//                                    colorPreViewHeight = (short) bestPreviewSize.height;
//                                    paramOne.setPreviewSize(colorPreViewWidth, colorPreViewHeight);
//                                }
//                                if(blackWhitePreViewWidth > 0 && blackWhitePreViewHeight > 0 && isSupportedPreviewSize(blackWhitePreViewWidth, blackWhitePreViewHeight, paramTwo)) {
//                                    paramTwo.setPreviewSize(blackWhitePreViewWidth, blackWhitePreViewHeight);
//                                } else {
//                                    Camera.Size bestPreviewSize = getBestPreviewSize(paramTwo);
//                                    LogUtil.i("paramTwo", "best height is" + bestPreviewSize.height + "width is " + bestPreviewSize.width);
//                                    blackWhitePreViewWidth = (short) bestPreviewSize.width;
//                                    blackWhitePreViewHeight = (short) bestPreviewSize.height;
//                                    paramTwo.setPreviewSize(blackWhitePreViewWidth, blackWhitePreViewHeight);
//                                }
////                                LogUtil.d(TAG,"黑白摄像分辨率："+blackWhitePreViewWidth+"*"+blackWhitePreViewHeight);
////                                LogUtil.d(TAG,"彩色摄像分辨率："+colorPreViewWidth+"*"+colorPreViewHeight);
//                            }else {
//                                if (colorPreViewWidth > 0 && colorPreViewHeight > 0 && isSupportedPreviewSize(colorPreViewWidth, colorPreViewHeight, paramTwo)) {
//                                    paramTwo.setPreviewSize(colorPreViewWidth, colorPreViewHeight);
//                                } else {
//                                    Camera.Size bestPreviewSize = getBestPreviewSize(paramTwo);
//                                    LogUtil.i("paramOne", "best height is" + bestPreviewSize.height + "width is " + bestPreviewSize.width);
//                                    colorPreViewWidth = (short) bestPreviewSize.width;
//                                    colorPreViewHeight = (short) bestPreviewSize.height;
//                                    paramTwo.setPreviewSize(colorPreViewWidth, colorPreViewHeight);
//                                }
//                                if(blackWhitePreViewWidth > 0 && blackWhitePreViewHeight > 0 && isSupportedPreviewSize(blackWhitePreViewWidth, blackWhitePreViewHeight, paramOne)) {
//                                    paramOne.setPreviewSize(blackWhitePreViewWidth, blackWhitePreViewHeight);
//                                } else {
//                                    Camera.Size bestPreviewSize = getBestPreviewSize(paramOne);
//                                    LogUtil.i("paramTwo", "best height is" + bestPreviewSize.height + "width is " + bestPreviewSize.width);
//                                    blackWhitePreViewWidth = (short) bestPreviewSize.width;
//                                    blackWhitePreViewHeight = (short) bestPreviewSize.height;
//                                    paramOne.setPreviewSize(blackWhitePreViewWidth, blackWhitePreViewHeight);
//                                }
////                                LogUtil.d(TAG,"黑白摄像分辨率："+blackWhitePreViewWidth+"*"+blackWhitePreViewHeight);
////                                LogUtil.d(TAG,"彩色摄像分辨率："+colorPreViewWidth+"*"+colorPreViewHeight);
//                            }
//                            paramOne.setPreviewFormat(ImageFormat.NV21);
//                            paramTwo.setPreviewFormat(ImageFormat.NV21);
//                            cameraOne.setParameters(paramOne);
//                            cameraTwo.setParameters(paramTwo);
//                            PixelFormat pixelinfo = new PixelFormat();
//                            int pixelformat = paramOne.getPreviewFormat();
//
//                            PixelFormat.getPixelFormatInfo(pixelformat, pixelinfo);
//                            Camera.Size szOne = paramOne.getPreviewSize();
//                            Camera.Size szTwo = paramTwo.getPreviewSize();
//                            int bufSize = szOne.width * szOne.height * pixelinfo.bitsPerPixel / 8;
//                            LogUtil.i("Camera1  Size", "camerawidth : " + szOne.width +
//                                    "  height  : " + szOne.height+" bufSize--> "+bufSize);
//
//                            cameraOne.addCallbackBuffer(new byte[bufSize]);
//                            bufSize = szTwo.width * szTwo.height * pixelinfo.bitsPerPixel / 8;
//                            LogUtil.i("Camera2 Size", "camerawidth : " + szTwo.width +
//                                    "  height  : " + szTwo.height+" bufSize--> "+bufSize);
//                            cameraTwo.addCallbackBuffer(new byte[bufSize]);
//                            cameraData.setDisplayView(cameraPreview);
//                            cameraData.initHolder();
//                            mFacePass.setListener(listener);
//                            mFacePass.setCameraPreSize(colorPreViewWidth,colorPreViewHeight);
//                            mFacePass.setRect(rect);
//                            mFacePass.feedFrame();
//
//                            if (colorCameraId ==0){
//                                cameraData.setParameter(szOne,szTwo,colorCameraRotation,colorPreRotation,
//                                        blackWhiteCameraRotation, blackWhitePreRotation,colorMirror,blackWhiteMirror);
//                                cameraData.setCamera(cameraOne,1);
//                                cameraData.setCamera(cameraTwo,2);
//                            }else {
//                                cameraData.setParameter(szTwo ,szOne,colorCameraRotation,colorPreRotation, blackWhiteCameraRotation, blackWhitePreRotation,colorMirror,blackWhiteMirror);
//                                cameraData.setCamera(cameraOne,2);
//                                cameraData.setCamera(cameraTwo,1);
//                            }
//                            if (listener != null)listener.onInitCode(Constant.NO_ERROR,"相机打开成功");
//                            state = CameraState.OPENED;
//                            LogUtil.i("cameraManager", "ok camera..");
//                        }
//                    } catch (Exception e) {
//                        if ( listener != null)listener.onInitCode(Constant.OPEN_CAMERA_ERROR_TYPE ,"相机打开失败");
//                        e.printStackTrace();
//                        MyService boardService = new MyService(mContext);
//                        boardService.rebootSystem();
//                    }
//                    return null;
//                }
//
//                @Override
//                protected void onPostExecute(Object o) {
//                    super.onPostExecute(o);
//                }
//            }.execute();
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//
//    public void closeCamera() {
//        state = CameraState.IDEL;
//        if (mFacePass != null) mFacePass.closeJob();
//        if (cameraOne != null || cameraTwo != null) {
//            cameraData.release();
//            cameraTwo = null;
//            cameraOne = null;
//        }
//    }
//
//    public void release(){
//        closeCamera();
//        if (mFacePass != null)
//            mFacePass.release();
//    }
//
//
//
//    public enum CameraState {
//        IDEL,
//        OPENING,
//        OPENED
//    }
//
//    public static class Builder {
//
//        public Builder setBlackWhitePreViewWidth(int blackWhitePreViewWidth) {
//            this.blackWhitePreViewWidth = blackWhitePreViewWidth;
//            return this;
//        }
//
//        public Builder setBlackWhitePreViewHeight(int blackWhitePreViewHeight) {
//            this.blackWhitePreViewHeight = blackWhitePreViewHeight;
//            return this;
//        }
//
//        public Builder setColorPreViewWidth(int colorPreViewWidth) {
//            this.colorPreViewWidth = colorPreViewWidth;
//            return this;
//        }
//
//        public Builder setColorPreViewHeight(int colorPreViewHeight) {
//            this.colorPreViewHeight = colorPreViewHeight;
//            return this;
//        }
//
//        public Builder setColorCameraId(int colorCameraId) {
//            this.colorCameraId = colorCameraId;
//            return this;
//        }
//
//        public Builder setColorPreRotation(int colorPreRotation) {
//            this.colorPreRotation = colorPreRotation;
//            return this;
//        }
//
//        public Builder setColorCameraRotation(int colorCameraRotation) {
//            this.colorCameraRotation = colorCameraRotation;
//            return this;
//        }
//
//        public Builder setBlackWhitePreRotation(int blackWhitePreRotation) {
//            this.blackWhitePreRotation = blackWhitePreRotation;
//            return this;
//        }
//
//        public Builder setBlackWhiteCameraRotation(int blackWhiteCameraRotation) {
//            this.blackWhiteCameraRotation = blackWhiteCameraRotation;
//            return this;
//        }
//
//        public Builder setColorMirror(boolean colorMirror) {
//            this.colorMirror = colorMirror;
//            return this;
//        }
//
//        public Builder setBlackWhiteMirror(boolean blackWhiteMirror) {
//            this.blackWhiteMirror = blackWhiteMirror;
//            return this;
//        }
//
////        int blackWhitePreViewWidth = 1920;   //黑白预览宽度
////        int blackWhitePreViewHeight = 1080;  //黑白预览高度
////        int colorPreViewWidth = 1920;   //彩色预览宽度
////        int colorPreViewHeight = 1080;  //彩色预览高度
//
//        int blackWhitePreViewWidth = 640;   //黑白预览宽度
//        int blackWhitePreViewHeight = 480;  //黑白预览高度
//        int colorPreViewWidth = 640;   //彩色预览宽度
//        int colorPreViewHeight = 480;  //彩色预览高度
//        int colorCameraId = 0;     //彩色摄像头
//        int colorPreRotation = 90;       //彩色预览旋转
//        int colorCameraRotation = 270;       //彩色摄像头旋转
//        int blackWhitePreRotation = 90;       //黑白预览旋转
//        int blackWhiteCameraRotation = 270;       //黑白摄像头旋转
//        boolean colorMirror = false;         //彩色摄像头镜像
//        boolean blackWhiteMirror = false;         //黑白摄像头镜像
//        Context context;
//
//        public Builder(Context context) {
//            this.context = context;
//        }
//
//        public CameraManager build(){
//
//            return new CameraManager(this);
//        }
//    }
//}

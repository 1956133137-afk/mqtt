package com.my.yuvconvert;


/**
 * 1、所有数据都得先转成I420三面存储格式，
 * 2、再进行具体处理，
 * 3、处理完成再转成所需的格式输出
 */
public class YuvUtil {
    static {
        System.loadLibrary("yuvconvert");
    }


    /**
     * YUV数据的基本的处理
     * 内部处理优先顺序
     * 1、旋转
     * 2、镜像
     * 3、缩放
     *
     * @param nv21Src    原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param i420Dst    输出数据
     * @param scale_width  输出的宽,缩放比例
     * @param scale_height 输出的高，缩放比例
     * @param mode       压缩模式。这里为0，1，2，3 速度由快到慢，质量由低到高，一般用0就好了，因为0的速度最快
     * @param degree     旋转的角度，90，180和270三种
     * @param isMirror   是否镜像，一般只有270的时候才需要镜像
     **/
    public native void yuvCompress(byte[] nv21Src, int width,int height,byte[] i420Dst,float scale_width,
                                   float scale_height,int mode,int degree, boolean isMirror);


    /**
     * yuv数据的裁剪操作
     *
     * @param i420Src    原始数据
     * @param width      原始的宽
     * @param height     原始的高
     * @param i420Dst    输出数据
     * @param dst_width  输出的宽
     * @param dst_height 输出的高
     * @param left       裁剪的x的开始位置，必须为偶数，否则显示会有问题
     * @param top        裁剪的y的开始位置，必须为偶数，否则显示会有问题
     **/
    public native void yuvCropI420(byte[] i420Src,int width,int height, byte[] i420Dst,int dst_width,int dst_height,int left,int top);

    /**
     * yuv数据的镜像操作
     * @param i420Src i420原始数据
     * @param width   原始宽度
     * @param height  原始高度
     * @param i420Dst 目标数据
     */
    public native void yuvMirrorI420(byte[] i420Src,int width, int height,byte[] i420Dst);


    /**
     * yuv数据的缩放操作
     * @param i420Src i420原始数据
     * @param width   原始宽度
     * @param height  原始高度
     * @param i420Dst i420目标数据
     * @param dstWidth 目标宽度
     * @param dstHeight 目标高度
     * @param mode   压缩模式，0~3，质量由低到高，一般传入0
     */
    public native void yuvScaleI420(byte[] i420Src,int width, int height,byte[] i420Dst,
                                    int dstWidth, int dstHeight,int mode);


    /**
     * yuv数据的旋转
     * @param i420Src i420原始数据
     * @param width   原始宽度
     * @param height  原始高度
     * @param i420Dst  目标数据
     * @param degree   旋转角度
     */
    public native void yuvRotateI420(byte[] i420Src ,int width,int height,byte[] i420Dst,int degree);

    /**
     * 将NV21转化为I420
     * @param nv21Src 原始I420数据
     * @param width   原始宽度
     * @param height  原始高度
     * @param i420Dst  目标数据
     */
    public native void yuvNV21ToI420(byte[] nv21Src,int width, int height,byte[] i420Dst);

    /**
     * 将I420转化为NV21
     * @param i420Src  原始数据
     * @param width    原始宽度
     * @param height    原始高度
     * @param nv21Src   目标数据
     */
    public native void yuvI420ToNV21(byte[] i420Src, int width,int height,byte[] nv21Src);


































}

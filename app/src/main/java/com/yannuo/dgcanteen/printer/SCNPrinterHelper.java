package com.yannuo.dgcanteen.printer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;

import com.csnprintersdk.csnio.CSNCanvas;
import com.csnprintersdk.csnio.CSNPOS;
import com.csnprintersdk.csnio.csnbase.CSNCOMIO;
import com.csnprintersdk.csnio.csnbase.CSNIOCallBack;
import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.EscapeUtil;
import com.yannuo.dgcanteen.util.LogUtil;


import java.util.LinkedList;
import java.util.List;

public class SCNPrinterHelper implements CSNIOCallBack {
    private final String TAG = getClass().getSimpleName();
    private int mBaudrate  = 0;
    //    private final String mPort = "/dev/ttyXRUSB1";
    private String mPort = null;
    private CSNPOS mPos;
    private CSNCanvas mPages;
    private CSNCOMIO mCom;
    private boolean isOpenPrinter = false;
    private LinkedList<Object> prepareData  ;//添加的打印准备列表
    private LinkedList<Object> cacheData  ;//缓存的打印列表，将要打印或者正在打印的列表
    private int mGray = 3 ;         //灰度
    private PrinterConfig config = null ;

    public SCNPrinterHelper() {
        mPos = new CSNPOS();
        mCom = new CSNCOMIO();
        mPos.Set(mCom);

        mPages = new CSNCanvas();
        mPages.Set(mCom);
        mCom.SetCallBack(this);

        prepareData = new LinkedList();
        cacheData = new LinkedList();
//        MMKV mv = MMKV.defaultMMKV();
//        mPort = mv.decodeString(Constant.PRINTER_PATH_SET);
//        String tr = mv.decodeString(Constant.PRINTER_BAUD_SET);
//        if (TextUtils.isEmpty(tr))tr = "9600";
//        mBaudrate = Integer.parseInt(tr);
//        if (TextUtils.isEmpty(mPort))mPort =  "/dev/ttyXRUSB1";
    }

    /**
     * 打开设备
     * @return
     */
    public synchronized int openPrinter(){
        MMKV mv = MMKV.defaultMMKV();
        mPort = mv.decodeString(Constant.PRINTER_PATH_SET);
        mBaudrate = Integer.parseInt(mv.decodeString(Constant.PRINTER_BAUD_SET));
        LogUtil.d(TAG,"正在连接打印机端口！");
        boolean res = mCom.Open(mPort, mBaudrate, 1, 8 ,0,0,0);
        isOpenPrinter = res;
        config(new Bundle());
        return res ? 0 : 1;
    }

    public int config(Bundle params){
        StringBuffer buffer = new StringBuffer();
        int pageW= params.getInt("pageW",48); //默认纸张宽度58mm
        int offsetX= params.getInt("OffsetX",0); //默认左偏移量0mm
        buffer.append("纸张宽度(mm):");
        buffer.append(pageW);
        buffer.append(",纸张左偏(mm):");
        buffer.append(offsetX);
        if (config == null)config = new PrinterConfig();
        if (pageW == 0) pageW = 48;
        pageW = (int) (pageW * 8f);
        //默认减去5毫米左偏
        offsetX -= 5;
        if(offsetX < 0) offsetX = 0;
        offsetX =  (int) (offsetX * 8f);
        if (pageW > 384)pageW = 384;
        if (offsetX > 384 )offsetX = 0;
        if ((pageW + offsetX) > 384){
            pageW = 384 - offsetX;
        }
        buffer.append(",纸张宽度(px):");
        buffer.append(pageW);
        buffer.append(",纸张左偏(px):");
        buffer.append(offsetX);
        LogUtil.d(TAG,buffer.toString());
        config.setPageW(pageW);
        config.setOffsetX(offsetX);
        return 0;
    }

    //添加到预打印列表
    public void addElement(Object obj){
        prepareData.add(obj);
    }

    //添加到打印列表
    public void addElements(){
        if ( prepareData.size() > 0){
            cacheData.addAll(prepareData);
            prepareData.clear();
        }
    }

    public void clearPreData(){
        prepareData.clear();
    }

    //清空打印列表
    public void clearPrintCache(){
        cacheData.clear();
    }

    //关闭打印机
    public void closePrinter(){
        isOpenPrinter = false;
        mCom.Close();
    }

    public boolean isOpenPrinter() {
        return isOpenPrinter;
    }


    public int queryStatus(){
        if (!isOpenPrinter) return  5; // 打印机通讯异常
        int bPrintResult = 0 ;
        byte[] status = new byte[1];
        if (mPos.POS_RTQueryStatus(status,3,1000,2)) {
            if ((status[0] & 0x08)== 0x08)
                return bPrintResult = -2; // 切刀有错误
            if ((status[0] & 0x40)== 0x40)
                return bPrintResult = -3; // 打印头温度或电压超出范围
            if ((status[0] & 0x20)== 0x20)
                return bPrintResult = 4; // 有不可恢复错误
        }else return bPrintResult = 5; // 打印机通讯异常
        if (mPos.POS_RTQueryStatus(status, 2, 1000, 2)) {
            if ((status[0] & 0x04) == 0x04)    //判断合盖是否正常,上盖开
                return bPrintResult = -6;
            if ((status[0] & 0x20) == 0x20)    //判断是否缺纸,打印机缺纸
                return bPrintResult = -5;
        }else return bPrintResult = 5; // 打印机通讯异常
        if (mPos.POS_RTQueryStatus(status, 1, 1000, 2)) {
            if ((status[0] & 0x08) == 0x08)    //脱机
                return bPrintResult = -4;
            if ((status[0] & 0x80) == 0x80)    //判断是否缺纸,打印机缺纸
                return bPrintResult = 3;   //纸未撕走
        }else return bPrintResult = 5; // 打印机通讯异常
        if (mPos.POS_RTQueryStatus(status, 4, 1000, 2)) {
            if ((status[0] & 0x0C) == 0x0C)    //纸将近
                return bPrintResult = 1;
            if ((status[0] & 0x60) == 0x60)    //纸尽
                return bPrintResult = -5;
        }else return bPrintResult = 5; // 打印机通讯异常
        return bPrintResult;
    }


    public int transitionCode(int code){
        int resCode = 0;  //-1未定义的其他错误
        switch (code){
            case -2:
                resCode = 0xE2; //切纸刀不在原位(自助热敏打印机特有返回值)
                break;
            case -3:
                resCode = 0xf3; //打印头温度或电压超出范围
                break;
            case 5:// 打印机通讯异常
            case 4:// 有不可恢复错误
            case -4: //脱机
            case 3://纸未撕走
                resCode = -1;
                break;
            case -6:
                resCode = 0xE0; //打印头抬起
                break;
            case -5:
                resCode = 0xF0; //缺纸， 不能打印
                break;

            case 1:
                resCode = 0xF4; //纸将近
                break;

        }
        return resCode ;
    }


    public String resultCodeToString(int code) {
        switch (code) {
            case 5:
                return "打印机通讯异常";
            case 4:
                return "有不可恢复错误票";
            case 3:
                return "出纸口有未取小票，请注意及时取走小票";
            case 2:
                return "紙将尽 且 出纸口有未取小票，请注意更换纸卷 和 及时取走小票";
            case 1:
                return "紙将尽，请注意更换纸卷";
            case 0:
                return "打印机正常";
            case -1:
                return "未打印小票，请检查是否卡纸";
            case -2:
                return "切刀异常，请手动排除";
            case -3:
                return "打印头过热，请等待打印机冷却";
            case -4:
                return "打印机脱机";
            case -5:
                return "打印机缺纸";
            case -6:
                return "上盖打开";
            case -7:
                return "实时状态查询失败";
            case -8:
                return "查询状态失败，请检查通讯端口是否连接正常";
            case -9:
                return "打印过程中缺纸，请检查单据完整性";
            case -10:
                return "打印过程中上盖开启，请重新打印";
            case -11:
                return "连接中断，请确认打印机是否连线";
            case -12:
                return  "请取走打印完的票据后，再进行打印！";
            case -13:
            default:
                return "未知错误";
        }
    }

    @Override
    public void OnOpen() {
        isOpenPrinter = true;
//        mPos.POS_SetBasic(1,1,0,1,1);
        LogUtil.i(TAG,"打印机已连接！");
    }



    @Override
    public void OnOpenFailed() {
        isOpenPrinter = false;
        LogUtil.e(TAG,"打印机连接失败！");
    }

    @Override
    public void OnClose() {
        isOpenPrinter = false;
        LogUtil.w(TAG,"打印机连接异常中断！");

    }

    public void release(){
        prepareData.clear();
        cacheData.clear();
        closePrinter();
    }

    public void printTicket() throws RemoteException, InterruptedException {
        try {
            int bPrintResult = 0;
            byte[] status = new byte[1];

            if (mPos.POS_RTQueryStatus(status, 3, 1000, 2)) {
                if ((status[0] & 0x08) == 0x08) {
                    bPrintResult = -2; // 切刀有错误
                    LogUtil.i(TAG, resultCodeToString(bPrintResult));
                    cacheData.clear();
                    return;
                }
                if ((status[0] & 0x40) == 0x40) {
                    bPrintResult = -3; // 打印头温度或电压超出范围
                    LogUtil.i(TAG, resultCodeToString(bPrintResult));
                    cacheData.clear();
                    return;
                }

                if (mPos.POS_RTQueryStatus(status, 2, 1000, 2)) {
                    if ((status[0] & 0x04) == 0x04) {
                        bPrintResult = -6; //判断合盖是否正常,上盖开
                        LogUtil.i(TAG, resultCodeToString(bPrintResult));
                        cacheData.clear();
                        return;
                    }

                    if ((status[0] & 0x20) == 0x20) {
                        bPrintResult = -5;//判断是否缺纸,打印机缺纸
                        LogUtil.i(TAG, resultCodeToString(bPrintResult));
                        cacheData.clear();
                        return;
                    } else {
                        mPos.POS_Reset();
                        for (int i = 0; i < cacheData.size(); ) {
                            Object obj = cacheData.get(0);
                            if (obj instanceof TextPrint) {//添加文本
                                TextPrint textPrint = (TextPrint) obj;
                                String content = textPrint.getText();

                                //内容分行处理
                                int size = 1;
                                if (textPrint.getFont() > 0)size = 2; //中号字体
                                List<String> lines = EscapeUtil.strLeng(textPrint,content, size, config.getPageW());//分行

                                if (textPrint.getAlign() == 0){
                                    mPos.POS_S_Align(0); //居左对齐
                                } else if (textPrint.getAlign() == 1) {
                                    mPos.POS_S_Align(1); //居中对齐
                                }
                                else if (textPrint.getAlign() == 2){
                                    mPos.POS_S_Align(2); //居右对齐
                                }
                                for (String str : lines) {
                                    mPos.POS_TextOut(str+"\r\n", 0, config.getOffsetX(),
                                            textPrint.getScaleW(), textPrint.getScaleH(), textPrint.getFont(),
                                            textPrint.getStyle());
                                }
                            }
                            else if (obj instanceof QrCodePrint) {//添加二维码
                                QrCodePrint qrCodePrint = (QrCodePrint) obj;
                                int interval = (int) (qrCodePrint.getOffset() *8 + config.getOffsetX());
                                if (interval <= (384 / 3)) mPos.POS_S_Align(0); //左对齐
                                else if (interval <= ((384 / 3) * 2)) mPos.POS_S_Align(1); //居中对齐
                                else mPos.POS_S_Align(2); //居右对齐
                                int size = (int) (qrCodePrint.getExpectedHeight() * 8f) / 24;
                                if (size >16) size = 16;
                                if (size<= 3) size  = 4;
                                mPos.POS_S_SetQRcode(qrCodePrint.getQrCode(), size, 0, 2);
                            }
                            else if (obj instanceof BarCodePrint) {//添加条码
                                BarCodePrint qrCodePrint = (BarCodePrint) obj;
                                int interval = 0 ;
                                if (qrCodePrint.getAlign()== 0) {
                                    interval = (int) (qrCodePrint.getOffset() *8 + config.getOffsetX());
                                    mPos.POS_S_Align(0); //左对齐
                                }
                                else if (qrCodePrint.getAlign() == 1) mPos.POS_S_Align(1); //居中对齐
                                else mPos.POS_S_Align(2); //居右对齐

                                mPos.POS_S_SetBarcode(qrCodePrint.getBarcode(), interval, 0x49, 2,qrCodePrint.getHeight(),0,2);
                            }
                            else if (obj instanceof PicturePrint) {//添加图片
                                PicturePrint picturePrint = (PicturePrint) obj;
                                byte[] decode = Base64.decode(picturePrint.getImageData(),Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
                                if ( picturePrint.getRotation() != 0){
                                    Matrix matrix = new Matrix();
                                    matrix.setRotate(picturePrint.getRotation());
                                    bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                                }
                                if (bitmap == null) return;
                                int bitWid = (int) (picturePrint.getWidth() * 8f);
                                if (bitWid >384) bitWid = 384;

                                int interval = (int) (picturePrint.getILeft() * 8f);
                                if (interval >384) interval = 384;
                                if (interval <= (384 / 3)) mPos.POS_S_Align(0); //左对齐
                                else if (interval <= ((384 / 3) * 2)) mPos.POS_S_Align(1); //居中对齐
                                else mPos.POS_S_Align(2); //居右对齐

                                mPos.POS_PrintPicture(bitmap, bitWid, 1, 0);
//                                if (!bitmap.isRecycled())bitmap.recycle();
                            }else if (obj instanceof Integer){
                                mPos.POS_FeedLine();
                            }
                            else {
                                LogUtil.w(TAG, "不合法打印数据类型");
                            }
                            cacheData.remove(obj);
                        }
                        mPos.POS_FeedLine();
                        mPos.POS_FeedLine();
                        mPos.POS_FullCutPaper();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }





}

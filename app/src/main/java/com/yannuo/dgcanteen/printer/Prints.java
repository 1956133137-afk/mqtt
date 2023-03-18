package com.yannuo.dgcanteen.printer;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.ccb.mis.CcbMisSdk;
import com.ccb.mis.common.utils.RSAUtils;
import com.ccb.mis.entity.ParamEntity;
import com.ccb.mis.entity.TransResult;
import com.ccb.mis.service.inf.MisAggregateService;
import com.csnprintersdk.csnio.CSNPOS;
import com.google.gson.Gson;
import com.yannuo.dgcanteen.MyApplication;
import com.yannuo.dgcanteen.model.PrinterTicker;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.paylib.utils.EscapeUtil;

import java.io.IOException;
import java.io.InputStream;

public class Prints {

    public static void paymm(){
        //商户号
        String merchantId = "105005253999624";
        //终端号
        String terminalId = "071984009";
/**
 * 终端通过RSA生成自己的公钥、私钥的密匙对。
 * 主要的流程，终端或服务器通过getAuthKey方法将终端生成的公钥，传递给云MIS，
 * 云MIS通过公钥对交易密匙再传递回给终端，终端用生成的私钥进行解密，得到的明文交易秘钥
 * 注意：尽量不要存明文的交易秘钥，要在初始化时或其他使用交易秘钥的才解密秘钥。
 */
        //公钥
        String pubKey = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALomsaWsfWF5OglGEF9QIsPsYfYSzHt4" +
                "O9lCRcWwNr9U0RTjYHLE08x8OJ8ST1qb483bKtcCgsrTOLXYu4hPI2UCAwEAAQ==";
        //私钥
        String privateKey = "MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAuiaxpax9YXk6CUYQ" +
                "X1Aiw+xh9hLMe3g72UJFxbA2v1TRFONgcsTTzHw4nxJPWpvjzdsq1wKCytM4tdi7" +
                "iE8jZQIDAQABAkEAi7mPjnOyiP+deGiG0YQtFDLSNQpXQjY1dhYwmPpznZib6Bht" +
                "xcQW0xRHe3DLWmygVT4NXpL2DCSn8b7rwyWbAQIhAOqNLd4DOnkJMT5Fr25Iy0iu" +
                "JmDgEgsI+kwG0KPcSUV9AiEAyyx4EhQFmvcGunxl4g1RkenM1s4YiT9l1IuGVgbx" +
                "GgkCIBBHzmg7pyJhnfZpldy81sdrtyRASWBUtjLywiatkrRtAh8OSRIoM0AxpSzT" +
                "7+s5+bXCjK3abKNNAyglyamoYtSRAiAWqk89CfrbM9VwePobYXy67dpav/kplbOV" +
                "joBP1nRFAQ==";

        String authCode = "1234567";

        String key ="yann20230306";
        //初始化
        ParamEntity paramEntity = new ParamEntity(merchantId+"0001", merchantId, terminalId, key);


        //获得密钥
        String result = CcbMisSdk.getInstance().getAuthService(paramEntity).getAuthKey(pubKey, authCode, merchantId, terminalId);
        LogUtil.d("paymm",result);
        TransResult transResult = new Gson().fromJson(result, TransResult.class);
        //解密，获得密码
        String newKey = RSAUtils.decrypt(transResult.getTransData().getAuthKey(), privateKey);
        ParamEntity paramEntity1 = new ParamEntity(merchantId+"0001", merchantId, terminalId, newKey);
        String result1 = CcbMisSdk.getInstance().getMisAggregateService(paramEntity).aggregatePay("0.01", "A123456789", "282577553456145698");
        LogUtil.d("paymm",result1);
    }


    public static int PrintTicket(CSNPOS pos, PrinterTicker bean){
        int bPrintResult = 0 ;
        byte[] status = new byte[1];

        if (pos.POS_RTQueryStatus(status,3,1000,2)){
            if ((status[0] & 0x08)== 0x08)
                return bPrintResult = -2; // 切刀有错误
            if ((status[0] & 0x40)== 0x40)
                return bPrintResult = -3; // 打印头温度或电压超出范围

            if (pos.POS_RTQueryStatus(status, 2, 1000, 2)) {

                if ((status[0] & 0x04) == 0x04)    //判断合盖是否正常,上盖开
                    return bPrintResult = -6;
                if ((status[0] & 0x20) == 0x20)    //判断是否缺纸,打印机缺纸
                    return bPrintResult = -5;

                else {
                    if (!pos.GetIO().IsOpened())bPrintResult = -64 ;
                    Bitmap pBitmap = getImageFromAssetsFile(MyApplication.applicationContext, "logo.png");
                    pos.POS_Reset();
//                    pos.POS_S_Align(1); //居中
                    pos.POS_PrintPicture(pBitmap, 380, 1, 0);
                    try {
//                        Thread.sleep(1500);
                        pos.POS_S_Align(0); //左对齐
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    pos.POS_TextOut("单号:"+bean.getJournalNumber()+"\r\n", 0, 0, 0, 0, 0, 0);
                    pos.POS_TextOut("--------------------------------\r\n", 0, 0, 0, 0, 0, 8);
                    int[] length = new int[bean.getStitle().length];
                    int width = 3*9;
                    length[0] = width;
                    pos.POS_TextOut(bean.getStitle()[0], 0, width, 0, 0, 0, 8);
                    width +=160;
                    length[1] = width;
                    pos.POS_TextOut(bean.getStitle()[1], 0, width, 0, 0, 0, 8);
                    width +=70;
                    length[2] = width;
                    pos.POS_TextOut(bean.getStitle()[2], 0, width, 0, 0, 0, 8);
                    width +=70;
                    length[3] = width;
                    pos.POS_TextOut(bean.getStitle()[3]+"\r\n", 0, width, 0, 0, 0, 8);
                    //商品
                    for (int i=0 ;i<bean.getProducts().size();i++){
                        pos.POS_TextOut((i+1)+".", 0, 0, 0, 0, 0, 0);

                        int position = 0;
                        pos.POS_TextOut(bean.getProducts().get(i)[position], 0, length[position], 0, 0, 0, 0);

                        position++;
                        pos.POS_TextOut(bean.getProducts().get(i)[position], 0, length[position], 0, 0, 0, 0);

                        position++;
                        pos.POS_TextOut(bean.getProducts().get(i)[position], 0, length[position], 0, 0, 0, 0);

                        position++;
                        pos.POS_TextOut(bean.getProducts().get(i)[position]+"\r\n", 0, length[position], 0, 0, 0, 0);

                        position++;
                        int[] posit = EscapeUtil.strLeng(bean.getProducts().get(i)[position]);
                        int stop;
                        int star;
                        for (int p =0 ;p < posit.length;p++){
                            if (p==0){
                                 star = 0;
                            }
                            else {
                                star = posit[p -1 ];
                            }
                            stop = posit[p]== 0 ? bean.getProducts().get(i)[position].length() : posit[p];
                            pos.POS_TextOut(bean.getProducts().get(i)[position].substring(star,stop)+"\r\n", 0, 12, 0, 0, 0, 0);
                            if (posit[p] == 0)break;
                        }
                    }
                    pos.POS_TextOut("--------------------------------\r\n", 0, 0, 0, 0, 0, 8);
                    pos.POS_S_Align(2); //右对齐
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    pos.POS_TextOut("消费金额(￥)：        "+bean.getAmount()+"\r\n", 0, 0, 0, 0, 0, 0);
                    pos.POS_TextOut("售出商品数量(件)：     "+bean.getQuantity()+"\r\n", 0, 0, 0, 0, 0, 0);
                    pos.POS_TextOut(bean.getTimeBuying()+"\r\n", 0, 0, 0, 0, 0, 0);
                    pos.POS_TextOut(bean.getWelcomeSpeech()+"\r\n", 0, 0, 0, 0, 0, 0);
//                    pos.POS_TextOut(bean.getStoreName()+"\r\n", 0, 0, 0, 0, 0, 0);
                    pos.POS_TextOut("收银员："+bean.getCashier()+"\r\n", 0, 0, 0, 0, 0, 0);

                    pos.POS_FeedLine();
                    pos.POS_FeedLine();
                    pos.POS_FeedLine();
                    pos.POS_FullCutPaper();

                    if (!pBitmap.isRecycled()) {
                        pBitmap.recycle();
                    }
                }
            }
            else {
                return bPrintResult = -8; //查询失败
            }

        }else {
            return bPrintResult = -8; //查询失败
        }

        return bPrintResult;
    }


    public static int PrintTicket(Context ctx, CSNPOS pos,int nPrintWidth,
                                  boolean bCutter,boolean bDrawer,boolean bBeeper,
                                  int nCount, int nPrintContent,int nCompressMethod){

        int bPrintResult = 0 ;
        byte[] status = new byte[1];
        if (pos.POS_RTQueryStatus(status,3,1000,2)){
            if ((status[0] & 0x08)== 0x08)
                return bPrintResult = -2; // 切刀有错误
            if ((status[0] & 0x40)== 0x40)
                return bPrintResult = -3; // 打印头温度或电压超出范围

            if (pos.POS_RTQueryStatus(status, 2, 1000, 2)) {

                if ((status[0] & 0x04) == 0x04)    //判断合盖是否正常,上盖开
                    return bPrintResult = -6;
                if ((status[0] & 0x20) == 0x20)    //判断是否缺纸,打印机缺纸
                    return bPrintResult = -5;

                else {
                    Bitmap bm1 = getTestImage1(nPrintWidth,nPrintWidth);
                    Bitmap bm2 = getTestImage2(nPrintWidth,nPrintWidth);
                    Bitmap bmBlackWhite = getImageFromAssetsFile(ctx,"blackwhite.png");
                    Bitmap bmIu = getImageFromAssetsFile(ctx, "iu.jpeg");
                    Bitmap bmYellowmen = getImageFromAssetsFile(ctx, "yellowmen.png");

                    for(int i= 0;i < nCount; i++){
                        if (!pos.GetIO().IsOpened())break;

                        if (nPrintContent >= 1) {
                            if (nPrintWidth == 384) {  //2寸打印机
                                pos.POS_Reset();
                                pos.POS_FeedLine();
                                pos.POS_TextOut("UTF-8 ==> $$$ \r\n", 3, 0, 0, 0, 0, 0);
                                pos.POS_FeedLine();
                                pos.POS_FeedLine();
                            } else { //其他尺寸打印机
                                pos.POS_Reset();
                                pos.POS_FeedLine();
                                pos.POS_TextOut("电子发票证明联\r\n", 0, 96, 1, 1, 0, 0);
                                pos.POS_FeedLine();
                                pos.POS_TextOut("小票：270500027719 收银员：010121212122121\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("------------------------------------------\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("   商品编码        单价  数量       小计\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("01.9940228004700    3.98   1.181  20080616\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("   番石榴     小计：4.70   小计： 4.70小计\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("02.996100800220     6.00   0.376  20080617\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("   白面条     小计：2.20          4.70小计\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("03.6921644701204    3.50   1(包)  20080617\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("   恒源德调味 小计：3.50          3.50小计\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("04.9940316000602    5.16   0.116  20080617\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("   生葱       小计：0.60          0.60小计   \r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("------------------------------------------\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("购货总额：                         11.00   \r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("付款：   现金       人民币         101.00\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("找零：   现金       人民币         90.00  \r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("            售出商品数量：4件         \r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("           2005-09-13  16:50:19\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("            欢迎光临   多谢惠顾\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("             （开发票当月有效）\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("              满家福百货南邮店\r\n", 0, 0, 0, 0, 0, 0);
                                pos.POS_TextOut("小票：270500027721           收银员：01012\r\n", 0, 0, 0, 0, 0, 0);

                                pos.POS_FeedLine();
                                pos.POS_FeedLine();
                                pos.POS_TextOut("REC" + String.format("%03d", i + 1) + "\r\nPrinter\r\n简体中文测试\r\n\r\n", 0, 1, 1, 0, 0, 0);
                                pos.POS_FeedLine();
                                pos.POS_FeedLine();
                                pos.POS_FeedLine();
                                pos.POS_FeedLine();
                            }
                            if (nPrintContent == 1 && nCount >1){
                                pos.POS_HalfCutPaper();
                                try {
                                    Thread.currentThread();
                                    Thread.sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        if (nPrintContent >= 2){
                            if (bm1 != null) {
                                pos.POS_PrintPicture(bm1, nPrintWidth, 1, nCompressMethod);
                            }
                            if (bm2 != null) {
                                pos.POS_PrintPicture(bm2, nPrintWidth, 1, nCompressMethod);
                            }

                            if (nPrintContent == 2 && nCount > 1) {
                                pos.POS_HalfCutPaper();
                                try {
                                    Thread.currentThread();
                                    Thread.sleep(4500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (nPrintContent == 2 && nCount == 1) {
                                if (bBeeper)
                                    pos.POS_Beep(1, 5);
                                if (bCutter)
                                    pos.POS_FullCutPaper();
                                if (bDrawer)
                                    pos.POS_KickDrawer(0, 100);
                            }
                        }
                    }
                    if (nPrintContent >= 3) {
                        if (bmBlackWhite != null) {
                            pos.POS_PrintPicture(bmBlackWhite, nPrintWidth, 1, nCompressMethod);
                        }
                        if (bmIu != null) {
                            pos.POS_PrintPicture(bmIu, nPrintWidth, 0, nCompressMethod);
                        }

                        if (bmYellowmen != null) {
                            pos.POS_PrintPicture(bmYellowmen, nPrintWidth, 0, nCompressMethod);
                        }

                        if (nPrintContent == 3 && nCount > 1) {
                            pos.POS_HalfCutPaper();
                            try {
                                Thread.currentThread();
                                Thread.sleep(6000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (nPrintContent == 3 && nCount == 1) {
                            if (bBeeper)
                                pos.POS_Beep(1, 5);
                            if (bCutter)
                                pos.POS_FullCutPaper();
                            if (bDrawer)
                                pos.POS_KickDrawer(0, 100);
                        }
                    }
                }
                if (bBeeper)
                    pos.POS_Beep(1, 5);
                if (bCutter && nCount == 1)
                    pos.POS_FullCutPaper();
                if (bDrawer)
                    pos.POS_KickDrawer(0, 100);

                if (nCount == 1) {
                    try {
                        Thread.currentThread();
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }else {
            return bPrintResult = -8; //查询失败
        }

        return bPrintResult;

    }


    public static String ResultCodeToString(int code) {
        switch (code) {
            case 3:
                return "出纸口有未取小票，请注意及时取走小票";
            case 2:
                return "紙将尽 且 出纸口有未取小票，请注意更换纸卷 和 及时取走小票";
            case 1:
                return "紙将尽，请注意更换纸卷";
            case 0:
                return " ";
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

    /**
     * 从Assets中读取图片
     */
    public static Bitmap getImageFromAssetsFile(Context ctx,String filename){
        Bitmap image = null;
        AssetManager am = ctx.getResources().getAssets();

        try {
            InputStream is = am.open(filename);
            image = BitmapFactory.decodeStream(is);
            is.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        return image;
    }

    public static Bitmap getTestImage1(int width,int height){
        Bitmap bitmap = Bitmap.createBitmap(width, 4, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(0,0,width,4,paint);
        return bitmap;
    }

    public static Bitmap getTestImage2(int width ,int height){
        Bitmap bitmap = Bitmap.createBitmap(width, 4, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        paint.setColor(Color.WHITE);
        canvas.drawRect(0,0,width,height,paint);

        for (int y= 0;y<height;y+=4){
            for(int x = y%32; x < width; x += 32){
                canvas.drawRect(x,y,x+4,y+4,paint);
            }
        }
        return bitmap;
    }
}






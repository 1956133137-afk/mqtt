package com.yannuo.dgcanteen.printer;

import com.csnprintersdk.csnio.CSNPOS;
import com.csnprintersdk.csnio.csnbase.CSNCOMIO;
import com.csnprintersdk.csnio.csnbase.CSNIOCallBack;
import com.yannuo.dgcanteen.model.PrinterTicker;
import com.yannuo.dgcanteen.util.LogUtil;
import com.yannuo.paylib.utils.EscapeUtil;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class SCNPrinterHelper implements CSNIOCallBack {
    private final String TAG = getClass().getSimpleName();
//    private final int mBaudrate  = 19200;
    private final int mBaudrate  = 9600;
//    private final String mPort = "/dev/ttyXRUSB1";
    private final String mPort = "/dev/ttyXRUSB1";
    private CSNPOS mPos;
    private CSNCOMIO mCom;
    private boolean isOpenPrinter = false;


    public SCNPrinterHelper() {
        mPos = new CSNPOS();
        mCom = new CSNCOMIO();
        mPos.Set(mCom);
        mCom.SetCallBack(this);



    }

    public void openPrinter(){
        Observable.just(1)
                .doOnNext(integer -> {
                    LogUtil.d(TAG,"正在连接打印机端口！");
                //    mCom.Open(mPort, mBaudrate, 1, 8 ,0,0,0);
//                    mPos.POS_SetBaudrate(19200);
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public int printerSomething(PrinterTicker data){
       return Prints.PrintTicket(mPos,data);
    }


    public void closePrinter(){
        mCom.Close();
    }

    @Override
    public void OnOpen() {
        isOpenPrinter = true;
        LogUtil.d(TAG,"打印机已连接！");
    }

    @Override
    public void OnOpenFailed() {
        isOpenPrinter = false;
        LogUtil.d(TAG,"打印机连接失败！");
    }

    @Override
    public void OnClose() {
        isOpenPrinter = false;
        LogUtil.d(TAG,"打印机连接异常中断！");

    }
}

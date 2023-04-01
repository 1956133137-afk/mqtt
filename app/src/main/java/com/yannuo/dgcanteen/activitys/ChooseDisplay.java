package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.ccb.smartcanteen.PayResultListener;
import com.ccb.smartcanteen.ZHSTFacePayService;
import com.google.gson.Gson;
import com.yannuo.dgcanteen.adapters.ShopsAdapter;
import com.yannuo.dgcanteen.databinding.ChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.interfaces.CallbackListener;
import com.yannuo.dgcanteen.model.CcbFacePayBean;
import com.yannuo.dgcanteen.util.LogUtil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

public class ChooseDisplay extends Presentation {

    private ChooseSecondDisplayBinding binding;
    private String TAG = getClass().getSimpleName();

    private CallbackListener listener;
    private ZHSTFacePayService  mFacePayService = null;
    private ShopsAdapter mShopsAdapter;

    public ChooseDisplay(Context outerContext, Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ChooseSecondDisplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
        initData();

//        binding.tvSecondName.setText("请支付");
        Intent lIntent =new Intent();
        lIntent.setAction("com.ccb.smartcanteen.FacePayService")  ;
        lIntent.setPackage("com.ccb.smartcanteen");
        getContext().bindService(lIntent, mServiceConnection, AppCompatActivity.BIND_AUTO_CREATE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getDisplay().getMetrics(displayMetrics);
        LogUtil.d(TAG,"开始绑定服务: "+displayMetrics.density);

    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG, " onServiceConnected");
            mFacePayService = ZHSTFacePayService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.d(TAG, " onServiceDisconnected");
        }

    };

    private void initView() {
        binding.btPayFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
//            @Override
//            public void onClick(View v) {
//                CcbFacePayBean bean = new CcbFacePayBean();
//                bean.setCAMPUS_ID("441999527");
//                bean.setCORP_ID("1041");
//                bean.setPAYMENT("0.01");
//                bean.setBUSINESS_ID("SJ2023032511004");
//                bean.setVPOS_ID("V00443832");
//                bean.setTXCODE("ZF0001") ;
//                bean.setOFFLINE("0");
//                try {
//                    mFacePayService.startFacePay(new Gson().toJson(bean), "0", new PayResultListener.Stub() {
//                        @Override
//                        public void onResult(String result) throws RemoteException {
//                            LogUtil.d(TAG,""+result);
//                            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
//                                    .subscribe(new Consumer<Integer>() {
//                                        @Override
//                                        public void accept(Integer integer) throws Exception {
//                                            show();
//                                        }
//                                    });
//                        }
//                    });
//                    dismiss();
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//
//                if (listener != null) {
//
//                }
//            }
        });
    }

    private void initData() {
        mShopsAdapter = new ShopsAdapter(getContext());
        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSecondDetail.setAdapter(mShopsAdapter);
        binding.rvSecondDetail.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

    }
//        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));
//        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "font/kai.ttf");
//        binding.tvSecondName.setTypeface(typeface);


//    }

    /*public void showWaitingView(){
        binding.btSecondSure.setEnabled(false);
        binding.ibSecondCancel.setEnabled(false);
        binding.tvSecondTextHit.setVisibility(View.VISIBLE);
    }

    public void noShowWaitingView(){
            binding.btSecondSure.setEnabled(true);
            binding.ibSecondCancel.setEnabled(true);
            binding.tvSecondTextHit.setVisibility(View.GONE);
    }*/


}

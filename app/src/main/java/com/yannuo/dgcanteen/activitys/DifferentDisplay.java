package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.yannuo.dgcanteen.databinding.ChooseSecondDisplayBinding;
import com.yannuo.dgcanteen.interfaces.CallbackListener;
import com.yannuo.dgcanteen.util.LogUtil;

import androidx.recyclerview.widget.LinearLayoutManager;

public class DifferentDisplay extends Presentation {
    private String TAG = getClass().getSimpleName();
    private ChooseSecondDisplayBinding binding;

    private CallbackListener listener;

    public DifferentDisplay(Context outerContext, Display display) {
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


    }



    private void initView() {

        binding.btPayFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSureListener(1,null);
                }
            }
        });

    }

    private void initData() {
        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));

    }




    public void setSureCallback(CallbackListener listener) {
        this.listener = listener;
    }

}

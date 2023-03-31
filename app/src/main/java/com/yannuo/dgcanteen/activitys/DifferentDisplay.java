package com.yannuo.dgcanteen.activitys;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;


import com.yannuo.dgcanteen.databinding.DifferrentDialogBinding;

import java.lang.ref.WeakReference;

import androidx.recyclerview.widget.LinearLayoutManager;

public class DifferentDisplay extends Presentation {

    private DifferrentDialogBinding binding;

    private CallbackListener listener;

    public DifferentDisplay(Context outerContext, Display display) {
        super(outerContext, display);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//        getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DifferrentDialogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
        initData();
    }



    private void initView() {
        binding.ibSecondCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (listener != null) {
//                    listener.onCancelListener();
//                }
               dismiss();
            }
        });

        binding.btSecondSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                MediaRouter mediaRouter = (MediaRouter) getContext().getSystemService(Context.MEDIA_ROUTER_SERVICE);
//                DisplayManager displayManager = (DisplayManager)  getContext().getSystemService(Context.DISPLAY_SERVICE);
//                Display[] displays = displayManager.getDisplays();
//                MediaRouter.RouteInfo route = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO);
//                if (route != null) {
//                    Display presentationDisplay = route.getPresentationDisplay();
//                    if (presentationDisplay != null) {
//                        ChooseDisplay   presentation = new ChooseDisplay( getContext(), displays[1]);
//                        presentation.show();
//                    }
//                }
                if (listener != null) {
                    listener.onSureListener(1,null);
                }
            }
        });

    }

    private void initData() {
        binding.rvSecondDetail.setLayoutManager(new LinearLayoutManager(getContext()));
//        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "font/kai.ttf");
//        binding.tvSecondName.setTypeface(typeface);

    }

    public void showWaitingView(){
        binding.btSecondSure.setEnabled(false);
        binding.ibSecondCancel.setEnabled(false);
        binding.tvSecondTextHit.setVisibility(View.VISIBLE);
    }

    public void noShowWaitingView(){
            binding.btSecondSure.setEnabled(true);
            binding.ibSecondCancel.setEnabled(true);
            binding.tvSecondTextHit.setVisibility(View.GONE);
    }


    public void setSureCallback(CallbackListener listener) {
        this.listener = listener;
    }

    public interface CallbackListener{
        void onSureListener(int event,Object object);

    }
}

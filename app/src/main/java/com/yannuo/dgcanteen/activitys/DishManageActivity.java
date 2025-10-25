package com.yannuo.dgcanteen.activitys;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.google.gson.Gson;
import com.tencent.mmkv.MMKV;
import com.yannuo.dgcanteen.adapters.DishesManageAdapter;
import com.yannuo.dgcanteen.adapters.DropDownAdapter;
import com.yannuo.dgcanteen.greendao.entity.DishesTable;
import com.yannuo.dgcanteen.greendao.entity.MealTable;
import com.yannuo.dgcanteen.greendao.dbHelper.DishesDBHelper;
import com.yannuo.dgcanteen.databinding.ActivityDishManageBinding;
import com.yannuo.dgcanteen.model.DishesInfo;
import com.yannuo.dgcanteen.model.MessageEvent;
import com.yannuo.dgcanteen.util.Constant;
import com.yannuo.dgcanteen.util.DisplayUtils;
import com.yannuo.dgcanteen.util.TimeUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DishManageActivity extends AppCompatActivity implements DishesManageAdapter.WorkListener, AdapterView.OnItemClickListener {

    private ActivityDishManageBinding binding;
    private List<MealTable> mealTables;
    private ArrayList<String> dataMeal;
    private ListView listView;
    private PopupWindow popup;
    private DishesManageAdapter adapterDishes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayUtils.setCustomDensity(1920, null, this, getApplication());
        initScreen();
        initView();
        initData();
        initObject();
        initEvent();
    }

    private void initView() {
        binding = ActivityDishManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initObject() {
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(new DropDownAdapter(this, dataMeal));
        listView.setOnItemClickListener(this);

        adapterDishes = new DishesManageAdapter(this);
        adapterDishes.setListener(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 6);
        binding.rvGridManage.setLayoutManager(gridLayoutManager);
        binding.rvGridManage.setAdapter(adapterDishes);
        binding.rvGridManage.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.bottom = 35;
            }
        });
        int mealId = TimeUtil.CurrentTimeSection();
        MealTable mealTable = DishesDBHelper.getInstance().queryToMeals(mealId);
        if (mealTable != null) binding.spinnerText.setText(mealTable.getMealName());
        dishesData(mealId);
    }

    private void initEvent() {
        binding.spinnerText.setOnClickListener(view -> popupWindow());
        binding.spinnerImg.setOnClickListener(view -> popupWindow());

        binding.ibtBack.setOnClickListener(view -> {
            Intent intent;
//            if (MMKV.defaultMMKV().decodeString(Constant.APP_MODE).equals(Constant.ORDERING_FOOD_MODE))
//                intent = new Intent(this, CommodityActivity.class);
//            else intent = new Intent(this, OrderMenuActivity.class);
            switch (Objects.requireNonNull(MMKV.defaultMMKV().decodeString(Constant.APP_MODE))){
                case Constant.ORDERING_FOOD_MODE:
                    intent = new Intent(this, CommodityActivity.class);
                    break;
                case Constant.NO_PIC_MODE:
                    intent = new Intent(this, CommodityOnActivity.class);
                    break;
                default:
                    intent = new Intent(this, OrderMenuActivity.class);
                    break;
            }
            startActivity(intent);
            finish();
        });
    }

    private void initData() {
        mealTables = new ArrayList<>();
        dataMeal = new ArrayList<>();
        mealTables = DishesDBHelper.getInstance().queryAllMeals();
        for (MealTable u : mealTables) {
            dataMeal.add(u.getMealName());
        }
    }

    private void popupWindow() {
        if (popup == null) popup = new PopupWindow();
        if (popup.isShowing()) popup.dismiss();
        else {
            popup.setWidth(binding.spinnerText.getWidth() + binding.spinnerImg.getWidth() - 15);
            popup.setHeight(600);
            popup.setContentView(listView);
            popup.setOutsideTouchable(true);
            popup.showAsDropDown(binding.spinnerText, 0, 0);
        }
    }

    @Override
    public void onEventClick(int position) {
        EventBus.getDefault().post(new MessageEvent(Constant.EVENT_FIFTH, null));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.d("TAG", "onItemClick: position："+position);
        binding.spinnerText.setText(dataMeal.get(position));
        popup.dismiss();
        runOnUiThread(() -> {
            int mealId = -1;
            for (MealTable u : mealTables) {
                if (dataMeal.get(position).equals(u.getMealName())) {
                    mealId = u.getMealId();
                }
            }
            dishesData(mealId);
        });
    }

    private void dishesData(int mealId) {
        List<DishesInfo> dataList = new ArrayList<>();
        List<DishesTable> list = new ArrayList<>();
        if (mealId == -1) {
            list = DishesDBHelper.getInstance().queryDishes();
        } else {
            list = DishesDBHelper.getInstance().queryDishesByMealId(mealId);
        }
        for (DishesTable u : list) {
            String imgUrl = (u.getImgUrl() == null || u.getImgUrl().isEmpty()) ? "" : u.getImgUrl();
            Integer status = u.getStatus();
            if (status == null) status = 1;
            dataList.add(new DishesInfo(
                    u.getDishesId(),
                    u.getDishesName(),
                    u.getMealId(),
                    null,
                    u.getPrice(),
                    u.getUnit(),
                    imgUrl,
                    status,
                    0
            ));
        }
        /* 按价格排序 */
        Collections.sort(dataList, new Comparator<DishesInfo>() {
            @Override
            public int compare(DishesInfo p1, DishesInfo p2) {
                int compareOne = String.valueOf(p1.getPrice()).compareTo(String.valueOf(p2.getPrice()));
                if (compareOne != 0) return compareOne;
                return p1.getDishesName().compareTo(p2.getDishesName());
            }
        });
        adapterDishes.setData(dataList);
    }

    private void initScreen() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().setAttributes(params);
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar
        if (Build.VERSION.SDK_INT >= 19) {
            uiFlags |= 0x00001000;  //SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide navigation bars - compatibility: building API level is lower thatn 19, use magic number directly for higher API target level
        } else {
            uiFlags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }

}
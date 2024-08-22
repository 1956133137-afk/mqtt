package com.yannuo.dgcanteen.activitys;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

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

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DishManageActivity extends AppCompatActivity implements DishesManageAdapter.WorkListener,AdapterView.OnItemClickListener {

    private ActivityDishManageBinding binding;
    private List<MealTable> mealTables;
    private ArrayList<String> dataMeal;
    private ListView listView;
    private PopupWindow popup;
    private DishesManageAdapter adapterDishes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();
        initView();
        initData();
        initObject();
        initEvent();
    }

    private void initView(){
        binding = ActivityDishManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void initObject(){
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(new DropDownAdapter(this,dataMeal));
        listView.setOnItemClickListener(this);

        adapterDishes = new DishesManageAdapter(this);
        adapterDishes.setListener(this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,6);
        binding.rvGridManage.setLayoutManager(gridLayoutManager);
        binding.rvGridManage.setAdapter(adapterDishes);
        binding.rvGridManage.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.bottom = 35;
            }
        });
        dishesData(0);
    }

    private void initEvent(){
        binding.spinnerText.setOnClickListener(view -> {
            popupWindow();
        });
        binding.spinnerImg.setOnClickListener(view -> {
            popupWindow();
        });

        binding.ibtBack.setOnClickListener(view -> {
            Intent intent ;
            if(MMKV.defaultMMKV().decodeString(Constant.APP_MODE).equals(Constant.ORDERING_FOOD_MODE))
                intent = new Intent(this,CommodityActivity.class);
            else intent = new Intent(this,OrderMenuActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void initData(){
        mealTables = new ArrayList<>();
        dataMeal = new ArrayList<>();
        mealTables = DishesDBHelper.getInstance().queryAllMeals();
        for (MealTable u : mealTables){
            dataMeal.add(u.getMealName());
        }
    }

    private void popupWindow(){
        popup = new PopupWindow();
        popup.setWidth(binding.spinnerText.getWidth() + binding.spinnerImg.getWidth() - 15);
        popup.setHeight(600);
        popup.setContentView(listView);
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(binding.spinnerText,0,0);
    }

    @Override
    public void onEventClick(int position) {
        EventBus.getDefault().post(new MessageEvent(Constant.EVENT_FIFTH, null));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        binding.spinnerText.setText(dataMeal.get(position));
        popup.dismiss();
        runOnUiThread(() -> {
            int mealId = 0;
            for (MealTable u : mealTables){
                if (dataMeal.get(position).equals(u.getMealName())){
                    mealId = u.getMealId();
                }
            }
            dishesData(mealId);
        });
    }

    private void dishesData(int mealId){
        List<DishesInfo> dataList = new ArrayList<>();
        List<DishesTable> list = new ArrayList<>();
        if (mealId == 0){
            list = DishesDBHelper.getInstance().queryDishes();
        }else {
            list = DishesDBHelper.getInstance().queryDishesByMealId(mealId);
        }
        for (DishesTable u : list){
            dataList.add(new DishesInfo(
                    u.getDishesId(),
                    u.getDishesName(),
                    u.getMealId(),
                    null,
                    u.getPrice(),
                    u.getUnit(),
                    u.getImgUrl(),
                    u.getStatus(),
                    0
            ));
        }
        adapterDishes.setData(dataList);
    }

    private void initScreen(){
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
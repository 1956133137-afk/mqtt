package com.yannuo.dgcanteen.activitys;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.adapters.DropDownAdapter;
import com.yannuo.dgcanteen.adapters.MealDataAdapter;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DishManageActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private final static String[] mealArray = {"早餐","午餐","晚餐"};
    private ArrayList<String> dataMeal;
    private TextView spinnerText;
    private ListView listView;
    private PopupWindow popup;
    private ImageButton spinnerImg;
    private List<DishesTable> mMealData;
    private GridView gridManage;
    private MealDataAdapter adapter;
    private DishesDBHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();
        setContentView(R.layout.activity_dish_manage);
        mHelper = DishesDBHelper.getInstance(this);
        initUI();
    }

    private void initUI(){
        findViewById(R.id.ibt_back).setOnClickListener(this);
        spinnerText = findViewById(R.id.spinner_text);
        spinnerText.setOnClickListener(this);
        spinnerImg = findViewById(R.id.spinner_img);
        spinnerImg.setOnClickListener(this);

        listView = new ListView(this);
        dataMeal = new ArrayList<String>();
        dataMeal.addAll(Arrays.asList(mealArray));
        listView.setDivider(null);
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(new DropDownAdapter(this,dataMeal));
        listView.setOnItemClickListener(this);

        gridManage = findViewById(R.id.grid_manage);
        mMealData = mHelper.queryDishes();
        adapter = new MealDataAdapter(this,mMealData,mHelper);
        gridManage.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.spinner_text:
            case R.id.spinner_img:
                popupWindow();
                break;
            case R.id.ibt_back:
                finish();
                break;
            default:break;
        }
    }
    private void popupWindow(){
        popup = new PopupWindow();
        popup.setWidth(spinnerText.getWidth() + spinnerImg.getWidth() - 15);
        popup.setHeight(600);
        popup.setContentView(listView);
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(spinnerText,0,0);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        spinnerText.setText(dataMeal.get(position));
        popup.dismiss();
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int i = 0;
                        if (dataMeal.get(position).equals("早餐")){
                            i = 1;
                        }else if (dataMeal.get(position).equals("午餐")){
                            i = 2;
                        }else if (dataMeal.get(position).equals("晚餐")){
                            i = 3;
                        }
                        mMealData = mHelper.queryDishesByMealId(i);
                        adapter = new MealDataAdapter(DishManageActivity.this,mMealData,mHelper);
                        gridManage.setAdapter(adapter);
//                      adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
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
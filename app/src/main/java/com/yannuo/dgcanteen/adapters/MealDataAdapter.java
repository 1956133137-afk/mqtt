package com.yannuo.dgcanteen.adapters;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.activitys.DifferentDisplay;
import com.yannuo.dgcanteen.dao.DishesTable;
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper;
import com.yannuo.dgcanteen.util.ToastShowUtil;

import java.util.List;

public class MealDataAdapter extends BaseAdapter {

    private Context mContext;
    private List<DishesTable> mMealData;

    private DishesDBHelper mHelper;
    private Display displays;
    private DifferentDisplay mProductsDisplay;

    public MealDataAdapter(Context context , List<DishesTable> mealData, DishesDBHelper mHelper){
        this.mContext = context;
        this.mMealData = mealData;
        this.mHelper = mHelper;
        initPresentation();
    }

    private void initPresentation() {
        MediaRouter mediaRouter = (MediaRouter) mContext.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        DisplayManager displayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        displays = displayManager.getDisplay(1);
        MediaRouter.RouteInfo route = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO);
        if (route != null) {
            Display presentationDisplay = route.getPresentationDisplay();
            if (presentationDisplay != null){
                mProductsDisplay = new DifferentDisplay( mContext, displays);
                mProductsDisplay.show();
            }
        }
    }

    @Override
    public int getCount() {
        return mMealData.size();
    }

    @Override
    public Object getItem(int position) {
        return mMealData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        ViewHolder holder;
        if (convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_grid_layout,null);
            holder = new ViewHolder();
            holder.igImg = convertView.findViewById(R.id.ig_img);
            holder.dropdownIc = convertView.findViewById(R.id.dropdown_ic);
            holder.dropdownBg = convertView.findViewById(R.id.dropdown_bg);
            holder.tvName = convertView.findViewById(R.id.tv_name);
            holder.tvPrice = convertView.findViewById(R.id.tv_price);
            holder.btnStatus = convertView.findViewById(R.id.btn_status);

            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        DishesTable mealData = mMealData.get(position);

        Glide.with(mContext).load(mealData.getImgUrl())
                .placeholder(R.drawable.ic_wait)
                .error(R.drawable.ic_error)
                .fallback(R.drawable.ic_unpictrue)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transform(new CenterCrop(), new GranularRoundedCorners(20f,20f,0,0))
                .into(holder.igImg);
        holder.tvName.setText(mealData.getDishesName());
        holder.tvPrice.setText("¥ " + mealData.getPrice());

        updateData(holder,mealData.getStatus());

        convertView.findViewById(R.id.btn_status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mealData.getStatus() == 0){
                    mealData.setStatus(1);
                    mHelper.updateDishes(mealData.getDishesId(), mealData.getMealId(), 1);
                    mProductsDisplay.dishesData();
                    ToastShowUtil.show(mContext,"菜品已经上架");
                }else {
                    mealData.setStatus(0);
                    mHelper.updateDishes(mealData.getDishesId(), mealData.getMealId(),0);
                    mProductsDisplay.dishesData();
                    ToastShowUtil.show(mContext,"菜品已经下架");
                }
                updateData(holder,mealData.getStatus());
            }
        });

        return convertView;
    }

    private void updateData(ViewHolder holder,int status){

        if (status == 1){
            holder.btnStatus.setText(R.string.under_shelf);
            holder.btnStatus.setTextColor(mContext.getColor(R.color.under_black));
            holder.btnStatus.setBackground(mContext.getDrawable(R.drawable.shape_btn_one));
            holder.dropdownIc.setVisibility(View.INVISIBLE);
            holder.dropdownBg.setVisibility(View.INVISIBLE);
        }else {
            holder.btnStatus.setText(R.string.shelf);
            holder.btnStatus.setTextColor(mContext.getColor(R.color.white));
            holder.btnStatus.setBackground(mContext.getDrawable(R.drawable.shape_btn_two));
            holder.dropdownIc.setVisibility(View.VISIBLE);
            holder.dropdownBg.setVisibility(View.VISIBLE);
        }

    }

    public final class ViewHolder{
        public ImageView igImg;
        public TextView dropdownIc;
        public ImageView dropdownBg;
        public TextView tvName;
        public TextView tvPrice;
        public Button btnStatus;
    }
}

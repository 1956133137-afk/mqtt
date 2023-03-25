package com.yannuo.dgcanteen.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.yannuo.dgcanteen.R;
import com.yannuo.dgcanteen.entity.MealData;
import com.yannuo.dgcanteen.util.ToastShowUtil;

import java.util.List;

public class MealDataAdapter extends BaseAdapter {

    private Context mContext;
    private List<MealData> mMealData;

    public MealDataAdapter(Context context , List<MealData> mealData){
        this.mContext = context;
        this.mMealData = mealData;
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

        MealData mealData = mMealData.get(position);
        holder.igImg.setImageResource(R.drawable.ic_drama);
        holder.tvName.setText(mealData.dishesName);
        holder.tvPrice.setText("¥ " + mealData.price);

        updateData(holder,mMealData.get(position).status);

        convertView.findViewById(R.id.btn_status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMealData.get(position).status == 0){
                    mMealData.get(position).status = 1;
                    ToastShowUtil.show(mContext,"菜品已经上架");
                }else {
                    mMealData.get(position).status = 0;
                    ToastShowUtil.show(mContext,"菜品已经下架");
                }
                updateData(holder,mMealData.get(position).status);
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

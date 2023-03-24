package com.yannuo.dgcanteen.entity;

import com.yannuo.dgcanteen.R;

import java.util.ArrayList;
import java.util.List;

public class MealData {
    public String dishesName;
    public double price;
    public int image;
    public int status;

    public MealData(String dishesName,double price,int image,int status){
        this.dishesName = dishesName;
        this.price = price;
        this.image = image;
        this.status = status;
    }

    private static String[] nameArray = {"茉莉绿茶","波霸奶茶","锦鲤红茶","四季春茶","清香乌龙茶","抹茶","红茶玛奇朵","乌龙玛奇朵","多多绿"};

    private static double[] priceArray = {9.9,11.2,6.8,7.8,9.9,12.5,15.99,12.9,8.4};

    private static int[] imgArray = {R.raw.image_one,R.raw.image_two,R.raw.image_thr,R.raw.image_fou,
            R.raw.image_fiv,R.raw.image_six,R.raw.image_sve,R.raw.image_eig,R.raw.image_nin};

    private static int[] statusArray = {1,1,1,1,1,1,1,1,1};

    public static List<MealData> getDefaultList() {
        List<MealData> mealList = new ArrayList<>();
        for (int i = 0; i < nameArray.length; i++){
            mealList.add(new MealData(nameArray[i],priceArray[i],imgArray[i],statusArray[i]));
        }
        return mealList;
    }

}

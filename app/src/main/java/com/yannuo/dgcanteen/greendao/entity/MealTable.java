package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Unique;

@Entity
public class MealTable {
    @Id
    private String mealId;  //餐别ID
    @NotNull
    private String mealName;  //餐别名称：早餐/午餐/晚餐/下午茶/夜宵等
    @NotNull
//    private String windowId;  //档口ID
    private Date startTime;  //餐别每日开始时间
    private Date endTime;  //餐别每日结束时间
    @Generated(hash = 249638237)
    public MealTable(String mealId, @NotNull String mealName,
            @NotNull Date startTime, Date endTime) {
        this.mealId = mealId;
        this.mealName = mealName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    @Generated(hash = 1028783363)
    public MealTable() {
    }
    public String getMealId() {
        return this.mealId;
    }
    public void setMealId(String mealId) {
        this.mealId = mealId;
    }
    public String getMealName() {
        return this.mealName;
    }
    public void setMealName(String mealName) {
        this.mealName = mealName;
    }
    public Date getStartTime() {
        return this.startTime;
    }
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    public Date getEndTime() {
        return this.endTime;
    }
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

}

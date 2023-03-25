package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.NotNull;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class MealTable {

    private String userId;  //用户ID
    private Long businessID;  //商家号ID
    @NotNull
    private String windowID;  //档口ID
    @NotNull
    private int mealID;  //餐别ID
    @NotNull
    private String mealName;  //餐别名称：早餐/午餐/晚餐/下午茶/夜宵等
    private Date startTime;  //餐别每日开始时间
    private Date endTime;  //餐别每日结束时间
    @Generated(hash = 193848820)
    public MealTable(String userId, Long businessID, @NotNull String windowID,
            int mealID, @NotNull String mealName, Date startTime, Date endTime) {
        this.userId = userId;
        this.businessID = businessID;
        this.windowID = windowID;
        this.mealID = mealID;
        this.mealName = mealName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    @Generated(hash = 1028783363)
    public MealTable() {
    }
    public String getUserId() {
        return this.userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public Long getBusinessID() {
        return this.businessID;
    }
    public void setBusinessID(Long businessID) {
        this.businessID = businessID;
    }
    public String getWindowID() {
        return this.windowID;
    }
    public void setWindowID(String windowID) {
        this.windowID = windowID;
    }
    public int getMealID() {
        return this.mealID;
    }
    public void setMealID(int mealID) {
        this.mealID = mealID;
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

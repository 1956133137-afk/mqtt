package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class VerifyDishes {
    @Id(autoincrement = true)
    private Long id;
    private String personName;
    private String dish;
    private String window;
    private String unDish;
    private String time;
    @Generated(hash = 1351256132)
    public VerifyDishes(Long id, String personName, String dish, String window,
            String unDish, String time) {
        this.id = id;
        this.personName = personName;
        this.dish = dish;
        this.window = window;
        this.unDish = unDish;
        this.time = time;
    }
    @Generated(hash = 799901656)
    public VerifyDishes() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getPersonName() {
        return this.personName;
    }
    public void setPersonName(String personName) {
        this.personName = personName;
    }
    public String getDish() {
        return this.dish;
    }
    public void setDish(String dish) {
        this.dish = dish;
    }
    public String getWindow() {
        return this.window;
    }
    public void setWindow(String window) {
        this.window = window;
    }
    public String getUnDish() {
        return this.unDish;
    }
    public void setUnDish(String unDish) {
        this.unDish = unDish;
    }
    public String getTime() {
        return this.time;
    }
    public void setTime(String time) {
        this.time = time;
    }

}

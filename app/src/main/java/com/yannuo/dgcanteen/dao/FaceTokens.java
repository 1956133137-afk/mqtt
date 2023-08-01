package com.yannuo.dgcanteen.dao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class FaceTokens {
    @Id(autoincrement = true)
    private Long id;

    @Unique
    private String number ; //人员 ID 编号

    @Index
    private String token ; //人员 特征

    @Generated(hash = 676724979)
    public FaceTokens(Long id, String number, String token) {
        this.id = id;
        this.number = number;
        this.token = token;
    }

    @Generated(hash = 1261144437)
    public FaceTokens() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

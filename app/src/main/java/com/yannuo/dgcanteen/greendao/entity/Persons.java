package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.Unique;

@Entity
public class Persons {
    //{"campusId":"441999527","cardId":"2230933173","createTime":"2023-10-17T16:08:08","custId":"100018148","delete_flag":0,"deptId":"a328673c76a145a1ab713bc431ac11f9","grade":"2023年下届",
    // "personName":"郑煜寿","personNumber":"12356","phone":"18300074086","sex":2,"updateTime":"2025-11-11T10:10:22","userClass":"基础班1","userId":"caced98abe5e493baf17877da3014189","userTypeId":"V0000245"}
    @Id(autoincrement = true)
    private Long id;
    @Index
    private String cardId;  //卡号
    @Unique()
    private String custId;//智慧食堂用户唯一标识（建行平台）
    private String grade;//年级
    private String personName;//人员姓名
    private String personNumber;// 学号/工号
    private String phone;//
    private String userClass;// 班级
    private String userId;   //用户ID
    private String image;// 人脸图片路径
    private String messageId; //消息ID
    @Generated(hash = 547563088)
    public Persons(Long id, String cardId, String custId, String grade, String personName, String personNumber, String phone, String userClass, String userId, String image, String messageId) {
        this.id = id;
        this.cardId = cardId;
        this.custId = custId;
        this.grade = grade;
        this.personName = personName;
        this.personNumber = personNumber;
        this.phone = phone;
        this.userClass = userClass;
        this.userId = userId;
        this.image = image;
        this.messageId = messageId;
    }
    @Generated(hash = 1519000671)
    public Persons() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getCardId() {
        return this.cardId;
    }
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }
    public String getCustId() {
        return this.custId;
    }
    public void setCustId(String custId) {
        this.custId = custId;
    }
    public String getGrade() {
        return this.grade;
    }
    public void setGrade(String grade) {
        this.grade = grade;
    }
    public String getPersonName() {
        return this.personName;
    }
    public void setPersonName(String personName) {
        this.personName = personName;
    }
    public String getPersonNumber() {
        return this.personNumber;
    }
    public void setPersonNumber(String personNumber) {
        this.personNumber = personNumber;
    }
    public String getPhone() {
        return this.phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getUserClass() {
        return this.userClass;
    }
    public void setUserClass(String userClass) {
        this.userClass = userClass;
    }
    public String getUserId() {
        return this.userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getImage() {
        return this.image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public String getMessageId() {
        return this.messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

}

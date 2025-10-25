package com.yannuo.dgcanteen.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;


@Entity
public class CategoryTable {
        @Id(autoincrement = true)
        private Long id;
        private String categoryId;
        private String categoryName;
        private String sort;
        private int mealId;

        @Generated(hash = 1679078959)
        public CategoryTable() {
        }

        @Generated(hash = 816922273)
        public CategoryTable(Long id, String categoryId, String categoryName, String sort, int mealId) {
            this.id = id;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.sort = sort;
            this.mealId = mealId;
        }

        public int getMealId() {
            return mealId;
        }

        public void setMealId(int mealId) {
            this.mealId = mealId;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(String categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }

        @Override
        public String toString() {
            return "CategoryTable{" +
                    "id=" + id +
                    ", categoryId='" + categoryId + '\'' +
                    ", categoryName='" + categoryName + '\'' +
                    ", sort='" + sort + '\'' +
                    ", mealId=" + mealId +
                    '}';
        }
}

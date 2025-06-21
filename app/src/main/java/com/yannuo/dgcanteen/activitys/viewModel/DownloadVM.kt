package com.yannuo.dgcanteen.activitys.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.repositorys.PayRepositoryOfPay
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.model.CategoryBean
import com.yannuo.dgcanteen.model.DateMenu
import com.yannuo.dgcanteen.model.DishBean
import com.yannuo.dgcanteen.model.MealMenu
import com.yannuo.dgcanteen.model.MealSizeBean
import com.yannuo.dgcanteen.model.MealSizeReceive
import com.yannuo.dgcanteen.model.OrderMeal
import com.yannuo.dgcanteen.model.PayCfg
import com.yannuo.dgcanteen.model.RequeCategoryIdBean
import com.yannuo.dgcanteen.model.SelectDateBean
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.TimeUtil
import com.yannuo.dgcanteen.util.ToastShowUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Date

/**
 * Author: filowl
 * Description: ***
 * Date: 2024/9/23 18:28
 **/
class DownloadVM : ViewModel() {
    private val TAG = javaClass.simpleName
    private val kv = MMKV.defaultMMKV()
    private val DAY_TIME: Long = 86400000L
    private val mRepository: PayRepositoryOfPay = PayRepositoryOfPay()
    private val payCfg = kv.decodeParcelable(Constant.PAY_CONFIG, PayCfg::class.java) ?: PayCfg()
    private val mealList: MutableList<OrderMeal> = mutableListOf()
    private val dishList: MutableList<DishBean> = mutableListOf()
    private val menuList: MutableList<DateMenu> = mutableListOf()
    private var orderSize: Int = 0

    private val mHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        LogUtil.e(TAG, "Exception: $throwable")
        throwable.printStackTrace()
    }

    fun getMenuList(): MutableList<DateMenu> = menuList

    fun setMenuList(list: MutableList<DateMenu>) {
        menuList.clear()
        menuList.addAll(list)
    }

    fun synOrderMeal(ccbToken: String, boolean: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            if (payCfg.campusId.isEmpty() || payCfg.businessId.isEmpty()) return@launch
            boolean(false)
            mealList.clear()
            val orderMeal = mRepository.queryOrderMeal(ccbToken, payCfg.campusId, payCfg.businessId)
            LogUtil.d(TAG, "云端获取菜品信息："+Gson().toJson(orderMeal))
            if (orderMeal.code == "200") {
                val orderMealList = orderMeal.data?.orderMealList
                val delFlag = orderMeal.data?.delFlag ?: "1"
                if (orderMealList != null && orderMealList.size > 0 && delFlag != "2") mealList.addAll(orderMealList)
            } else withContext(Dispatchers.Main) { ToastShowUtil.show("同步餐别失败") }
            boolean(true)
        }
    }

    /**
     * 获取菜品列表
     */
    fun synOrderDish(ccbToken: String, date: String, mealId: String, custId: String, res: (Boolean, MutableList<DishBean>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            if (payCfg.businessId.isEmpty()) return@launch
            dishList.clear()
            res(false, dishList)
            /*获取用户餐别已订份数*/
            queryMealOrderSize(ccbToken, date, mealId, custId)
            /*获取餐别菜品信息*/
            val orderDish = mRepository.queryOrderDish(ccbToken, date, mealId, payCfg.businessId, payCfg.campusId)
            if (orderDish.code == "200") {
                val orderDishList = orderDish.data?.batchDishes
                if (orderDishList != null && orderDishList.size > 0) {
                    LogUtil.d(TAG, Gson().toJson(orderDishList))
                    orderDishList.forEach { dish ->
                        if (dish.status == "1") {
                            val bean = DishBean().apply {
                                dishId = dish.dishesId
                                dishName = dish.dishesName
                                dishPrice = dish.price
                                dishUnit = dish.unit
                                dishCount = queryDishNumber(date, mealId, dishId)
                                imgUrl = dish.imgUrl ?: ""
                                windowIdList = dish.windowIdList
                                description = if (dish.description.isNullOrEmpty()) "" else dish.description
                                orderMealQuota = if (dish.orderMealQuota.isNullOrEmpty()) "" else dish.orderMealQuota
                                orderMealQuotaNum = if (dish.orderMealQuotaNum.isNullOrEmpty()) "" else dish.orderMealQuotaNum
                                categoryId = if(!dish.categoryId.isNullOrEmpty()) dish.categoryId else ""
                            }
                            dishList.add(bean)
                        }
                        downloadImgUrl(dish.imgUrl)
                    }
                }
                if (dishList.size < 1) withContext(Dispatchers.Main) { ToastShowUtil.show("未设置菜品信息") }
            } else withContext(Dispatchers.Main) { ToastShowUtil.show("同步菜品失败") }
            res(true, dishList)
        }
    }

    /**
     * 获取类别Id列表
     */
    fun queryCategoryIdList(ccbToken: String, idList: MutableList<String>,queryDate: String, function: (MutableList<CategoryBean>) -> Unit){
        viewModelScope.launch(Dispatchers.IO + mHandler) {
            val bean = RequeCategoryIdBean().apply {
                businessId = payCfg.businessId
                campusId = payCfg.campusId
                date = queryDate
                categoryIdList = idList
            }
            val categoryIdList = mRepository.getCategoryIdList(ccbToken, bean)
            if (categoryIdList.code == "200"){
                val data = categoryIdList.data
                val list = mutableListOf<CategoryBean>()
                data?.forEach {
                    val categoryBean = Gson().fromJson(it, CategoryBean::class.java)
                    list.add(categoryBean)
                }
                function(list)
            }
        }
    }
    fun queryMealOrderSize(ccbToken: String, date: String, mealId: String, custId: String) {
        orderSize = runBlocking(Dispatchers.IO + mHandler) {
            val bean = MealSizeBean(payCfg.campusId, date, mealId, custId)
            val receive = mRepository.getMealOrderSize(ccbToken, bean)
            if (receive.code == "200") {
                val fromJson = Gson().fromJson(Gson().toJson(receive.data), MealSizeReceive::class.java)
                fromJson.num
            } else 0
        }
        kv.encode(Constant.ORDER_MEAL_SIZE, orderSize)
    }

    private fun queryDishNumber(date: String, mealId: String, dishId: String): Int {
        val dateMenu1 = menuList.find { it.date == date }
        if (dateMenu1 != null) {
            val mealMenu1 = dateMenu1.mealList.find { it.mealId == mealId }
            if (mealMenu1 != null) {
                val dishMenu1 = mealMenu1.dishList.find { it.dishId == dishId }
                if (dishMenu1 != null) return dishMenu1.dishCount
            }
        }
//        menuList.forEach { dateMenu ->
//            if (dateMenu.date == date) dateMenu.mealList.forEach { mealMenu ->
//                if (mealMenu.mealId == mealId) mealMenu.dishList.forEach { dishMenu ->
//                    if (dishMenu.dishId == dishId) return dishMenu.dishCount
//                }
//            }
//        }
        return 0
    }

    fun getDateWeek(): MutableList<SelectDateBean> {
        val beanList = mutableListOf<SelectDateBean>()
        for (i in 0..kv.decodeInt(Constant.ORDER_ADVANCE_DAY, 6)) {
            val millis = System.currentTimeMillis() + i * DAY_TIME
            val dateFormat = TimeUtil.timeFormat("yyyy-MM-dd", millis)
            val dateBean = SelectDateBean()
            dateBean.date = dateFormat
            dateBean.value = getWeekDay(dateFormat)
            dateBean.mealList = getDayMeal(dateFormat, dateBean.value)
            beanList.add(dateBean)
        }
        return beanList
    }

    private fun getDayMeal(date: String, value: String): MutableList<OrderMeal> {
        LogUtil.d(TAG, "data: $date, value: $value")
        val orderMeal: MutableList<OrderMeal> = mutableListOf()
        mealList.forEach {
            /*餐别时间是否已过*/
            if (!isJudgeTime("$date ${it.endTime}")) return@forEach
            /*是否使用限制*/
            if (!kv.decodeBool(Constant.ORDER_MEAL_LIMIT, false)) {
                orderMeal.add(it)
                return@forEach
            }
            /*判断后台限制*/
            val split = it.orderMealDay.split(", ".toRegex())
            if (split.contains(value) && it.delFlag != "1" && isJudgeLimitTime(date, it)) orderMeal.add(it)
        }
        return orderMeal
    }

    private fun getWeekDay(date: String): String {
        if (!isValidDate(date)) return ""
        val split = date.split("-".toRegex())
        val calendar = Calendar.getInstance()
        calendar.set(split[0].toInt(), split[1].toInt() - 1, split[2].toInt())
        return when (calendar[Calendar.DAY_OF_WEEK]) {
            Calendar.SUNDAY -> "7"
            Calendar.MONDAY -> "1"
            Calendar.TUESDAY -> "2"
            Calendar.WEDNESDAY -> "3"
            Calendar.THURSDAY -> "4"
            Calendar.FRIDAY -> "5"
            Calendar.SATURDAY -> "6"
            else -> "0"
        }
    }

    /*判断餐别结束时间*/
    private fun isJudgeTime(time: String): Boolean {
        val timeMillis = Date(time.replace("-", "/")).time
        return timeMillis >= System.currentTimeMillis()
    }

    /*判断限制时间*/
    private fun isJudgeLimitTime(date: String, orderMeal: OrderMeal): Boolean {
        val time = "$date ${orderMeal.orderDelineTime}".replace("-", "/")
        val timeMillis = Date(time).time
        return (timeMillis - orderMeal.orderDelineDate.toLong() * DAY_TIME) > System.currentTimeMillis()
    }

    private fun isValidDate(dateFormat: String): Boolean {
        // yyyy-MM-dd
        val regex = Regex("""^\d{4}-\d{2}-\d{2}$""", RegexOption.IGNORE_CASE)
        return regex.matches(dateFormat)
    }

    private fun downloadImgUrl(imgUrl: String?) {
        if (imgUrl == null || imgUrl.isEmpty()) return
        val fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1)
        val fileDir = File(MyApplication.applicationContext.filesDir.absolutePath, Constant.PIC_DIR)
        val filePath = File("${fileDir.path}/${fileName}")
        if (filePath.exists()) return
        //下载图片
        val bitmap = runBlocking {
            withContext(Dispatchers.IO) {
                try { //图片加载失败返回null
                    Glide.with(MyApplication.applicationContext)
                        .asBitmap()
                        .load(imgUrl)
                        .submit()
                        .get()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
        //保存图片
        if (bitmap != null) {
            var fos: FileOutputStream? = null
            try {
                if (!fileDir.exists()) fileDir.mkdirs()
                if (!filePath.exists()) filePath.createNewFile()
                fos = FileOutputStream(filePath)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    fos?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * @param dateBean 日期
     * @param mealBean 餐别
     * @param dishBean 菜品
     */
    fun selectDateMealDish(dateBean: SelectDateBean, mealBean: OrderMeal?, dishBean: DishBean): MutableList<DateMenu> {
        if (mealBean == null) return menuList
        var dateIndex = -1
        menuList.forEachIndexed { index1, dateMenu ->
            if (dateMenu.date == dateBean.date) {
                dateIndex = index1
                var mealIndex = -1
                dateMenu.mealList.forEachIndexed { index2, mealMenu ->
                    if (mealBean.mealId == mealMenu.mealId) {
                        mealIndex = index2
                        var dishIndex = -1
                        mealMenu.dishList.forEachIndexed { index3, dishMenu ->
                            if (dishBean.dishId == dishMenu.dishId) {
                                dishIndex = index3
                                dishMenu.dishCount = dishBean.dishCount
                            }
                        }
                        if (dishIndex == -1) mealMenu.dishList.add(dishBean)
                        if (dishIndex != -1 && mealMenu.dishList[dishIndex].dishCount == 0) mealMenu.dishList.removeAt(dishIndex)
                    }
                }
                if (mealIndex == -1) dateMenu.mealList.add(addMealMenu(mealBean, dishBean))
                if (mealIndex != -1 && dateMenu.mealList[mealIndex].dishList.size < 1) dateMenu.mealList.removeAt(mealIndex)
            }
        }
        if (dateIndex == -1) {
            val date = DateMenu()
            date.date = dateBean.date
            date.value = dateBean.value
            date.mealList.add(addMealMenu(mealBean, dishBean))
            menuList.add(date)
        }
        if (dateIndex != -1 && menuList[dateIndex].mealList.size < 1) menuList.removeAt(dateIndex)
        // 排序
        menuList.sortWith(compareBy { it.date })
        menuList.forEach { dateMenu -> dateMenu.mealList.sortWith(compareBy { it.startTime }) }
        return menuList
    }

    private fun addMealMenu(mealBean: OrderMeal, dishBean: DishBean): MealMenu {
        val meal = MealMenu().apply {
            mealId = mealBean.mealId
            mealName = mealBean.mealName
            startTime = mealBean.startTime
            endTime = mealBean.endTime
            isLimit = mealBean.orderQuota == "1"
            limitSize = mealBean.orderQuotaNum.toInt()
            size = orderSize
            dishList.add(dishBean)
        }
        return meal
    }
}
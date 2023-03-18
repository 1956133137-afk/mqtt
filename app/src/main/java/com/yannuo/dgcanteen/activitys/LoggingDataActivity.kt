package com.yannuo.dgcanteen.activitys

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.presenters.LoggingDataPresenter
import com.yannuo.dgcanteen.adapters.ProductAddAdapter
import com.yannuo.dgcanteen.dao.ProductsTable
import com.yannuo.dgcanteen.dao.dbhelp.DbHelper
import com.yannuo.dgcanteen.databinding.ActivityLoggingDataBinding
import com.yannuo.dgcanteen.util.LogUtil
import com.yannuo.dgcanteen.util.ScanDevice
import com.yannuo.dgcanteen.util.ToastShowUtil
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


class LoggingDataActivity :BaseActivity<ActivityLoggingDataBinding>(), View.OnClickListener  {
    private lateinit var mPresenter : LoggingDataPresenter
    private lateinit var mAdapter : ProductAddAdapter
    private val REQUEST_IMAGE_GET = 101
    private var picPath :String ?= null   //选择的图片路径
    private var mLock = AtomicBoolean(false) // 按钮锁
    private lateinit var photosPath :String //商品图片存放路径
    private var editPicName  :String ?= null   //编辑保存的中间图片名称


    override fun bindLayout() {
        binding = ActivityLoggingDataBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initInstance()
        initEvent()
        initData()

    }


    private fun initInstance() {
        photosPath =  filesDir?.absolutePath.let {
            "$it/myPic/"
        }
        mPresenter = LoggingDataPresenter(this)
        mAdapter = ProductAddAdapter(photosPath)

    }

    private fun initEvent() {
        binding.btSelector.setOnClickListener(this)
        binding.ivPic.setOnClickListener(this)
        binding.imageButton.setOnClickListener(this)

    }



    private fun initData() {
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = mAdapter
        mAdapter.setListener(ProductChangeEvent())
        mAdapter.data = DbHelper.getInstance().queryProducts()
        mPresenter.addScanListener(ScanCallback())
        binding.rvProducts.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            ))
    }





    @SuppressLint("CheckResult")
    override fun onClick(v: View) {
        when(v.id){
            binding.imageButton.id ->{
                val intent = Intent(this,CommodityActivity::class.java)
                startActivity(intent)
                finish()
            }

            binding.ivPic.id ->{
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, REQUEST_IMAGE_GET)
                }
            }
            //创建一条商品
            binding.btSelector.id ->{
                mLock.set(true)

                Observable.create(ObservableOnSubscribe<Int> {
                    val bean = createData()
                    if (bean == null){
                        it.onNext(1)
                        it.onComplete()
                        return@ObservableOnSubscribe
                    }
                    //TODO 判断商品是否之前录入了
                    DbHelper.getInstance().queryProduct(bean.barCode)?.apply {
                        it.onNext(2)
                        it.onComplete()
                        return@ObservableOnSubscribe
                    }
                    mPresenter.saveProductData(picPath,bean,null,editPicName.isNullOrEmpty())
                    it.onNext(3)
                    it.onComplete()
                }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Int> {
                        override fun onNext(it: Int) {
                            when(it){
                                1->{
                                    ToastShowUtil.show("请补充完成商品信息")
                                }
                                2 ->{
                                    AlertDialog.Builder(this@LoggingDataActivity)
                                        .setTitle("小提示！")
                                        .setMessage("该商品之前已添加，是否更新此商品。")
                                        .setCancelable(false)
                                        .setNeutralButton("取消",null)
                                        .setPositiveButton("确定"
                                        ) { dialog, which ->
                                            val bean = createData()
                                            DbHelper.getInstance().queryProduct(bean!!.barCode)?.apply {
                                                mPresenter.saveProductData(picPath,bean,this.pictureName,editPicName.isNullOrEmpty())
                                                mAdapter.insertOrUpdate(bean)
//                                                binding.rvProducts.layoutManager?.scrollToPosition(0)
                                                binding.rvProducts.scrollToPosition(0)
                                            }
                                        }.show()
                                }
                                3->{
                                    val bean = createData()
                                    bean?.pictureName = picPath!!.substring(picPath!!.lastIndexOf("/"))
                                    mAdapter.insertOrUpdate(bean!!)
//                                    binding.rvProducts.layoutManager?.scrollToPosition(0)
                                    binding.rvProducts.scrollToPosition(0)
                                }
                            }
                        }

                        override fun onSubscribe(d: Disposable) {}
                        override fun onError(e: Throwable) {}
                        override fun onComplete() {}
                    })
            }
        }
    }

    private fun createData() : ProductsTable?{
        val barcode = binding.tvBarCode.text.toString()
        val type = binding.tvProductType.text.toString()
        val name = binding.tvProductName.text.toString()
        val price = binding.tvPrice.text.toString()

        if (barcode.isEmpty() or
            type.isEmpty() or
            name.isEmpty() or
            price.isEmpty() or
            (picPath.isNullOrEmpty() && editPicName.isNullOrEmpty())){
            return null
        }

        val bean = ProductsTable()
        bean.barCode = barcode
        bean.pName = name
        bean.pMoney = price
        bean.type = type
        return bean
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dat: Intent?) {
        super.onActivityResult(requestCode, resultCode, dat)
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            try {
                dat?.data?.also {
                    LogUtil.d(TAG, "$it")
                    picPath =  mPresenter.getPicturePath(it)

                    if (picPath.isNullOrEmpty()) {
                        ToastShowUtil.show("该图片无权限操作")
                    }else {
                        binding.ivPic.setImageURI(it)
                        editPicName = null  //有更新变动就置空
                    }
                }
            }catch(e: Exception){
                e.printStackTrace()
            }
        }

    }



    override fun onBackPressed() {

    }

    inner class ScanCallback : ScanDevice.DataCallBack{
        override fun onData(data: String) {
            runOnUiThread {
                binding.tvBarCode.setText(data)
            }
        }
    }

    inner class ProductChangeEvent :ProductAddAdapter.WorkListener{
        override fun onEventClick(evnt :Int ,position: Object?) {
            var bean =  position as? ProductsTable
            bean?.also {
                when(evnt){
                    0->{
                        LogUtil.d(TAG,"event 编辑")
                        binding.tvBarCode.setText(it.barCode)
                        binding.tvProductType.setText(it.type)
                        binding.tvProductName.setText(it.pName)
                        binding.tvPrice.setText(it.pMoney)

                        Glide.with(this@LoggingDataActivity)
                            .load("$photosPath${it.pictureName}")
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .placeholder(R.drawable.no_picture)
                            .into(binding.ivPic)

                        editPicName = it.pictureName
                    }
                    1->{
                        LogUtil.d(TAG,"event 删除")
                        File("$photosPath${it.pictureName}").delete() // 删除图片
                        mPresenter.deleteProduct(it)
                    }
                }
            }
        }

    }
}
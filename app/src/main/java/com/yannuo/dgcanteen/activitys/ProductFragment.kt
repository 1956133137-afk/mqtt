package com.yannuo.dgcanteen.activitys

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.yannuo.dgcanteen.activitys.viewModel.ProductsVM
import com.yannuo.dgcanteen.adapters.ProductsAdapter
import com.yannuo.dgcanteen.dao.dbhelp.DishesDBHelper
import com.yannuo.dgcanteen.databinding.FragmentProductBinding
import com.yannuo.dgcanteen.model.DishesInfo
import com.yannuo.dgcanteen.util.LogUtil


open class ProductFragment : Fragment, ProductsAdapter.WorkListener {
    private val TAG = javaClass.simpleName
    lateinit var binding:FragmentProductBinding

    private var pager = 0
    private var title :Int? =null
    private lateinit var adapter :ProductsAdapter
    private lateinit var model : ProductsVM

    constructor():super(){

    }

    constructor(mealId : Int?):super(){
        title = mealId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObject()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        initView()
        initEvent()
        return binding.root
    }


    private fun initObject() {
        adapter = ProductsAdapter(title,context)
        adapter.setListener(this)

    }

    override fun onResume() {
        super.onResume()
        Log.d("ning", title.toString())
        val dataList = mutableListOf<DishesInfo>()
//        val direction =  context?.filesDir?.absolutePath.let {
//            "$it/myPic/"
//        }
//        DbHelper.getInstance().queryProductsByType("1")
//        DishesDBHelper.getInstance(activity).queryDishesByMealId(1)
        val list = DishesDBHelper.getInstance(activity).queryDishesByStatus(1)
        list.forEach {
//                val path = it.pictureName.let {
//                    "$direction$it"
//                }
            dataList.add(DishesInfo (
                it.dishesId,
                it.dishesName,
                it.mealId,
                it.windowId,
                it.price,
                it.unit,
                it.imgUrl,
                it.status
            ))
        }
        adapter.data = dataList
    }


    //
    private fun initView() {

        val gridLayoutManager = GridLayoutManager(context,4)
        binding.rvManInfo.layoutManager = gridLayoutManager
        binding.rvManInfo.adapter = adapter
        adapter.setImgSize(gridLayoutManager)


    }

    private fun initEvent() {
        model = ViewModelProvider(requireActivity()).get(ProductsVM::class.java)
        model.receiveCountNotify.observe(viewLifecycleOwner) {
           //全部清空
            if (it == null){
                adapter.data.forEachIndexed { index, it ->
                    if (it.count !=0){
                        it.count = 0
                        adapter.notifyItemChanged(index,"count")
                    }
                }
            }else {
                LogUtil.d(TAG, it.imgUrl)

                //相同的类型就进行页面更新
//                if (it.type.equals(title)) {
//                    adapter.update(it)
//                }
                adapter.data.indexOf(it).apply {
                    adapter.notifyItemChanged(this, "count")
                }
            }
        }
//        binding.rvManInfo.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    override fun onEventClick(position: Int) {
        model.sendCountNotify.postValue(adapter.getData(position))
    }





}
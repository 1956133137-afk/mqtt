package com.yannuo.dgcanteen.activitys

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.fragment.NavHostFragment
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ActivityHostBinding
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil


class HostActivity : BaseActivity<ActivityHostBinding>() {


    override fun bindLayout() {
        binding = ActivityHostBinding.inflate(layoutInflater)

    }

    override fun onInit() {
        val payInfo = intent.getParcelableExtra<OrderPayInfo>(Constant.PAY_DATE)
        if (payInfo == null) {
            LogUtil.e(TAG, "非法参数")
            return
        }
        val navHostFragment = supportFragmentManager.findFragmentById(binding.mainFragmentContainer.id) as NavHostFragment?
        val bundle = Bundle()
        bundle.putParcelable(Constant.PAY_DATE, payInfo)
        LogUtil.d(TAG,"onInit")
        if (payInfo.type != Constant.PAY_FACE_TYPE) {
            navHostFragment?.navController!!.setGraph(R.navigation.ic_graph, bundle)
//            navHostFragment!!.navController.navigate(R.id.scanFragment, bundle)
        } else {
            navHostFragment?.navController!!.setGraph(R.navigation.nav_graph, bundle)
        }
    }

    override fun onResume() {
        super.onResume()
//        supportFragmentManager.isStateSaved
//        super.onPostResume()
        LogUtil.d(TAG,"ON RESUME")
    }


    override fun onBackPressed() {
    }

}



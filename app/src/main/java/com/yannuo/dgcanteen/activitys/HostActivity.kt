package com.yannuo.dgcanteen.activitys

import android.os.Bundle
import android.os.PersistableBundle
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.databinding.ActivityHostBinding
import com.yannuo.dgcanteen.model.OrderPayInfo
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.LogUtil


class HostActivity : BaseActivity<ActivityHostBinding>() {
    private var navHostFragment: NavHostFragment? = null

    override fun bindLayout() {
        binding = ActivityHostBinding.inflate(layoutInflater)

    }

    override fun onInit() {
        val payInfo = Gson().fromJson(intent.getStringExtra(Constant.PAY_DATE), OrderPayInfo::class.java)
        if (payInfo == null) {
            LogUtil.e(TAG, "非法参数")
            return
        }
        navHostFragment = supportFragmentManager.findFragmentById(binding.mainFragmentContainer.id) as NavHostFragment?
        val bundle = Bundle()
        bundle.putParcelable(Constant.PAY_DATE, payInfo)
        LogUtil.d(TAG, "onInit")
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
        LogUtil.d(TAG, "ON RESUME")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        if (navHostFragment != null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.remove(navHostFragment!!)
            fragmentTransaction.commit()
        }
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onBackPressed() {
    }

}



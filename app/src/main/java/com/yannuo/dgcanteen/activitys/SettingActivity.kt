package com.yannuo.dgcanteen.activitys

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.fragment.BasicSettingFragment
import com.yannuo.dgcanteen.activitys.fragment.CameraSettingFragment
import com.yannuo.dgcanteen.activitys.fragment.FaceSettingFragment
import com.yannuo.dgcanteen.activitys.fragment.ModeSettingFragment
import com.yannuo.dgcanteen.databinding.ActivitySettingBinding
import com.yannuo.dgcanteen.util.LogUtil

class SettingActivity : BaseActivity<ActivitySettingBinding>() {

    private lateinit var basicFragment: BasicSettingFragment
    private lateinit var modeFragment: ModeSettingFragment
    private lateinit var cameraFragment: CameraSettingFragment
    private lateinit var faceFragment: FaceSettingFragment
    private lateinit var fragments: Array<Fragment>

    override fun bindLayout() {
        binding = ActivitySettingBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initFragment()
        initView()
        initEvent()
        initListener()
    }

    private fun initFragment() {
        basicFragment = BasicSettingFragment()
        modeFragment = ModeSettingFragment()
        cameraFragment = CameraSettingFragment()
        faceFragment = FaceSettingFragment()
        fragments = arrayOf(basicFragment, modeFragment, cameraFragment, faceFragment)
    }

    private fun initView() {
        binding.radioGroup.check(R.id.basic)
    }

    private fun initEvent() {
        binding.ibtBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener {
            when (binding.viewPager.currentItem) {
                0 -> basicFragment.save()
                1 -> modeFragment.save()
                2 -> LogUtil.d(TAG, "2")
                3 -> LogUtil.d(TAG, "3")
            }
        }
        binding.viewPager.adapter = object :
            FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int {
                return fragments.size
            }

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }
        }
    }

    private fun initListener() {
        //滑动切换界面
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.radioGroup.check(R.id.basic)
                    1 -> binding.radioGroup.check(R.id.mode)
                    2 -> binding.radioGroup.check(R.id.camera)
                    3 -> binding.radioGroup.check(R.id.face)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}

        })
        //点击切换界面
        binding.radioGroup.setOnCheckedChangeListener { radioGroup, id ->
            when (id) {
                R.id.basic -> binding.viewPager.currentItem = 0
                R.id.mode -> binding.viewPager.currentItem = 1
                R.id.camera -> binding.viewPager.currentItem = 2
                R.id.face -> binding.viewPager.currentItem = 3
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
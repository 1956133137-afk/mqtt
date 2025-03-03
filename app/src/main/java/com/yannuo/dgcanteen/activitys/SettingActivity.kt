package com.yannuo.dgcanteen.activitys

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.yannuo.dgcanteen.R
import com.yannuo.dgcanteen.activitys.fragment.*
import com.yannuo.dgcanteen.common.MyApplication
import com.yannuo.dgcanteen.databinding.ActivitySettingBinding

class SettingActivity : BaseActivity<ActivitySettingBinding>() {

    private lateinit var basicFragment: BasicSettingFragment
    private lateinit var modeFragment: ModeSettingFragment
    private lateinit var faceFragment: FaceSettingFragment
    private lateinit var payOrderFragment: PayOrderFragment
    private lateinit var mealTimePayOrderFragment: MealTimePayOrderFragment
    private lateinit var olOrderFragment: OfflineOrderFragment
    private lateinit var verifyOrderFragment: VerifyOrderFragment
    private lateinit var deviceFragment: DeviceInfoFragment
    private lateinit var fragments: Array<Fragment>

    private lateinit var displayManager: DisplayManager
    private lateinit var secondDisplays: Display
    private lateinit var settingDisplay: SettingDisplay

    override fun bindLayout() {
        binding = ActivitySettingBinding.inflate(layoutInflater)
    }

    override fun onInit() {
        initFragment()
        initPresentation()
        initEvent()
    }

    private fun initFragment() {
        basicFragment = BasicSettingFragment()
        modeFragment = ModeSettingFragment()
        faceFragment = FaceSettingFragment()
        payOrderFragment = PayOrderFragment()
        mealTimePayOrderFragment = MealTimePayOrderFragment()
        olOrderFragment = OfflineOrderFragment()
        verifyOrderFragment = VerifyOrderFragment()
        deviceFragment = DeviceInfoFragment()
        fragments = arrayOf(basicFragment, modeFragment, faceFragment, payOrderFragment, mealTimePayOrderFragment, olOrderFragment, verifyOrderFragment, deviceFragment)
        binding.radioGroup.check(R.id.basic)
    }

    private fun initEvent() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener {
            when (binding.viewPager.currentItem) {
                0 -> basicFragment.save()
                1 -> modeFragment.save()
                2 -> faceFragment.save()
                else -> return@setOnClickListener
            }
        }
        binding.viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int {
                return fragments.size
            }

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }
        }
        //滑动切换界面
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.radioGroup.check(R.id.basic)
                    1 -> binding.radioGroup.check(R.id.mode)
                    2 -> binding.radioGroup.check(R.id.face)
                    3 -> binding.radioGroup.check(R.id.pay_order)
                    4 -> binding.radioGroup.check(R.id.meal_time_pay_order)
                    5 -> binding.radioGroup.check(R.id.offline_order)
                    6 -> binding.radioGroup.check(R.id.verify_order)
                    7 -> binding.radioGroup.check(R.id.device)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}

        })
        //点击切换界面
        binding.radioGroup.setOnCheckedChangeListener { radioGroup, id ->
            when (id) {
                R.id.basic -> binding.viewPager.currentItem = 0
                R.id.mode -> binding.viewPager.currentItem = 1
                R.id.face -> binding.viewPager.currentItem = 2
                R.id.pay_order -> binding.viewPager.currentItem = 3
                R.id.meal_time_pay_order -> binding.viewPager.currentItem = 4
                R.id.offline_order -> binding.viewPager.currentItem = 5
                R.id.verify_order -> binding.viewPager.currentItem = 6
                R.id.device -> binding.viewPager.currentItem = 7
            }
        }
    }

    private fun initPresentation() {
        if (!this::displayManager.isInitialized) {
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.also { secondDisplays = it[1] }
        }
        settingDisplay = SettingDisplay(this, secondDisplays)
        settingDisplay.show()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            val inputMethodManager = MyApplication.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager != null && view != null) inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        settingDisplay.safeCancel()
    }
}
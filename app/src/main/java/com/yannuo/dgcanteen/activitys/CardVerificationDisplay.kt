package com.yannuo.dgcanteen.activitys

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.view.Display
import androidx.core.content.ContextCompat.getSystemService
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.activitys.viewModel.VerificationVM
import com.yannuo.dgcanteen.databinding.DisplayCardVerificationBinding
import com.yannuo.dgcanteen.model.MessageEvent
import com.yannuo.dgcanteen.util.Constant
import org.greenrobot.eventbus.EventBus

class CardVerificationDisplay(context: Context, display: Display) : Presentation(context, display) {
    private val TAG = javaClass.simpleName
    private var binding: DisplayCardVerificationBinding? = null
    private lateinit var verificationVM: VerificationVM
    private val handler = Handler()
    private val kv by lazy {
        MMKV.defaultMMKV()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DisplayCardVerificationBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        initOpear()
        initEvent()
    }

    private fun initOpear() {
        if (!this::verificationVM.isInitialized) verificationVM = VerificationVM()
        verificationVM.openQrCode()

    }

    private fun initEvent() {
        binding?.btnBack?.setOnClickListener { back() }
        binding?.toFace?.setOnClickListener {
            EventBus.getDefault().post(MessageEvent(Constant.EVENT_FACE, null))
        }
    }
    private fun back() {
        cancel()
        when (kv.decodeString(Constant.APP_MODE)) {
            Constant.ORDERING_FOOD_MODE -> handler.post{
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_VERIFICATION, null))
            }
            Constant.PROCEEDS_MODE -> handler.post{
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_THIRTY, null))
            }
            Constant.ORDERING_TWO_MODE -> handler.post{
                EventBus.getDefault().post(MessageEvent(Constant.EVENT_THIRTY_ONE, null))
            }
        }
    }

    override fun cancel() {
        super.cancel()
        verificationVM.closeQrCode()
    }

}
package com.yannuo.dgcanteen.activitys.fragment

import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tencent.mmkv.MMKV
import com.yannuo.dgcanteen.databinding.FragmentTerminalPasswordBinding
import com.yannuo.dgcanteen.util.Constant
import com.yannuo.dgcanteen.util.ToastShowUtil

class TerminalPasswordFragment : BaseFragment<FragmentTerminalPasswordBinding>() {
    private val mmkv = MMKV.defaultMMKV()

    override fun bindLayout(inflater: LayoutInflater, container: ViewGroup?) {
        binding = FragmentTerminalPasswordBinding.inflate(inflater, container, false)
    }

    override fun onInit() {
        initEvent()
    }

    fun changeTerminalPassword() {
        if (verifyPassword()) {
            mmkv.encode(Constant.TERMINAL_PASSWORD, binding.etConfirmPassword.text.toString())
            binding.tvPasswordTips.setTextColor(Color.parseColor("#B3B3B3"))
            binding.etOldPassword.setText("")
            binding.etNewPassword.setText("")
            binding.etConfirmPassword.setText("")
        } else binding.tvPasswordTips.setTextColor(Color.parseColor("#FA5151"))
    }

    private fun initEvent() {
        binding.btnPasswordType.isChecked = mmkv.decodeBool(Constant.TERMINAL_PASSWORD_TYPE, false)
        binding.btnPasswordType.setOnClickListener { mmkv.encode(Constant.TERMINAL_PASSWORD_TYPE, binding.btnPasswordType.isChecked) }
        binding.etOldPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                val oldPassword = editable.toString()
                if (oldPassword.isEmpty()) {
                    binding.btnOldPassword.isChecked = false
                    binding.btnOldPassword.visibility = View.GONE
                } else binding.btnOldPassword.visibility = View.VISIBLE
            }
        })
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                val newPassword = editable.toString()
                if (newPassword.isEmpty()) {
                    binding.btnNewPassword.isChecked = false
                    binding.btnNewPassword.visibility = View.GONE
                } else binding.btnNewPassword.visibility = View.VISIBLE
            }
        })
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(charSequence: CharSequence, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable) {
                val confirmPassword = editable.toString()
                if (confirmPassword.isEmpty()) {
                    binding.btnConfirmPassword.isChecked = false
                    binding.btnConfirmPassword.visibility = View.GONE
                } else binding.btnConfirmPassword.visibility = View.VISIBLE
            }
        })
        binding.btnOldPassword.setOnClickListener {
            if (binding.btnOldPassword.isChecked) binding.etOldPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else binding.etOldPassword.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            /* 光标位置 */
            binding.etOldPassword.setSelection(binding.etOldPassword.text.length)
        }
        binding.btnNewPassword.setOnClickListener {
            if (binding.btnNewPassword.isChecked) binding.etNewPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else binding.etNewPassword.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            /* 光标位置 */
            binding.etNewPassword.setSelection(binding.etNewPassword.text.length)
        }
        binding.btnConfirmPassword.setOnClickListener {
            if (binding.btnConfirmPassword.isChecked) binding.etConfirmPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else binding.etConfirmPassword.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            /* 光标位置 */
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
        }
    }

    private fun verifyPassword(): Boolean {
        val oldPassword: String = binding.etOldPassword.text.toString()
        val newPassword: String = binding.etNewPassword.text.toString()
        val confirmPassword: String = binding.etConfirmPassword.text.toString()
        /* 判断是否全是数字组成并为4位 */
        val oldStatus = isJudgmentFormat("^\\d{4}$", oldPassword)
        val newStatus = isJudgmentFormat("^\\d{4}$", newPassword)
        val confirmStatus = isJudgmentFormat("^\\d{4}$", confirmPassword)
        if (!oldStatus || !newStatus || !confirmStatus) {
            ToastShowUtil.show("密码格式异常")
            return false
        }
        /* 判断密码是否一致 */
        if (oldPassword != mmkv.decodeString(Constant.TERMINAL_PASSWORD, "0000").toString() || oldPassword == newPassword || newPassword != confirmPassword) {
            ToastShowUtil.show("密码不一致或相同")
            return false
        }
        ToastShowUtil.show("密码更新成功")
        return true
    }

    private fun isJudgmentFormat(format: String, content: String): Boolean {
        val regex = Regex(format, RegexOption.IGNORE_CASE)
        return regex.matches(content)
    }

}
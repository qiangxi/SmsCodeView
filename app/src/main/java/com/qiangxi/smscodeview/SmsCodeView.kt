package com.qiangxi.smscodeview

/**
 * Created by qiangxi(任强强) on 2018/5/12.
 */

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethod
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Created by qiangxi(Ray) on 2018/5/9.
 *
 */
class SmsCodeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr), TextWatcher, View.OnFocusChangeListener {

    private val space = "\uFEFF"//无宽度空格
    private var mBlueLayer: Drawable = resources.getDrawable(R.drawable.layer_blue_line)
    private var mGrayLayer: Drawable = resources.getDrawable(R.drawable.layer_gray_line)

    private var mCurrentFocusPosition = 0//当前聚焦的位置
    private var mLastFocusPosition = 0//上个聚焦的位置

    private val mCount = 4  //et的数量

    var inputCompletedListener: ((text: String) -> Unit)? = null //输入完成回调

    init {
        gravity = Gravity.CENTER
        (0 until mCount).forEach {
            //internal
            val child = InternalEditText(context)
            //设置child状态
            //et
            val et = child.mEditText
            if (it == 0) {
                child.obtainFocus()
                et.setText(space)
                et.setSelection(et.text.toString().length)
                child.setBackgroundDrawable(mBlueLayer)
            } else {
                child.disabled()
                child.setBackgroundDrawable(mGrayLayer)
            }
            et.addTextChangedListener(this)
            et.onFocusChangeListener = this
            //lp
            val lp = LinearLayout.LayoutParams(dpToPx(context, 35F), LinearLayout.LayoutParams.MATCH_PARENT)
            lp.rightMargin = dpToPx(context, 8F)
            lp.leftMargin = dpToPx(context, 8F)
            addView(child, lp)
        }
    }

    /**
     * 更新et背景色，每次文本或焦点变化都要更新颜色
     */
    private fun updateLineColor() {
        (0 until childCount).forEachIndexed { index, _ ->
            val child = getChildAt(index) as InternalEditText
            val et = child.mEditText
            var content = et.text.toString()
            //设置背景并填充space
            if (et.isFocused || content.length == 2) {
                //在删除时，自动清空et内容
                if (et.isFocused && mLastFocusPosition > mCurrentFocusPosition) {
                    et.text = null
                }
                content = et.text.toString()//ensure
                //每当et聚焦时，判断是否已填充space，没有则填充
                if (!content.contains(space)) {
                    et.setText(space)
                    et.setSelection(et.text.toString().length)
                }
                child.setBackgroundDrawable(mBlueLayer)
            } else {
                child.setBackgroundDrawable(mGrayLayer)
            }
        }
    }

    private fun requestNextFocus() {
        (0 until childCount).forEachIndexed { index, _ ->
            val child = getChildAt(index) as InternalEditText
            if (mCurrentFocusPosition == index) {
                child.obtainFocus()
            } else {
                child.disabled()
            }
        }
    }

    fun getText(): String {
        val sb = StringBuilder()
        return sb.apply {
            (0 until childCount).forEachIndexed { index, _ ->
                val child = getChildAt(index) as InternalEditText
                val et = child.mEditText
                val realContent = et.text.toString().filterNot { TextUtils.equals(it.toString(), space) }
                append(realContent)
            }
        }.toString()
    }

    override fun afterTextChanged(s: Editable?) {
        //mCurrentFocusPosition > childCount - 1 说明已经输入完毕，可以触发输入完成的回调
        if (mCurrentFocusPosition > childCount - 1) {
            mCurrentFocusPosition = childCount - 1
            inputCompletedListener?.invoke(getText())
            return
        }
        //mCurrentFocusPosition < 0说明已经删除到最后一个et，需要在手动补一个space，否则，会出现第一个et可以输入两个字符的情况
        if (mCurrentFocusPosition < 0) {
            mCurrentFocusPosition = 0
            val et = (getChildAt(0) as InternalEditText).mEditText
            et.setText(space)
            et.setSelection(et.text.toString().length)
            return
        }
        requestNextFocus()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //说明增加的内容为space，不做处理
        if (TextUtils.equals(s, space) && before == 0) return

        //增
        if (before == 0) {
            mLastFocusPosition = mCurrentFocusPosition
            ++mCurrentFocusPosition
        }
        //删
        else if (before == 1 && TextUtils.isEmpty(s)) {//说明删除的内容为space，此时位置左移
            mLastFocusPosition = mCurrentFocusPosition
            --mCurrentFocusPosition
        }
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) updateLineColor()
    }

    /**
     * dp转px
     */
    private fun dpToPx(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}

class InternalEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private val PHONE_NUMBER_CHARS = "0123456789"
    var mEditText = EditText(context)
    private var mOverlayView = View(context)

    init {
        //et
        mEditText.setTextColor(Color.parseColor("#3c4f5e"))
        mEditText.setBackgroundDrawable(null)
        mEditText.maxLines = 1
        mEditText.gravity = Gravity.CENTER
        mEditText.keyListener = DigitsKeyListener.getInstance(PHONE_NUMBER_CHARS)
        mEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(2))
        val etLp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        addView(mEditText, etLp)
        //overlay
        val overlayLp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        addView(mOverlayView, overlayLp)
        mOverlayView.isFocusable = false
        mOverlayView.isFocusableInTouchMode = false
        mOverlayView.setOnClickListener {
            obtainFocus()
            showKeyboard()
        }
    }

    fun obtainFocus() {
        //Overlay
        mOverlayView.isEnabled = true
        //et
        mEditText.isFocusable = true
        mEditText.isFocusableInTouchMode = true
        mEditText.requestFocus()
    }

    fun disabled() {
        mOverlayView.isEnabled = false
        mEditText.isFocusable = false
        mEditText.isFocusableInTouchMode = false
    }

    private fun showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(mEditText, InputMethod.SHOW_FORCED)
    }

}
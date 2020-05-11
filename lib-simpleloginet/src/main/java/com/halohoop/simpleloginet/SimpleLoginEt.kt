package com.halohoop.simpleloginet

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.preference.PreferenceManager
import android.text.InputType
import android.util.AttributeSet
import android.view.*
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import java.util.*
import kotlin.math.abs

/**
 * @author halohoop
 * email:halohoopwong@gmail.com
 */
@SuppressLint("AppCompatCustomView")
class SimpleLoginEt : EditText, View.OnTouchListener {

    private val TAG = javaClass.simpleName

    @StringRes
    private var clearTips: Int = R.string.default_clear_tips

    @ColorRes
    private var clearTipsBgColor: Int = R.color.default_clear_tips_bg_color

    @DimenRes
    private var clearTipsBgRadius: Int = R.dimen.default_clear_tips_bg_radius

    private val eyeOn: Drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        context.resources.getDrawable(R.mipmap.eye_open, null)
    } else {
        context.resources.getDrawable(R.mipmap.eye_open)
    }
    private val eyeClose: Drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        context.resources.getDrawable(R.mipmap.eye_close, null)
    } else {
        context.resources.getDrawable(R.mipmap.eye_close)
    }
    private var eyeDrawable = eyeClose

    private var clickPair: Pair<Long, Pair<Float, Float>>? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val clearTouchVelocityMinSlop =
        ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val clearTouchVelocityMaxSlop =
        ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val velocityTracker: VelocityTracker = VelocityTracker.obtain()
    private val clearTouchSlop = touchSlop * 2
    private val tapTimeout = ViewConfiguration.getTapTimeout() * 2
    private var isFocusdWhenDown = false

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private var isLastLaunchExist: Boolean

    init {
        PreferenceManager.getDefaultSharedPreferences(context).apply {
            isLastLaunchExist = this.getLong("LAST_INSTANCE_DATE_MS", -1) > 0
            edit().putLong("LAST_INSTANCE_DATE_MS", Date().getTime()).apply()
        }
        setOnTouchListener(this@SimpleLoginEt)
    }

    var onEyeClickListener: OnEyeClickListener? = null

    interface OnEyeClickListener {
        /**
         * @param eyeOpen 点击后眼睛是否是实心状态
         */
        fun onClick(eyeOpen: Boolean)
    }

    private fun isClickTap(v0: Float, v1: Float): Boolean = touchSlop >= abs(v0 - v1)

    private fun isClearMove1(v0: Float, v1: Float): Boolean = clearTouchSlop <= abs(v0 - v1)

    private fun isClearMove0(): Boolean {
        velocityTracker.computeCurrentVelocity(100, clearTouchVelocityMaxSlop.toFloat())
        val result = abs(velocityTracker.xVelocity) >= clearTouchVelocityMinSlop
        velocityTracker.clear()
        return result
    }

//    override fun onFinishInflate() {
//        super.onFinishInflate()
//        if (InputType.TYPE_CLASS_TEXT == inputType and InputType.TYPE_MASK_CLASS) {
//            if (InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == inputType and InputType.TYPE_MASK_VARIATION) {
//
//            }
//        }
//    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (!isLastLaunchExist) {
            isLastLaunchExist = true
            //显示popupwindow 提示滑动清空
            showClearTips()
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return event?.let {
            velocityTracker.addMovement(it)
            var consumed = false
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    //只有密码框[2]才会有值
                    val drawable = compoundDrawables[2]
                    if (drawable != null) {
                        val width = drawable.bounds.width()
                        if (it.x > measuredWidth - width) {
                            clickPair = Pair(it.eventTime, Pair(it.x, it.y))
                            consumed = true
                            isFocusdWhenDown = isFocused
                            clearFocus()
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (tapTimeout > it.eventTime.minus(clickPair?.first ?: 0L)
                        && isClickTap(it.x, clickPair?.second?.first ?: 0f)
                        && isClickTap(it.y, clickPair?.second?.second ?: 0f)
                    ) {
                        onEyeClickListener?.onClick(updateAndGetEyeOpen())
                        consumed = true
                    }
                    if (isClearMove0() && isClearMove1(it.x, clickPair?.second?.first ?: 0f)) {
                        //删除所有字符
                        setText("")
                    } else {
                        if (isFocusdWhenDown) {
                            isFocusdWhenDown = false
                            requestFocus()
                        }
                    }
                }
            }
            return consumed
        } ?: false
    }

    private fun showClearTips() {
        val textView = TextView(context)
        textView.setText(clearTips)
        textView.gravity = Gravity.CENTER
        textView.setTextColor(Color.WHITE)
        val popupWindow = PopupWindow(textView)
        popupWindow.width = this.width
        popupWindow.height = this.height
        popupWindow.isOutsideTouchable = false
        val radius: Float =
            resources.getDimensionPixelSize(clearTipsBgRadius).toFloat()
        val bgDrawable = ShapeDrawable(
            RoundRectShape(
                FloatArray(8) { radius },
                null,
                null
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bgDrawable.setTintList(ColorStateList.valueOf(context.getColor(clearTipsBgColor)))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bgDrawable.setTintList(
                ColorStateList.valueOf(
                    context.resources.getColor(
                        clearTipsBgColor
                    )
                )
            )
        } else {
            //TODO 低于21的ShapeDrawable颜色定义
//            bgDrawable.setColorFilter()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            textView.background = bgDrawable
        } else {
            textView.setBackgroundDrawable(bgDrawable)
        }
        popupWindow.isTouchable = false
        postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                popupWindow.showAsDropDown(this, 0, 10, Gravity.BOTTOM)
            } else {
                popupWindow.showAsDropDown(this, 0, 10)
            }
            val dismissRunnable = Runnable { popupWindow.dismiss() }
            popupWindow.setOnDismissListener { removeCallbacks(dismissRunnable) }
            postDelayed(dismissRunnable, 3000)
        }, 500)
    }

    private fun updateAndGetEyeOpen(): Boolean {
        if (eyeDrawable == eyeClose) {
            eyeDrawable = eyeOn
        } else {
            eyeDrawable = eyeClose
        }
        setCompoundDrawablesWithIntrinsicBounds(compoundDrawables[0], null, eyeDrawable, null)
        return (eyeDrawable == eyeOn).apply {
            if (this == true) {
                inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            setSelection(text.toString().length)
        }
    }

}
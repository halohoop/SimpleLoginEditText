package com.halohoop.simpleloginet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.text.InputType
import android.util.AttributeSet
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatEditText
import java.util.*
import kotlin.math.abs

/**
 * @author halohoop
 * email:halohoopwong@gmail.com
 */
class AppCompatSimpleLoginEt : AppCompatEditText, View.OnTouchListener {

    private val TAG = javaClass.simpleName

    @StringRes
    private var clearTips: Int = R.string.default_clear_tips

    @ColorRes
    private var clearTipsBgColor: Int = R.color.default_clear_tips_bg_color

    @DimenRes
    private var clearTipsBgRadius: Int = R.dimen.default_clear_tips_bg_radius

    @DrawableRes
    private var textVisibleOpenDrawableResId: Int = R.mipmap.eye_open

    @DrawableRes
    private var textVisibleCloseDrawableResId: Int = R.mipmap.eye_close

    private fun getDrawableCompat(@DrawableRes id: Int): Drawable {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.resources.getDrawable(id, null)
        } else {
            context.resources.getDrawable(id)
        }
    }

    private var textVisibleDrawableResId = textVisibleOpenDrawableResId

    private data class TouchTripleVo(val touchEventTime: Long, val x: Float, val y: Float)

    private var touchEventTriple: TouchTripleVo? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val clearTouchVelocityMinSlop =
        ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val clearTouchVelocityMaxSlop =
        ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val velocityTracker: VelocityTracker = VelocityTracker.obtain()
    private val clearTouchSlop = touchSlop * 2
    private val tapTimeout = ViewConfiguration.getTapTimeout() * 2
    private var isFocusdWhenDown = false

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        parseAttrs(attrs)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        parseAttrs(attrs)
    }

    private fun isPasswordMode(): Boolean =
        InputType.TYPE_CLASS_TEXT == inputType and InputType.TYPE_MASK_CLASS
                && (InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == inputType and InputType.TYPE_MASK_VARIATION
                || InputType.TYPE_TEXT_VARIATION_PASSWORD == inputType and InputType.TYPE_MASK_VARIATION)

    override fun setCompoundDrawables(
        left: Drawable?,
        top: Drawable?,
        right: Drawable?,
        bottom: Drawable?
    ) {
        super.setCompoundDrawables(left, null, right, null)
    }

    private fun parseAttrs(attrs: AttributeSet?) {
        attrs?.let {
            context.obtainStyledAttributes(attrs, R.styleable.SimpleLoginEt).apply {
                clearTips =
                    getResourceId(R.styleable.SimpleLoginEt_clear_tips, R.string.default_clear_tips)
                clearTipsBgColor = getResourceId(
                    R.styleable.SimpleLoginEt_clear_tips_bg_color,
                    R.color.default_clear_tips_bg_color
                )
                clearTipsBgRadius = getResourceId(
                    R.styleable.SimpleLoginEt_clear_tips_bg_radius,
                    R.dimen.default_clear_tips_bg_radius
                )
                textVisibleOpenDrawableResId =
                    getResourceId(R.styleable.SimpleLoginEt_icon_end_open, R.mipmap.eye_open)
                textVisibleCloseDrawableResId =
                    getResourceId(R.styleable.SimpleLoginEt_icon_end_close, R.mipmap.eye_close)
                recycle()
            }
        }
        setCompoundDrawablesWithIntrinsicBounds(
            compoundDrawables[0], null,
            if (isPasswordMode()) getDrawableCompat(textVisibleOpenDrawableResId) else null, null
        )
    }

    private var isLastLaunchExist: Boolean

    init {
        try {
            context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                .apply {
                    isLastLaunchExist = this.getLong("LAST_INSTANCE_DATE_MS", -1) > 0
                    edit().putLong("LAST_INSTANCE_DATE_MS", Date().time).apply()
                }
        } catch (e: Exception) {
            //如果有IO错误就不再显示了
            isLastLaunchExist = true
        }
        setOnTouchListener(this@AppCompatSimpleLoginEt)
    }

    var onEyeClickListener: OnEyeClickListener? = null

    interface OnEyeClickListener {
        /**
         * @param isToggleOpen 点击后开关是否是打开状态
         */
        fun onClick(isToggleOpen: Boolean)
    }

    private fun isClickTap(v0: Float, v1: Float): Boolean = touchSlop >= abs(v0 - v1)

    private fun isClearMoveWhenUp(v0: Float, v1: Float): Boolean {
        fun isClearMoveWhenUp0(): Boolean {
            velocityTracker.computeCurrentVelocity(100, clearTouchVelocityMaxSlop.toFloat())
            val result = abs(velocityTracker.xVelocity) >= clearTouchVelocityMinSlop
            velocityTracker.clear()
            return result
        }

        fun isClearMoveWhenUp1(v0: Float, v1: Float): Boolean = clearTouchSlop <= abs(v0 - v1)

        return isClearMoveWhenUp0() && isClearMoveWhenUp1(v0, v1)
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
                    //只有密码框可以点击右边的icon
                    if (isPasswordMode()) {
                        //只有密码框的[2]才能有值 0左1上2右3下
                        compoundDrawables[2]?.apply {
                            val width = bounds.width()
                            if (it.x > measuredWidth - width) {
                                touchEventTriple = TouchTripleVo(it.eventTime, it.x, it.y)
                                consumed = true
                                isFocusdWhenDown = isFocused
                                clearFocus()
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (tapTimeout > it.eventTime.minus(touchEventTriple?.touchEventTime ?: 0L)
                        && isClickTap(it.x, touchEventTriple?.x ?: 0f)
                        && isClickTap(it.y, touchEventTriple?.y ?: 0f)
                    ) {
                        updateAndGetTextVisible().apply {
                            onEyeClickListener?.onClick(this)
                        }
                        consumed = true
                    }
                    if (isClearMoveWhenUp(it.x, touchEventTriple?.x ?: 0f)) {
                        //删除所有字符
                        setText("")
                    } else {
                        if (isFocusdWhenDown) {
                            isFocusdWhenDown = false
                            requestFocus()
                        }
                    }
                    touchEventTriple = null
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

    private fun updateAndGetTextVisible(): Boolean {
        if (textVisibleDrawableResId == textVisibleCloseDrawableResId) {
            textVisibleDrawableResId = textVisibleOpenDrawableResId
        } else {
            textVisibleDrawableResId = textVisibleCloseDrawableResId
        }
        setCompoundDrawablesWithIntrinsicBounds(
            compoundDrawables[0],
            null,
            getDrawableCompat(textVisibleDrawableResId),
            null
        )
        return (textVisibleDrawableResId == textVisibleCloseDrawableResId).apply {
            if (this == true) {
                inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            setSelection(text.toString().length)
        }
    }

    override fun setError(error: CharSequence?, icon: Drawable?) {
        //禁用此功能，因为会影响设置drawable，感兴趣的同学也可以自定义自己的错误提示
//        super.setError(error, icon)
    }

}
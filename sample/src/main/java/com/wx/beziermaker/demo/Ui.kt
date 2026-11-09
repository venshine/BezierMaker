/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wx.beziermaker.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat

/*
 * 示例页共用的小工具：取色、单位换算、着色图标、画布纸面、分段按钮、开关、代码高亮、复制
 */

/**
 * 示例里用到的 BezierView 常量
 *
 * 库里对应的字段是私有的，这里照抄一份，改库时要同步：
 * 阶数上限对应 BezierView.MAX_COUNT，采样点数对应 BezierView.FRAME，
 * 各层切线颜色对应 BezierView.TANGENT_COLORS，控制多边形、曲线、移动点分别是 LTGRAY、RED、BLACK
 */
object DemoBezier {
    const val MAX_ORDER = 7
    const val FRAMES = 1000
    const val DEFAULT_ORDER = 3

    val TANGENT_COLORS = intArrayOf(
        0xFF7FFF00.toInt(), 0xFF7A67EE.toInt(), 0xFFEE82EE.toInt(),
        0xFFFFD700.toInt(), 0xFF1C86EE.toInt(), 0xFF8B8B00.toInt(),
    )
    const val POLYGON_COLOR = Color.LTGRAY
    const val CURVE_COLOR = Color.RED
    const val POINT_COLOR = Color.BLACK

    /** 一遍动画大约要画多少帧：每帧在 [FRAMES] 个采样点里前进 rate 个 */
    fun framesOf(rate: Int) = (FRAMES + rate - 1) / rate
}

@ColorInt
fun Context.colorOf(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

fun Context.font(@FontRes id: Int): Typeface? = ResourcesCompat.getFont(this, id)

/** 着色后的线性图标，用作 TextView 的 compound drawable */
fun Context.tintedIcon(@DrawableRes id: Int, @ColorInt color: Int, sizeDp: Float): Drawable {
    val drawable = DrawableCompat.wrap(ContextCompat.getDrawable(this, id)!!.mutate())
    DrawableCompat.setTint(drawable, color)
    val size = dp(sizeDp)
    drawable.setBounds(0, 0, size, size)
    return drawable
}

fun TextView.setStartIcon(icon: Drawable?) {
    setCompoundDrawablesRelative(icon, null, null, null)
}

fun Context.copyToClipboard(text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("BezierMaker", text))
}

/** 复制后把按钮文字短暂换成「已复制」 */
fun TextView.flashCopied(@StringRes copied: Int, @StringRes normal: Int) {
    setText(copied)
    removeCallbacks(tag as? Runnable)
    val restore = Runnable { setText(normal) }
    tag = restore
    postDelayed(restore, 1600)
}

/**
 * 把 BezierView 的容器铺成带点阵的纸面，并按圆角裁切
 *
 * BezierView 自己不画背景，控制点和文字固定是黑色、灰色，所以纸面在深色模式下也保持浅色
 */
fun View.usePaperBackground() {
    background = LayerDrawable(arrayOf(
        ContextCompat.getDrawable(context, R.drawable.bg_canvas)!!,
        GridPaperDrawable(context),
    ))
    clipToOutline = true
}

/**
 * 等 BezierView 布局完成后再执行（setOrder / addPoint 等加点操作要放在这里）
 *
 * 布局前 BezierView 不知道自己的宽高，按屏幕短边估算可加点的区域；画布比屏幕窄时，超出的点会在首次布局时
 * 被挤到画布边缘。布局后再加点，新点按画布的真实大小选位置，分布更舒展。post 的任务在首次布局完成后才执行
 */
fun View.afterLayout(action: () -> Unit) {
    if (isLaidOut) action() else post(action)
}

/** 点阵网格，间距 24dp */
class GridPaperDrawable(context: Context) : Drawable() {

    private val step = context.dp(24f).toFloat()
    private val radius = context.dp(1.2f).toFloat()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.colorOf(R.color.canvas_grid) }

    override fun draw(canvas: Canvas) {
        val b = bounds
        var y = b.top + step
        while (y < b.bottom) {
            var x = b.left + step
            while (x < b.right) {
                canvas.drawCircle(x, y, radius, paint)
                x += step
            }
            y += step
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/**
 * 分段按钮：在 [container] 里横向排出 [labels]，点击回调下标；选中态由调用方通过 [select] 设置
 */
class Segmented(container: LinearLayout, labels: List<String>, descriptions: List<String>, onClick: (Int) -> Unit) {

    private val items: List<TextView>

    init {
        val context = container.context
        val typeface = Typeface.create(context.font(R.font.space_grotesk), Typeface.BOLD)
        items = labels.mapIndexed { i, label ->
            TextView(context).apply {
                text = label
                contentDescription = descriptions[i]
                gravity = Gravity.CENTER
                textSize = 15f
                this.typeface = typeface
                setTextColor(ContextCompat.getColorStateList(context, R.color.segment_text))
                setBackgroundResource(R.drawable.bg_segment_item)
                setOnClickListener { onClick(i) }
                container.addView(this, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    .apply { if (i > 0) marginStart = context.dp(4f) })
            }
        }
    }

    fun item(index: Int): View = items[index]

    /** 下标越界时（如阶数不在可选范围内）全部取消选中 */
    fun select(index: Int) {
        items.forEachIndexed { i, view -> view.isSelected = i == index }
    }
}

/** 开关胶囊：选中态设在容器上，文字跟随变色；对勾挂在文字前面 */
fun bindToggle(chip: View, label: TextView, on: Boolean) {
    chip.isSelected = on
    val context = chip.context
    label.setStartIcon(if (on) context.tintedIcon(R.drawable.ic_check, context.colorOf(R.color.accent_ink), 16f) else null)
}

/**
 * 示例代码的简易高亮：关键字用强调色，引号内字符串和整行注释各用一种颜色
 */
object CodeHighlighter {

    private val STRING = Regex("\"[^\"\\n]*\"|'[^'\\n]*'")

    // 只认整行注释，避免把 'https://…' 里的 // 当成注释
    private val COMMENT = Regex("(?m)^\\s*//.*$")

    fun highlight(context: Context, code: String, keywords: List<String>): CharSequence {
        val builder = SpannableStringBuilder(code)
        val keywordColor = context.colorOf(R.color.accent_ink)
        for (keyword in keywords) {
            // 只匹配完整的单词，避免 start 命中 started 的前半段
            Regex("(?<![A-Za-z])" + Regex.escape(keyword) + "(?![A-Za-z])").findAll(code)
                .forEach { builder.color(keywordColor, it.range.first, it.range.last + 1) }
        }
        STRING.findAll(code).forEach { builder.color(context.colorOf(R.color.code_string), it.range.first, it.range.last + 1) }
        COMMENT.findAll(code).forEach { builder.color(context.colorOf(R.color.code_comment), it.range.first, it.range.last + 1) }
        return builder
    }

    private fun SpannableStringBuilder.color(@ColorInt color: Int, start: Int, end: Int) {
        setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

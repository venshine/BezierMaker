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
package com.wx.beziermaker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.text.DecimalFormat
import java.util.Random

/**
 * 贝塞尔曲线
 *
 * 类和公开方法保持 open、三个构造函数分别调用 View 的三个父构造，Java 使用方的调用和继承方式与 Java 版一致
 *
 * @author venshine
 */
open class BezierView : View {

    private val bezierPath = Path() // 贝塞尔曲线路径

    private val bezierPaint = Paint().apply {   // 贝塞尔曲线画笔
        color = Color.RED
        strokeWidth = BEZIER_WIDTH.toFloat()
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val movingPaint = Paint().apply {   // 移动点画笔
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val controlPaint = Paint().apply {  // 控制点画笔
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
    private val tangentPaint = Paint().apply {  // 切线画笔
        color = Color.parseColor(TANGENT_COLORS[0])
        isAntiAlias = true
        strokeWidth = TANGENT_WIDTH.toFloat()
        style = Paint.Style.FILL
    }
    private val linePaint = Paint().apply { // 固定线画笔
        color = Color.LTGRAY
        strokeWidth = CONTROL_WIDTH.toFloat()
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val textPointPaint = Paint().apply {    // 点画笔
        color = Color.BLACK
        isAntiAlias = true
        textSize = TEXT_SIZE.toFloat()
    }
    private val textPaint = Paint().apply { // 文字画笔
        color = Color.GRAY
        isAntiAlias = true
        textSize = TEXT_SIZE.toFloat()
    }

    private var bezierPoints: List<PointF> = emptyList()  // 贝塞尔曲线点集，start() 时生成
    private var bezierPoint: PointF? = null // 贝塞尔曲线移动点

    private val controlPoints = ArrayList<PointF>(MAX_COUNT + 1)   // 控制点集

    private var tangentPoints: List<List<List<PointF>>> = emptyList() // 切线点集：层 → 线段端点 → 帧

    private var instantTangentPoints: List<List<PointF>>? = null  // 当前帧的切线端点

    private var r = 0   // 当前帧序号

    private var rate = RATE // 速率

    private var state = 0   // 状态

    private var loop = false    // 设置是否循环

    private var tangent = true  // 设置是否显示切线

    private var viewWidth = 0   // 画布宽
    private var viewHeight = 0  // 画布高

    private var curPoint: PointF? = null    // 当前移动的控制点

    // View 只能在主线程使用，绑定主线程 Looper，与 Java 版在主线程创建的无参 Handler 等价
    private val frameHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == HANDLER_WHAT) {
                nextFrame()
            }
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        // 初始坐标
        val w = resources.displayMetrics.widthPixels
        controlPoints.add(PointF((w / 5).toFloat(), (w / 5).toFloat()))
        controlPoints.add(PointF((w / 3).toFloat(), (w / 2).toFloat()))
        controlPoints.add(PointF((w / 3 * 2).toFloat(), (w / 4).toFloat()))

        state = state or STATE_READY or STATE_TOUCH
    }

    /**
     * 推进一帧
     */
    private fun nextFrame() {
        r += rate
        if (r >= bezierPoints.size) {
            frameHandler.removeMessages(HANDLER_WHAT)
            r = 0
            state = state and STATE_RUNNING.inv()
            state = state and STATE_STOP.inv()
            state = state or STATE_READY or STATE_TOUCH
            if (loop) {
                start()
            }
            return
        }
        if (r != bezierPoints.size - 1 && r + rate >= bezierPoints.size) {
            r = bezierPoints.size - 1
        }
        // Bezier点
        bezierPoint = PointF(bezierPoints[r].x, bezierPoints[r].y)
        // 切线点
        if (tangent) {
            instantTangentPoints = tangentPoints.map { lines ->
                lines.map { line -> PointF(line[r].x, line[r].y) }
            }
        }
        if (r == bezierPoints.size - 1) {
            state = state or STATE_STOP
        }
        invalidate()
    }

    /**
     * 按 FRAME 步长从 0 累加到 1 依次回调 t
     *
     * 沿用 Java 版的 float 累加（不是 i / FRAME），采样点数和每个 t 的取值都与旧版一致
     */
    private inline fun forEachT(action: (Float) -> Unit) {
        val delta = 1.0f / FRAME
        var t = 0f
        while (t <= 1f) {
            action(t)
            t += delta
        }
    }

    /**
     * 创建Bezier点集
     */
    private fun buildBezierPoints(): List<PointF> {
        val points = ArrayList<PointF>()
        val order = controlPoints.size - 1
        forEachT { t ->
            points.add(PointF(deCasteljauX(order, 0, t), deCasteljauY(order, 0, t)))
        }
        return points
    }

    /**
     * 创建切线点集
     */
    private fun buildTangentPoints(): List<List<List<PointF>>> {
        val allPoints = ArrayList<List<List<PointF>>>()   // 所有点集
        val order = controlPoints.size - 1
        for (i in 0 until order - 1) {
            val morePoints = ArrayList<List<PointF>>()  // 多条线点集
            for (j in 0 until order - i) {
                val points = ArrayList<PointF>()    // 1条线点集
                forEachT { t ->
                    val p0: PointF
                    val p1: PointF
                    if (i > 0) {
                        val z = (t * FRAME).toInt()
                        p0 = allPoints[i - 1][j][z]
                        p1 = allPoints[i - 1][j + 1][z]
                    } else {
                        p0 = controlPoints[j]
                        p1 = controlPoints[j + 1]
                    }
                    points.add(PointF((1 - t) * p0.x + t * p1.x, (1 - t) * p0.y + t * p1.y))
                }
                morePoints.add(points)
            }
            allPoints.add(morePoints)
        }
        return allPoints
    }

    /**
     * deCasteljau算法
     *
     * @param i 阶数
     * @param j 点
     * @param t 时间
     */
    private fun deCasteljauX(i: Int, j: Int, t: Float): Float {
        if (i == 1) {
            return (1 - t) * controlPoints[j].x + t * controlPoints[j + 1].x
        }
        return (1 - t) * deCasteljauX(i - 1, j, t) + t * deCasteljauX(i - 1, j + 1, t)
    }

    /**
     * deCasteljau算法
     *
     * @param i 阶数
     * @param j 点
     * @param t 时间
     */
    private fun deCasteljauY(i: Int, j: Int, t: Float): Float {
        if (i == 1) {
            return (1 - t) * controlPoints[j].y + t * controlPoints[j + 1].y
        }
        return (1 - t) * deCasteljauY(i - 1, j, t) + t * deCasteljauY(i - 1, j + 1, t)
    }

    /**
     * 判断坐标是否在合法区域中
     */
    private fun isLegalTouchRegion(x: Float, y: Float): Boolean {
        if (x <= REGION_WIDTH || x >= areaWidth - REGION_WIDTH || y <= REGION_WIDTH || y >= areaHeight - REGION_WIDTH) {
            return false
        }
        val rectF = RectF()
        for (point in controlPoints) {
            if (curPoint == point) { // 判断是否是当前控制点
                continue
            }
            rectF.set(point.x - REGION_WIDTH, point.y - REGION_WIDTH, point.x + REGION_WIDTH, point.y + REGION_WIDTH)
            if (rectF.contains(x, y)) {
                return false
            }
        }
        return true
    }

    /**
     * 控制点可用区域的宽度
     */
    private val areaWidth: Int
        get() = if (viewWidth != 0) viewWidth else estimatedAreaSize

    /**
     * 控制点可用区域的高度
     */
    private val areaHeight: Int
        get() = if (viewHeight != 0) viewHeight else estimatedAreaSize

    /**
     * 布局前控件宽高为 0，按屏幕短边围成的正方形估算可用区域：初始控制点也在这个范围内，
     * 控件比它小时（如放在带边距的卡片里），超出的控制点在首次布局时由 keepPointsInside 移回控件内
     */
    private val estimatedAreaSize: Int
        get() {
            val metrics = resources.displayMetrics
            return minOf(metrics.widthPixels, metrics.heightPixels)
        }

    /**
     * 获取合法控制点
     */
    private fun getLegalControlPoint(x: Float, y: Float): PointF? {
        val rectF = RectF()
        for (point in controlPoints) {
            rectF.set(point.x - REGION_WIDTH, point.y - REGION_WIDTH, point.x + REGION_WIDTH, point.y + REGION_WIDTH)
            if (rectF.contains(x, y)) {
                return point
            }
        }
        return null
    }

    /**
     * 判断手指坐标是否在合法区域中
     */
    private fun isLegalFingerRegion(x: Float, y: Float): Boolean {
        val point = curPoint ?: return false
        val half = FINGER_RECT_SIZE shr 1
        return RectF(point.x - half, point.y - half, point.x + half, point.y + half).contains(x, y)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (viewWidth == 0 || viewHeight == 0) {
            viewWidth = width
            viewHeight = height
            if (viewWidth > 0 && viewHeight > 0) {
                keepPointsInside()
            }
        }
    }

    /**
     * 布局前的控制点（初始点和布局前添加的点）按屏幕短边估算位置，控件比它小时会落到控件外；
     * 首次拿到真实宽高后把它们移回可拖动区域
     */
    private fun keepPointsInside() {
        for (point in controlPoints) {
            point.x = clampToArea(point.x, viewWidth)
            point.y = clampToArea(point.y, viewHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isRunning && !isTouchable) {
            val moving = bezierPoint ?: bezierPoints[0].also {
                bezierPath.reset()
                bezierPath.moveTo(it.x, it.y)
                bezierPoint = it
            }
            // 控制点、控制点连线和坐标文本
            drawControlPoints(canvas)

            // 切线
            val tangents = instantTangentPoints
            if (tangent && tangents != null && !isStop) {
                for ((i, tps) in tangents.withIndex()) {
                    for (j in 0 until tps.size - 1) {
                        tangentPaint.color = Color.parseColor(TANGENT_COLORS[i])
                        canvas.drawLine(tps[j].x, tps[j].y, tps[j + 1].x, tps[j + 1].y, tangentPaint)
                        canvas.drawCircle(tps[j].x, tps[j].y, CONTROL_RADIUS.toFloat(), tangentPaint)
                        canvas.drawCircle(tps[j + 1].x, tps[j + 1].y, CONTROL_RADIUS.toFloat(), tangentPaint)
                    }
                }
            }

            // Bezier曲线
            bezierPath.lineTo(moving.x, moving.y)
            canvas.drawPath(bezierPath, bezierPaint)
            // Bezier曲线起始移动点
            canvas.drawCircle(moving.x, moving.y, CONTROL_RADIUS.toFloat(), movingPaint)
            // 时间展示
            canvas.drawText("t:" + DecimalFormat("##0.000").format((r.toFloat() / FRAME).toDouble()),
                (viewWidth - TEXT_HEIGHT * 3).toFloat(), (viewHeight - TEXT_HEIGHT).toFloat(), textPaint)

            frameHandler.removeMessages(HANDLER_WHAT)
            frameHandler.sendEmptyMessage(HANDLER_WHAT)
        }
        if (isTouchable) {
            drawControlPoints(canvas)
        }
    }

    /**
     * 画控制点、控制点连线和左下角的控制点坐标
     */
    private fun drawControlPoints(canvas: Canvas) {
        val size = controlPoints.size
        val format = DecimalFormat("##0.0")
        for ((i, point) in controlPoints.withIndex()) {
            if (i > 0) {
                // 控制点连线
                val prev = controlPoints[i - 1]
                canvas.drawLine(prev.x, prev.y, point.x, point.y, linePaint)
            }
            // 控制点
            canvas.drawCircle(point.x, point.y, CONTROL_RADIUS.toFloat(), controlPaint)
            // 控制点文本
            canvas.drawText("p$i", (point.x + CONTROL_RADIUS * 2), (point.y + CONTROL_RADIUS * 2), textPointPaint)
            // 控制点文本展示
            canvas.drawText("p$i ( ${format.format(point.x.toDouble())} , ${format.format(point.y.toDouble())}) ",
                REGION_WIDTH.toFloat(), (viewHeight - (size - i) * TEXT_HEIGHT).toFloat(), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isTouchable) {
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> state = state and STATE_READY.inv()
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                if (curPoint == null) {
                    curPoint = getLegalControlPoint(x, y)
                }
                val point = curPoint
                // 判断手指移动区域和触摸区域是否合法
                if (point != null && isLegalTouchRegion(x, y) && isLegalFingerRegion(x, y)) {
                    point.x = x
                    point.y = y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                curPoint = null
                state = state or STATE_READY
            }
        }
        return true
    }

    private val isReady: Boolean
        get() = (state and STATE_READY) == STATE_READY

    private val isRunning: Boolean
        get() = (state and STATE_RUNNING) == STATE_RUNNING

    private val isTouchable: Boolean
        get() = (state and STATE_TOUCH) == STATE_TOUCH

    private val isStop: Boolean
        get() = (state and STATE_STOP) == STATE_STOP

    /**
     * 开始
     */
    open fun start() {
        if (isReady) {
            bezierPoint = null
            instantTangentPoints = null
            bezierPoints = buildBezierPoints()
            // 不管当前是否显示切线都生成：播放中可能再打开切线，按需生成会读到空或上一次的旧点集
            tangentPoints = buildTangentPoints()
            state = state and STATE_READY.inv()
            state = state and STATE_TOUCH.inv()
            state = state or STATE_RUNNING
            invalidate()
        }
    }

    /**
     * 停止
     */
    open fun stop() {
        if (isRunning) {
            frameHandler.removeMessages(HANDLER_WHAT)
            r = 0
            state = state and STATE_RUNNING.inv()
            state = state and STATE_STOP.inv()
            state = state or STATE_READY or STATE_TOUCH
            invalidate()
        }
    }

    /**
     * 添加控制点
     */
    open fun addPoint(): Boolean {
        if (!isReady) {
            return false
        }
        val size = controlPoints.size
        if (size >= MAX_COUNT + 1) {
            return false
        }
        val last = controlPoints[size - 1]
        val step = (areaWidth / 5).toFloat()
        // 以最后一个控制点为中心的候选偏移
        val region = arrayOf(
            floatArrayOf(0f, step), floatArrayOf(0f, -step),
            floatArrayOf(step, step), floatArrayOf(-step, -step),
            floatArrayOf(step, 0f), floatArrayOf(-step, 0f),
            floatArrayOf(0f, 1.5f * step), floatArrayOf(0f, -1.5f * step),
            floatArrayOf(1.5f * step, 1.5f * step), floatArrayOf(-1.5f * step, -1.5f * step),
            floatArrayOf(1.5f * step, 0f), floatArrayOf(-1.5f * step, 0f),
            floatArrayOf(0f, 2 * step), floatArrayOf(0f, -2 * step),
            floatArrayOf(2 * step, 2 * step), floatArrayOf(-2 * step, -2 * step),
            floatArrayOf(2 * step, 0f), floatArrayOf(-2 * step, 0f)
        )
        // 先随机试 region.size 次，都不合法再按顺序逐个试
        val random = Random()
        val candidates = List(region.size) { region[random.nextInt(region.size)] } + region
        for (offset in candidates) {
            val px = last.x + offset[0]
            val py = last.y + offset[1]
            if (isLegalTouchRegion(px, py)) {
                controlPoints.add(PointF(px, py))
                invalidate()
                return true
            }
        }
        // 所有候选位置都不合法时没有加上控制点，如实返回 false
        return false
    }

    /**
     * 删除控制点
     */
    open fun delPoint(): Boolean {
        if (isReady) {
            val size = controlPoints.size
            if (size <= 2) {
                return false
            }
            controlPoints.removeAt(size - 1)
            invalidate()
            return true
        }
        return false
    }

    /**
     * 贝塞尔曲线阶数
     */
    open fun getOrder(): Int = controlPoints.size - 1

    /**
     * 设置贝塞尔曲线阶数：先停止播放，再逐个增删控制点
     */
    open fun setOrder(order: Int) {
        if (getOrder() == order) {
            return
        }
        stop()
        val size = getOrder() - order
        if (size > 0) {
            repeat(size) { delPoint() }
        } else if (size < 0) {
            repeat(-size) { addPoint() }
        }
    }

    /**
     * 贝塞尔曲线阶数（中文）
     */
    open fun getOrderStr(): String = ORDER_STRS[getOrder() - 1]

    // getOrder / setOrder / getOrderStr 保持为函数，Kotlin 使用方原来的 setOrder(n) 写法才能编译；
    // 再提供同名属性兼容原来的 bezier.order 写法。访问器对 Java 隐藏，Java 只看到上面的函数

    /**
     * 贝塞尔曲线阶数，读写分别等同 getOrder / setOrder
     */
    var order: Int
        @JvmSynthetic @JvmName("orderProperty") get() = getOrder()
        @JvmSynthetic @JvmName("orderProperty") set(value) = setOrder(value)

    /**
     * 贝塞尔曲线阶数（中文），等同 getOrderStr
     */
    val orderStr: String
        @JvmSynthetic @JvmName("orderStrProperty") get() = getOrderStr()

    /**
     * 设置移动速率
     */
    open fun setRate(rate: Int) {
        this.rate = rate
    }

    /**
     * 设置是否显示切线
     */
    open fun setTangent(tangent: Boolean) {
        this.tangent = tangent
    }

    /**
     * 设置是否循环
     */
    open fun setLoop(loop: Boolean) {
        this.loop = loop
    }

    // private：不给 Java 使用方暴露 BezierView.Companion

    // companion 和成员都要 private：companion 里非 private 的 const val 会编译成 BezierView 的 public 静态字段
    private companion object {
        private const val MAX_COUNT = 7  // 贝塞尔曲线最大阶数
        private const val REGION_WIDTH = 30  // 合法区域宽度
        private const val FINGER_RECT_SIZE = 60   // 矩形尺寸
        private const val BEZIER_WIDTH = 10   // 贝塞尔曲线线宽
        private const val TANGENT_WIDTH = 6  // 切线线宽
        private const val CONTROL_WIDTH = 12    // 控制点连线线宽
        private const val CONTROL_RADIUS = 12  // 控制点半径
        private const val TEXT_SIZE = 40    // 文字画笔尺寸
        private const val TEXT_HEIGHT = 60  // 文本高度
        private const val RATE = 10 // 移动速率
        private const val HANDLER_WHAT = 100
        private const val FRAME = 1000  // 1000帧
        private val TANGENT_COLORS = arrayOf(
            "#7fff00",
            "#7a67ee",
            "#ee82ee",
            "#ffd700",
            "#1c86ee",
            "#8b8b00"
        )  // 切线颜色
        private const val STATE_READY = 0x0001
        private const val STATE_RUNNING = 0x0002
        private const val STATE_STOP = 0x0004
        private const val STATE_TOUCH = 0x0010

        private val ORDER_STRS = arrayOf("一", "二", "三", "四", "五", "六", "七")

        /**
         * 限制在离边缘超过 REGION_WIDTH 的范围内，与 isLegalTouchRegion 一致；控件太小放不下时居中
         */
        private fun clampToArea(value: Float, size: Int): Float {
            val min = (REGION_WIDTH + 1).toFloat()
            val max = (size - REGION_WIDTH - 1).toFloat()
            if (min > max) {
                return size / 2f
            }
            return value.coerceIn(min, max)
        }
    }
}

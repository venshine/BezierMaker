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
package com.wx.beziermaker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * 贝塞尔曲线
 *
 * @author venshine
 */
public class BezierView extends View {

    private static final int MAX_COUNT = 7;  // 贝塞尔曲线最大阶数
    private static final int REGION_WIDTH = 30;  // 合法区域宽度
    private static final int FINGER_RECT_SIZE = 60;   // 矩形尺寸
    private static final int BEZIER_WIDTH = 10;   // 贝塞尔曲线线宽
    private static final int TANGENT_WIDTH = 6;  // 切线线宽
    private static final int CONTROL_WIDTH = 12;    // 控制点连线线宽
    private static final int CONTROL_RADIUS = 12;  // 控制点半径
    private static final int TEXT_SIZE = 40;    // 文字画笔尺寸
    private static final int TEXT_HEIGHT = 60;  // 文本高度
    private static final int RATE = 10; // 移动速率
    private static final int HANDLER_WHAT = 100;
    private static final int FRAME = 1000;  // 1000帧
    private static final String[] TANGENT_COLORS = {"#7fff00", "#7a67ee", "#ee82ee", "#ffd700", "#1c86ee",
            "#8b8b00"};  // 切线颜色
    private static final int STATE_READY = 0x0001;
    private static final int STATE_RUNNING = 0x0002;
    private static final int STATE_STOP = 0x0004;
    private static final int STATE_TOUCH = 0x0010;

    private Path mBezierPath = null;    // 贝塞尔曲线路径

    private Paint mBezierPaint = null;  // 贝塞尔曲线画笔
    private Paint mMovingPaint = null;  // 移动点画笔
    private Paint mControlPaint = null;  // 控制点画笔
    private Paint mTangentPaint = null;  // 切线画笔
    private Paint mLinePaint = null;    // 固定线画笔
    private Paint mTextPointPaint = null;    // 点画笔
    private Paint mTextPaint = null;    // 文字画笔

    private ArrayList<PointF> mBezierPoints = null; // 贝塞尔曲线点集
    private PointF mBezierPoint = null; // 贝塞尔曲线移动点

    private ArrayList<PointF> mControlPoints = null;    // 控制点集

    private ArrayList<ArrayList<ArrayList<PointF>>> mTangentPoints; // 切线点集

    private ArrayList<ArrayList<PointF>> mInstantTangentPoints;

    private int mR = 0;  // 移动速率

    private int mRate = RATE;   // 速率

    private int mState; // 状态

    private boolean mLoop = false;  // 设置是否循环

    private boolean mTangent = true;   // 设置是否显示切线

    private int mWidth = 0, mHeight = 0;    // 画布宽高

    private PointF mCurPoint; // 当前移动的控制点

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == HANDLER_WHAT) {
                mR += mRate;
                if (mR >= mBezierPoints.size()) {
                    removeMessages(HANDLER_WHAT);
                    mR = 0;
                    mState &= ~STATE_RUNNING;
                    mState &= ~STATE_STOP;
                    mState |= STATE_READY | STATE_TOUCH;
                    if (mLoop) {
                        start();
                    }
                    return;
                }
                if (mR != mBezierPoints.size() - 1 && mR + mRate >= mBezierPoints.size()) {
                    mR = mBezierPoints.size() - 1;
                }
                // Bezier点
                mBezierPoint = new PointF(mBezierPoints.get(mR).x, mBezierPoints.get(mR).y);
                // 切线点
                if (mTangent) {
                    int size = mTangentPoints.size();
                    ArrayList<PointF> instantpoints;
                    mInstantTangentPoints = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        int len = mTangentPoints.get(i).size();
                        instantpoints = new ArrayList<>();
                        for (int j = 0; j < len; j++) {
                            float x = mTangentPoints.get(i).get(j).get(mR).x;
                            float y = mTangentPoints.get(i).get(j).get(mR).y;
                            instantpoints.add(new PointF(x, y));
                        }
                        mInstantTangentPoints.add(instantpoints);
                    }
                }
                if (mR == mBezierPoints.size() - 1) {
                    mState |= STATE_STOP;
                }
                invalidate();
            }
        }
    };

    public BezierView(Context context) {
        super(context);
        init();
    }

    public BezierView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BezierView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始坐标
        mControlPoints = new ArrayList<>(MAX_COUNT + 1);
        int w = getResources().getDisplayMetrics().widthPixels;
        mControlPoints.add(new PointF(w / 5, w / 5));
        mControlPoints.add(new PointF(w / 3, w / 2));
        mControlPoints.add(new PointF(w / 3 * 2, w / 4));

        // 贝塞尔曲线画笔
        mBezierPaint = new Paint();
        mBezierPaint.setColor(Color.RED);
        mBezierPaint.setStrokeWidth(BEZIER_WIDTH);
        mBezierPaint.setStyle(Paint.Style.STROKE);
        mBezierPaint.setAntiAlias(true);

        // 移动点画笔
        mMovingPaint = new Paint();
        mMovingPaint.setColor(Color.BLACK);
        mMovingPaint.setAntiAlias(true);
        mMovingPaint.setStyle(Paint.Style.FILL);

        // 控制点画笔
        mControlPaint = new Paint();
        mControlPaint.setColor(Color.BLACK);
        mControlPaint.setAntiAlias(true);
        mControlPaint.setStyle(Paint.Style.STROKE);

        // 切线画笔
        mTangentPaint = new Paint();
        mTangentPaint.setColor(Color.parseColor(TANGENT_COLORS[0]));
        mTangentPaint.setAntiAlias(true);
        mTangentPaint.setStrokeWidth(TANGENT_WIDTH);
        mTangentPaint.setStyle(Paint.Style.FILL);

        // 固定线画笔
        mLinePaint = new Paint();
        mLinePaint.setColor(Color.LTGRAY);
        mLinePaint.setStrokeWidth(CONTROL_WIDTH);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL);

        // 点画笔
        mTextPointPaint = new Paint();
        mTextPointPaint.setColor(Color.BLACK);
        mTextPointPaint.setAntiAlias(true);
        mTextPointPaint.setTextSize(TEXT_SIZE);

        // 文字画笔
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.GRAY);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(TEXT_SIZE);

        mBezierPath = new Path();

        mState |= STATE_READY | STATE_TOUCH;
    }

    /**
     * 创建Bezier点集
     *
     * @return
     */
    private ArrayList<PointF> buildBezierPoints() {
        ArrayList<PointF> points = new ArrayList<>();
        int order = mControlPoints.size() - 1;
        float delta = 1.0f / FRAME;
        for (float t = 0; t <= 1; t += delta) {
            // Bezier点集
            points.add(new PointF(deCasteljauX(order, 0, t), deCasteljauY(order, 0, t)));
        }
        return points;
    }

    /**
     * 创建切线点集
     */
    private ArrayList<ArrayList<ArrayList<PointF>>> buildTangentPoints() {
        ArrayList<PointF> points;   // 1条线点集
        ArrayList<ArrayList<PointF>> morepoints;    // 多条线点集
        ArrayList<ArrayList<ArrayList<PointF>>> allpoints = new ArrayList<>();  // 所有点集
        PointF point;
        int order = mControlPoints.size() - 1;
        float delta = 1.0f / FRAME;
        for (int i = 0; i < order - 1; i++) {
            int size = allpoints.size();
            morepoints = new ArrayList<>();
            for (int j = 0; j < order - i; j++) {
                points = new ArrayList<>();
                for (float t = 0; t <= 1; t += delta) {
                    float p0x = 0;
                    float p1x = 0;
                    float p0y = 0;
                    float p1y = 0;
                    int z = (int) (t * FRAME);
                    if (size > 0) {
                        p0x = allpoints.get(i - 1).get(j).get(z).x;
                        p1x = allpoints.get(i - 1).get(j + 1).get(z).x;
                        p0y = allpoints.get(i - 1).get(j).get(z).y;
                        p1y = allpoints.get(i - 1).get(j + 1).get(z).y;
                    } else {
                        p0x = mControlPoints.get(j).x;
                        p1x = mControlPoints.get(j + 1).x;
                        p0y = mControlPoints.get(j).y;
                        p1y = mControlPoints.get(j + 1).y;
                    }
                    float x = (1 - t) * p0x + t * p1x;
                    float y = (1 - t) * p0y + t * p1y;
                    point = new PointF(x, y);
                    points.add(point);
                }
                morepoints.add(points);
            }
            allpoints.add(morepoints);
        }

        return allpoints;
    }

    /**
     * deCasteljau算法
     *
     * @param i 阶数
     * @param j 点
     * @param t 时间
     * @return
     */
    private float deCasteljauX(int i, int j, float t) {
        if (i == 1) {
            return (1 - t) * mControlPoints.get(j).x + t * mControlPoints.get(j + 1).x;
        }
        return (1 - t) * deCasteljauX(i - 1, j, t) + t * deCasteljauX(i - 1, j + 1, t);
    }

    /**
     * deCasteljau算法
     *
     * @param i 阶数
     * @param j 点
     * @param t 时间
     * @return
     */
    private float deCasteljauY(int i, int j, float t) {
        if (i == 1) {
            return (1 - t) * mControlPoints.get(j).y + t * mControlPoints.get(j + 1).y;
        }
        return (1 - t) * deCasteljauY(i - 1, j, t) + t * deCasteljauY(i - 1, j + 1, t);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mWidth == 0 || mHeight == 0) {
            mWidth = getWidth();
            mHeight = getHeight();
        }
    }

    /**
     * 判断坐标是否在合法区域中
     *
     * @param x
     * @param y
     * @return
     */
    private boolean isLegalTouchRegion(float x, float y) {
        if (x <= REGION_WIDTH || x >= mWidth - REGION_WIDTH || y <= REGION_WIDTH || y >= mHeight - REGION_WIDTH) {
            return false;
        }
        RectF rectF = new RectF();
        for (PointF point : mControlPoints) {
            if (mCurPoint != null && mCurPoint.equals(point)) { // 判断是否是当前控制点
                continue;
            }
            rectF.set(point.x - REGION_WIDTH, point.y - REGION_WIDTH, point.x + REGION_WIDTH, point.y + REGION_WIDTH);
            if (rectF.contains(x, y)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取合法控制点
     *
     * @param x
     * @param y
     * @return
     */
    private PointF getLegalControlPoint(float x, float y) {
        RectF rectF = new RectF();
        for (PointF point : mControlPoints) {
            rectF.set(point.x - REGION_WIDTH, point.y - REGION_WIDTH, point.x + REGION_WIDTH, point.y + REGION_WIDTH);
            if (rectF.contains(x, y)) {
                return point;
            }
        }
        return null;
    }


    /**
     * 判断手指坐标是否在合法区域中
     *
     * @param x
     * @param y
     * @return
     */
    private boolean isLegalFingerRegion(float x, float y) {
        if (mCurPoint != null) {
            RectF rectF = new RectF(mCurPoint.x - FINGER_RECT_SIZE / 2, mCurPoint.y - FINGER_RECT_SIZE / 2, mCurPoint
                    .x +
                    FINGER_RECT_SIZE / 2, mCurPoint.y +
                    FINGER_RECT_SIZE / 2);
            if (rectF.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isRunning() && !isTouchable()) {
            if (mBezierPoint == null) {
                mBezierPath.reset();
                mBezierPoint = mBezierPoints.get(0);
                mBezierPath.moveTo(mBezierPoint.x, mBezierPoint.y);
            }
            // 控制点和控制点连线
            int size = mControlPoints.size();
            PointF point;
            for (int i = 0; i < size; i++) {
                point = mControlPoints.get(i);
                if (i > 0) {
                    // 控制点连线
                    canvas.drawLine(mControlPoints.get(i - 1).x, mControlPoints.get(i - 1).y, point.x, point.y,
                            mLinePaint);
                }
                // 控制点
                canvas.drawCircle(point.x, point.y, CONTROL_RADIUS, mControlPaint);
                // 控制点文本
                canvas.drawText("p" + i, point.x + CONTROL_RADIUS * 2, point.y + CONTROL_RADIUS * 2, mTextPointPaint);
                // 控制点文本展示
                canvas.drawText("p" + i + " ( " + new DecimalFormat("##0.0").format(point.x) + " , " + new DecimalFormat
                        ("##0.0").format(point.y) + ") ", REGION_WIDTH, mHeight - (size - i) * TEXT_HEIGHT, mTextPaint);

            }

            // 切线
            if (mTangent && mInstantTangentPoints != null && !isStop()) {
                int tsize = mInstantTangentPoints.size();
                ArrayList<PointF> tps;
                for (int i = 0; i < tsize; i++) {
                    tps = mInstantTangentPoints.get(i);
                    int tlen = tps.size();
                    for (int j = 0; j < tlen - 1; j++) {
                        mTangentPaint.setColor(Color.parseColor(TANGENT_COLORS[i]));
                        canvas.drawLine(tps.get(j).x, tps.get(j).y, tps.get(j + 1).x, tps.get(j + 1).y,
                                mTangentPaint);
                        canvas.drawCircle(tps.get(j).x, tps.get(j).y, CONTROL_RADIUS, mTangentPaint);
                        canvas.drawCircle(tps.get(j + 1).x, tps.get(j + 1).y, CONTROL_RADIUS, mTangentPaint);
                    }
                }
            }

            // Bezier曲线
            mBezierPath.lineTo(mBezierPoint.x, mBezierPoint.y);
            canvas.drawPath(mBezierPath, mBezierPaint);
            // Bezier曲线起始移动点
            canvas.drawCircle(mBezierPoint.x, mBezierPoint.y, CONTROL_RADIUS, mMovingPaint);
            // 时间展示
            canvas.drawText("t:" + (new DecimalFormat("##0.000").format((float) mR / FRAME)), mWidth - TEXT_HEIGHT *
                    3, mHeight - TEXT_HEIGHT, mTextPaint);

            mHandler.removeMessages(HANDLER_WHAT);
            mHandler.sendEmptyMessage(HANDLER_WHAT);
        }
        if (isTouchable()) {
            // 控制点和控制点连线
            int size = mControlPoints.size();
            PointF point;
            for (int i = 0; i < size; i++) {
                point = mControlPoints.get(i);
                if (i > 0) {
                    canvas.drawLine(mControlPoints.get(i - 1).x, mControlPoints.get(i - 1).y, point.x, point.y,
                            mLinePaint);
                }
                canvas.drawCircle(point.x, point.y, CONTROL_RADIUS, mControlPaint);
                canvas.drawText("p" + i, point.x + CONTROL_RADIUS * 2, point.y + CONTROL_RADIUS * 2, mTextPointPaint);
                canvas.drawText("p" + i + " ( " + new DecimalFormat("##0.0").format(point.x) + " , " + new DecimalFormat
                        ("##0.0").format(point.y) + ") ", REGION_WIDTH, mHeight - (size - i) * TEXT_HEIGHT, mTextPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouchable()) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mState &= ~STATE_READY;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                if (mCurPoint == null) {
                    mCurPoint = getLegalControlPoint(x, y);
                }
                if (mCurPoint != null && isLegalTouchRegion(x, y)) {  // 判断手指移动区域是否合法
                    if (isLegalFingerRegion(x, y)) {    // 判断手指触摸区域是否合法
                        mCurPoint.x = x;
                        mCurPoint.y = y;
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mCurPoint = null;
                mState |= STATE_READY;
                break;
        }
        return true;
    }

    private boolean isReady() {
        return (mState & STATE_READY) == STATE_READY;
    }

    private boolean isRunning() {
        return (mState & STATE_RUNNING) == STATE_RUNNING;
    }

    private boolean isTouchable() {
        return (mState & STATE_TOUCH) == STATE_TOUCH;
    }

    private boolean isStop() {
        return (mState & STATE_STOP) == STATE_STOP;
    }

    /**
     * 开始
     */
    public void start() {
        if (isReady()) {
            mBezierPoint = null;
            mInstantTangentPoints = null;
            mBezierPoints = buildBezierPoints();
            if (mTangent) {
                mTangentPoints = buildTangentPoints();
            }
            mState &= ~STATE_READY;
            mState &= ~STATE_TOUCH;
            mState |= STATE_RUNNING;
            invalidate();
        }
    }

    /**
     * 停止
     */
    public void stop() {
        if (isRunning()) {
            mHandler.removeMessages(HANDLER_WHAT);
            mR = 0;
            mState &= ~STATE_RUNNING;
            mState &= ~STATE_STOP;
            mState |= STATE_READY | STATE_TOUCH;
            invalidate();
        }
    }

    /**
     * 添加控制点
     */
    public boolean addPoint() {
        if (isReady()) {
            int size = mControlPoints.size();
            if (size >= MAX_COUNT + 1) {
                return false;
            }
            float x = mControlPoints.get(size - 1).x;
            float y = mControlPoints.get(size - 1).y;
            int r = mWidth / 5;
            float[][] region = {{0, r}, {0, -r}, {r, r}, {-r, -r}, {r, 0}, {-r, 0}, {0, 1.5f * r}, {0, -1.5f * r}, {1.5f
                    * r, 1.5f *
                    r}, {-1.5f * r, -1.5f * r}, {1.5f * r, 0}, {-1.5f * r, 0}, {0, 2 * r}, {0, -2 * r}, {2 * r, 2 *
                    r}, {-2 * r, -2 * r}, {2 * r, 0}, {-2 * r, 0}};
            int t = 0;
            int len = region.length;
            while (true) {  // 随机赋值
                t++;
                if (t > len) {  // 超出region长度，跳出随机赋值
                    t = 0;
                    break;
                }
                int rand = new Random().nextInt(len);
                float px = x + region[rand][0];
                float py = y + region[rand][1];
                if (isLegalTouchRegion(px, py)) {
                    mControlPoints.add(new PointF(px, py));
                    invalidate();
                    break;
                }
            }
            if (t == 0) {   // 超出region长度而未赋值时，循环赋值
                for (int i = 0; i < len; i++) {
                    float px = x + region[i][0];
                    float py = y + region[i][1];
                    if (isLegalTouchRegion(px, py)) {
                        mControlPoints.add(new PointF(px, py));
                        invalidate();
                        break;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 删除控制点
     */
    public boolean delPoint() {
        if (isReady()) {
            int size = mControlPoints.size();
            if (size <= 2) {
                return false;
            }
            mControlPoints.remove(size - 1);
            invalidate();
            return true;
        }
        return false;
    }

    /**
     * 贝塞尔曲线阶数
     *
     * @return
     */
    public int getOrder() {
        return mControlPoints.size() - 1;
    }

    /**
     * 设置贝塞尔曲线阶数
     *
     * @param order
     */
    public void setOrder(int order) {
        if (getOrder() == order) {
            return;
        }
        stop();
        int size = getOrder() - order;
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                delPoint();
            }
        } else if (size < 0) {
            for (int i = -size; i > 0; i--) {
                addPoint();
            }
        }
    }

    /**
     * 贝塞尔曲线阶数
     *
     * @return
     */
    public String getOrderStr() {
        String str = "";
        switch (getOrder()) {
            case 1:
                str = "一";
                break;
            case 2:
                str = "二";
                break;
            case 3:
                str = "三";
                break;
            case 4:
                str = "四";
                break;
            case 5:
                str = "五";
                break;
            case 6:
                str = "六";
                break;
            case 7:
                str = "七";
                break;
            default:
                break;
        }
        return str;
    }

    /**
     * 设置移动速率
     *
     * @param rate
     */
    public void setRate(int rate) {
        mRate = rate;
    }

    /**
     * 设置是否显示切线
     *
     * @param tangent
     */
    public void setTangent(boolean tangent) {
        mTangent = tangent;
    }

    /**
     * 设置是否循环
     *
     * @param loop
     */
    public void setLoop(boolean loop) {
        mLoop = loop;
    }

}

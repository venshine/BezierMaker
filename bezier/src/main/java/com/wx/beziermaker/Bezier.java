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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author venshine
 */
public class Bezier extends View {

    private static final int COUNT = 9;  // 贝塞尔曲线阶数
    private static final int REGION_WIDTH = 30;  // 合法区域宽度
    private static final int FINGER_RECT_SIZE = 60;   // 矩形尺寸
    private static final int BEZIER_WIDTH = 10;   // 贝塞尔曲线线宽
    private static final int TANGENT_WIDTH = 10;  // 切线线宽
    private static final int CONTROL_WIDTH = 12;    // 控制点连线线宽
    private static final int CONTROL_RADIUS = 12;  // 控制点半径
    private static final int RATE = 10; // 移动速率
    private static final int HANDLER_WHAT = 100;

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

    private ArrayList<PointF> mPoints1 = new ArrayList<>();
    private ArrayList<PointF> mPoints2 = new ArrayList<>();

    private int mR = 0;  // 移动速率

    private int mRate = RATE;   // 速率

    private boolean mLoop = false;  // 设置是否循环

    private boolean mRun = true;   // 运行状态

    private boolean mTouch = true; // 控制状态

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
                    mRun = false;
                    mTouch = true;
                    return;
                }
                if (mR != mBezierPoints.size() - 1 && mR + mRate >= mBezierPoints.size()) {
                    mBezierPoint = new PointF(mBezierPoints.get(mBezierPoints.size() - 1).x, mBezierPoints.get
                            (mBezierPoints.size() - 1).y);
                } else {
                    mBezierPoint = new PointF(mBezierPoints.get(mR).x, mBezierPoints.get(mR).y);
                }
//                mP1 = new PointF(mPoints1.get(mR).x, mPoints1.get(mR).y);
//                mP2 = new PointF(mPoints2.get(mR).x, mPoints2.get(mR).y);
                invalidate();
            }
        }
    };

    public Bezier(Context context) {
        super(context);
        init();
    }

    public Bezier(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Bezier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始坐标
        mControlPoints = new ArrayList<>(COUNT + 1);
        mControlPoints.add(new PointF(150, 300));
        mControlPoints.add(new PointF(100, 100));
        mControlPoints.add(new PointF(400, 100));
        mControlPoints.add(new PointF(450, 300));
        mControlPoints.add(new PointF(500, 120));

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
        mTangentPaint.setColor(Color.GREEN);
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
        mTextPointPaint.setTextSize(45);

        // 文字画笔
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.GRAY);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(40);

        mBezierPath = new Path();
    }

    /**
     * 创建Bezier点集
     *
     * @return
     */
    private ArrayList<PointF> buildBezierPoints() {
        ArrayList<PointF> points = new ArrayList<>();
        int order = mControlPoints.size() - 1;
        for (float t = 0; t <= 1; t += 0.001f) {
            points.add(new PointF(deCasteljauX(order, 0, t), deCasteljauY(order, 0, t)));
        }
        return points;
    }

    /**
     * deCasteljau算法
     *
     * @param i 阶数
     * @param j 点
     * @param t 时间
     * @return
     */
    public float deCasteljauX(int i, int j, float t) {
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
    public float deCasteljauY(int i, int j, float t) {
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
        if (mRun && !mTouch) {
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
                    canvas.drawLine(mControlPoints.get(i - 1).x, mControlPoints.get(i - 1).y, point.x, point.y,
                            mLinePaint);
                }
                canvas.drawCircle(point.x, point.y, CONTROL_RADIUS, mControlPaint);
                canvas.drawText("p" + i, point.x + CONTROL_RADIUS * 2, point.y + CONTROL_RADIUS * 2, mTextPointPaint);
                canvas.drawText("p" + i + " ( " + new DecimalFormat("##0.0").format(point.x) + " , " + new DecimalFormat
                        ("##0.0").format(point.y) + ") ", REGION_WIDTH, mHeight - (size - i) * 60, mTextPaint);
            }

            // 切线
//            canvas.drawLine(mP1.x, mP1.y, mP2.x, mP2.y, mTangentPaint);

            // Bezier曲线
            mBezierPath.lineTo(mBezierPoint.x, mBezierPoint.y);
            canvas.drawPath(mBezierPath, mBezierPaint);
//             Bezier曲线起始移动点
            canvas.drawCircle(mBezierPoint.x, mBezierPoint.y, CONTROL_RADIUS, mMovingPaint);

            mHandler.removeMessages(HANDLER_WHAT);
            mHandler.sendEmptyMessage(HANDLER_WHAT);
        }
        if (mTouch) {
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
                        ("##0.0").format(point.y) + ") ", REGION_WIDTH, mHeight - (size - i) * 60, mTextPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouch) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mRun = false;
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
                mRun = true;
                break;
        }
        return true;
    }

    /**
     * 开始
     */
    public void start() {
        if (mRun) {
            mBezierPoint = null;
            mBezierPoints = buildBezierPoints();
            mRun = true;
            mTouch = false;
            invalidate();
        }
    }

    /**
     * 添加控制点
     */
    public boolean addPoint() {
        mRun = false;
        int size = mControlPoints.size();
        if (size >= COUNT + 1) {
            mRun = true;
            return false;
        }
        float x = mControlPoints.get(size - 1).x;
        float y = mControlPoints.get(size - 1).y;
        int r = mWidth / 5;
        float[][] region = {{0, r}, {0, -r}, {r, r}, {-r, -r}, {r, 0}, {-r, 0}};
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
        mRun = true;
        return true;
    }

    /**
     * 删除控制点
     */
    public boolean delPoint() {
        mRun = false;
        int size = mControlPoints.size();
        if (size <= 2) {
            mRun = true;
            return false;
        }
        mControlPoints.remove(size - 1);
        invalidate();
        mRun = true;
        return true;
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
     * 设置是否循环
     *
     * @param loop
     */
    public void setLoop(boolean loop) {
        mLoop = loop;
    }

    private void log(String msg) {
        Log.d("venshine", msg);
    }

}

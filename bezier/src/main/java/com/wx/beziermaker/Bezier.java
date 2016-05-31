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

import java.util.ArrayList;

/**
 * @author venshine
 */
public class Bezier extends View {

    public static final int COUNT = 9;  // 贝塞尔曲线阶数
    public static final int RECT_RADIUS = 30;   // 矩形半径
    public static final int BEZIER_WIDTH = 10;   // 贝塞尔曲线线宽
    public static final int TANGENT_WIDTH = 10;  // 切线线宽
    public static final int CONTROL_WIDTH = 12;    // 控制点连线线宽
    public static final int CONTROL_RADIUS = 12;  // 控制点半径

    private Path mBezierPath = null;    // 贝塞尔曲线路径

    private Paint mBezierPaint = null;  // 贝塞尔曲线画笔
    private Paint mMovingPaint = null;  // 移动点画笔
    private Paint mControlPaint = null;  // 控制点画笔
    private Paint mTangentPaint = null;  // 切线画笔
    private Paint mLinePaint = null;    // 固定线画笔
    private Paint mTextPaint = null;    // 文字画笔

    private ArrayList<PointF> mBezierPoints = null; // 贝塞尔曲线点集
    private PointF mBezierPoint = null; // 贝塞尔曲线移动点

    private PointF mPoint1, mPoint2;

//    private PointF p1, p2, p3;  // 控制点
    private PointF pStart, pEnd, p1, p2, p3, p4, p5, p6, p7, p8;
    private ArrayList<PointF> mControlPoints;    // 控制点集

    private ArrayList<PointF> mPoints1 = new ArrayList<>();
    private ArrayList<PointF> mPoints2 = new ArrayList<>();

    private int i = 0;

    private int mCurOrder = 2;  // 当前阶数

    private boolean mRun = false;   // 运行状态

    private boolean mTouch = true; // 控制状态

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                i += 3;
                if (i >= mBezierPoints.size() || i >= mPoints1.size() || i >= mPoints2.size()) {
                    removeMessages(100);
                    mRun = false;
                    mTouch = true;
                    return;
                }
                mBezierPoint = new PointF(mBezierPoints.get(i).x, mBezierPoints.get(i).y);
                mPoint1 = new PointF(mPoints1.get(i).x, mPoints1.get(i).y);
                mPoint2 = new PointF(mPoints2.get(i).x, mPoints2.get(i).y);
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
        log(mControlPoints.size() + "");
        p1 = new PointF(100, 100);
        p2 = new PointF(200, 300);
        p3 = new PointF(500, 200);
        mControlPoints.add(p1);
        mControlPoints.add(p2);
        mControlPoints.add(p3);

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

        // 文字画笔
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setAntiAlias(true);

        mPoint1 = p1;
        mPoint2 = p2;

        mBezierPoints = createQuad(p1, p2, p3);

        mBezierPath = new Path();
    }

    private ArrayList<PointF> createQuad(PointF start, PointF con, PointF end) {
        ArrayList<PointF> points = new ArrayList<>();
        float x, y;
        for (float t = 0; t <= 1; t += 0.001f) {
            x = (1 - t) * (1 - t) * start.x + 2 * t * (1 - t) * con.x + t * t * end.x;
            y = (1 - t) * (1 - t) * start.y + 2 * t * (1 - t) * con.y + t * t * end.y;
            points.add(new PointF(x, y));
        }
        for (float i = 0; i <= 1; i += 0.001f) {
            float xx = start.x + (con.x - start.x) * i;
            float yy = start.y + (con.y - start.y) * i;
            float xxx = con.x + (end.x - con.x) * i;
            float yyy = con.y + (end.y - con.y) * i;
            mPoints1.add(new PointF(xx, yy));
            mPoints2.add(new PointF(xxx, yyy));
        }
        return points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRun && !mTouch) {
            if (mBezierPoint == null) {
                mBezierPoint = mBezierPoints.get(0);
                mBezierPath.moveTo(mBezierPoint.x, mBezierPoint.y);
            }
            // 固定点连线
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mLinePaint);
            canvas.drawLine(p2.x, p2.y, p3.x, p3.y, mLinePaint);

            // 固定点
            canvas.drawCircle(p1.x, p1.y, CONTROL_RADIUS, mControlPaint);
            canvas.drawCircle(p2.x, p2.y, CONTROL_RADIUS, mControlPaint);
            canvas.drawCircle(p3.x, p3.y, CONTROL_RADIUS, mControlPaint);

            // 连线
            canvas.drawLine(mPoint1.x, mPoint1.y, mPoint2.x, mPoint2.y, mTangentPaint);

            // Bezier曲线
            mBezierPath.lineTo(mBezierPoint.x, mBezierPoint.y);
            canvas.drawPath(mBezierPath, mBezierPaint);
            // Bezier曲线起始移动点
            canvas.drawCircle(mBezierPoint.x, mBezierPoint.y, CONTROL_RADIUS, mMovingPaint);

            mHandler.removeMessages(100);
            mHandler.sendEmptyMessage(100);
        }
        if (mTouch) {
            // 固定点连线
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mLinePaint);
            canvas.drawLine(p2.x, p2.y, p3.x, p3.y, mLinePaint);

            // 固定点
            canvas.drawCircle(p1.x, p1.y, CONTROL_RADIUS, mControlPaint);
            canvas.drawCircle(p2.x, p2.y, CONTROL_RADIUS, mControlPaint);
            canvas.drawCircle(p3.x, p3.y, CONTROL_RADIUS, mControlPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        log("touch:" + mTouch);
        if (!mTouch) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mRun = false;
                log("down");
                break;
            case MotionEvent.ACTION_MOVE:
                log("move");
                float x = event.getX();
                float y = event.getY();
                RectF rectF = new RectF(p1.x - RECT_RADIUS, p1.y - RECT_RADIUS, p1.x + RECT_RADIUS, p1.y + RECT_RADIUS);
                if (rectF.contains(x, y)) {
                    p1.x = x;
                    p1.y = y;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                log("up");
                mRun = true;
                break;
        }
        return true;
    }

    public void start() {
        if (mRun) {
            mRun = true;
            mTouch = false;
            invalidate();
        }
    }

    private void log(String msg) {
        Log.d("venshine", msg);
    }

}

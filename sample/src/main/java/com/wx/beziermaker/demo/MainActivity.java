package com.wx.beziermaker.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wx.beziermaker.Bezier;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new Bezier(this));
    }
}

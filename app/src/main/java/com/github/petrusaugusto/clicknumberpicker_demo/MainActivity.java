package com.github.petrusaugusto.clicknumberpicker_demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import pl.polak.clicknumberpicker.ClickNumberPickerView;


public class MainActivity extends AppCompatActivity {
    protected ClickNumberPickerView clickNumberPickerView, clickNumberPickerView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.clickNumberPickerView = (ClickNumberPickerView) findViewById(R.id.clicknumberpicker);
        this.clickNumberPickerView2 = (ClickNumberPickerView) findViewById(R.id.clicknumberpicker2);
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.clickNumberPickerView.setMaxValue(10);
        this.clickNumberPickerView.setMinValue(1);

        this.clickNumberPickerView2.setStepValue(0.5f);
        this.clickNumberPickerView2.setDecimalNumbers(2);
    }
}

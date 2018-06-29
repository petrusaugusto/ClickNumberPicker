package pl.polak.clicknumberpicker;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * I am click number picker view
 */
public class ClickNumberPickerView extends LinearLayout {
    private static final float CLICK_NUMBER_PICKER_MIN_VALUE_DEFAULT = 0.0f;
    private static final float CLICK_NUMBER_PICKER_MAX_VALUE_DEFAULT = 100;
    private static final float CLICK_NUMBER_PICKER_VALUE_DEFAULT = 0;
    private static final float CLICK_NUMBER_PICKER_STEP_DEFAULT = 1;
    private static final int CLICK_NUMBER_PICKER_VALUE_TEXT_SIZE_DEFAULT = 15;
    private static final int CLICK_NUMBER_PICKER_VALUE_ANIMATION_MIN_TEXT_SIZE_DEFAULT = 10;
    private static final int CLICK_NUMBER_PICKER_VALUE_ANIMATION_MAX_TEXT_SIZE_DEFAULT = 22;
    private static final int CLICK_NUMBER_PICKER_VALUE_VIEW_OFFSET_DEFAULT = 20;
    private static final float CLICK_NUMBER_PICKER_CORNER_RADIUS_DEFAULT = 10;
    private static final int CLICK_NUMBER_PICKER_DECIMAL_NUMBER_DEFAULT = 2;
    private static final int CLICK_NUMBER_PICKER_UP_DOWN_DURATION_DEFAULT = 200;
    private static final int CLICK_NUMBER_PICKER_OFFSET_ANIMATION_DURATION_DEFAULT = 150;

    private FrameLayout flLeftPicker;
    private FrameLayout flRightPicker;
    private EditText tvValue;
    private LinearLayout rlRootView;
    private RelativeLayout rlCenter;

    private Pattern inputFitlerPattern;
    private boolean showKeyboard;
    private boolean swipeEnabled;
    private float initialValue;
    private float minValue;
    private float maxValue;
    private float step;
    private boolean integerPriority;
    private int valueBackgroundColor;
    private int pickersBackgroundColor;
    private int animationUpDuration;
    private int animationDownDuration;
    private boolean animationUpEnabled;
    private boolean animationDownEnabled;
    private boolean animationSwipeEnabled;
    private int valueColor;
    private int valueTextSize;
    private int valueMinTextSize;
    private int valueMaxTextSize;
    private float valueViewOffset;
    private float pickerCornerRadius;
    private int pickerBorderStrokeWidth;
    private int pickerBorderStrokeColor;
    private int decimalNumbers;
    private int animationOffsetLeftDuration;
    private int animationOffsetRightDuration;
    private int leftPickerLayout;
    private int rightPickerLayout;

    private ClickNumberPickerListener clickNumberPickerListener = new ClickNumberPickerListener() {
        @Override
        public void onValueChange(float previousValue, float currentValue, PickerClickType pickerClickType) {}
    };

    private ObjectAnimator leftPickerTranslationXAnimator;
    private ObjectAnimator rightPickerTranslationXAnimator;
    private ValueAnimator valueUpChangeAnimator;
    private ValueAnimator valueDownChangeAnimator;

    private String valueFormatter;
    private Handler swipeValueChangeHandler = new Handler();
    private PickerClickType swipeDirection = PickerClickType.NONE;
    private float swipeStep = 1;

    private Runnable valueChangeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                switch (swipeDirection) {
                    case LEFT:
                        updatePickerValueByStep(-(swipeStep * swipeStep));
                        break;
                    case RIGHT:
                        updatePickerValueByStep(swipeStep * swipeStep);
                        break;
                }
            } finally {
                ++swipeStep;
                swipeValueChangeHandler.postDelayed(valueChangeRunnable, 200);
            }
        }
    };

    private OnTouchListener touchListener = new OnTouchListener() {
        private float dX = 0.0f;
        private float initTouchX = 0.0f;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    swipeStep = 1;
                    dX = rlCenter.getX() - event.getRawX();
                    initTouchX = rlCenter.getX();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if ((initTouchX - valueViewOffset * 2) > event.getRawX() + dX){
                        swipeDirection = PickerClickType.LEFT;
                        valueChangeRunnable.run();
                        break;
                    } else if ((initTouchX + valueViewOffset * 2) < event.getRawX() + dX) {
                        swipeDirection = PickerClickType.RIGHT;
                        valueChangeRunnable.run();
                        break;
                    }

                    rlCenter.animate()
                            .x(event.getRawX() + dX)
                            .setDuration(0)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                    swipeValueChangeHandler.removeCallbacks(valueChangeRunnable);

                    rlCenter.animate()
                            .x(initTouchX)
                            .setDuration(250)
                            .start();
                    break;
                default:
                    return false;
            }

            return true;
        }
    };

    private OnClickListener leftPickerListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (animationSwipeEnabled) {
                if (leftPickerTranslationXAnimator.isRunning()) {
                    leftPickerTranslationXAnimator.end();
                }

                leftPickerTranslationXAnimator.start();
            }

            if (animationDownEnabled) {
                if (valueDownChangeAnimator.isRunning()) {
                    valueDownChangeAnimator.end();
                }
                valueDownChangeAnimator.start();
            }


            updatePickerValueByStep(-step);
        }
    };

    private OnClickListener rightPickerListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (animationSwipeEnabled) {
                if (rightPickerTranslationXAnimator.isRunning()) {
                    rightPickerTranslationXAnimator.end();
                }
                rightPickerTranslationXAnimator.start();
            }

            if (animationUpEnabled) {
                if (valueUpChangeAnimator.isRunning()) {
                    valueUpChangeAnimator.end();
                }
                valueUpChangeAnimator.start();
            }

            updatePickerValueByStep(step);
        }
    };

    public ClickNumberPickerView(Context context) {
        this(context, null);
    }

    public ClickNumberPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClickNumberPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        readAttributes(context, attrs);

        init();
    }

    private void readAttributes(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ClickNumberPicker);

        showKeyboard = typedArray.getBoolean(R.styleable.ClickNumberPicker_show_keyboard, true);
        swipeEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_swipe_enabled, true);
        initialValue = typedArray.getFloat(R.styleable.ClickNumberPicker_value, CLICK_NUMBER_PICKER_VALUE_DEFAULT);
        minValue = typedArray.getFloat(R.styleable.ClickNumberPicker_min_value, CLICK_NUMBER_PICKER_MIN_VALUE_DEFAULT);
        maxValue = typedArray.getFloat(R.styleable.ClickNumberPicker_max_value, CLICK_NUMBER_PICKER_MAX_VALUE_DEFAULT);
        step = typedArray.getFloat(R.styleable.ClickNumberPicker_step, CLICK_NUMBER_PICKER_STEP_DEFAULT);
        integerPriority = typedArray.getBoolean(R.styleable.ClickNumberPicker_integer_priority, false);
        valueBackgroundColor = typedArray.getColor(R.styleable.ClickNumberPicker_value_background_color, 0);
        pickersBackgroundColor = typedArray.getColor(R.styleable.ClickNumberPicker_pickers_background_color, 0);
        animationUpEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_value_animation_up, false);
        animationDownEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_value_animation_down, false);
        valueColor = typedArray.getColor(R.styleable.ClickNumberPicker_value_text_color, 0);
        valueTextSize = typedArray.getDimensionPixelSize(R.styleable.ClickNumberPicker_value_text_size, CLICK_NUMBER_PICKER_VALUE_TEXT_SIZE_DEFAULT);
        valueMinTextSize = typedArray.getDimensionPixelSize(R.styleable.ClickNumberPicker_value_min_text_size, CLICK_NUMBER_PICKER_VALUE_ANIMATION_MIN_TEXT_SIZE_DEFAULT);
        valueMaxTextSize = typedArray.getDimensionPixelSize(R.styleable.ClickNumberPicker_value_max_text_size, CLICK_NUMBER_PICKER_VALUE_ANIMATION_MAX_TEXT_SIZE_DEFAULT);
        valueViewOffset = typedArray.getFloat(R.styleable.ClickNumberPicker_value_view_offset, CLICK_NUMBER_PICKER_VALUE_VIEW_OFFSET_DEFAULT);
        animationSwipeEnabled = typedArray.getBoolean(R.styleable.ClickNumberPicker_swipe_animation, false);
        pickerCornerRadius = typedArray.getFloat(R.styleable.ClickNumberPicker_picker_corner_radius, CLICK_NUMBER_PICKER_CORNER_RADIUS_DEFAULT);
        pickerBorderStrokeWidth = typedArray.getInt(R.styleable.ClickNumberPicker_picker_border_stroke_width, 0);
        pickerBorderStrokeColor = typedArray.getColor(R.styleable.ClickNumberPicker_picker_border_stroke_color, 0);
        decimalNumbers = typedArray.getInt(R.styleable.ClickNumberPicker_decimal_number, CLICK_NUMBER_PICKER_DECIMAL_NUMBER_DEFAULT);
        animationUpDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_value_up_duration, CLICK_NUMBER_PICKER_UP_DOWN_DURATION_DEFAULT);
        animationDownDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_value_down_duration, CLICK_NUMBER_PICKER_UP_DOWN_DURATION_DEFAULT);
        animationOffsetRightDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_offset_right_duration, CLICK_NUMBER_PICKER_OFFSET_ANIMATION_DURATION_DEFAULT);
        animationOffsetLeftDuration = typedArray.getInt(R.styleable.ClickNumberPicker_animation_offset_left_duration, CLICK_NUMBER_PICKER_OFFSET_ANIMATION_DURATION_DEFAULT);
        leftPickerLayout = typedArray.getResourceId(R.styleable.ClickNumberPicker_left_picker_layout, R.layout.left_picker_view_default);
        rightPickerLayout = typedArray.getResourceId(R.styleable.ClickNumberPicker_right_picker_layout, R.layout.right_picker_view_default);
        typedArray.recycle();
    }

    private void init() {
        initViews();
        initAnimators();
        initListeners();

        applyViewAttributes();
    }

    private void applyViewAttributes() {
        GradientDrawable gd = (GradientDrawable) rlRootView.getBackground().getCurrent();
        gd.setColor(pickersBackgroundColor);
        gd.setCornerRadius(pickerCornerRadius);
        gd.setStroke(pickerBorderStrokeWidth, pickerBorderStrokeColor);

        valueFormatter = NumberFormatUtils.provideFloatFormater(decimalNumbers);
        swipeStep = step;
        rlCenter.setBackgroundColor(valueBackgroundColor);
        tvValue.setTextColor(valueColor);
        setPickerValue(initialValue);
        tvValue.setTextSize(valueTextSize);

        if ( decimalNumbers > 0) inputFitlerPattern = Pattern.compile("[0-9]{0,}+((\\.[0-9]{0," + (decimalNumbers-1) + "})?)||(\\.)?");
        else inputFitlerPattern = Pattern.compile("[0-9]{0,}");

        if ( showKeyboard ) tvValue.setFilters(new InputFilter[]{ new DecimalInputFilter() });
    }

    private void initListeners() {
        if(swipeEnabled) {
            rlCenter.setOnTouchListener(touchListener);
        }

        flLeftPicker.setOnClickListener(leftPickerListener);
        flRightPicker.setOnClickListener(rightPickerListener);
    }

    private void initViews() {
        View view = inflate(getContext(), R.layout.view_click_numberpicker, this);

        flLeftPicker = (FrameLayout) view.findViewById(R.id.fl_click_numberpicker_left);
        rlRootView = (LinearLayout) view.findViewById(R.id.rl_pickers_root);
        flRightPicker = (FrameLayout) view.findViewById(R.id.fl_click_numberpicker_right);
        rlCenter = (RelativeLayout) view.findViewById(R.id.center_picker);
        tvValue = (EditText) view.findViewById(R.id.tv_value_numberpicker);
        View leftPickerView = inflate(getContext(), leftPickerLayout, null);
        flLeftPicker.addView(leftPickerView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        View rightPickerView = inflate(getContext(), rightPickerLayout, null);
        flRightPicker.addView(rightPickerView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Disable softkeyboard?
        if ( showKeyboard == false ) {
            if (Build.VERSION.SDK_INT >= 11) {
                tvValue.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                tvValue.setTextIsSelectable(false);
                tvValue.setEnabled(false);
            } else {
                tvValue.setRawInputType(InputType.TYPE_NULL);
                tvValue.setEnabled(false);
                tvValue.setFocusable(false);
            }
        }
    }

    private void initAnimators() {
        leftPickerTranslationXAnimator = ObjectAnimator.ofFloat(rlCenter, "translationX", -valueViewOffset);
        leftPickerTranslationXAnimator.setInterpolator(new FastOutLinearInInterpolator());
        leftPickerTranslationXAnimator.setRepeatMode(ValueAnimator.REVERSE);
        leftPickerTranslationXAnimator.setRepeatCount(1);
        leftPickerTranslationXAnimator.setDuration(animationOffsetLeftDuration);

        rightPickerTranslationXAnimator = ObjectAnimator.ofFloat(rlCenter, "translationX", valueViewOffset);
        rightPickerTranslationXAnimator.setInterpolator(new FastOutLinearInInterpolator());
        rightPickerTranslationXAnimator.setRepeatMode(ValueAnimator.REVERSE);
        rightPickerTranslationXAnimator.setRepeatCount(1);
        rightPickerTranslationXAnimator.setDuration(animationOffsetRightDuration);

        valueDownChangeAnimator = ObjectAnimator.ofFloat(tvValue, "textSize", valueTextSize, valueMinTextSize);
        valueDownChangeAnimator.setDuration(animationDownDuration);
        valueDownChangeAnimator.setRepeatCount(1);
        valueDownChangeAnimator.setRepeatMode(ValueAnimator.REVERSE);

        valueUpChangeAnimator = ObjectAnimator.ofFloat(tvValue, "textSize", valueTextSize, valueMaxTextSize);
        valueUpChangeAnimator.setDuration(animationUpDuration);
        valueUpChangeAnimator.setRepeatCount(1);
        valueUpChangeAnimator.setRepeatMode(ValueAnimator.REVERSE);
    }

    private String formatValue(float value) {
        if(integerPriority && Math.round(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }

        return String.format(Locale.US, valueFormatter, value);
    }

    /**
     * Set picker current value
     * @param value
     */
    public void setPickerValue(float value) {
        if(value < minValue || value > maxValue) {
            return;
        }

        float oldValue = getValue();
        clickNumberPickerListener.onValueChange(oldValue, value, oldValue > value ? PickerClickType.LEFT : PickerClickType.RIGHT);

        final String newValue = formatValue(value);
        tvValue.setText(newValue);
        tvValue.setSelection(newValue.length());
    }

    /**
     * Set step value
     * @param step
     */
    public void setStepValue(float step) {
        this.step = step;
    }

    /**
     * Get picker's current value directly instead through listener {@link ClickNumberPickerListener}
     * @return current picker value
     */
    public float getValue(){
        // Checando se esta em branco
        if ( tvValue.getText().length() <= 0 ) return 0f;

        float oldValue = Float.parseFloat(tvValue.getText().toString());
        return oldValue;
    }

    /**
     * Update current picker value by provided step
     * @param step
     */
    public void updatePickerValueByStep(float step) {
        float oldValue = getValue();
        if(oldValue + step < minValue) {
            setPickerValue(minValue);
        } else if (oldValue + step > maxValue) {
            setPickerValue(maxValue);
        }

        setPickerValue(oldValue + step);
    }

    /**
     * Set picker number value change listener
     * @param clickNumberPickerListener
     */
    public void setClickNumberPickerListener(ClickNumberPickerListener clickNumberPickerListener) {
        this.clickNumberPickerListener = clickNumberPickerListener;
    }

    /**
     * Get current mininum picker value
     * @return current mininum value
     */
    public float getMinValue() {
        return minValue;
    }

    /**
     * Set minimum picker value
     * @param minValue new minimum value
     */
    public void setMinValue(float minValue) {
        this.minValue = minValue;
        float oldValue = getValue();
        if ( oldValue < minValue ) {
            tvValue.setText(formatValue(minValue));
        }
    }

    /**
     * Get current maximum picker value
     * @return current maximum value
     */
    public float getMaxValue() {
        return maxValue;
    }

    /**
     * Set maximum picker value
     * @param maxValue new maximum value
     */
    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
        float oldValue = getValue();
        if ( oldValue > maxValue ) {
            tvValue.setText(formatValue(maxValue));
        }
    }

    /**
     * Get current decimal numbers value
     * @return current decimal number value
     */
    public int getDecimalNumbers() {
        return decimalNumbers;
    }

    /**
     * Set new decimal number value
     * @param decimalNumbers new Decimal Number value
     */
    public void setDecimalNumbers(int decimalNumbers) {
        this.decimalNumbers = decimalNumbers;
        this.valueFormatter = NumberFormatUtils.provideFloatFormater(decimalNumbers);
        float oldValue = getValue();
        tvValue.setText(formatValue(oldValue));

        // Definindo o modo do teclado
        if ( decimalNumbers > 0 ) {
            this.inputFitlerPattern = Pattern.compile("[0-9]{0,}+((\\.[0-9]{0," + (decimalNumbers-1) + "})?)||(\\.)?");
            tvValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            this.inputFitlerPattern = Pattern.compile("[0-9]{0,}");
            tvValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }

    // Class to implement decimal filter
    private class DecimalInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if ( inputFitlerPattern == null ) return null;
            else if ( !inputFitlerPattern.matcher(dest.toString()).matches()) return "";
            else return null;
        }
    }
}

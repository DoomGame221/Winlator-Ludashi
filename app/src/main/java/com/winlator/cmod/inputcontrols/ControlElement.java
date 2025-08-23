package com.winlator.cmod.inputcontrols;



import android.graphics.Bitmap;

import android.graphics.Canvas;

import android.graphics.Paint;

import android.graphics.Path;

import android.graphics.PointF;

import android.graphics.Rect;



import androidx.core.graphics.ColorUtils;



import com.winlator.cmod.core.CubicBezierInterpolator;

import com.winlator.cmod.math.Mathf;

import com.winlator.cmod.widget.InputControlsView;

import com.winlator.cmod.widget.TouchpadView;

import com.winlator.cmod.winhandler.MouseEventFlags;

import com.winlator.cmod.xserver.XServer;



import org.json.JSONArray;

import org.json.JSONException;

import org.json.JSONObject;



import java.util.Arrays;



public class ControlElement {

    public static final float STICK_DEAD_ZONE = 0.15f;

    public static final float DPAD_DEAD_ZONE = 0.3f;

    public static final float STICK_SENSITIVITY = 3.0f;

    public static final float TRACKPAD_MIN_SPEED = 0.8f;

    public static final float TRACKPAD_MAX_SPEED = 20.0f;

    public static final byte TRACKPAD_ACCELERATION_THRESHOLD = 4;

    public static final short BUTTON_MIN_TIME_TO_KEEP_PRESSED = 300;

    public enum Type {

        BUTTON, D_PAD, RANGE_BUTTON, STICK, TRACKPAD;



        public static String[] names() {

            Type[] types = values();

            String[] names = new String[types.length];

            for (int i = 0; i < types.length; i++) names[i] = types[i].name().replace("_", "-");

            return names;

        }

    }

    public enum Shape {

        CIRCLE, RECT, ROUND_RECT, SQUARE;



        public static String[] names() {

            Shape[] shapes = values();

            String[] names = new String[shapes.length];

            for (int i = 0; i < shapes.length; i++) names[i] = shapes[i].name().replace("_", " ");

            return names;

        }

    }

    public enum Range {

        FROM_A_TO_Z(26), FROM_0_TO_9(10), FROM_F1_TO_F12(12), FROM_NP0_TO_NP9(10);

        public final byte max;



        Range(int max) {

            this.max = (byte)max;

        }



        public static String[] names() {

            Range[] ranges = values();

            String[] names = new String[ranges.length];

            for (int i = 0; i < ranges.length; i++) names[i] = ranges[i].name().replace("_", " ");

            return names;

        }

    }

    private final InputControlsView inputControlsView;

    private Type type = Type.BUTTON;

    private Shape shape = Shape.CIRCLE;

    private Binding[] bindings = {Binding.NONE, Binding.NONE, Binding.NONE, Binding.NONE};

    private float scale = 1.0f;

    private short x;

    private short y;

    private boolean selected = false;

    private boolean toggleSwitch = false;

    private int currentPointerId = -1;

    private final Rect boundingBox = new Rect();

    private boolean[] states = new boolean[4];

    private boolean boundingBoxNeedsUpdate = true;

    private String text = "";

    private byte iconId;

    private Range range;

    private byte orientation;

    private PointF currentPosition;

    private RangeScroller scroller;

    private CubicBezierInterpolator interpolator;

    private Object touchTime;



    private final PointF touchDownOrigin = new PointF();





    public ControlElement(InputControlsView inputControlsView) {

        this.inputControlsView = inputControlsView;

    }



    private void reset() {

        setBinding(Binding.NONE);

        scroller = null;



        if (type == Type.D_PAD || type == Type.STICK) {

            bindings[0] = Binding.KEY_W;

            bindings[1] = Binding.KEY_D;

            bindings[2] = Binding.KEY_S;

            bindings[3] = Binding.KEY_A;

        }

        else if (type == Type.TRACKPAD) {

            bindings[0] = Binding.MOUSE_MOVE_UP;

            bindings[1] = Binding.MOUSE_MOVE_RIGHT;

            bindings[2] = Binding.MOUSE_MOVE_DOWN;

            bindings[3] = Binding.MOUSE_MOVE_LEFT;

        }

        else if (type == Type.RANGE_BUTTON) {

            scroller = new RangeScroller(inputControlsView, this);

        }



        text = "";

        iconId = 0;

        range = null;

        boundingBoxNeedsUpdate = true;

    }



    public Type getType() {

        return type;

    }



    public void setType(Type type) {

        this.type = type;

        reset();

    }



    public int getBindingCount() {

        return bindings.length;

    }



    public void setBindingCount(int bindingCount) {

        bindings = new Binding[bindingCount];

        setBinding(Binding.NONE);

        states = new boolean[bindingCount];

        boundingBoxNeedsUpdate = true;

    }



    public Shape getShape() {

        return shape;

    }



    public void setShape(Shape shape) {

        this.shape = shape;

        boundingBoxNeedsUpdate = true;

    }



    public Range getRange() {

        return range != null ? range : Range.FROM_A_TO_Z;

    }



    public void setRange(Range range) {

        this.range = range;

    }



    public byte getOrientation() {

        return orientation;

    }



    public void setOrientation(byte orientation) {

        this.orientation = orientation;

        boundingBoxNeedsUpdate = true;

    }



    public boolean isToggleSwitch() {

        return toggleSwitch;

    }



    public void setToggleSwitch(boolean toggleSwitch) {

        this.toggleSwitch = toggleSwitch;

    }



    public Binding getBindingAt(int index) {

        return index < bindings.length ? bindings[index] : Binding.NONE;

    }



    public void setBindingAt(int index, Binding binding) {

        if (index >= bindings.length) {

            int oldLength = bindings.length;

            bindings = Arrays.copyOf(bindings, index+1);

            Arrays.fill(bindings, oldLength-1, bindings.length, Binding.NONE);

            states = new boolean[bindings.length];

            boundingBoxNeedsUpdate = true;

        }

        bindings[index] = binding;

    }



    public void setBinding(Binding binding) {

        Arrays.fill(bindings, binding);

    }



    public float getScale() {

        return scale;

    }



    public void setScale(float scale) {

        this.scale = scale;

        boundingBoxNeedsUpdate = true;

    }



    public short getX() {

        return x;

    }



    public void setX(int x) {

        this.x = (short)x;

        boundingBoxNeedsUpdate = true;

    }



    public short getY() {

        return y;

    }



    public void setY(int y) {

        this.y = (short)y;

        boundingBoxNeedsUpdate = true;

    }



    public boolean isSelected() {

        return selected;

    }



    public void setSelected(boolean selected) {

        this.selected = selected;

    }



    public String getText() {

        return text;

    }



    public void setText(String text) {

        this.text = text != null ? text : "";

    }



    public byte getIconId() {

        return iconId;

    }



    public void setIconId(int iconId) {

        this.iconId = (byte)iconId;

    }



    public Rect getBoundingBox() {

        if (boundingBoxNeedsUpdate) computeBoundingBox();

        return boundingBox;

    }



    private Rect computeBoundingBox() {

        int snappingSize = inputControlsView.getSnappingSize();

        int halfWidth = 0;

        int halfHeight = 0;



        switch (type) {

            case BUTTON:

                switch (shape) {

                    case RECT:

                    case ROUND_RECT:

                        halfWidth = snappingSize * 4;

                        halfHeight = snappingSize * 2;

                        break;

                    case SQUARE:

                        halfWidth = (int)(snappingSize * 2.5f);

                        halfHeight = (int)(snappingSize * 2.5f);

                        break;

                    case CIRCLE:

                        halfWidth = snappingSize * 3;

                        halfHeight = snappingSize * 3;

                        break;

                }

                break;

            case D_PAD: {

                halfWidth = snappingSize * 7;

                halfHeight = snappingSize * 7;

                break;

            }

            case TRACKPAD:

            case STICK: {

                halfWidth = snappingSize * 6;

                halfHeight = snappingSize * 6;

                break;

            }

            case RANGE_BUTTON: {

                halfWidth = snappingSize * ((bindings.length * 4) / 2);

                halfHeight = snappingSize * 2;



                if (orientation == 1) {

                    int tmp = halfWidth;

                    halfWidth = halfHeight;

                    halfHeight = tmp;

                }

                break;

            }

        }



        halfWidth *= scale;

        halfHeight *= scale;

        boundingBox.set(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight);

        boundingBoxNeedsUpdate = false;

        return boundingBox;

    }







    private String getDisplayText() {

        if (text != null && !text.isEmpty()) {

            return text;

        }

        else {

            Binding binding = getBindingAt(0);

            String text = binding.toString().replace("NUMPAD ", "NP").replace("BUTTON ", "");

            if (text.length() > 7) {

                String[] parts = text.split(" ");

                StringBuilder sb = new StringBuilder();

                for (String part : parts) sb.append(part.charAt(0));

                return (binding.isMouse() ? "M" : "")+ sb;

            }

            else return text;

        }

    }



    private static float getTextSizeForWidth(Paint paint, String text, float desiredWidth) {

        final byte testTextSize = 48;

        paint.setTextSize(testTextSize);

        return testTextSize * desiredWidth / paint.measureText(text);

    }



    private static String getRangeTextForIndex(Range range, int index) {

        String text = "";

        switch (range) {

            case FROM_A_TO_Z:

                text = String.valueOf((char)(65 + index));

                break;

            case FROM_0_TO_9:

                text = String.valueOf((index + 1) % 10);

                break;

            case FROM_F1_TO_F12:

                text = "F"+(index + 1);

                break;

            case FROM_NP0_TO_NP9:

                text = "NP"+((index + 1) % 10);

                break;

        }

        return text;

    }



    public void draw(Canvas canvas) {

        int snappingSize = inputControlsView.getSnappingSize();

        Paint paint = inputControlsView.getPaint();

        int primaryColor = inputControlsView.getPrimaryColor();



        int fillColor = ColorUtils.setAlphaComponent(primaryColor, 70);



        paint.setColor(selected ? inputControlsView.getSecondaryColor() : primaryColor);

        paint.setStyle(Paint.Style.STROKE);

        float strokeWidth = snappingSize * 0.25f;

        paint.setStrokeWidth(strokeWidth);

        Rect boundingBox = getBoundingBox();



        switch (type) {

            case BUTTON: {

                float cx = boundingBox.centerX();

                float cy = boundingBox.centerY();



                if (isEngaged()) {

                    paint.setStyle(Paint.Style.FILL);

                    paint.setColor(fillColor);

                    switch (shape) {

                        case CIRCLE:

                            canvas.drawCircle(cx, cy, boundingBox.width() * 0.5f, paint);

                            break;

                        case RECT:

                            canvas.drawRect(boundingBox, paint);

                            break;

                        case ROUND_RECT: {

                            float r = boundingBox.height() * 0.5f;

                            canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, r, r, paint);

                            break;

                        }

                        case SQUARE: {

                            float r = snappingSize * 0.75f * scale;

                            canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, r, r, paint);

                            break;

                        }

                    }

                }



                paint.setStyle(Paint.Style.STROKE);

                paint.setColor(selected ? inputControlsView.getSecondaryColor() : primaryColor);

                paint.setStrokeWidth(strokeWidth);



                switch (shape) {

                    case CIRCLE:

                        canvas.drawCircle(cx, cy, boundingBox.width() * 0.5f, paint);

                        break;

                    case RECT:

                        canvas.drawRect(boundingBox, paint);

                        break;

                    case ROUND_RECT: {

                        float r = boundingBox.height() * 0.5f;

                        canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, r, r, paint);

                        break;

                    }

                    case SQUARE: {

                        float r = snappingSize * 0.75f * scale;

                        canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, r, r, paint);

                        break;

                    }

                }





                if (iconId > 0) {

                    drawIcon(canvas, cx, cy, boundingBox.width(), boundingBox.height(), iconId);

                }

                else {

                    String text = getDisplayText();

                    paint.setTextSize(Math.min(getTextSizeForWidth(paint, text, boundingBox.width() - strokeWidth * 2), snappingSize * 2 * scale));

                    paint.setTextAlign(Paint.Align.CENTER);

                    paint.setStyle(Paint.Style.FILL);

                    paint.setColor(primaryColor);

                    canvas.drawText(text, x, (y - ((paint.descent() + paint.ascent()) * 0.5f)), paint);

                }

                break;

            }

            case D_PAD: {

                float cx = boundingBox.centerX();

                float cy = boundingBox.centerY();

                float offsetX = snappingSize * 2 * scale;

                float offsetY = snappingSize * 3 * scale;

                float start = snappingSize * scale;

                Path path = inputControlsView.getPath();

                path.reset();



                path.moveTo(cx, cy - start);

                path.lineTo(cx - offsetX, cy - offsetY);

                path.lineTo(cx - offsetX, boundingBox.top);

                path.lineTo(cx + offsetX, boundingBox.top);

                path.lineTo(cx + offsetX, cy - offsetY);

                path.close();



                path.moveTo(cx - start, cy);

                path.lineTo(cx - offsetY, cy - offsetX);

                path.lineTo(boundingBox.left, cy - offsetX);

                path.lineTo(boundingBox.left, cy + offsetX);

                path.lineTo(cx - offsetY, cy + offsetX);

                path.close();



                path.moveTo(cx, cy + start);

                path.lineTo(cx - offsetX, cy + offsetY);

                path.lineTo(cx - offsetX, boundingBox.bottom);

                path.lineTo(cx + offsetX, boundingBox.bottom);

                path.lineTo(cx + offsetX, cy + offsetY);

                path.close();



                path.moveTo(cx + start, cy);

                path.lineTo(cx + offsetY, cy - offsetX);

                path.lineTo(boundingBox.right, cy - offsetX);

                path.lineTo(boundingBox.right, cy + offsetX);

                path.lineTo(cx + offsetY, cy + offsetX);

                path.close();



                // -- FILL first if engaged

                if (isEngaged()) {

                    paint.setStyle(Paint.Style.FILL);

                    paint.setColor(fillColor);

                    canvas.drawPath(path, paint);

                }



                // -- then STROKE (existing)

                paint.setStyle(Paint.Style.STROKE);

                paint.setColor(selected ? inputControlsView.getSecondaryColor() : primaryColor);

                paint.setStrokeWidth(strokeWidth);

                canvas.drawPath(path, paint);

                break;

            }

            case RANGE_BUTTON: {

                Range range = getRange();

                int oldColor = paint.getColor();

                float radius = snappingSize * 0.75f * scale;



                if (isEngaged()) {

                    paint.setStyle(Paint.Style.FILL);

                    paint.setColor(fillColor);

                    canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);

                }



                paint.setStyle(Paint.Style.STROKE);

                paint.setColor(oldColor);

                canvas.drawRoundRect(boundingBox.left, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);



                float elementSize = scroller.getElementSize();

                float minTextSize = snappingSize * 2 * scale;

                float scrollOffset = scroller.getScrollOffset();

                byte[] rangeIndex = scroller.getRangeIndex();

                Path path = inputControlsView.getPath();

                path.reset();



                if (orientation == 0) {

                    float lineTop = boundingBox.top + strokeWidth * 0.5f;

                    float lineBottom = boundingBox.bottom - strokeWidth * 0.5f;

                    float startX = boundingBox.left;

//                    canvas.drawRoundRect(startX, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, paint);



                    canvas.save();

                    path.addRoundRect(startX, boundingBox.top, boundingBox.right, boundingBox.bottom, radius, radius, Path.Direction.CW);

                    canvas.clipPath(path);

                    startX -= scrollOffset % elementSize;



                    for (byte i = rangeIndex[0]; i < rangeIndex[1]; i++) {

                        int index = i % range.max;

                        paint.setStyle(Paint.Style.STROKE);

                        paint.setColor(oldColor);



                        if (startX > boundingBox.left && startX  < boundingBox.right) canvas.drawLine(startX, lineTop, startX, lineBottom, paint);

                        String text = getRangeTextForIndex(range, index);



                        if (startX < boundingBox.right && startX + elementSize > boundingBox.left) {

                            paint.setStyle(Paint.Style.FILL);

                            paint.setColor(primaryColor);

                            paint.setTextSize(Math.min(getTextSizeForWidth(paint, text, elementSize - strokeWidth * 2), minTextSize));

                            paint.setTextAlign(Paint.Align.CENTER);

                            canvas.drawText(text, startX + elementSize * 0.5f, (y - ((paint.descent() + paint.ascent()) * 0.5f)), paint);

                        }

                        startX += elementSize;

                    }



                    paint.setStyle(Paint.Style.STROKE);

                    paint.setColor(oldColor);

                    canvas.restore();

                }

                else {

                    float lineLeft = boundingBox.left + strokeWidth * 0.5f;

                    float lineRight = boundingBox.right - strokeWidth * 0.5f;

                    float startY = boundingBox.top;

//                    canvas.drawRoundRect(boundingBox.left, startY, boundingBox.right, boundingBox.bottom, radius, radius, paint);



                    canvas.save();

                    p

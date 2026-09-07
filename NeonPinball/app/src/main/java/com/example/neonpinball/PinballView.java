package com.example.neonpinball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PinballView extends View {

    // --- PHYSICS CONFIGURATION ---
    private static final float GRAVITY = 1500f;     // Ball acceleration down the playfield
    private static final int SUB_STEPS = 8;         // Micro-steps to stop wall-phasing/tunneling
    private static final float RESTITUTION = 0.7f;   // Bounce elasticity on wall impacts

    // --- BALL STATE ---
    private float ballX = 300f;
    private float ballY = 400f;
    private float ballVX = 200f;
    private float ballVY = 0f;
    private final float ballRadius = 25f;

    // --- FLIPPER STATE ---
    private float leftFlipperAngle = 0.3f;
    private float rightFlipperAngle = -0.3f;
    private boolean isLeftFlipping = false;
    private boolean isRightFlipping = false;

    private long lastTime = System.currentTimeMillis();
    private final Paint paint = new Paint();

    public PinballView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Dynamic Delta-Time Calculation
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000f, 0.033f);
        lastTime = now;

        // Run Sub-Stepped Physics Loop
        updatePhysics(dt);

        // Render Background
        canvas.drawColor(Color.BLACK);

        // Render Ball
        paint.setColor(Color.CYAN);
        canvas.drawCircle(ballX, ballY, ballRadius, paint);

        // Render Flippers
        paint.setColor(Color.MAGENTA);
        paint.setStrokeWidth(18f);

        float leftPivotX = getWidth() * 0.25f;
        float leftPivotY = getHeight() - 200f;
        float rightPivotX = getWidth() * 0.75f;
        float rightPivotY = getHeight() - 200f;
        float flipperLength = 180f;

        // Left Flipper Line
        canvas.drawLine(
            leftPivotX, leftPivotY,
            leftPivotX + (float) Math.cos(leftFlipperAngle) * flipperLength,
            leftPivotY + (float) Math.sin(leftFlipperAngle) * flipperLength,
            paint
        );

        // Right Flipper Line
        canvas.drawLine(
            rightPivotX, rightPivotY,
            rightPivotX - (float) Math.cos(rightFlipperAngle) * flipperLength,
            rightPivotY + (float) Math.sin(rightFlipperAngle) * flipperLength,
            paint
        );

        invalidate();
    }

    private void updatePhysics(float dt) {
        float subDt = dt / SUB_STEPS;
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        // Sub-step loop prevents ball tunneling through fast boundaries
        for (int step = 0; step < SUB_STEPS; step++) {
            // Velocity & Position Update
            ballVY += GRAVITY * subDt;
            ballX += ballVX * subDt;
            ballY += ballVY * subDt;

            // Screen Side Wall Collisions
            if (ballX - ballRadius < 0) {
                ballX = ballRadius;
                ballVX = -ballVX * RESTITUTION;
            } else if (ballX + ballRadius > width) {
                ballX = width - ballRadius;
                ballVX = -ballVX * RESTITUTION;
            }

            // Screen Top and Bottom Collisions
            if (ballY - ballRadius < 0) {
                ballY = ballRadius;
                ballVY = -ballVY * RESTITUTION;
            } else if (ballY + ballRadius > height) {
                // Ball Drain - Reset Position
                ballX = width / 2f;
                ballY = 200f;
                ballVX = 150f;
                ballVY = 0f;
            }

            // Left Flipper Impulse
            float leftPivotX = width * 0.25f;
            float leftPivotY = height - 200f;
            if (Math.hypot(ballX - leftPivotX, ballY - leftPivotY) < ballRadius + 100f) {
                if (isLeftFlipping) {
                    ballVY = -850f; // Dynamic upward kick
                    ballVX += 200f;
                }
            }

            // Right Flipper Impulse
            float rightPivotX = width * 0.75f;
            float rightPivotY = height - 200f;
            if (Math.hypot(ballX - rightPivotX, ballY - rightPivotY) < ballRadius + 100f) {
                if (isRightFlipping) {
                    ballVY = -850f; // Dynamic upward kick
                    ballVX -= 200f;
                }
            }
        }

        // Flipper Motion Smoothing
        float targetLeft = isLeftFlipping ? -0.5f : 0.3f;
        leftFlipperAngle += (targetLeft - leftFlipperAngle) * 0.35f;

        float targetRight = isRightFlipping ? -0.5f : 0.3f;
        rightFlipperAngle += (targetRight - rightFlipperAngle) * 0.35f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < getWidth() / 2f) {
                    isLeftFlipping = true;
                } else {
                    isRightFlipping = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                isLeftFlipping = false;
                isRightFlipping = false;
                break;
        }
        return true;
    }
}

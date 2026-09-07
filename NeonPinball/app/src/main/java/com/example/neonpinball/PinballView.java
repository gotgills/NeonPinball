package com.example.neonpinball;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

public class PinballView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng = new Random();
    private float W,H,s;
    private float bx,by,bvx,bvy,br;
    private boolean leftDown,rightDown,gameOver=false;
    private int score=0,balls=3;
    private long last=System.nanoTime();

    private final float[][] bumpers={{.32f,.30f},{.68f,.30f},{.50f,.48f}};
    private final boolean[] bumperHot={false,false,false};

    public PinballView(Context c){ super(c); setFocusable(true); }

    private void resetBall(){
        bx=W*.82f; by=H*.82f; br=s*.022f;
        bvx=-s*.08f; bvy=-s*.75f;
        gameOver=false;
    }

    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        W=w;H=h;s=Math.min(W,H);
        resetBall();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        long now=System.nanoTime();
        float dt=Math.min(.025f,(now-last)/1_000_000_000f); last=now;
        update(dt);
        drawGame(c);
        postInvalidateOnAnimation();
    }

    private void update(float dt){
        if(gameOver) return;
        float g=s*1.45f;
        bvy += g*dt;
        bx += bvx*dt; by += bvy*dt;

        float left=W*.08f+br, right=W*.92f-br, top=H*.10f+br;
        if(bx<left){ bx=left; bvx=Math.abs(bvx)*.88f; }
        if(bx>right){ bx=right; bvx=-Math.abs(bvx)*.88f; }
        if(by<top){ by=top; bvy=Math.abs(bvy)*.9f; }

        // slanted side rails near drain
        bounceSegment(W*.08f,H*.67f,W*.25f,H*.80f,.88f);
        bounceSegment(W*.92f,H*.67f,W*.75f,H*.80f,.88f);

        // bumpers
        for(int i=0;i<bumpers.length;i++){
            float x=W*bumpers[i][0], y=H*bumpers[i][1], r=s*.07f;
            float dx=bx-x,dy=by-y,d=(float)Math.sqrt(dx*dx+dy*dy);
            if(d<br+r){
                if(d<1)d=1;
                float nx=dx/d,ny=dy/d;
                bx=x+nx*(br+r); by=y+ny*(br+r);
                float speed=Math.max(s*.65f,(float)Math.sqrt(bvx*bvx+bvy*bvy)*1.05f);
                bvx=nx*speed; bvy=ny*speed;
                score+=100; bumperHot[i]=true;
            } else bumperHot[i]=false;
        }

        // center posts
        bounceCircle(W*.38f,H*.71f,s*.024f,20);
        bounceCircle(W*.62f,H*.71f,s*.024f,20);

        // flippers
        float lfY=H*.83f, len=W*.24f;
        float la=(float)Math.toRadians(leftDown?-28:14);
        float ra=(float)Math.toRadians(rightDown?208:166);
        bounceFlipper(W*.38f,lfY,len,la,leftDown,true);
        bounceFlipper(W*.62f,lfY,len,ra,rightDown,false);

        if(by>H+br){
            balls--;
            if(balls<=0){ gameOver=true; balls=0; }
            else resetBall();
        }
    }

    private void bounceCircle(float x,float y,float r,int pts){
        float dx=bx-x,dy=by-y,d=(float)Math.sqrt(dx*dx+dy*dy);
        if(d<br+r){
            if(d<1)d=1; float nx=dx/d,ny=dy/d;
            bx=x+nx*(br+r);by=y+ny*(br+r);
            float dot=bvx*nx+bvy*ny;
            if(dot<0){bvx-=1.9f*dot*nx;bvy-=1.9f*dot*ny;}
            score+=pts;
        }
    }

    private void bounceSegment(float x1,float y1,float x2,float y2,float restitution){
        float vx=x2-x1,vy=y2-y1;
        float t=((bx-x1)*vx+(by-y1)*vy)/(vx*vx+vy*vy);
        t=Math.max(0,Math.min(1,t));
        float px=x1+t*vx,py=y1+t*vy,dx=bx-px,dy=by-py,d=(float)Math.sqrt(dx*dx+dy*dy);
        if(d<br){
            if(d<1){dx=-vy;dy=vx;d=(float)Math.sqrt(dx*dx+dy*dy);}
            float nx=dx/d,ny=dy/d;
            bx=px+nx*br;by=py+ny*br;
            float dot=bvx*nx+bvy*ny;
            if(dot<0){bvx-=(1+restitution)*dot*nx;bvy-=(1+restitution)*dot*ny;}
        }
    }

    private void bounceFlipper(float px,float py,float len,float angle,boolean active,boolean left){
        float x2=px+(float)Math.cos(angle)*len, y2=py+(float)Math.sin(angle)*len;
        float vx=x2-px,vy=y2-py;
        float t=((bx-px)*vx+(by-py)*vy)/(vx*vx+vy*vy);
        t=Math.max(0,Math.min(1,t));
        float qx=px+t*vx,qy=py+t*vy,dx=bx-qx,dy=by-qy,d=(float)Math.sqrt(dx*dx+dy*dy);
        float thick=s*.018f;
        if(d<br+thick){
            if(d<1){dx=-vy;dy=vx;d=(float)Math.sqrt(dx*dx+dy*dy);}
            float nx=dx/d,ny=dy/d;
            bx=qx+nx*(br+thick);by=qy+ny*(br+thick);
            float dot=bvx*nx+bvy*ny;
            if(dot<0){bvx-=1.8f*dot*nx;bvy-=1.8f*dot*ny;}
            if(active){
                bvy-=s*1.05f;
                bvx+=(left?1:-1)*s*.26f;
                score+=10;
            }
        }
    }

    private void drawGame(Canvas c){
        c.drawColor(Color.rgb(5,8,22));
        // table glow bands
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.008f);p.setColor(Color.rgb(0,229,255));
        RectF table=new RectF(W*.07f,H*.09f,W*.93f,H*.98f);c.drawRoundRect(table,s*.06f,s*.06f,p);
        p.setStrokeWidth(s*.003f);p.setColor(Color.rgb(255,64,190));c.drawRoundRect(new RectF(W*.09f,H*.11f,W*.91f,H*.96f),s*.05f,s*.05f,p);

        p.setStyle(Paint.Style.FILL);
        // title / score
        p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(s*.055f);p.setColor(Color.WHITE);c.drawText("NEON PINBALL",W/2,H*.065f,p);
        p.setTextSize(s*.040f);p.setColor(Color.rgb(0,229,255));c.drawText("SCORE  "+score,W*.31f,H*.145f,p);
        p.setColor(Color.rgb(255,215,64));c.drawText("BALLS  "+balls,W*.72f,H*.145f,p);

        // bumpers
        for(int i=0;i<bumpers.length;i++){
            float x=W*bumpers[i][0],y=H*bumpers[i][1],r=s*.07f;
            p.setColor(bumperHot[i]?Color.WHITE:Color.rgb(255,64,190));c.drawCircle(x,y,r,p);
            p.setColor(Color.rgb(20,20,50));c.drawCircle(x,y,r*.62f,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(s*.008f);p.setColor(Color.rgb(0,229,255));c.drawCircle(x,y,r*.82f,p);p.setStyle(Paint.Style.FILL);
        }

        // rails
        p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(s*.018f);p.setColor(Color.rgb(110,100,255));
        c.drawLine(W*.08f,H*.67f,W*.25f,H*.80f,p);c.drawLine(W*.92f,H*.67f,W*.75f,H*.80f,p);

        // posts
        p.setColor(Color.rgb(255,215,64));c.drawCircle(W*.38f,H*.71f,s*.024f,p);c.drawCircle(W*.62f,H*.71f,s*.024f,p);

        // flippers
        float len=W*.24f,fy=H*.83f;
        float la=(float)Math.toRadians(leftDown?-28:14),ra=(float)Math.toRadians(rightDown?208:166);
        p.setStrokeWidth(s*.036f);p.setColor(Color.rgb(0,229,255));
        c.drawLine(W*.38f,fy,W*.38f+(float)Math.cos(la)*len,fy+(float)Math.sin(la)*len,p);
        p.setColor(Color.rgb(255,64,190));
        c.drawLine(W*.62f,fy,W*.62f+(float)Math.cos(ra)*len,fy+(float)Math.sin(ra)*len,p);
        p.setStrokeCap(Paint.Cap.BUTT);

        // ball
        p.setColor(Color.WHITE);c.drawCircle(bx,by,br,p);
        p.setColor(Color.rgb(0,229,255));c.drawCircle(bx-br*.3f,by-br*.3f,br*.25f,p);

        // touch labels
        p.setTextSize(s*.034f);p.setColor(Color.argb(180,255,255,255));p.setTextAlign(Paint.Align.CENTER);
        c.drawText("LEFT",W*.23f,H*.94f,p);c.drawText("RIGHT",W*.77f,H*.94f,p);

        if(gameOver){
            p.setColor(Color.argb(210,5,8,22));c.drawRect(0,H*.32f,W,H*.68f,p);
            p.setColor(Color.WHITE);p.setTextSize(s*.075f);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("GAME OVER",W/2,H*.46f,p);
            p.setTextSize(s*.043f);p.setColor(Color.rgb(0,229,255));c.drawText("FINAL SCORE: "+score,W/2,H*.54f,p);
            p.setTextSize(s*.033f);p.setColor(Color.WHITE);c.drawText("Tap anywhere to restart",W/2,H*.61f,p);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN && gameOver){score=0;balls=3;resetBall();invalidate();return true;}
        leftDown=false;rightDown=false;
        for(int i=0;i<e.getPointerCount();i++){
            float x=e.getX(i),y=e.getY(i);
            if(y>H*.72f){ if(x<W/2) leftDown=true; else rightDown=true; }
        }
        if(e.getActionMasked()==MotionEvent.ACTION_UP || e.getActionMasked()==MotionEvent.ACTION_CANCEL){leftDown=false;rightDown=false;}
        invalidate(); return true;
    }
}

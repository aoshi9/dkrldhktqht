package com.sot.baby.WatBot;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class PowerpointActivity extends AppCompatActivity implements OnGestureListener {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    ImageView ppt1, ppt2, ppt3;
    int count=0;

    private GestureDetector gestureScanner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ppt_activity);
        gestureScanner = new GestureDetector(this);

        ppt1 = (ImageView) findViewById(R.id.ppt1);
        ppt2 = (ImageView) findViewById(R.id.ppt2);
        ppt3 = (ImageView) findViewById(R.id.ppt3);


        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));




        //UI 객체생성
      //  txtText = (TextView)findViewById(R.id.txtText);

        //데이터 가져오기
        Intent intent = getIntent();
        String data = intent.getStringExtra("data");
     //   txtText.setText(data);
    }


    @Override
    public boolean onTouchEvent(MotionEvent me) {
        //바깥레이어 클릭시 안닫히게
        this.gestureScanner.onTouchEvent(me);
         return super.onTouchEvent(me);
    }
    public boolean onDown(MotionEvent e) {
       // viewA.setText("-" + "DOWN" + "-");
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;

            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
              //  Toast.makeText(getApplicationContext(), "Left Swipe", Toast.LENGTH_SHORT).show();
                if (count < 2 ) {
                    count ++;
                }
                changeImage();

            }
            // left to right swipe
            else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
              //  Toast.makeText(getApplicationContext(), "Right Swipe", Toast.LENGTH_SHORT).show();


                if (count > 0 ) {
                    count --;
                }
                changeImage();
            }

        } catch (Exception e) {

        }
        return true;
    }

    private void changeImage(){
        if (count == 0) {
            ppt1.setVisibility(View.VISIBLE);
            ppt2.setVisibility(View.INVISIBLE);
            ppt3.setVisibility(View.INVISIBLE);

        }else if (count == 1){
            ppt1.setVisibility(View.INVISIBLE);
            ppt2.setVisibility(View.VISIBLE);
            ppt3.setVisibility(View.INVISIBLE);

        }else if (count == 2){
            ppt1.setVisibility(View.INVISIBLE);
            ppt2.setVisibility(View.INVISIBLE);
            ppt3.setVisibility(View.VISIBLE);

        }
    }




    //확인 버튼 클릭
    public void mOnClose(View v){
        //데이터 전달하기
        Intent intent = new Intent();
        intent.putExtra("result", "Close Popup");
        setResult(RESULT_OK, intent);

        //액티비티(팝업) 닫기
        finish();
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }

}

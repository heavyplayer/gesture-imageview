package com.polites.android.example;

import com.polites.android.GestureImageView;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;

public class SampleActivity extends Activity {
	
	private GestureImageView view;
	private static final int IMAGE_LOAD = 10001;
	private final Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if(msg.what == IMAGE_LOAD) {
				view.setImageResource(R.drawable.image);
			}
			super.handleMessage(msg);
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        
        view = new GestureImageView(this);
        view.setImageResource(R.drawable.image_small);
        view.setLayoutParams(params);
        
        ViewGroup layout = (ViewGroup) findViewById(R.id.layout);

        layout.addView(view);
        
        handler.sendEmptyMessageDelayed(IMAGE_LOAD, 2000);
    }
}
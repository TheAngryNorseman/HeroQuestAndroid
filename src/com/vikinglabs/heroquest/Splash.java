package com.vikinglabs.heroquest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Splash extends Activity {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        
        Thread splashTimer = new Thread(){
			@Override
			public void run() {
				try{
					
					// Database population or upgrade will execute while splash screen is active for efficiency.
					HQDatabaseAdapter sqLiteAdapter = new HQDatabaseAdapter(Splash.this);
					sqLiteAdapter.openToRead();
					sqLiteAdapter.close();
					
					sleep(1000);
					Intent i = new Intent("com.vikinglabs.heroquest.HOME");
					startActivity(i);
				}catch (InterruptedException e) {
					e.printStackTrace();
				}finally{
					finish();
				}
			}
        };
        
        splashTimer.start();
    }

	@Override
	protected void onPause() {
		super.onPause();
	}
    
    
}
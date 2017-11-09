package com.vikinglabs.heroquest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class SettingsScreen extends Activity{
	
	// Debug filters.
	private static final String SQL = "HQSQL";
	
	// Widgets used in settings_home.xml
	private Button deleteButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_home);
		
        deleteButton = (Button) findViewById(R.id.delete_database_button);
        deleteButton.setEnabled(true);
        deleteButton.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			SettingsScreen.this.deleteDatabase(HQDatabaseAdapter.DATABASE_NAME);
        			Log.v(SQL,"Database Deleted");
        			
        			//TODO Delete SharedPreferences (Gold).
        		}
        		return true;
        	}
        });
	}
}
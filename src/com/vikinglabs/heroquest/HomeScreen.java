package com.vikinglabs.heroquest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class HomeScreen extends Activity{

	// Debug filters.
	private static final String HOME = "Home";
	private static final String MISC = "Misc";
	
	// SharedPreferences used for persistent application data.
	private static final String SAVE_KEY = "hero-quest";
	private SharedPreferences gameState;		
	private SharedPreferences.Editor gameStateEditor;
			
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        gameState = getSharedPreferences(SAVE_KEY, MODE_PRIVATE);
        gameStateEditor = gameState.edit();
        
        Button newGame = (Button) findViewById(R.id.new_game_button);
        newGame.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Starting New Game.");
        			if(!gameState.getBoolean("inProgress", false)){
            			Intent genIntent = new Intent("com.vikinglabs.heroquest.CREATEGAMESCREEN");
    					startActivity(genIntent);
        			}else{
        				AlertDialog.Builder alertDialog = new AlertDialog.Builder(HomeScreen.this);
        		        alertDialog.setMessage("Quit existing game and start a new one?");
        		        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        		            public void onClick(DialogInterface dialog,int which) {
        		            	gameStateEditor.putBoolean("inProgress", false);
        		            	gameStateEditor.commit();
        		            	Intent genIntent = new Intent("com.vikinglabs.heroquest.CREATEGAMESCREEN");
            					startActivity(genIntent);
        		            }
        		        });
        		        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
        		            public void onClick(DialogInterface dialog, int which) {
        		            }
        		        });
        		        alertDialog.show();
        			}
        		}
        		return true;
        	}
        });
       
        
        Button resumeGame = (Button) findViewById(R.id.resume_game_button);
        resumeGame.setEnabled(gameState.getBoolean("inProgress", false));
        resumeGame.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Resuming Game.");
        			Intent genIntent = new Intent("com.vikinglabs.heroquest.GAME");
        			startActivity(genIntent);
        		}
        		return true;
        	}
        });
        
        
        // Multiplayer to be implemented in a future release.
        Button multiplayerGame = (Button) findViewById(R.id.multiplayer_button);
        multiplayerGame.setEnabled(false);
        multiplayerGame.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Starting Multiplayer Game.");
        		}
        		return true;
        	}
        });
        
        
        Button store = (Button) findViewById(R.id.store_button);
        store.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Going to the Armory.");
        			Intent genIntent = new Intent("com.vikinglabs.heroquest.ARMORY");
        			startActivity(genIntent);
        		}
        		return true;
        	}
        });
        
        Button achievements = (Button) findViewById(R.id.achievements_button);
        achievements.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Opening Achievements List.");
        			Intent genIntent = new Intent("com.vikinglabs.heroquest.ACHIEVEMENTS");
        			startActivity(genIntent);
        		}
        		return true;
        	}
        });
        
        Button statistics = (Button) findViewById(R.id.statistics_button);
        statistics.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Opening Statistics Page.");
        			Intent genIntent = new Intent("com.vikinglabs.heroquest.STATISTICS");
        			startActivity(genIntent);
        		}
        		return true;
        	}
        });
        
        Button settings = (Button) findViewById(R.id.settings_button);
        settings.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Opening Settings Menu.");
        			Intent genIntent = new Intent("com.vikinglabs.heroquest.SETTINGS");
        			startActivity(genIntent);
        		}
        		return true;
        	}
        });
        
        Button exitGame = (Button) findViewById(R.id.exit_button);
        exitGame.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
            		Log.v(HOME,"Exiting Game.");
            		finish();
        		}
        		return true;
        	}
        });
    }

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		Button resumeGame = (Button) findViewById(R.id.resume_game_button);
		resumeGame.setEnabled(gameState.getBoolean("inProgress", false));
	}

	@Override
	public void finish() {
		super.finish();
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		Log.v(MISC,"HomeScreen is Finalized!");
		super.finalize();
	}
}
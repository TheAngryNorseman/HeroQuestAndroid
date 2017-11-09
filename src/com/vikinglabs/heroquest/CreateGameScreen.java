package com.vikinglabs.heroquest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Gallery;
import android.widget.Spinner;
import android.widget.TextView;

public class CreateGameScreen extends Activity{

	// Debug filters.
	private static final String HOME = "Home";
	private static final String MISC = "Misc";

	// CreateGameScreen finals.
	private final String[] DIFFICULTIES = {"Easy", "Normal","Hard","Insane"};
	private final int NORMAL_INDEX = 1;
	
	// Bundle variables for thread lifecycle state changes.
	private static final String SAVE_KEY = "hero-quest";
	private SharedPreferences gameState;
	private SharedPreferences.Editor gameStateEditor;
	private HQDatabaseAdapter sqLiteAdapter;
	private Cursor sqCursor;
	
		
	/** A widget from create_game.xml. It is allowed to be a class field only because
	 * the class (activity) is never held on the stack. It is either finished when starting
	 * the actual game or popped off the stack when the back button is pressed. */
	private Gallery questGallery;
	private Spinner difficultySpinner;
	private Button startGameButton;
	private CheckBox barbCheckBox;
	private CheckBox dwarfCheckBox;
	private CheckBox elfCheckBox;
	private CheckBox wizardCheckBox;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set the view to allow widget use.
		setContentView(R.layout.create_game);
		
		
		// The database exists as soon as the application runs once.
		sqLiteAdapter = new HQDatabaseAdapter(this);
		sqLiteAdapter.openToRead();
		sqCursor = sqLiteAdapter.getCompletedQuests();
		
		// Set up the quest gallery.
		questGallery = (Gallery) findViewById(R.id.gallery1);
		questGallery.setAdapter(new QuestAdapter(this, sqCursor));
		
		
		// Boolean values for game creation.
		barbCheckBox = (CheckBox) findViewById(R.id.barb_checkbox);
		dwarfCheckBox = (CheckBox) findViewById(R.id.dwarf_checkbox);
		elfCheckBox = (CheckBox) findViewById(R.id.elf_checkbox);
		wizardCheckBox = (CheckBox) findViewById(R.id.wizard_checkbox);
       
		
		// Set up the AI difficulty setting.
		difficultySpinner = (Spinner) findViewById(R.id.difficulty_spinner);
		difficultySpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, DIFFICULTIES));
		difficultySpinner.setSelection(NORMAL_INDEX);
		
        
        // Starts the currently selected quest using current options configuration.
		startGameButton = (Button) findViewById(R.id.start_game_button);
        startGameButton.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me){
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(HOME,"Starting Quest Index -> " + Integer.toString(questGallery.getSelectedItemPosition()));
        			
        			gameState = getSharedPreferences(SAVE_KEY, MODE_PRIVATE);
        			gameStateEditor = gameState.edit();
        			gameStateEditor.putBoolean("barb_boolean", barbCheckBox.isChecked());
        			gameStateEditor.putBoolean("dwarf_boolean", dwarfCheckBox.isChecked());
        			gameStateEditor.putBoolean("elf_boolean", elfCheckBox.isChecked());
        			gameStateEditor.putBoolean("wizard_boolean", wizardCheckBox.isChecked());
        			gameStateEditor.putInt("quest_index", questGallery.getSelectedItemPosition());
        			gameStateEditor.putInt("difficulty", difficultySpinner.getSelectedItemPosition());
        			gameStateEditor.commit();
        			
        			Intent genIntent = new Intent("com.vikinglabs.heroquest.GAME");
					startActivity(genIntent);
				
					finish();
        		}
        		return true;
        	}
        });        
	}
	
	@Override
	public void finish() {
		sqCursor.close();
		sqLiteAdapter.close();
		super.finish();
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
	}




	@Override
	protected void onResume() {
		super.onResume();
	}







	private class QuestAdapter extends BaseAdapter {
		private final Cursor cursor;
				
		private QuestAdapter(Context con, Cursor cur){
			cursor = cur;
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View rowView = inflater.inflate(R.layout.quest_entry, parent, false);
			TextView titleTextView = (TextView) rowView.findViewById(R.id.title_text_view);
			TextView descriptionTextView = (TextView) rowView.findViewById(R.id.story_text_view);
			
			cursor.moveToPosition(position);
			titleTextView.setText(cursor.getString(HQDatabaseAdapter.QUEST_TITLE_COLUMN_OFFSET));
			descriptionTextView.setText(cursor.getString(HQDatabaseAdapter.QUEST_STORY_COLUMN_OFFSET));

			return rowView;
		}
		
		public int getCount() {
			return cursor.getCount();
		}

		public Object getItem(int position) {
			return null;
		}

		public long getItemId(int position) {
			return 0;
		}
	}


	@Override
	protected void finalize() throws Throwable {
		Log.v(MISC,"CreateGameScreen is Finalized!");
		super.finalize();
	}
	
	
	
}

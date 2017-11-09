package com.vikinglabs.heroquest;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class AchievementsScreen extends Activity{
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// The database exists as soon as the application runs once.
		HQDatabaseAdapter sqLiteAdapter = new HQDatabaseAdapter(this);
		sqLiteAdapter.openToRead();
		Cursor sqCursor = sqLiteAdapter.getAllAchievements();
		
		// Set the view to allow widget use.
		setContentView(R.layout.achievements);
		
		// If achievements exist, populate the ScrollView and assign a listener to it.
		if(sqCursor.getCount() > 0){
			AchievementAdapter achievementAdapter = new AchievementAdapter(sqCursor);
			ListView achievementListView = (ListView) findViewById(R.id.achievements_list_view);
			achievementListView.setAdapter(achievementAdapter);
		}
		sqLiteAdapter.close();
	}
	
	
	/** AchievementAdapter is a custom ArrayAdapter used by the ListView to show the custom XML layout
	 * for each row. It contains a title, description, a date completed (if completed), and a gold or gray
	 * star depending on whether the achievement has been obtained. New instances should pass the context 
	 * as well as a Cursor to the achievement table in the global database.	 */
	private class AchievementAdapter extends ArrayAdapter<String> {
		private final Cursor cursor;
		
		private AchievementAdapter(Cursor cursor){
			super(getApplicationContext(), R.layout.achievement_entry);
			this.cursor = cursor;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View rowView = inflater.inflate(R.layout.achievement_entry, parent, false);
			
			TextView titleTextView = (TextView) rowView.findViewById(R.id.title);
			TextView descriptionTextView = (TextView) rowView.findViewById(R.id.description);
			TextView dateTextView = (TextView) rowView.findViewById(R.id.achieved_date);
			ImageView achievedImageView = (ImageView) rowView.findViewById(R.id.achieved_image);
			
			cursor.moveToPosition(position);
			titleTextView.setText(cursor.getString(HQDatabaseAdapter.ACHIEVEMENT_TITLE_COLUMN_OFFSET));
			descriptionTextView.setText(cursor.getString(HQDatabaseAdapter.ACHIEVEMENT_DESCRIPTION_COLUMN_OFFSET));
			if(cursor.getInt(HQDatabaseAdapter.ACHIEVEMENT_ACHIEVED_COLUMN_OFFSET) == 1){
				achievedImageView.setImageDrawable(getResources().getDrawable(R.drawable.gold_star));
				dateTextView.setText(cursor.getString(HQDatabaseAdapter.ACHIEVEMENT_DATE_COLUMN_OFFSET));
			}else{
				achievedImageView.setImageDrawable(getResources().getDrawable(R.drawable.gray_star));
				dateTextView.setText("");
			}

			return rowView;
		}
		
		@Override
		public int getCount() {
			return cursor.getCount();
		}
	}
}
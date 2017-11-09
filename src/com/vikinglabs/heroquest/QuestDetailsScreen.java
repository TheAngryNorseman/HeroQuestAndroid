package com.vikinglabs.heroquest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;


public class QuestDetailsScreen extends Activity{

	// General purpose variables.
	Intent genIntent;
	String title, description;
	
	TextView titleTextView, descriptionTextView, objectivesTextView, statisticsTextView;
	TabHost tabs;
	TabSpec descriptionTab, objectivesTab, statisticsTab;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		genIntent = getIntent();
		title = genIntent.getStringExtra("title");
		description = genIntent.getStringExtra("extended");
		
		setContentView(R.layout.quest_details);
		
		titleTextView = (TextView) findViewById(R.id.title_text_view);
		titleTextView.setText(title);
		
		tabs = (TabHost) findViewById(android.R.id.tabhost);
		tabs.setup();
		
		descriptionTab = tabs.newTabSpec("Description");
		descriptionTab.setIndicator("Description");
		descriptionTab.setContent(R.id.description_scroll_view);
		objectivesTab = tabs.newTabSpec("Objectives");
		objectivesTab.setIndicator("Objectives");
		objectivesTab.setContent(R.id.objectives_scroll_view);
		statisticsTab = tabs.newTabSpec("Statistics");
		statisticsTab.setIndicator("Statistics");
		statisticsTab.setContent(R.id.statistics_scroll_view);
		
		tabs.addTab(descriptionTab);
		tabs.addTab(objectivesTab);
		tabs.addTab(statisticsTab);
		
		descriptionTextView = (TextView) findViewById(R.id.description_text_view);
		
		
		descriptionTextView.setText(description);
	}
	
	
	
	
}
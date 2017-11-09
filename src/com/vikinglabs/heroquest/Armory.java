package com.vikinglabs.heroquest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;


public class Armory extends Activity{

	TabHost tabs;
	TabSpec weaponsTab, armorTab, otherTab;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.armory_home);
		
		tabs = (TabHost) findViewById(android.R.id.tabhost);
		tabs.setup();
		
		weaponsTab = tabs.newTabSpec("Weapons");
		weaponsTab.setIndicator("", getResources().getDrawable(R.drawable.sword));
		weaponsTab.setContent(R.id.tab1);
		armorTab = tabs.newTabSpec("Armor");
		armorTab.setIndicator("", getResources().getDrawable(R.drawable.shield));
		armorTab.setContent(R.id.tab2);
		otherTab = tabs.newTabSpec("Other");
		otherTab.setIndicator("", getResources().getDrawable(R.drawable.potion));
		otherTab.setContent(R.id.tab3);
		
		tabs.addTab(weaponsTab);
		tabs.addTab(armorTab);
		tabs.addTab(otherTab);
	}
	
}
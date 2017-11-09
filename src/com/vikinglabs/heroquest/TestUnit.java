package com.vikinglabs.heroquest;

import android.util.Log;

public class TestUnit{
	
	// Debug filters.
	private static final String UNIT = "Unit";
	
	// Unit types.
	public static final int HERO = 0;
	public static final int MONSTER = 1;
	public static final int DOODAD = 2;
	
	// Hero classes.
	public static final int BARBARIAN = 0;
	public static final int DWARF = 1;
	public static final int ELF = 2;
	public static final int WIZARD = 3;
	
	
	// Instantiation variables.
	public int unitType, unitClass;
	
	public int room, action, direction;
	public float xPixel, yPixel;
	public int bodyCurrent, bodyMax, mindCurrent, mindMax, movesLeft, moveDice, attackDice, defendDice, actionsRemaining, actionsPerRound;
	
	public int isEnabled; 		// Must be an int for storing in SQL. (Traveling must also be).
	
	// Variables used by roll methods.
	private int rollCounter;
	private float rollValue;
	
	public TestUnit(int uType, int uClass){
		unitType = uType;
		unitClass = uClass;
		switch(unitType){
		case HERO:
			switch(unitClass){
			case BARBARIAN:
				bodyCurrent = bodyMax = 8;
				mindCurrent = mindMax = 2;
				attackDice = 3;
				defendDice = 2;
				break;
			case DWARF:
				bodyCurrent = bodyMax = 7;
				mindCurrent = mindMax = 3;
				attackDice = 2;
				defendDice = 2;
				break;
			case ELF:
				bodyCurrent = bodyMax = 4;
				mindCurrent = mindMax = 6;
				attackDice = 2;
				defendDice = 2;
				break;
			case WIZARD:
				bodyCurrent = bodyMax = 4;
				mindCurrent = mindMax = 6;
				attackDice = 1;
				defendDice = 2;
				break;
			}
			moveDice = 2;
			movesLeft = refreshMoves();
			actionsRemaining = actionsPerRound = 1;
			break;
		case MONSTER:
			break;
		case DOODAD:
			break;
		}
		isEnabled = 1;
	}
	
	public void disable(){
		isEnabled = 0;
	}
	
	public void enable(){
		isEnabled = 1;
	}
	
	public boolean isEnabled(){
		if(isEnabled == 1){
			return true;
		}else{
			return false;
		}
	}
	
	public void setLocationInformation(int x, int y, int dir, int r, int a){
		room = r;
		// existLoc = new Tile(y, x);
		direction = dir;
		action = a;
	}
	
	public int attack(){
		rollCounter = 0;
		for(int i = 0; i < attackDice; i++){
			rollValue = (float)Math.random() * 6;
			if(rollValue < 3){
				rollCounter++;
			}
		}
		Log.v(UNIT,"Attacker rolled " + Integer.toString(rollCounter) + " skulls.");
		//Draw the skull bitmaps here for a thread timer amount of time.
		// Use a toast.
		return rollCounter;
	}
	
	public int defend(){
		rollCounter = 0;
		for(int i = 0; i < defendDice; i++){
			rollValue = (float)Math.random() * 6;
			if(unitType == HERO && rollValue < 2){
				rollCounter++;
			}else if(unitType == MONSTER && rollValue < 1){
				rollCounter++;
			}else{
				Log.e(UNIT,"An unknown class attempted to roll for defend.");
			}
		}
		Log.v(UNIT,"Defender rolled " + Integer.toString(rollCounter) + " shields to defend.");
		//Draw the shield bitmaps here for a thread timer amount of time.
		// Use a toast.
		return rollCounter;
	}
	
	public int refreshMoves(){
		rollCounter = 0;
		for(int i = 0; i < moveDice; i++){
			rollCounter += Math.round((float)Math.random() * 6);
		}
		return rollCounter;
	}
}
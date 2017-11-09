package com.vikinglabs.heroquest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.vikinglabs.heroquest.Board.Unit;

public class Game extends Activity{
	
	// Debug filters.
	private static final String GAME = "Game";
	private static final String BOARD = "Board";
	private static final String COMBAT = "Combat";
	private static final String MISC = "Misc";
	
	// Game finals.
	private static final String[] DIFFICULTIES = {"Easy", "Normal","Hard","Insane"};
	
	// Unit action states.
	private static final int INACTIVE = 0;
	private static final int SELECTED = 1;
	private static final int MOVING = 2;
	private static final int ATTACKING = 3;
	private static final int DYING = 4;
	private static final int SEARCHING = 5;
	
	// Bundle and database variables for thread lifecycle state changes.
	private static final String SAVE_KEY = "hero-quest";
	private SharedPreferences gameState;
	private SharedPreferences.Editor gameStateEditor;
	private HQDatabaseAdapter sqLiteAdapter;
	private Cursor sqCursor;
	
	// Board dimensions.
	private static final int DEFAULT_TILE_SIZE = 48;
	
	// Board objects.
	private Board hqBoard;
	
	private Button storeButton;
	private TextView barbHPTextView, dwarfHPTextView, elfHPTextView, wizardHPTextView;
	private TextView barbMPTextView, dwarfMPTextView, elfMPTextView, wizardMPTextView;
	private TextView barbMoveTextView, dwarfMoveTextView, elfMoveTextView, wizardMoveTextView;
	
	private boolean inventoryOpenFlag;
	
	public interface OnGoldChangeListener {
		public abstract void onGoldChange(int goldValue);
	}
	
	public interface OnStatChangeListener {
		public abstract void onBodyChange(int hero);
		
		public abstract void onMindChange(int hero);
		
		public abstract void onMoveChange(int hero);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.game);
		hqBoard = (Board) findViewById(R.id.board);
		hqBoard.initBoard();
		
		/******************** GET INFORMATION FROM CALLING ACTIVITY ***********************/
		Intent genIntent = getIntent();
		gameState = getSharedPreferences(SAVE_KEY, MODE_PRIVATE);
		
		/* When a Game instance is created, it is either a new game or resuming an existing one. If new
		 * the Shared Preferences are set with what was passed in from CreateGameScreen. If resuming, we
		 * load those values instead. */
		if(!gameState.getBoolean("inProgress", false)){
			Log.v(GAME,"Creating New Game.");
			
			// Add the four heroes.
			hqBoard.heroSet.add(hqBoard.new Unit(Board.Unit.HERO, Board.Unit.BARBARIAN));
			hqBoard.heroSet.add(hqBoard.new Unit(Board.Unit.HERO, Board.Unit.DWARF));
			hqBoard.heroSet.add(hqBoard.new Unit(Board.Unit.HERO, Board.Unit.ELF));
			hqBoard.heroSet.add(hqBoard.new Unit(Board.Unit.HERO, Board.Unit.WIZARD));
			
			// Set enabled heroes from the checkboxes.
			if(!gameState.getBoolean("barb_boolean", true)){
				hqBoard.heroSet.get(Board.Unit.BARBARIAN).disable();
			}
			if(!gameState.getBoolean("dwarf_boolean", true)){
				hqBoard.heroSet.get(Board.Unit.DWARF).disable();
			}
			if(!gameState.getBoolean("elf_boolean", true)){
				hqBoard.heroSet.get(Board.Unit.ELF).disable();
			}
			if(!gameState.getBoolean("wizard_boolean", true)){
				hqBoard.heroSet.get(Board.Unit.WIZARD).disable();
			}
			
			// Set the game in progress so that Resume Game can be used.
			gameStateEditor = gameState.edit();
			gameStateEditor.putBoolean("inProgress", true);
			gameStateEditor.commit();
			
			
			/******************** IMPORT THE QUEST FILE ASSOCIATED WITH SELECTION ***********************/
			sqLiteAdapter = new HQDatabaseAdapter(this);
			sqLiteAdapter.openToRead();
			sqCursor = sqLiteAdapter.getAllQuests();
			if(sqCursor.getCount() > 0){
				sqCursor.moveToPosition(genIntent.getIntExtra("quest_index", 0));
				
				gameStateEditor = gameState.edit();
				gameStateEditor.putString("quest_title", sqCursor.getString(HQDatabaseAdapter.QUEST_TITLE_COLUMN_OFFSET));
				gameStateEditor.commit();
			}else{
				Log.e(GAME, "Error: Did not find quest by ID.");
			}
			
			
			InputStream inputFile = this.getResources().openRawResource(getResources().getIdentifier("quest_" + sqCursor.getString(HQDatabaseAdapter.QUEST_TAG_COLUMN_OFFSET), "raw", getPackageName()));
			InputStreamReader inputReader = new InputStreamReader(inputFile);
			BufferedReader bufferReader = new BufferedReader(inputReader);
			String currentLine;
			String[] currentLineSplit;
			
			sqLiteAdapter.close();
			
			try{
				currentLine = bufferReader.readLine();
				if(currentLine != null){
					currentLineSplit = currentLine.split(",");
					
					hqBoard.numXTiles = Integer.parseInt(currentLineSplit[0]);
					hqBoard.numYTiles = Integer.parseInt(currentLineSplit[1]);
					hqBoard.tileSize = DEFAULT_TILE_SIZE;
					hqBoard.leftEdge = hqBoard.topEdge = 0;
					hqBoard.rightEdge = hqBoard.numXTiles * hqBoard.tileSize;
					hqBoard.bottomEdge = hqBoard.numYTiles * hqBoard.tileSize;
					
					Log.v(BOARD, "Size of board is " + Integer.toString(hqBoard.numXTiles) + " W x " + Integer.toString(hqBoard.numYTiles) + " H");
				}else{
					Log.e(GAME,"Quest File did not contain a proper height and width on the first line.");
				}
			}catch(IOException e){
			}
			
			String[][] readArray = new String[(2*hqBoard.numYTiles)+1][(2*hqBoard.numXTiles)+1];
			
			try{
				int yIndex = 0;
				while((currentLine = bufferReader.readLine()) != null){
					currentLineSplit = currentLine.split(",");
					
					for(int xIndex = 0; xIndex < currentLineSplit.length; xIndex++){
						readArray[yIndex][xIndex] = new String(currentLineSplit[xIndex]);
					}
					yIndex++;
				}
			}catch(IOException e){
			}
			
			
			/*********************************** PARSE THE READ ARRAY ***************************************/	
			// Initialize the board.
			hqBoard.tileSet = new Board.Tile[hqBoard.numYTiles][hqBoard.numXTiles];

			for(int y = 0; y < hqBoard.numYTiles; y++){
				for(int x = 0; x < hqBoard.numXTiles; x++){
					// Create a new tile (hallway).
					hqBoard.tileSet[y][x] = hqBoard.new Tile(y, x);

					// Wall and door building. All doors are spawned closed. Secrets undiscovered.
					if(readArray[(2*y)][(2*x)+1].equals("W")){
						hqBoard.tileSet[y][x].northWall = 1;
					}else if(readArray[(2*y)][(2*x)+1].equals("E")){
						hqBoard.tileSet[y][x].northWall = 2;
					}else if(readArray[(2*y)][(2*x)+1].equals("D")){
						hqBoard.tileSet[y][x].northWall = 3;
					}else if(readArray[(2*y)][(2*x)+1].equals("S")){
						hqBoard.tileSet[y][x].northWall = 5;
					}
					if(readArray[(2*y)+2][(2*x)+1].equals("W")){
						hqBoard.tileSet[y][x].southWall = 1;
					}else if(readArray[(2*y)+2][(2*x)+1].equals("E")){
						hqBoard.tileSet[y][x].southWall = 2;
					}else if(readArray[(2*y)+2][(2*x)+1].equals("D")){
						hqBoard.tileSet[y][x].southWall = 3;
					}else if(readArray[(2*y)+2][(2*x)+1].equals("S")){
						hqBoard.tileSet[y][x].southWall = 5;
					}
					if(readArray[(2*y)+1][(2*x)].equals("W")){
						hqBoard.tileSet[y][x].westWall = 1;
					}else if(readArray[(2*y)+1][(2*x)].equals("E")){
						hqBoard.tileSet[y][x].westWall = 2;
					}else if(readArray[(2*y)+1][(2*x)].equals("D")){
						hqBoard.tileSet[y][x].westWall = 3;
					}else if(readArray[(2*y)+1][(2*x)].equals("S")){
						hqBoard.tileSet[y][x].westWall = 5;
					}
					if(readArray[(2*y)+1][(2*x)+2].equals("W")){
						hqBoard.tileSet[y][x].eastWall = 1;
					}else if(readArray[(2*y)+1][(2*x)+2].equals("E")){
						hqBoard.tileSet[y][x].eastWall = 2;
					}else if(readArray[(2*y)+1][(2*x)+2].equals("D")){
						hqBoard.tileSet[y][x].eastWall = 3;
					}else if(readArray[(2*y)+1][(2*x)+2].equals("S")){
						hqBoard.tileSet[y][x].eastWall = 5;
					}

					// Tile Building. (Heroes, Monsters, Doodads, Traps and Treasures.)
					try{
						switch(readArray[(2*y)+1][(2*x)+1].charAt(0)){
						case 'B':
							hqBoard.tileSet[y][x].isTravelable = 0;
							hqBoard.tileSet[y][x].isBlack = 1;
							break;
						case 'R':
							hqBoard.tileSet[y][x].isTravelable = 0;
							// TODO Add rubble doodad object.
							break;
						case 'H':
							
							if(hqBoard.heroSet.get(getIndex(readArray[(2*y)+1][(2*x)+1])).isEnabled()){
								Log.v(GAME, "Hero spawned at " + Integer.toString(y) + " " + Integer.toString(x));
								// TODO Set stats based on difficulty. HP boost for easy, handicap for hard, etc.
								hqBoard.heroSet.get(getIndex(readArray[(2*y)+1][(2*x)+1])).spawnUnit(y, x, getDirection(readArray[(2*y)+1][(2*x)+1]), getRoomNumber(readArray[(2*y)+1][(2*x)+1]), INACTIVE);
								hqBoard.heroCounter++;
							}else{
								Log.v(GAME,"Hero found at "  + Integer.toString(y) + " " + Integer.toString(x) + " but not spawned (disabled).");
								hqBoard.heroSet.get(getIndex(readArray[(2*y)+1][(2*x)+1])).spawnUnit(0, 0, 0, 0, INACTIVE);
							}
							break;
						case 'M':
							Log.v(GAME, "Monster " + Integer.toString(getIndex(readArray[(2*y)+1][(2*x)+1])) + " spawned at " + Integer.toString(y) + " " + Integer.toString(x) + " facing " + getDirection(readArray[(2*y)+1][(2*x)+1]) + " in room " + getRoomNumber(readArray[(2*y)+1][(2*x)+1]));
							// TODO Boost / nerf monsters based on difficulty.
							hqBoard.monsterSet.add(hqBoard.new Unit(Board.Unit.MONSTER, getIndex(readArray[(2*y)+1][(2*x)+1])));
							hqBoard.monsterSet.get(hqBoard.monsterCounter).spawnUnit(y, x, getDirection(readArray[(2*y)+1][(2*x)+1]), getRoomNumber(readArray[(2*y)+1][(2*x)+1]), INACTIVE);
							hqBoard.monsterCounter++;
							break;
						case 'D':
							if(getSpecialTag(readArray[(2*y)+1][(2*x)+1]) == 1){
								Log.v(GAME,"Doodad spawned at " + Integer.toString(y) + " " + Integer.toString(x));
								hqBoard.doodadSet.add(hqBoard.new Unit(Board.Unit.DOODAD, getIndex(readArray[(2*y)+1][(2*x)+1])));
								hqBoard.doodadSet.get(hqBoard.doodadCounter).spawnUnit(y, x, getDirection(readArray[(2*y)+1][(2*x)+1]), getRoomNumber(readArray[(2*y)+1][(2*x)+1]), INACTIVE);
								hqBoard.doodadCounter++;
							}
							hqBoard.tileSet[y][x].isTravelable = 0;
							break;
						case 'T':
							Log.v(GAME,"It's a Trap!");
							// TODO
							break;
						case 'G':
							Log.v(GAME,"Treasure!");
							// TODO
							break;
						case 'S':
							Log.v(GAME,"Staircase Added");
							if(getSpecialTag(readArray[(2*y)+1][(2*x)+1]) == 1){
								hqBoard.doodadSet.add(hqBoard.new Unit(Board.Unit.DOODAD, getIndex(readArray[(2*y)+1][(2*x)+1])));
								hqBoard.doodadSet.get(hqBoard.doodadCounter).spawnUnit(y, x, getDirection(readArray[(2*y)+1][(2*x)+1]), getRoomNumber(readArray[(2*y)+1][(2*x)+1]), INACTIVE);
								hqBoard.doodadCounter++;
							}
							hqBoard.tileSet[y][x].isStair = 1;
						case 'X':					// This is probably going to be default:

							break;
						}

						// Assign the room number. Fixed position in tile code.
						hqBoard.tileSet[y][x].room = getRoomNumber(readArray[(2*y)+1][(2*x)+1]);
					}catch(IndexOutOfBoundsException e){
						Log.e(GAME,"String value of the tile at row " + Integer.toString(y) + ", column " + Integer.toString(x) + " was not properly formed.");
					}
				}
			}
			
			// TODO Set objectives.
				
			
			// Push all data into databases. Most efficient method of correctly using onPause() and onResume();
			sqLiteAdapter.openToWrite();
			
			// Save numXTiles and numYTiles as SharedPreferences to rebuild during onResume().
			gameStateEditor = gameState.edit();
			gameStateEditor.putInt("tile_size", hqBoard.tileSize);
			gameStateEditor.putInt("num_x_tiles", hqBoard.numXTiles);
			gameStateEditor.putInt("num_y_tiles", hqBoard.numYTiles);
			gameStateEditor.commit();
			
			//Push tileSet with current states.
			sqLiteAdapter.clearTiles();
			for(int y = 0; y < hqBoard.numYTiles; y++){
				for(int x = 0; x < hqBoard.numXTiles; x++){
					sqLiteAdapter.addTile(x, y, 
							hqBoard.tileSet[y][x].northWall, 
							hqBoard.tileSet[y][x].eastWall,
							hqBoard.tileSet[y][x].southWall, 
							hqBoard.tileSet[y][x].westWall, 
							hqBoard.tileSet[y][x].isTrap, 
							hqBoard.tileSet[y][x].room, 
							hqBoard.tileSet[y][x].isTravelable, 
							hqBoard.tileSet[y][x].isBlack, 
							hqBoard.tileSet[y][x].isStair);
				}
			}
			
			// Push heroSet with current states.
			sqLiteAdapter.clearUnits();
			for(int genIndex = 0; genIndex < hqBoard.heroSet.size(); genIndex++){
				sqLiteAdapter.addUnit(hqBoard.heroSet.get(genIndex).unitType, 
						hqBoard.heroSet.get(genIndex).unitClass, 
						hqBoard.heroSet.get(genIndex).isEnabled, 
						hqBoard.heroSet.get(genIndex).bodyCurrent, 
						hqBoard.heroSet.get(genIndex).bodyMax, 
						hqBoard.heroSet.get(genIndex).mindCurrent, 
						hqBoard.heroSet.get(genIndex).mindMax, 
						hqBoard.heroSet.get(genIndex).movesLeft, 
						hqBoard.heroSet.get(genIndex).moveDice, 
						hqBoard.heroSet.get(genIndex).attackDice, 
						hqBoard.heroSet.get(genIndex).defendDice, 
						hqBoard.heroSet.get(genIndex).actionsPerRound, 
						hqBoard.heroSet.get(genIndex).actionsRemaining, 
						hqBoard.heroSet.get(genIndex).existLoc.xTile, 
						hqBoard.heroSet.get(genIndex).existLoc.yTile, 
						hqBoard.heroSet.get(genIndex).destLoc.xTile, 
						hqBoard.heroSet.get(genIndex).destLoc.yTile,
						hqBoard.heroSet.get(genIndex).action, 
						hqBoard.heroSet.get(genIndex).traveling, 
						hqBoard.heroSet.get(genIndex).direction);
			}
			
			// Push monsterSet with current states.
			for(int genIndex = 0; genIndex < hqBoard.monsterSet.size(); genIndex++){
				sqLiteAdapter.addUnit(hqBoard.monsterSet.get(genIndex).unitType, 
						hqBoard.monsterSet.get(genIndex).unitClass, 
						hqBoard.monsterSet.get(genIndex).isEnabled, 
						hqBoard.monsterSet.get(genIndex).bodyCurrent, 
						hqBoard.monsterSet.get(genIndex).bodyMax, 
						hqBoard.monsterSet.get(genIndex).mindCurrent, 
						hqBoard.monsterSet.get(genIndex).mindMax, 
						hqBoard.monsterSet.get(genIndex).movesLeft, 
						hqBoard.monsterSet.get(genIndex).moveDice, 
						hqBoard.monsterSet.get(genIndex).attackDice, 
						hqBoard.monsterSet.get(genIndex).defendDice, 
						hqBoard.monsterSet.get(genIndex).actionsPerRound, 
						hqBoard.monsterSet.get(genIndex).actionsRemaining, 
						hqBoard.monsterSet.get(genIndex).existLoc.xTile, 
						hqBoard.monsterSet.get(genIndex).existLoc.yTile, 
						hqBoard.monsterSet.get(genIndex).destLoc.xTile, 
						hqBoard.monsterSet.get(genIndex).destLoc.yTile,
						hqBoard.monsterSet.get(genIndex).action, 
						hqBoard.monsterSet.get(genIndex).traveling, 
						hqBoard.monsterSet.get(genIndex).direction);
			}
			
			// Push doodadSet with current states.
			for(int genIndex = 0; genIndex < hqBoard.doodadSet.size(); genIndex++){
				sqLiteAdapter.addUnit(hqBoard.doodadSet.get(genIndex).unitType, 
						hqBoard.doodadSet.get(genIndex).unitClass, 
						hqBoard.doodadSet.get(genIndex).isEnabled, 
						hqBoard.doodadSet.get(genIndex).bodyCurrent, 
						hqBoard.doodadSet.get(genIndex).bodyMax, 
						hqBoard.doodadSet.get(genIndex).mindCurrent, 
						hqBoard.doodadSet.get(genIndex).mindMax, 
						hqBoard.doodadSet.get(genIndex).movesLeft, 
						hqBoard.doodadSet.get(genIndex).moveDice, 
						hqBoard.doodadSet.get(genIndex).attackDice, 
						hqBoard.doodadSet.get(genIndex).defendDice, 
						hqBoard.doodadSet.get(genIndex).actionsPerRound, 
						hqBoard.doodadSet.get(genIndex).actionsRemaining, 
						hqBoard.doodadSet.get(genIndex).existLoc.xTile, 
						hqBoard.doodadSet.get(genIndex).existLoc.yTile, 
						hqBoard.doodadSet.get(genIndex).destLoc.xTile, 
						hqBoard.doodadSet.get(genIndex).destLoc.yTile,
						hqBoard.doodadSet.get(genIndex).action, 
						hqBoard.doodadSet.get(genIndex).traveling, 
						hqBoard.doodadSet.get(genIndex).direction);
			}

			// TODO Other shit.
			
			sqLiteAdapter.close();
			sqCursor.close();
			
		}else{
			Log.v(GAME,"Rebuilding Existing Game.");

			// Need to set these for initImages to work properly.
			hqBoard.tileSize = gameState.getInt("tile_size",DEFAULT_TILE_SIZE);
			hqBoard.numXTiles = gameState.getInt("num_x_tiles", Board.MAX_X_TILES);
			hqBoard.numYTiles = gameState.getInt("num_y_tiles", Board.MAX_Y_TILES);
		}

		
		

		/******************** SET UP VIEW ***********************/

		hqBoard.initImages();
		hqBoard.goldCounter = gameState.getInt("gold_counter",0);
		
		hqBoard.setOnGoldChangeListener(new Board.OnGoldChangeListener(){
			public void onGoldChange(int toAdd){
				for(int i = 0; i < toAdd; i++){
					hqBoard.goldCounter++;
					storeButton.setText(Integer.toString(hqBoard.goldCounter));
				}
			}
		});
		
		hqBoard.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View v, MotionEvent event) {
				processBoardTouch(event);
				return true;
			}
		});
		
		// Set the flag.
		inventoryOpenFlag = false;
		LayoutInflater inflater = (LayoutInflater) Game.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View windowView = inflater.inflate(R.layout.hero_equipment_screen, null);
		final PopupWindow heroInventoryWindow = new PopupWindow(windowView);
		
		
		Button doneButton = (Button) windowView.findViewById(R.id.done_button);
		doneButton.setOnTouchListener(new View.OnTouchListener(){
			public boolean onTouch(View v, MotionEvent me) {
				if(me.getAction() == MotionEvent.ACTION_UP){
					heroInventoryWindow.dismiss();
				}
				return true;
			}
		});
		
		
        Button barbButton = (Button) findViewById(R.id.barbbutton);
        barbHPTextView = (TextView) findViewById(R.id.barb_hp_textview);
        barbMPTextView = (TextView) findViewById(R.id.barb_mp_textview);
        barbMoveTextView = (TextView) findViewById(R.id.barb_move_textview);
        if(hqBoard.heroSet.get(Unit.BARBARIAN).isEnabled()){
        	barbButton.setOnTouchListener(new View.OnTouchListener(){
        		public boolean onTouch(View v, MotionEvent me) {
        			if(me.getAction() == MotionEvent.ACTION_UP){
        				Log.v(GAME,"Touched Barbarian");
        				// TODO Change to disable touching of parent.        				
        				if(inventoryOpenFlag == false){
        					heroInventoryWindow.showAtLocation(Game.this.findViewById(R.id.board), Gravity.CENTER, 0, 0);
        					heroInventoryWindow.update((int)Math.round(0.8*Game.this.findViewById(R.id.board).getWidth()),(int)Math.round(0.8*Game.this.findViewById(R.id.board).getHeight()));
        					inventoryOpenFlag = true;
        				}else{
        					heroInventoryWindow.dismiss();
        					inventoryOpenFlag = false;
        				}
        			}
        			return true;
        		}
        	});
        }else{
        	// TODO Set to grayed out button instead if hero is disabled.
        	barbHPTextView.setVisibility(View.INVISIBLE);
            barbMPTextView.setVisibility(View.INVISIBLE);
            barbMoveTextView.setVisibility(View.INVISIBLE);
        }
        
        Button dwarfButton = (Button) findViewById(R.id.dwarfbutton);
        dwarfHPTextView = (TextView) findViewById(R.id.dwarf_hp_textview);
        dwarfMPTextView = (TextView) findViewById(R.id.dwarf_mp_textview);
        dwarfMoveTextView = (TextView) findViewById(R.id.dwarf_move_textview);
        if(hqBoard.heroSet.get(Unit.DWARF).isEnabled()){
        	dwarfButton.setOnTouchListener(new View.OnTouchListener(){
        		public boolean onTouch(View v, MotionEvent me) {
        			if(me.getAction() == MotionEvent.ACTION_UP){
        				Log.v(GAME,"Touched Dwarf");
        				// TODO Change to disable touching of parent.
        				if(inventoryOpenFlag == false){
        					heroInventoryWindow.showAtLocation(Game.this.findViewById(R.id.board), Gravity.CENTER, 0, 0);
        					heroInventoryWindow.update((int)Math.round(0.8*Game.this.findViewById(R.id.board).getWidth()),(int)Math.round(0.8*Game.this.findViewById(R.id.board).getHeight()));
        					inventoryOpenFlag = true;
        				}else{
        					heroInventoryWindow.dismiss();
        					inventoryOpenFlag = false;
        				}
        			}
        			return true;
        		}
        	});
        }else{
        	// TODO Set to grayed out button instead if hero is disabled.
        	dwarfHPTextView.setVisibility(View.INVISIBLE);
        	dwarfMPTextView.setVisibility(View.INVISIBLE);
        	dwarfMoveTextView.setVisibility(View.INVISIBLE);
        }
        
        Button elfButton = (Button) findViewById(R.id.elfbutton);
        elfHPTextView = (TextView) findViewById(R.id.elf_hp_textview);
        elfMPTextView = (TextView) findViewById(R.id.elf_mp_textview);
        elfMoveTextView = (TextView) findViewById(R.id.elf_move_textview);
        if(hqBoard.heroSet.get(Unit.ELF).isEnabled()){
        	elfButton.setOnTouchListener(new View.OnTouchListener(){
        		public boolean onTouch(View v, MotionEvent me) {
        			if(me.getAction() == MotionEvent.ACTION_UP){
        				Log.v(GAME,"Touched Elf");
        				// TODO Change to disable touching of parent.
        				if(inventoryOpenFlag == false){
        					heroInventoryWindow.showAtLocation(Game.this.findViewById(R.id.board), Gravity.CENTER, 0, 0);
        					heroInventoryWindow.update((int)Math.round(0.8*Game.this.findViewById(R.id.board).getWidth()),(int)Math.round(0.8*Game.this.findViewById(R.id.board).getHeight()));
        					inventoryOpenFlag = true;
        				}else{
        					heroInventoryWindow.dismiss();
        					inventoryOpenFlag = false;
        				}
        			}
        			return true;
        		}
        	});
        }else{
        	// TODO Set to grayed out button instead if hero is disabled.
        	elfHPTextView.setVisibility(View.INVISIBLE);
        	elfMPTextView.setVisibility(View.INVISIBLE);
        	elfMoveTextView.setVisibility(View.INVISIBLE);
        }
        
        
        
        Button wizardButton = (Button) findViewById(R.id.wizardbutton);
        wizardHPTextView = (TextView) findViewById(R.id.wizard_hp_textview);
        wizardMPTextView = (TextView) findViewById(R.id.wizard_mp_textview);
        wizardMoveTextView = (TextView) findViewById(R.id.wizard_move_textview);
        // TODO Set to grayed out button instead if hero is disabled.
        if(hqBoard.heroSet.get(Unit.WIZARD).isEnabled()){
        	wizardButton.setOnTouchListener(new View.OnTouchListener(){
        		public boolean onTouch(View v, MotionEvent me) {
        			if(me.getAction() == MotionEvent.ACTION_UP){
        				Log.v(GAME,"Touched Wizard");
        				// TODO Change to disable touching of parent.
        				if(inventoryOpenFlag == false){
        					heroInventoryWindow.showAtLocation(Game.this.findViewById(R.id.board), Gravity.CENTER, 0, 0);
        					heroInventoryWindow.update((int)Math.round(0.8*Game.this.findViewById(R.id.board).getWidth()),(int)Math.round(0.8*Game.this.findViewById(R.id.board).getHeight()));
        					inventoryOpenFlag = true;
        				}else{
        					heroInventoryWindow.dismiss();
        					inventoryOpenFlag = false;
        				}
        			}
        			return true;
        		}
        	});
        }else{
        	// TODO Set to grayed out button instead if hero is disabled.
        	wizardHPTextView.setVisibility(View.INVISIBLE);
        	wizardMPTextView.setVisibility(View.INVISIBLE);
        	wizardMoveTextView.setVisibility(View.INVISIBLE);
        }
        
		hqBoard.setOnStatChangeListener(new Board.OnStatChangeListener() {
			public void onBodyChange(int hero) {
				Log.v(MISC,"Body Change");
				switch(hero){
				case(Unit.BARBARIAN):
					barbHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).bodyMax));
					break;
				case(Unit.DWARF):
					dwarfHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.DWARF).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.DWARF).bodyMax));
					break;
				case(Unit.ELF):
					elfHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.ELF).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.ELF).bodyMax));
					break;
				case(Unit.WIZARD):
					wizardHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).bodyMax));
					break;
				}
			}

			public void onMindChange(int hero) {
				Log.v(MISC,"Mind Change");
				switch(hero){
				case(Unit.BARBARIAN):
					barbMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).mindMax));
					break;
				case(Unit.DWARF):
					dwarfMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.DWARF).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.DWARF).mindMax));
					break;
				case(Unit.ELF):
					elfMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.ELF).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.ELF).mindMax));
					break;
				case(Unit.WIZARD):
					wizardMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).mindMax));
					break;
				}
			}

			public void onMoveChange(int hero, int distance) {
				Log.v(MISC,"Move Change");
				switch(hero){
				case(Unit.BARBARIAN):
					if ((hqBoard.heroSet.get(Unit.BARBARIAN).movesLeft-distance) > 0){
						barbMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).movesLeft-distance));
					}else{
						barbMoveTextView.setText(Integer.toString(0));
					}
					break;
				case(Unit.DWARF):
					if ((hqBoard.heroSet.get(Unit.DWARF).movesLeft-distance) > 0){
						dwarfMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.DWARF).movesLeft-distance));
					}else{
						dwarfMoveTextView.setText(Integer.toString(0));
					}
					break;
				case(Unit.ELF):
					if ((hqBoard.heroSet.get(Unit.ELF).movesLeft-distance) > 0){
						elfMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.ELF).movesLeft-distance));
					}else{
						elfMoveTextView.setText(Integer.toString(0));
					}
					break;
				case(Unit.WIZARD):
					if ((hqBoard.heroSet.get(Unit.WIZARD).movesLeft-distance) > 0){
						wizardMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).movesLeft-distance));
					}else{
						wizardMoveTextView.setText(Integer.toString(0));
					}
					break;
				}
			}
		});
        
        storeButton = (Button) findViewById(R.id.storebutton1);
        // TODO Update to custom button.
        storeButton.setText(Integer.toString(hqBoard.goldCounter));
        storeButton.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me) {
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(GAME,"Open the Store?");
        		}	
        		return true;
        	}
        });
        
        Button gameInfoButton = (Button) findViewById(R.id.gameinfobutton);
        gameInfoButton.setText(gameState.getString("quest_title", "Unknown") + " (" + DIFFICULTIES[gameState.getInt("difficulty", 1)] +")");
        gameInfoButton.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me) {
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(GAME,"Open Game Info");
        		}
        		return true;
        	}
        });
        
        Button endTurnButton = (Button) findViewById(R.id.endturnbutton);
        endTurnButton.setOnTouchListener(new View.OnTouchListener(){
        	public boolean onTouch(View v, MotionEvent me) {
        		if(me.getAction() == MotionEvent.ACTION_UP){
        			Log.v(GAME,"End Turn");
        			hqBoard.moveZargon();
        			incrementRound();
        			// TODO Game completion check.
        		}

        		return true;
        	}
        });
        
	}
	
	private int getRoomNumber(String s){
		return ((Character.getNumericValue(s.charAt(1)) * 10) + Character.getNumericValue(s.charAt(2)));
	}
	
	private int getIndex(String s){
		return (Character.getNumericValue(s.charAt(3)));
	}
	
	private int getDirection(String s){
		return (Character.getNumericValue(s.charAt(4)));
	}
	
	private int getSpecialTag(String s){
		return (Character.getNumericValue(s.charAt(5)));
	}
	
	private void processBoardTouch(MotionEvent event) {
		try{
			Thread.sleep(25);   // TODO Custom frame rate.
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			hqBoard.setClickPoint(event.getRawX(), event.getRawY());
			break;
		case MotionEvent.ACTION_MOVE:
			hqBoard.moveBoard(event.getRawX(), event.getRawY());
			break;
		}
	}

	
	private void incrementRound(){
		Log.v(GAME,"Next Round.");

		for(int i = 0; i < hqBoard.heroCounter; i++){
			hqBoard.heroSet.get(i).movesLeft = hqBoard.heroSet.get(i).refreshMoves();
			Log.v(GAME,"Hero Gets " + Integer.toString(hqBoard.heroSet.get(i).movesLeft) + " moves this round."); 
			hqBoard.heroSet.get(i).actionsRemaining = hqBoard.heroSet.get(i).actionsPerRound;
		}
	}
	
	
    @Override
	protected void onPause() {
		super.onPause();
		hqBoard.pause();
		
		Log.v(GAME, "Pushing things to database...");
		
		sqLiteAdapter = new HQDatabaseAdapter(this);
		sqLiteAdapter.openToWrite();
		
		//Push tileSet with current states. numXTiles and numYTiles are already stored as SharedPreferences at game start.
		sqLiteAdapter.clearTiles();
		for(int y = 0; y < hqBoard.numYTiles; y++){
			for(int x = 0; x < hqBoard.numXTiles; x++){
				sqLiteAdapter.addTile(x, y, 
						hqBoard.tileSet[y][x].northWall, 
						hqBoard.tileSet[y][x].eastWall,
						hqBoard.tileSet[y][x].southWall, 
						hqBoard.tileSet[y][x].westWall, 
						hqBoard.tileSet[y][x].isTrap, 
						hqBoard.tileSet[y][x].room, 
						hqBoard.tileSet[y][x].isTravelable, 
						hqBoard.tileSet[y][x].isBlack, 
						hqBoard.tileSet[y][x].isStair);
			}
		}
		
		// Push heroSet with current states.
		sqLiteAdapter.clearUnits();
		for(int genIndex = 0; genIndex < hqBoard.heroSet.size(); genIndex++){
			if(hqBoard.heroSet.get(genIndex).isEnabled()){
				sqLiteAdapter.addUnit(hqBoard.heroSet.get(genIndex).unitType, 
					hqBoard.heroSet.get(genIndex).unitClass, 
					hqBoard.heroSet.get(genIndex).isEnabled, 
					hqBoard.heroSet.get(genIndex).bodyCurrent, 
					hqBoard.heroSet.get(genIndex).bodyMax, 
					hqBoard.heroSet.get(genIndex).mindCurrent, 
					hqBoard.heroSet.get(genIndex).mindMax, 
					hqBoard.heroSet.get(genIndex).movesLeft, 
					hqBoard.heroSet.get(genIndex).moveDice, 
					hqBoard.heroSet.get(genIndex).attackDice, 
					hqBoard.heroSet.get(genIndex).defendDice, 
					hqBoard.heroSet.get(genIndex).actionsPerRound, 
					hqBoard.heroSet.get(genIndex).actionsRemaining, 
					hqBoard.heroSet.get(genIndex).existLoc.xTile, 
					hqBoard.heroSet.get(genIndex).existLoc.yTile, 
					hqBoard.heroSet.get(genIndex).destLoc.xTile, 
					hqBoard.heroSet.get(genIndex).destLoc.yTile,
					hqBoard.heroSet.get(genIndex).action, 
					hqBoard.heroSet.get(genIndex).traveling, 
					hqBoard.heroSet.get(genIndex).direction);
			}else{
				sqLiteAdapter.addUnit(hqBoard.heroSet.get(genIndex).unitType, 
						hqBoard.heroSet.get(genIndex).unitClass, 
						hqBoard.heroSet.get(genIndex).isEnabled,
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			}
		}
		
		// Push monsterSet with current states.
		for(int genIndex = 0; genIndex < hqBoard.monsterSet.size(); genIndex++){
			sqLiteAdapter.addUnit(hqBoard.monsterSet.get(genIndex).unitType, 
					hqBoard.monsterSet.get(genIndex).unitClass, 
					hqBoard.monsterSet.get(genIndex).isEnabled, 
					hqBoard.monsterSet.get(genIndex).bodyCurrent, 
					hqBoard.monsterSet.get(genIndex).bodyMax, 
					hqBoard.monsterSet.get(genIndex).mindCurrent, 
					hqBoard.monsterSet.get(genIndex).mindMax, 
					hqBoard.monsterSet.get(genIndex).movesLeft, 
					hqBoard.monsterSet.get(genIndex).moveDice, 
					hqBoard.monsterSet.get(genIndex).attackDice, 
					hqBoard.monsterSet.get(genIndex).defendDice, 
					hqBoard.monsterSet.get(genIndex).actionsPerRound, 
					hqBoard.monsterSet.get(genIndex).actionsRemaining, 
					hqBoard.monsterSet.get(genIndex).existLoc.xTile, 
					hqBoard.monsterSet.get(genIndex).existLoc.yTile, 
					hqBoard.monsterSet.get(genIndex).destLoc.xTile, 
					hqBoard.monsterSet.get(genIndex).destLoc.yTile,
					hqBoard.monsterSet.get(genIndex).action, 
					hqBoard.monsterSet.get(genIndex).traveling, 
					hqBoard.monsterSet.get(genIndex).direction);
		}
		
		// Push doodadSet with current states.
		for(int genIndex = 0; genIndex < hqBoard.doodadSet.size(); genIndex++){
			sqLiteAdapter.addUnit(hqBoard.doodadSet.get(genIndex).unitType, 
					hqBoard.doodadSet.get(genIndex).unitClass, 
					hqBoard.doodadSet.get(genIndex).isEnabled, 
					hqBoard.doodadSet.get(genIndex).bodyCurrent, 
					hqBoard.doodadSet.get(genIndex).bodyMax, 
					hqBoard.doodadSet.get(genIndex).mindCurrent, 
					hqBoard.doodadSet.get(genIndex).mindMax, 
					hqBoard.doodadSet.get(genIndex).movesLeft, 
					hqBoard.doodadSet.get(genIndex).moveDice, 
					hqBoard.doodadSet.get(genIndex).attackDice, 
					hqBoard.doodadSet.get(genIndex).defendDice, 
					hqBoard.doodadSet.get(genIndex).actionsPerRound, 
					hqBoard.doodadSet.get(genIndex).actionsRemaining, 
					hqBoard.doodadSet.get(genIndex).existLoc.xTile, 
					hqBoard.doodadSet.get(genIndex).existLoc.yTile, 
					hqBoard.doodadSet.get(genIndex).destLoc.xTile, 
					hqBoard.doodadSet.get(genIndex).destLoc.yTile,
					hqBoard.doodadSet.get(genIndex).action, 
					hqBoard.doodadSet.get(genIndex).traveling, 
					hqBoard.doodadSet.get(genIndex).direction);
		}
		
		// TODO Push equipment.
		
		// Push gold.
		gameStateEditor = gameState.edit();
		gameStateEditor.putInt("gold_counter", hqBoard.goldCounter);
		gameStateEditor.commit();
		
		sqLiteAdapter.close();
	}
    


	@Override
	protected void onResume() {
		super.onResume();
		Log.v(GAME, "Quick Load");
		
		gameState = getSharedPreferences(SAVE_KEY, MODE_PRIVATE);
		sqLiteAdapter = new HQDatabaseAdapter(this);
		sqLiteAdapter.openToRead();

		// Rebuild tileSet.
		Log.v(GAME, "Rebuilding tileSet.");
		hqBoard.tileSize = gameState.getInt("tile_size",DEFAULT_TILE_SIZE);
		hqBoard.numXTiles = gameState.getInt("num_x_tiles", Board.MAX_X_TILES);
		hqBoard.numYTiles = gameState.getInt("num_y_tiles", Board.MAX_Y_TILES);
		hqBoard.tileSet = new Board.Tile[hqBoard.numYTiles][hqBoard.numXTiles];
		
		sqCursor = sqLiteAdapter.getAllTiles(); 
		int i = 0;
		while(i < sqCursor.getCount()){
			sqCursor.moveToPosition(i);
			Log.v(BOARD,"Tile: " + Integer.toString(sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)) + ", " + Integer.toString(sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)));
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)] = hqBoard.new Tile(sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET), sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET));
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].northWall = sqCursor.getInt(HQDatabaseAdapter.TILE_NORTH_WALL_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].eastWall = sqCursor.getInt(HQDatabaseAdapter.TILE_EAST_WALL_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].southWall = sqCursor.getInt(HQDatabaseAdapter.TILE_SOUTH_WALL_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].westWall = sqCursor.getInt(HQDatabaseAdapter.TILE_WEST_WALL_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].isTrap = sqCursor.getInt(HQDatabaseAdapter.TILE_TRAP_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].room = sqCursor.getInt(HQDatabaseAdapter.TILE_ROOM_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].isTravelable = sqCursor.getInt(HQDatabaseAdapter.TILE_TRAVELABLE_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].isBlack = sqCursor.getInt(HQDatabaseAdapter.TILE_BLACK_COLUMN_OFFSET);
			hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.TILE_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.TILE_X_COLUMN_OFFSET)].isStair = sqCursor.getInt(HQDatabaseAdapter.TILE_STAIR_COLUMN_OFFSET);
			i++;
		}

		// Rebuild the heroSet.
		Log.v(GAME, "Rebuilding heroSet.");
		hqBoard.heroSet.clear();
		hqBoard.monsterSet.clear();
		hqBoard.doodadSet.clear();
		hqBoard.heroCounter = hqBoard.monsterCounter = hqBoard.doodadCounter = 0;
		sqCursor = sqLiteAdapter.getAllUnits();
		for(int genIndex = 0; genIndex < sqCursor.getCount(); genIndex++){
			sqCursor.moveToPosition(genIndex);
			Log.v(GAME,Integer.toString(sqCursor.getInt(HQDatabaseAdapter.UNIT_TYPE_COLUMN_OFFSET)));
			switch(sqCursor.getInt(HQDatabaseAdapter.UNIT_TYPE_COLUMN_OFFSET)){
			case(Board.Unit.HERO):
				hqBoard.heroSet.add(hqBoard.new Unit(Board.Unit.HERO, sqCursor.getInt(HQDatabaseAdapter.UNIT_CLASS_COLUMN_OFFSET)));
				if(sqCursor.getInt(HQDatabaseAdapter.UNIT_ENABLED_COLUMN_OFFSET) == 0){
					Log.v(GAME, "Hero " + hqBoard.heroCounter + " is sleeping.");
					hqBoard.heroSet.get(hqBoard.heroCounter).disable();
				}else{
					Log.v(GAME, "Hero " + hqBoard.heroCounter + " is awake.");
					hqBoard.heroSet.get(hqBoard.heroCounter).spawnUnit(sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_Y_COLUMN_OFFSET), 
						sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_X_COLUMN_OFFSET), 
						sqCursor.getInt(HQDatabaseAdapter.UNIT_DIRECTION_COLUMN_OFFSET), 
						hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_X_COLUMN_OFFSET)].room,
						sqCursor.getInt(HQDatabaseAdapter.UNIT_ACTION_COLUMN_OFFSET));
				}
				hqBoard.heroCounter++;
				break;
			case(Board.Unit.MONSTER):
				hqBoard.monsterSet.add(hqBoard.new Unit(Board.Unit.MONSTER, sqCursor.getInt(HQDatabaseAdapter.UNIT_CLASS_COLUMN_OFFSET)));
				hqBoard.monsterSet.get(hqBoard.monsterCounter).spawnUnit(sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_Y_COLUMN_OFFSET), 
					sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_X_COLUMN_OFFSET), 
					sqCursor.getInt(HQDatabaseAdapter.UNIT_DIRECTION_COLUMN_OFFSET), 
					hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_X_COLUMN_OFFSET)].room,
					sqCursor.getInt(HQDatabaseAdapter.UNIT_ACTION_COLUMN_OFFSET));
				hqBoard.monsterCounter++;
				break;
			case(Board.Unit.DOODAD):
				hqBoard.doodadSet.add(hqBoard.new Unit(Board.Unit.DOODAD, sqCursor.getInt(HQDatabaseAdapter.UNIT_CLASS_COLUMN_OFFSET)));
				hqBoard.doodadSet.get(hqBoard.doodadCounter).spawnUnit(sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_Y_COLUMN_OFFSET), 
					sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_X_COLUMN_OFFSET), 
					sqCursor.getInt(HQDatabaseAdapter.UNIT_DIRECTION_COLUMN_OFFSET), 
					hqBoard.tileSet[sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_Y_COLUMN_OFFSET)][sqCursor.getInt(HQDatabaseAdapter.UNIT_EXIST_X_COLUMN_OFFSET)].room,
					sqCursor.getInt(HQDatabaseAdapter.UNIT_ACTION_COLUMN_OFFSET));
				hqBoard.doodadCounter++;
				break;
			}
		}

		// TODO Update hero Stats with buffs and equipment from database.

		// TODO Rebuild Other


		sqLiteAdapter.close();

		if(hqBoard.heroSet.get(Unit.BARBARIAN).isEnabled()){
			barbHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).bodyMax));
			barbHPTextView.setTextColor(Color.RED);
			
			barbMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).mindMax));
			barbMPTextView.setTextColor(Color.BLUE);
			// TODO Mind, Move.
			barbMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.BARBARIAN).movesLeft));
			barbMoveTextView.setTextColor(Color.GREEN);
		}
		if(hqBoard.heroSet.get(Unit.DWARF).isEnabled()){
			dwarfHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.DWARF).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.DWARF).bodyMax));
			dwarfHPTextView.setTextColor(Color.RED);
			// TODO Mind, Move.
			dwarfMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.DWARF).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.DWARF).mindMax));
			dwarfMPTextView.setTextColor(Color.BLUE);
			
			dwarfMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.DWARF).movesLeft));
			dwarfMoveTextView.setTextColor(Color.GREEN);
		}
		if(hqBoard.heroSet.get(Unit.ELF).isEnabled()){
			elfHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.ELF).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.ELF).bodyMax));
			elfHPTextView.setTextColor(Color.RED);
			// TODO Mind, Move.
			elfMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.ELF).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.ELF).mindMax));
			elfMPTextView.setTextColor(Color.BLUE);
			
			elfMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.ELF).movesLeft));
			elfMoveTextView.setTextColor(Color.GREEN);
		}
		if(hqBoard.heroSet.get(Unit.WIZARD).isEnabled()){
			wizardHPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).bodyCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).bodyMax));
			wizardHPTextView.setTextColor(Color.RED);
			// TODO Mind, Move.
			wizardMPTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).mindCurrent) + " / " + Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).mindMax));
			wizardMPTextView.setTextColor(Color.BLUE);
			
			wizardMoveTextView.setText(Integer.toString(hqBoard.heroSet.get(Unit.WIZARD).movesLeft));
			wizardMoveTextView.setTextColor(Color.GREEN);
		}
		
		hqBoard.resume();

		Toast genToast = Toast.makeText(getApplicationContext(), "Game Load Success", Toast.LENGTH_SHORT);
		genToast.show();
	}
}
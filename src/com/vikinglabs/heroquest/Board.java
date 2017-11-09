package com.vikinglabs.heroquest;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;


/** Board is a View object dedicated entirely to drawing the overall canvas in its own thread to optimize
 * performance. It is nested within Game to be able to help prevent memory leaks of the context and other
 * variables. Must be a static class to instantiate correctly by the XML inflater. <b> The board class is
 * required to contain the current state of all of the pieces currently in the game. The Game class must
 * check with the state of the board for any queries it needs to conduct. </b>
 * 
 * @author Odin */
public class Board extends SurfaceView implements Runnable {
	
	// Debug filters.
	private static final String GAME = "Game";
	private static final String BOARD = "Board";
	private static final String COMBAT = "Combat";
	private static final String PATH = "Path";
	private static final String MISC = "Misc";
	
	// Unit action states.
	private static final int INACTIVE = 0;
	private static final int SELECTED = 1;
	private static final int MOVING = 2;
	private static final int ATTACKING = 3;
	private static final int DYING = 4;
	private static final int SEARCHING = 5;
	
	// Directions used by traveling and facing.
	private static final int NORTH = 0;
	private static final int NORTHEAST = 1;
	private static final int EAST = 2;
	private static final int SOUTHEAST = 3;
	private static final int SOUTH = 4;
	private static final int SOUTHWEST = 5;
	private static final int WEST = 6;
	private static final int NORTHWEST = 7;
	
	public static final int MAX_X_TILES = 28;
	public static final int MAX_Y_TILES = 21;
	
	// SurfaceView thread items.
	private Thread t;
	private boolean threadRunning;
	private SurfaceHolder mHolder;
	
	private OnGoldChangeListener gListener;
	private OnStatChangeListener sListener;
	
	// Lists of objects to draw.
	public Tile[][] tileSet;
	public ArrayList<Unit> heroSet, monsterSet, doodadSet;
	public int heroCounter, monsterCounter, doodadCounter;
	
	// Drawing utility variables.
	private static final int MOVEMENT_FRAMES = 4;
	private static final int MOVEMENT_SPRITESHEET_WIDTH = 3;
	private Canvas finalCanvas;
	private Bitmap frame;
	
	// Images used in the game.
	private Bitmap board, black;
	
	// Heroes
	private Bitmap barbarian; 
	private Bitmap dwarf;
	private Bitmap elf;
	private Bitmap wizard;
	
	// Monsters
	private Bitmap skeleton;
	private Bitmap zombie;
	private Bitmap mummy;
	private Bitmap goblin;
	private Bitmap orc;
	private Bitmap fimir;
	private Bitmap chaos;
	private Bitmap gargoyle;
	
	// Doors and stairs
	private Bitmap stair_north;
	private Bitmap ewdoorclosed;
	private Bitmap ewdooropen;
	private Bitmap nsdoorclosed;
	private Bitmap nsdooropen;
	
	// Doodads
	private Bitmap altar_north;
	private Bitmap bookcase_north;
	private Bitmap desk_north;
	private Bitmap fireplace_north;
	private Bitmap table_north;
	private Bitmap throne_north;
	private Bitmap tomb_north;
	private Bitmap torture_rack_north;
	private Bitmap weapon_rack_north;
	
	
	// Properties of the board, including location information.
	public int numXTiles, numYTiles, tileSize;
	public float leftEdge, rightEdge, topEdge, bottomEdge;
	private float deltaX, deltaY, prevX, prevY;
	private Tile touchedTile;
	private int wallTouched, activeHero, activeMonster, combatResult;
	private boolean isValid, actionArmed, boardDrawing;
	private boolean isEnemyTurn;
	
	// Game variables.
	public int objectivesRemaining;
	public int goldCounter;
	
	private FrameLayout gameLayout;
	private Button storeButton;
	
	/******************BOARD CONSTRUCTION AND INITIALIZATION ***********************/
	public Board(Context context){
		super(context);
	}
	
	public Board(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void initBoard(){
		
		t = null;
		threadRunning = false;
		mHolder = getHolder();
		
		heroSet = new ArrayList<Unit>();
		heroCounter = 0;
		monsterSet = new ArrayList<Unit>();
		monsterCounter = 0;
		doodadSet = new ArrayList<Unit>();
		doodadCounter = 0;
		
		touchedTile = new Tile(-1, -1);
		wallTouched = -1;
		boardDrawing = false;
		
		activeHero = -1;
		actionArmed = false;
		activeMonster = -1;
		
		objectivesRemaining = 0;
		
		storeButton = (Button)findViewById(R.id.storebutton1);
		if(storeButton == null){
			Log.v(MISC,"YOLO!");
		}
	}
	
	public void initImages(){
		
		// TODO Naming convention updates.
		
		// Board
		board = BitmapFactory.decodeResource(getResources(), R.drawable.board);
		black = BitmapFactory.decodeResource(getResources(), R.drawable.black);
		
		// Heroes
		barbarian = BitmapFactory.decodeResource(getResources(), R.drawable.barb_checked);
		dwarf = BitmapFactory.decodeResource(getResources(), R.drawable.dwarf_checked);
		elf = BitmapFactory.decodeResource(getResources(), R.drawable.elf_checked);
		wizard = BitmapFactory.decodeResource(getResources(), R.drawable.wizard_checked);
		
		// Monsters
		skeleton = BitmapFactory.decodeResource(getResources(), R.drawable.skeleton);
		zombie = BitmapFactory.decodeResource(getResources(), R.drawable.zombie);
		mummy = BitmapFactory.decodeResource(getResources(), R.drawable.mummy);
		goblin = BitmapFactory.decodeResource(getResources(), R.drawable.goblin);
		orc = BitmapFactory.decodeResource(getResources(), R.drawable.orc);
		fimir = BitmapFactory.decodeResource(getResources(), R.drawable.fimir);
		chaos = BitmapFactory.decodeResource(getResources(), R.drawable.chaos);
		gargoyle = BitmapFactory.decodeResource(getResources(), R.drawable.gargoyle);
		
		// Doors and Stairs
		ewdoorclosed = BitmapFactory.decodeResource(getResources(), R.drawable.ewdoorclosed);
		ewdooropen = BitmapFactory.decodeResource(getResources(), R.drawable.ewdooropen);
		nsdoorclosed = BitmapFactory.decodeResource(getResources(), R.drawable.nsdoorclosed);
		nsdooropen = BitmapFactory.decodeResource(getResources(), R.drawable.nsdooropen);
		stair_north = BitmapFactory.decodeResource(getResources(), R.drawable.stair_north);
		
		// Doodads
		altar_north = BitmapFactory.decodeResource(getResources(), R.drawable.altar_north);
		bookcase_north = BitmapFactory.decodeResource(getResources(), R.drawable.bookcase_north);
		desk_north = BitmapFactory.decodeResource(getResources(), R.drawable.desk_north);
		fireplace_north = BitmapFactory.decodeResource(getResources(), R.drawable.fireplace_north);
		table_north = BitmapFactory.decodeResource(getResources(), R.drawable.table_north);
		throne_north = BitmapFactory.decodeResource(getResources(), R.drawable.throne_north);
		tomb_north = BitmapFactory.decodeResource(getResources(), R.drawable.tomb_north);
		torture_rack_north = BitmapFactory.decodeResource(getResources(), R.drawable.torture_rack_north);
		weapon_rack_north = BitmapFactory.decodeResource(getResources(), R.drawable.weapon_rack_north);
		
		resize();
	}
	
	/** Manually unloads (recycles) the images to prevent an OOM error when resuming the game 
	 * 'quickly' after hitting back. */
	public void unloadImages(){
		Log.v(MISC,"Recycling images.");
		
		// TODO Naming.
		board.recycle();
		black.recycle();
		
		ewdoorclosed.recycle();
		ewdooropen.recycle();
		nsdoorclosed.recycle();
		nsdooropen.recycle();
		
		barbarian.recycle();
		dwarf.recycle();
		elf.recycle();
		wizard.recycle();
		
		skeleton.recycle();
		zombie.recycle();
		mummy.recycle();
		goblin.recycle();
		orc.recycle();
		fimir.recycle();
		chaos.recycle();
		gargoyle.recycle();
		
		stair_north.recycle();
		altar_north.recycle();
		bookcase_north.recycle();
		desk_north.recycle();
		fireplace_north.recycle();
		table_north.recycle();
		throne_north.recycle();
		tomb_north.recycle();
		torture_rack_north.recycle();
		weapon_rack_north.recycle();
	}
	
	// TODO Set objectives.
	
	
	/*********************** THREAD METHODS ********************************/
	public void run() {
		while(threadRunning == true){
			if(!mHolder.getSurface().isValid()){
				continue;
			}
			try{
				// TODO Implement custom frame rate.
				Thread.sleep(25);		// 40 fps.
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			
			finalCanvas = mHolder.lockCanvas();
			redraw(finalCanvas);
			mHolder.unlockCanvasAndPost(finalCanvas);
		}
	}
	
	public void pause(){
		threadRunning = false;
		while(true){
			try{
				t.join();
				unloadImages();
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			break;
		}
		t = null;
	}
	
	public void resume(){
		threadRunning = true;
		t = new Thread(this);
		t.start();
	}
	
	/************************ CUSTOM LISTENERS *************************/
	
	public interface OnGoldChangeListener {
		public abstract void onGoldChange(int toAdd);
	}
	
	public void setOnGoldChangeListener(OnGoldChangeListener ogcl){
		gListener = ogcl;
	}
	
	public interface OnStatChangeListener {
		public abstract void onBodyChange(int hero);
		
		public abstract void onMindChange(int hero);
		
		public abstract void onMoveChange(int hero, int distance);
	}
	
	public void setOnStatChangeListener(OnStatChangeListener oscl){
		sListener = oscl;
	}
	
	/************************ IMAGE METHODS ****************************/
	private void resize(){
		Log.v(BOARD,"Invoked Resize");
		board = Bitmap.createScaledBitmap(board, 
				MAX_X_TILES * tileSize, 
				MAX_Y_TILES * tileSize, false);
		//TODO This is bad... Need a manual x/y offset import for quests with grayed out left and top area.
		board = Bitmap.createBitmap(board,0, 2*tileSize, numXTiles*tileSize, numYTiles*tileSize);
		black = Bitmap.createScaledBitmap(black, tileSize, tileSize, false);
		
		ewdoorclosed = Bitmap.createScaledBitmap(ewdoorclosed, tileSize, (tileSize/6), false);
		ewdooropen = Bitmap.createScaledBitmap(ewdooropen, tileSize, ((tileSize*2)/3), false);
		nsdoorclosed = Bitmap.createScaledBitmap(nsdoorclosed, (tileSize/6), tileSize, false);
		nsdooropen = Bitmap.createScaledBitmap(nsdooropen, ((tileSize*2)/3), tileSize, false);
	
		barbarian = Bitmap.createScaledBitmap(barbarian, tileSize, tileSize, false); 	// tileSize * 8 is 8 directions.
		dwarf = Bitmap.createScaledBitmap(dwarf, tileSize, tileSize, false); 	// tileSize * 8 is 8 directions.
		elf = Bitmap.createScaledBitmap(elf, tileSize, tileSize, false); 	// tileSize * 8 is 8 directions.
		wizard = Bitmap.createScaledBitmap(wizard, tileSize, tileSize, false); 	// tileSize * 8 is 8 directions.
		
		// TODO Sprite sheet adjustments.
		/*barbarian = Bitmap.createScaledBitmap(barbarian, tileSize*MOVEMENT_FRAMES, tileSize*8, false); 	// tileSize * 8 is 8 directions.
		dwarf = Bitmap.createScaledBitmap(dwarf, tileSize*MOVEMENT_FRAMES, tileSize*8, false); 	// tileSize * 8 is 8 directions.
		elf = Bitmap.createScaledBitmap(elf, tileSize*MOVEMENT_FRAMES, tileSize*8, false); 	// tileSize * 8 is 8 directions.
		wizard = Bitmap.createScaledBitmap(wizard, tileSize*MOVEMENT_FRAMES, tileSize*8, false); 	// tileSize * 8 is 8 directions. */

		// TODO Naming conventions.
		skeleton = Bitmap.createScaledBitmap(skeleton, tileSize, tileSize, false);
		zombie = Bitmap.createScaledBitmap(zombie, tileSize, tileSize, false);
		mummy = Bitmap.createScaledBitmap(mummy, tileSize, tileSize, false);
		goblin = Bitmap.createScaledBitmap(goblin, tileSize, tileSize, false);
		orc = Bitmap.createScaledBitmap(orc, tileSize, tileSize, false);
		fimir = Bitmap.createScaledBitmap(fimir, tileSize, tileSize, false);
		chaos = Bitmap.createScaledBitmap(chaos, tileSize, tileSize, false);
		gargoyle = Bitmap.createScaledBitmap(gargoyle, tileSize, tileSize, false);
		
		
		stair_north = Bitmap.createScaledBitmap(stair_north, tileSize*3, tileSize*2, false);
		altar_north = Bitmap.createScaledBitmap(altar_north, tileSize*3, tileSize*2, false);
		bookcase_north = Bitmap.createScaledBitmap(bookcase_north, tileSize*3, tileSize, false);
		desk_north = Bitmap.createScaledBitmap(desk_north, tileSize*3, tileSize*2, false);
		fireplace_north = Bitmap.createScaledBitmap(fireplace_north, tileSize*3, tileSize, false);
		table_north = Bitmap.createScaledBitmap(table_north, tileSize*3, tileSize*2, false);
		throne_north = Bitmap.createScaledBitmap(throne_north, tileSize, tileSize, false);
		tomb_north = Bitmap.createScaledBitmap(tomb_north, tileSize*2, tileSize*3, false);
		torture_rack_north = Bitmap.createScaledBitmap(torture_rack_north, tileSize*2, tileSize*3, false);
		weapon_rack_north = Bitmap.createScaledBitmap(weapon_rack_north, tileSize*3, tileSize, false);
		
		rightEdge = (numXTiles*tileSize) - leftEdge;
		bottomEdge = (numYTiles*tileSize) - topEdge;
	}
	
	private void redraw(Canvas c){

		if(boardDrawing == false){									// Lock out redrawing if we're in the process of doing it. Maintains synchronous drawing.
			boardDrawing = true;
			
			c.drawARGB(255, 0, 0, 0);								// Clear the background.
			
			c.drawBitmap(board, leftEdge, topEdge, null);			// Draw the board itself.
			for(int y = 0; y < numYTiles; y++){
				for(int x = 0; x < numXTiles; x++){
					if(tileSet[y][x].isBlack == 1){
						tileSet[y][x].drawBlackSpace(c);
					}
					tileSet[y][x].drawDoors(c);
				}
			}

			for(int i = 0; i < heroSet.size(); i++){
				if(heroSet.get(i).isEnabled()){
					heroSet.get(i).redraw(c);
				}
			}

			for(int i = 0; i < monsterSet.size(); i++){
				monsterSet.get(i).redraw(c);
			}
			
			for(int i = 0; i < doodadSet.size(); i++){
				doodadSet.get(i).redraw(c);
			}
			
			boardDrawing = false;
		}
	} 
	
	/********************** HUMAN INTERACTION METHODS **********************/
	public void setClickPoint(float x, float y){
		isValid = true;
		
		touchedTile.xTile = getXTile(x);
		touchedTile.yTile = getYTile(y);
		wallTouched = getWallTouched(y, x);
		
		Log.v(BOARD, "Touched Tile: " + new Integer(touchedTile.xTile).toString() + " " + new Integer(touchedTile.yTile).toString());
		if(touchedTile.xTile > (numXTiles-1) || touchedTile.xTile < 0 || touchedTile.yTile > (numYTiles-1) || touchedTile.yTile < 0){
			Log.v(BOARD,"Not Valid");
			isValid = false;
		}else if(tileSet[touchedTile.yTile][touchedTile.xTile].isBlack == 1){
			Log.v(BOARD,"Not Valid");
			isValid = false;
		}
		
		if(isValid){
			
			if(wallTouched > -1){
				// Add a check to see if hero is in one of the tiles. Unless it's a wizard knock.
				// Also add code to close an open door. Stops monster movement.
				switch(wallTouched){
				case NORTH:
					if(tileSet[touchedTile.yTile][touchedTile.xTile].northWall == 3){
						Log.v(BOARD, "Door opened!");
						tileSet[touchedTile.yTile][touchedTile.xTile].northWall = 4;
						tileSet[touchedTile.yTile-1][touchedTile.xTile].southWall = 4;
					}
					break;
				case EAST:
					if(tileSet[touchedTile.yTile][touchedTile.xTile].eastWall == 3){
						Log.v(BOARD, "Door opened!");
						tileSet[touchedTile.yTile][touchedTile.xTile].eastWall = 4;
						tileSet[touchedTile.yTile][touchedTile.xTile+1].westWall = 4;
					}
					break;
					
				case SOUTH:
					if(tileSet[touchedTile.yTile][touchedTile.xTile].southWall == 3){
						Log.v(BOARD, "Door opened!");
						tileSet[touchedTile.yTile][touchedTile.xTile].southWall = 4;
						tileSet[touchedTile.yTile+1][touchedTile.xTile].northWall = 4;
					}
					break;
					
				case WEST:
					if(tileSet[touchedTile.yTile][touchedTile.xTile].westWall == 3){
						Log.v(BOARD, "Door opened!");
						tileSet[touchedTile.yTile][touchedTile.xTile].westWall = 4;
						tileSet[touchedTile.yTile][touchedTile.xTile-1].eastWall = 4;
					}
					break;
				}
			}
			
			
			// TODO It seems like there should be a much cleaner way of doing all of this...
			
			// Hero action check.
			if(isEnemyTurn == false){
				if(activeHero >= 0){
					for (int i = 0; i < heroCounter; i++){
						if(heroSet.get(i).action == SELECTED){	// A hero is selected. Find out what actions he's doing.
							if(heroSet.get(i).actionsRemaining > 0 && heroSet.get(i).movesLeft > 0){		// We can still move or attack.
								if(isSameTile(heroSet.get(i).existLoc,touchedTile)){		// Click the hero again to deselect him.
									Log.v(BOARD, "Hero Deselected");
									heroSet.get(i).action = INACTIVE;
									if(activeMonster > -1){
										monsterSet.get(activeMonster).action = INACTIVE;
										activeMonster = -1;
									}
									activeHero = -1;
									actionArmed = false;
									heroSet.get(i).createPath(touchedTile);
								}else{																		// We touched somewhere else. What is there?
									for(int j = 0; j < monsterCounter; j++){							// Was a monster there? Set him if so.
										if(heroSet.get(i).isHittable(monsterSet.get(j).existLoc)){
											if(isSameTile(monsterSet.get(j).existLoc, touchedTile)){
												Log.v(COMBAT,"There be a monster in range.");
												activeMonster = j;
											}
										}
									}


									if(actionArmed == true){												// Did we create a path or draw a death circle?
										if(activeMonster > -1){												// We drew a death circle.
											if(isSameTile(monsterSet.get(activeMonster).existLoc, touchedTile)){		// We clicked again. Attack him.
												// TODO Create a 'defending' animation as well?
												heroSet.get(i).action = ATTACKING;
												combatResult = heroSet.get(i).attack() - monsterSet.get(activeMonster).defend();
												if(combatResult > 0){												// Did we hit him?
													Log.v(BOARD, "Right between the eyes!");						// We hit.
													monsterSet.get(activeMonster).bodyCurrent -= combatResult;
													if(monsterSet.get(activeMonster).bodyCurrent <= 0){
														Log.v(COMBAT,"Oh he dead");
														// Add haptic buzz here for cool effect.
														monsterSet.get(activeMonster).action = DYING;
														monsterSet.remove(activeMonster); // This should get moved into board so the full animation is seen.
														monsterCounter--;
														activeMonster = -1;
														//TODO Update the loot system.
														gListener.onGoldChange((int)Math.round(Math.random()*25));
													}
												}else{																// We missed.
													Log.v(BOARD, "You hit like a pansy.");
												}
												heroSet.get(i).actionsRemaining--;
												activeMonster = -1;
												if(activeMonster > -1){
													monsterSet.get(activeMonster).action = INACTIVE;
													activeMonster = -1;
												}
												activeHero = -1;
												actionArmed = false;
											}
										}else{																			// There's no active monster, so we're moving.
											if(isSameTile(heroSet.get(i).destLoc,touchedTile)){		// We clicked our destination again, so move.
												Log.v(BOARD,"Roll out.");
												heroSet.get(i).action = MOVING;

												activeHero = -1;
												actionArmed = false;
											}else{																		// We had a path, but we want to make a new one.
												Log.v(BOARD,"Creating New Path.");
												heroSet.get(i).createPath(touchedTile);
												actionArmed = true;
												if(activeMonster > -1){
													monsterSet.get(activeMonster).action = INACTIVE;
													activeMonster = -1;
												}
											}
										}
									}else{																				// No action armed, what did we click above?
										if(activeMonster > -1){															// We clicked a monster and there was no death circle.
											Log.v(BOARD, "Draw the death circle.");
											// TODO Play Swords scraping sound.
											// TODO Update direction facing. For both parties? Fight prep!
											monsterSet.get(activeMonster).action = SELECTED;
										}else{																			// No monster, we want to find a path to move.
											if(tileSet[touchedTile.yTile][touchedTile.xTile].isStair == 1){	// Did we move to a stair? Check if game is complete.
												if(objectivesRemaining == 0){
													Log.v(BOARD,"Creating Path.");
													heroSet.get(i).createPath(touchedTile);
												}else{																	// We don't create a path if the heroes have more to go.
													Log.v(GAME,"You haven't completed your objectives.");				// Now if existLoc for the heroSet is on a stair, we
													Log.v(BOARD, "Hero Deselected");										// know we can end the round.
													heroSet.get(i).action = INACTIVE;
													activeHero = -1;
													actionArmed = false;
												}																		
											}else{																		// Create a fresh path.
												Log.v(BOARD,"Creating Path.");
												heroSet.get(i).createPath(touchedTile);
											}
										}
										actionArmed = true;
									}
								}
							}else{																// We were out of moves, so deselect and kill the action.																	
								Log.v(BOARD, "Hero Deselected");
								heroSet.get(i).action = INACTIVE;
								if(activeMonster > -1){
									monsterSet.get(activeMonster).action = INACTIVE;
									activeMonster = -1;
								}
								activeHero = -1;
								actionArmed = false;
							}
						}
					}
				}else{																			// No hero was selected. Select him.
					for (int i = 0; i < heroCounter; i++){
						if(heroSet.get(i).isEnabled()){
							if(heroSet.get(i).existLoc.xTile == touchedTile.xTile && heroSet.get(i).existLoc.yTile == touchedTile.yTile){
								Log.v(BOARD, "Hero Selected.");
								heroSet.get(i).action = SELECTED;
								activeHero = i;
							}
						}
					}
				}
			}
		}

		prevX = x;
		prevY = y;
	}

	public void moveBoard(float newX, float newY){
		if(boardDrawing == false){					// Don't let the board move if we're drawing it. It will correct in the next frame anyway.

			deltaX = newX - prevX;
			deltaY = newY - prevY;

			if(leftEdge >= 2*tileSize && deltaX > 0){
				leftEdge = 2*tileSize;
				rightEdge = (numXTiles + 2)*tileSize;
			}else if(rightEdge <= (getRight()-(2*tileSize)) && deltaX < 0){
				leftEdge = getRight() - (tileSize*(numXTiles+2));
				rightEdge = getRight() - 2*tileSize;
			}else{
				leftEdge += deltaX;
				rightEdge += deltaX;
			}

			if(topEdge >= 2*tileSize && deltaY > 0){
				topEdge = 2*tileSize;
				bottomEdge = (numYTiles+2)*tileSize;
			}else if(bottomEdge <= (getBottom()-(2*tileSize)) && deltaY < 0){
				topEdge = getBottom() - (tileSize*(numYTiles+2));
				bottomEdge = getBottom() - 2*tileSize;
			}else{
				topEdge += deltaY;
				bottomEdge += deltaY;
			}

			// We moved the board, so pixel locations of heroes have to update.
			for(int i = 0; i < heroCounter; i++){
				if(heroSet.get(i).isEnabled()){
					heroSet.get(i).updatePixel();
				}
			}

			// We moved the board, so pixel locations of monsters have to update.
			for(int i = 0; i < monsterCounter; i++){
				monsterSet.get(i).updatePixel();
			}

			// We moved the board, so pixel locations of doodads have to update.
			for(int i = 0; i < doodadCounter; i++){
				doodadSet.get(i).updatePixel();
			}

			prevX = newX;
			prevY = newY;
			
		}
	}
	
	// Returns the X tile touched given an input screen pixel touched.
	public int getXTile(float xPixel){
		return (int)Math.floor((xPixel-leftEdge) / tileSize);
	}
	// Returns the X tile touched given an input screen pixel touched.
	public int getYTile(float yPixel){
		return (int)Math.floor((yPixel-topEdge) / tileSize);
	}
	
	// Checks if a wall was touched instead of the tile. Returns -1 if not.
	// TODO Need to change the 8 hardcode to some factor of tileSize.
	public int getWallTouched(float yPixel, float xPixel){
		if((xPixel-leftEdge) % tileSize < 8){
			Log.v(BOARD,"Touched a west wall.");
			return WEST;
		}else if((xPixel-leftEdge) % tileSize > (tileSize - 8)){
			Log.v(BOARD,"Touched an east wall.");
			return EAST;
		}else if((yPixel-topEdge) % tileSize < 8){
			Log.v(BOARD,"Touched a north wall.");
			return NORTH;
		}else if((yPixel-topEdge) % tileSize > (tileSize - 8)){
			Log.v(BOARD,"Touched a south wall.");
			return SOUTH;
		}else{
			return -1;
		}
	}

	
	// Gets the heading direction between two tiles. Must be separated in one axis only!
	public int getDirection(Tile src, Tile dest){
				
		// TODO Update to allow all 8 directions?
		
		if (src.xTile < dest.xTile){
			return EAST;
		}else if(src.xTile > dest.xTile){
			return WEST;
		}else if(src.yTile < dest.yTile){
			return NORTH;
		}else if(src.yTile > dest.yTile){
			return SOUTH;
		}else{
			Log.v(MISC, "Direction was not correctly calculated.");
			return -1;
		}
	}
	
	// Checks to see if two tiles are at the same location.
	public boolean isSameTile(Tile left, Tile right){
		if((left.xTile == right.xTile) && (left.yTile == right.yTile)){
			return true;
		}else{
			return false;
		}
	}
	
	// Posts a toast of the roll to the screen. Left in Board for context access.
	public void makeRollToast(int type, int value, int y, int x){
		Toast rollToast;
		switch(type){			// 1 is attack, 2 is hero defend, 3 is monster defend.
		case(1):
			rollToast = Toast.makeText(getContext(), Integer.toString(value) + " Skulls", Toast.LENGTH_SHORT);
			break;
		case(2):
			rollToast = Toast.makeText(getContext(), Integer.toString(value) + " Hero Shields", Toast.LENGTH_SHORT);
			break;
		case(3):
			rollToast = Toast.makeText(getContext(), Integer.toString(value) + " Zargon Shields", Toast.LENGTH_SHORT);
			break;
		default:
			rollToast = Toast.makeText(getContext(), "Fail", Toast.LENGTH_SHORT);
		}
		rollToast.setGravity(Gravity.TOP|Gravity.LEFT, x, y);
		rollToast.show();
	}
	
	
	public class Tile {
		public int xTile, yTile;
		
		public int isTravelable, isHero, isMonster;			// Path building statistics.
		public int isBlack;					// Boolean ints for SQL.
		public Tile parent;
		public int gCost, hCost, fCost;
		
		
		
		// Walls may want to create more ints to define which direction the door opens as well...
		public int northWall;				// 0 = No wall. 1 = Wall. 2 = Map Edge. (For clipping card).
		public int eastWall;				// 3 = Closed Door. 4 = Open Door. 5 = Undiscovered Secret Door
		public int southWall;				// 6 = Discovered Closed Secret Door. 7 = Discovered Open Secret Door.
		public int westWall;

		
		public int room;					// Rooms must be numbered for monster movement calculations.
		public boolean isVisible;			// Is a hero in proximity of the tile to be able to see it?
		public int isStair;				// Stairways are an objective check.
		public int isTrap;
		
		public Tile(){
			xTile = yTile = 0;
			room = 0;
			isTravelable = 1;
			isHero = 0;
			isMonster = 0;
			isBlack = 0;
			isVisible = true;
			isStair = 0;
			isTrap = 0;
			northWall = eastWall = southWall = westWall = 0;
		}
		
		public Tile(int y, int x){
			xTile = x;
			yTile = y;
			room = 0;
			isTravelable = 1;
			isHero = 0;
			isMonster = 0;
			isBlack = 0;
			isVisible = true;
			isStair = 0;
			isTrap = 0;
			northWall = eastWall = southWall = westWall = 0;
		}
		
		// These calculate the current bounds of the tile on the fly.
		public float getTileLeftEdge(){
			return (leftEdge + (xTile * tileSize));			
		}
		public float getTileRightEdge(){
			return (leftEdge + ((xTile+1) * tileSize));
		}
		public float getTileTopEdge(){
			return (topEdge + (yTile * tileSize));
		}
		public float getTileBottomEdge(){
			return (topEdge + ((yTile+1) * tileSize));
		}
		
		public void drawBlackSpace(Canvas c){
			frame = Bitmap.createBitmap(black,0,0,tileSize,tileSize);
			c.drawBitmap(frame, getTileLeftEdge(), getTileTopEdge(), null);
		}
		
		public void drawDoors(Canvas c){
			switch(southWall){
			case 3:
				c.drawBitmap(ewdoorclosed, getTileLeftEdge(), getTileBottomEdge()-(tileSize/12), null);
				break;
			case 4:
				c.drawBitmap(ewdooropen, getTileLeftEdge(), getTileBottomEdge()-((2*tileSize)/3), null);
				break;
			}
			switch(eastWall){
			case 3:
				c.drawBitmap(nsdoorclosed, getTileRightEdge()-(tileSize/12), getTileTopEdge(), null);
				break;
			case 4:
				c.drawBitmap(nsdooropen, getTileRightEdge()-((2*tileSize)/3), getTileTopEdge(), null);
				break;
			}
		}
		
		public void setParent(Tile t){
			parent = t;
		}
	}
	
	/** Uses A* pathing algorithm to create an ArrayList of Tiles containing the most
	 * efficient path to the destination, assuming it is reachable. */
	private class Path {
		private ArrayList<Tile> pathList, openList, closedList;
		private boolean[][] onOpenList, onClosedList;
		public boolean stopPathing;
		private Tile currentTile, nextTile, finalDestination;
		private int wall, tempIndex;
		public int distance;
		
		public Path(Tile src, Tile dest){
			if(isSameTile(src,dest)){
				Log.v(PATH,"Path creation has same source and destination.");
				pathList = new ArrayList<Tile>();
				pathList.add(src);
			}else if(dest.isTravelable == 0){
				Log.v(PATH,"Destination is not travelable.");
				pathList = null;
			}else{
				pathList = new ArrayList<Tile>();
				openList = new ArrayList<Tile>();
				closedList = new ArrayList<Tile>();
				onOpenList = new boolean[numYTiles][numXTiles];
				onClosedList = new boolean[numYTiles][numXTiles];
				for(int i = 0; i < numYTiles; i++){
					for(int j = 0; j < numXTiles; j++){
						onOpenList[i][j] = false;
						onClosedList[i][j] = false;
					}
				}
				finalDestination = new Tile(dest.yTile, dest.xTile);

				openList.add(new Tile(src.yTile, src.xTile));
				Log.v(PATH,"Added " + Integer.toString(src.yTile) + " " + Integer.toString(src.xTile) + " to the open list.");

				stopPathing = false;
				while(stopPathing == false){
					buildPath(src, dest);

					if(openList.size() == 0){
						stopPathing = true;
						Log.v(PATH,"Destination is unreachable.");
						pathList = null;
						distance = 0;
					}
					if(findClosedIndex(finalDestination.yTile, finalDestination.xTile) >= 0){
						stopPathing = true;
						// Now find the target tile and march back its parents.
						currentTile = closedList.get(findClosedIndex(finalDestination.yTile, finalDestination.xTile));
						while(currentTile.parent != null){
							pathList.add(0,tileSet[currentTile.yTile][currentTile.xTile]);		// Auto-transpose.
							currentTile = closedList.get(findClosedIndex(currentTile.parent.yTile, currentTile.parent.xTile));
						}
						
						distance = pathList.size();
						printPath();
					}
				}


			}
		}
		
		private void buildPath(Tile src, Tile dest){
			// Find lowest fCost on list. Call it currentTile;
			currentTile = openList.get(0);
			for(int i = 0; i < openList.size(); i++){
				if(openList.get(i).fCost < currentTile.fCost){
					currentTile = openList.get(i);
				}
			}
			// Switch it to closed list.
			switchToClosedList(currentTile);


			// Check north.
			wall = tileSet[currentTile.yTile][currentTile.xTile].northWall;
			if((wall == 0 || wall == 4 || wall == 7) && (tileSet[currentTile.yTile-1][currentTile.xTile].isTravelable == 1) && onClosedList[currentTile.yTile-1][currentTile.xTile] == false){
				if(onOpenList[currentTile.yTile-1][currentTile.xTile] == false){
					addToOpenList(new Tile(currentTile.yTile-1,currentTile.xTile));
				}else{
					tempIndex = findOpenIndex(currentTile.yTile-1, currentTile.xTile);
					if(currentTile.gCost + 1 < openList.get(tempIndex).gCost){
						Log.v(PATH,"Found a lower G cost route.");
						openList.get(tempIndex).parent = currentTile;
						openList.get(tempIndex).gCost = currentTile.gCost + 1;
						openList.get(tempIndex).fCost = openList.get(tempIndex).gCost + getHCost(openList.get(tempIndex), finalDestination);
					}
				}
			}
			// Check east.
			wall = tileSet[currentTile.yTile][currentTile.xTile].eastWall;
			if((wall == 0 || wall == 4 || wall == 7) && (tileSet[currentTile.yTile][currentTile.xTile+1].isTravelable == 1) && onClosedList[currentTile.yTile][currentTile.xTile+1] == false){
				if(onOpenList[currentTile.yTile][currentTile.xTile+1] == false){
					addToOpenList(new Tile(currentTile.yTile,currentTile.xTile+1));
				}else{
					tempIndex = findOpenIndex(currentTile.yTile, currentTile.xTile+1);
					if(currentTile.gCost + 1 < openList.get(tempIndex).gCost){
						Log.v(PATH,"Found a lower G cost route.");
						openList.get(tempIndex).parent = currentTile;
						openList.get(tempIndex).gCost = currentTile.gCost + 1;
						openList.get(tempIndex).fCost = openList.get(tempIndex).gCost + getHCost(openList.get(tempIndex), finalDestination);
					}
				}
			}
			// Check south.
			wall = tileSet[currentTile.yTile][currentTile.xTile].southWall;
			if((wall == 0 || wall == 4 || wall == 7) && (tileSet[currentTile.yTile][currentTile.xTile+1].isTravelable == 1) && onClosedList[currentTile.yTile+1][currentTile.xTile] == false){
				if(onOpenList[currentTile.yTile+1][currentTile.xTile] == false){
					addToOpenList(new Tile(currentTile.yTile+1,currentTile.xTile));
				}else{
					tempIndex = findOpenIndex(currentTile.yTile+1, currentTile.xTile);
					if(currentTile.gCost + 1 < openList.get(tempIndex).gCost){
						Log.v(PATH,"Found a lower G cost route.");
						openList.get(tempIndex).parent = currentTile;
						openList.get(tempIndex).gCost = currentTile.gCost + 1;
						openList.get(tempIndex).fCost = openList.get(tempIndex).gCost + getHCost(openList.get(tempIndex), finalDestination);
					}
				}
			}
			// Check west.
			wall = tileSet[currentTile.yTile][currentTile.xTile].westWall;
			if((wall == 0 || wall == 4 || wall == 7) && (tileSet[currentTile.yTile][currentTile.xTile+1].isTravelable == 1) && onClosedList[currentTile.yTile][currentTile.xTile-1] == false){
				if(onOpenList[currentTile.yTile][currentTile.xTile-1] == false){
					addToOpenList(new Tile(currentTile.yTile,currentTile.xTile-1));
				}else{
					tempIndex = findOpenIndex(currentTile.yTile, currentTile.xTile-1);
					if(currentTile.gCost + 1 < openList.get(tempIndex).gCost){
						Log.v(PATH,"Found a lower G cost route.");
						openList.get(tempIndex).parent = currentTile;
						openList.get(tempIndex).gCost = currentTile.gCost + 1;
						openList.get(tempIndex).fCost = openList.get(tempIndex).gCost + getHCost(openList.get(tempIndex), finalDestination);
					}
				}
			}
		}
		
		
		private void switchToClosedList(Tile t){
			closedList.add(t);
			onClosedList[t.yTile][t.xTile] = true;
			onOpenList[t.yTile][t.xTile] = false;
			openList.remove(t);
			Log.v(PATH,"Switched " + Integer.toString(t.yTile) + " " + Integer.toString(t.xTile) + " to the closed list from the open list.");
		}
		
		
		private void addToOpenList(Tile t){
			t.parent = currentTile;
			t.gCost = t.parent.gCost + 1;
			t.hCost = getHCost(t, finalDestination);
			t.fCost = t.gCost + t.hCost;
			openList.add(t);
			onOpenList[t.yTile][t.xTile] = true;
			Log.v(PATH,"Added " + Integer.toString(t.yTile) + " " + Integer.toString(t.xTile) + " to the open list with parent " + Integer.toString(t.parent.yTile) + " " + Integer.toString(t.parent.xTile) + ".");
			Log.v(PATH,"G -> " + Integer.toString(t.gCost)+ " H-> " + Integer.toString(t.hCost) + " F-> " + Integer.toString(t.fCost));
		}
		
		private int findOpenIndex(int y, int x){
			for(int i = 0; i < openList.size(); i++){
				if(openList.get(i).yTile == y && openList.get(i).xTile == x){
					return i;
				}
			}
			return -1;
		}
		
		private int findClosedIndex(int y, int x){
			for(int i = 0; i < closedList.size(); i++){
				if(closedList.get(i).yTile == y && closedList.get(i).xTile == x){
					return i;
				}
			}
			return -1;
		}
		
		
		// Manhattan heuristic.
		private int getHCost(Tile src, Tile dest){
			return Math.abs(src.xTile - dest.xTile) + Math.abs(src.yTile - dest.yTile);
		}
		
		public void printPath(){
			for(int i = 0; i < pathList.size(); i++){
				Log.v(PATH,"Step " + Integer.toString(i) + " -> " + Integer.toString(pathList.get(i).yTile) + " " + Integer.toString(pathList.get(i).xTile) + ".");
			}
		}
		
		public Tile travelToNextTile(){
			nextTile = pathList.get(0);
			Log.v(PATH,"Moving to " + Integer.toString(nextTile.xTile) + " " + Integer.toString(nextTile.yTile));
			pathList.remove(0);
			distance--;
			return tileSet[nextTile.yTile][nextTile.xTile];
		}
		
		public Tile getTileAtIndex(int index){
			return pathList.get(index);
		}
		
		public void removeTileAtIndex(int index){
			pathList.remove(index);
			distance--;
		}
		
		public boolean pathExists(){
			if(pathList == null){
				return false;
			}else{
				return true;
			}
		}
		
		public int length(){
			return pathList.size();
		}
	}
	
	public class Unit {
		
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
		
		// Monster classes.
		private static final int SKELETON = 1;
		private static final int ZOMBIE = 2;
		private static final int MUMMY = 3;
		private static final int GOBLIN = 4;
		private static final int ORC = 5;
		private static final int FIMIR = 6;
		private static final int CHAOS = 7;
		private static final int GARGOYLE = 8;
		private static final int UNIQUE = 9;
		
		// Doodad classes.
		private static final int STAIR = 0;
		private static final int ALTAR = 1;
		private static final int BOOKCASE = 2;
		private static final int DESK = 3;
		private static final int FIREPLACE = 4;
		private static final int TABLE = 5;
		private static final int THRONE = 6;
		private static final int TOMB = 7;
		private static final int TORTURE_RACK = 8;
		private static final int WEAPON_RACK = 9;
		
		// Instantiation variables.
		public int unitType, unitClass;
		public int isEnabled; 		// Must be an int for storing in SQL. (Traveling must also be).
		
		// Unit state.
		public int room, bodyCurrent, bodyMax, mindCurrent, mindMax, movesLeft, moveDice, attackDice, defendDice, actionsRemaining, actionsPerRound;
		// Amount of gold to gain when a monster is killed.
		public int lootMin, lootMax;
		
		// Unit drawing.
		public Path path;
		public int action, direction;
		public float xPixel, yPixel;
		public int frameIndex, spriteIndex;
		public int traveling;
		public Tile existLoc, destLoc;
		
		private Paint green, gray, red;
		
		// Variables used by roll methods.
		private int rollCounter;
		private float rollValue;
		
		public Unit(int uType, int uClass){
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
				switch(unitClass){
				case(SKELETON):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 0;
					attackDice = 2;
					defendDice = 2;
					movesLeft = 6;
					break;
				case(ZOMBIE):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 0;
					attackDice = 2;
					defendDice = 3;
					movesLeft = 4;
					break;
				case(MUMMY):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 0;
					attackDice = 3;
					defendDice = 4;
					movesLeft = 4;
					break;
				case(GOBLIN):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 1;
					attackDice = 2;
					defendDice = 1;
					movesLeft = 10;
					break;
				case(ORC):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 2;
					attackDice = 3;
					defendDice = 2;
					movesLeft = 8;
					break;
				case(FIMIR):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 3;
					attackDice = 3;
					defendDice = 3;
					movesLeft = 6;
					break;
				case(CHAOS):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 3;
					attackDice = 3;
					defendDice = 4;
					movesLeft = 6;
					break;
				case(GARGOYLE):
					bodyCurrent = bodyMax = 1;
					mindCurrent = mindMax = 4;
					attackDice = 4;
					defendDice = 4;
					movesLeft = 6;
					break;
				}
				break;
			case DOODAD:
				break;
			}
			isEnabled = 1;
			
			green = new Paint();
			green.setColor(Color.GREEN);
			green.setStyle(Paint.Style.FILL);
			
			red = new Paint();
			red.setColor(Color.RED);
			red.setStyle(Paint.Style.FILL);
			
			gray = new Paint();
			gray.setColor(Color.GRAY);
			gray.setStyle(Paint.Style.FILL);
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
		
		public void spawnUnit(int y, int x, int dir, int r, int a){
			room = r;
			existLoc = new Tile(y, x);
			destLoc = new Tile(y, x);
			direction = dir;
			action = a;
			tileSet[y][x].isHero = 0;
			xPixel = existLoc.getTileLeftEdge();
			yPixel = existLoc.getTileTopEdge();
			path = new Path(existLoc, existLoc);		// Generic 0 distance path.
			traveling = 0;
			frameIndex = spriteIndex = 0;
			if(unitType == HERO){
				existLoc.isHero = 1;
			}else if(unitType == MONSTER){
				existLoc.isMonster = 1;
			}
		}
		
		
		public void redraw(Canvas c){
			switch(action){ 			// Animation to draw is based on what action in progress.
			case(MOVING):				// Movement animation.
				if(traveling == 1){
					/* Traveling from one tile to the next involves moving a fixed percentage of the 
					 * distance between the respective left or top edges of each tile involved to ensure
					 * robustness in computing the correct pixel location since many things can affect
					 * the pixel location at assymetric times.
					 */
					switch(direction){
					case NORTH:
						yPixel = existLoc.getTileTopEdge() + ((frameIndex*tileSize)/MOVEMENT_FRAMES);
						break;
					case EAST:
						xPixel = existLoc.getTileLeftEdge() + ((frameIndex*tileSize)/MOVEMENT_FRAMES);
						break;
					case SOUTH:
						yPixel = existLoc.getTileTopEdge() - ((frameIndex*tileSize)/MOVEMENT_FRAMES);
						break;
					case WEST:
						xPixel = existLoc.getTileLeftEdge() - ((frameIndex*tileSize)/MOVEMENT_FRAMES);
						break;
					}
					// Check if we're done traveling to the next tile.
					if(frameIndex++ == MOVEMENT_FRAMES){
						if(unitType == HERO){
							tileSet[existLoc.yTile][existLoc.xTile].isHero = 1;	// No more object, can travel there.
							existLoc = destLoc;
							tileSet[existLoc.yTile][existLoc.xTile].isHero = 0;	// Object now here, no travel.
						}else if(unitType == MONSTER){
							tileSet[existLoc.yTile][existLoc.xTile].isMonster = 1;	// No more object, can travel there.
							existLoc = destLoc;
							tileSet[existLoc.yTile][existLoc.xTile].isMonster = 0;	// Object now here, no travel.
						}
						room = tileSet[existLoc.yTile][existLoc.xTile].room;
						frameIndex = 0;
						if(path.distance > 0 && movesLeft > 0){ 	// More to travel?
							destLoc = path.travelToNextTile();
							direction = getDirection(existLoc, destLoc);
							movesLeft--;
							//TODO
							//sListener.onMoveChange(unitClass);
							//onMove(unitType);
							Log.v(BOARD,Integer.toString(movesLeft) + " moves left this round.");
						}else{  									// Done traveling the path.
							path.distance = 0;
							traveling = 0;		
						}
					}
				}else{
					if(path.distance > 0 && movesLeft > 0){
						destLoc = path.travelToNextTile();
						direction = getDirection(existLoc, destLoc);
						movesLeft--;
						//TODO
						//onMove(unitType);
						//sListener.onMoveChange(unitClass);
						Log.v(BOARD,Integer.toString(movesLeft) + " moves left this round.");
						traveling = 1;
					}else{
						action = INACTIVE;
						spriteIndex = 0;
					}
				}
				spriteIndex = ++spriteIndex % MOVEMENT_SPRITESHEET_WIDTH;
				break;
			default:
				frameIndex = 0;
				spriteIndex = 0;
				break;
			}
			
			// Highlight the active hero or targetted monster.
			if(action == SELECTED){
				if(unitType == HERO){
					c.drawLine(existLoc.getTileLeftEdge(), existLoc.getTileTopEdge(), existLoc.getTileRightEdge(), existLoc.getTileTopEdge(), gray);
					c.drawLine(existLoc.getTileRightEdge(), existLoc.getTileTopEdge(), existLoc.getTileRightEdge(), existLoc.getTileBottomEdge(), gray);
					c.drawLine(existLoc.getTileLeftEdge(), existLoc.getTileBottomEdge(), existLoc.getTileRightEdge(), existLoc.getTileBottomEdge(), gray);
					c.drawLine(existLoc.getTileLeftEdge(), existLoc.getTileTopEdge(), existLoc.getTileLeftEdge(), existLoc.getTileBottomEdge(), gray);
				}else if(unitType == MONSTER){
					c.drawLine(existLoc.getTileLeftEdge(), existLoc.getTileTopEdge(), existLoc.getTileRightEdge(), existLoc.getTileTopEdge(), red);
					c.drawLine(existLoc.getTileRightEdge(), existLoc.getTileTopEdge(), existLoc.getTileRightEdge(), existLoc.getTileBottomEdge(), red);
					c.drawLine(existLoc.getTileLeftEdge(), existLoc.getTileBottomEdge(), existLoc.getTileRightEdge(), existLoc.getTileBottomEdge(), red);
					c.drawLine(existLoc.getTileLeftEdge(), existLoc.getTileTopEdge(), existLoc.getTileLeftEdge(), existLoc.getTileBottomEdge(), red);
				}
			}
			
			// TODO Make this drawing much nicer.
			// Should draw the full path to final destination and then make only the ones he will actually travel turn green or animate.
			if(path.pathExists() && path.distance > 0 && movesLeft > 0){
				//if(movesLeft > path.distance){
					// Draw from center of character to correct edge.
					c.drawLine(xPixel+(tileSize/2), yPixel+(tileSize/2), path.getTileAtIndex(0).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(0).getTileTopEdge()+(tileSize/2),green);

					// Draw the line accross the tile based on direction.
					if(path.distance > 2){
						for(int i = 0; i < path.distance-2; i++){
							if(i > movesLeft){
								c.drawLine(path.getTileAtIndex(i).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(i).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(i+1).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(i+1).getTileTopEdge()+(tileSize/2), gray);
							}else{
								c.drawLine(path.getTileAtIndex(i).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(i).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(i+1).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(i+1).getTileTopEdge()+(tileSize/2), green);
							}
						}
						if(path.distance > movesLeft){
							switch(getDirection(path.getTileAtIndex(path.distance-2),path.getTileAtIndex(path.distance-1))){
							case NORTH:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileBottomEdge(), gray);
								break;
							case EAST:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileRightEdge(), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2), gray);
								break;
							case SOUTH:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge(), gray);
								break;
							case WEST:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileLeftEdge(), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2), gray);
								break;
							}
						}else{
							switch(getDirection(path.getTileAtIndex(path.distance-2),path.getTileAtIndex(path.distance-1))){
							case NORTH:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileBottomEdge(), green);
								break;
							case EAST:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileRightEdge(), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2), green);
								break;
							case SOUTH:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge(), green);
								break;
							case WEST:
								c.drawLine(path.getTileAtIndex(path.distance-2).getTileLeftEdge()+(tileSize/2), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2),	path.getTileAtIndex(path.distance-2).getTileLeftEdge(), path.getTileAtIndex(path.distance-2).getTileTopEdge()+(tileSize/2), green);
								break;
							}
						}
					}

					//Draw a square around path final destination or end of moves.
					if(path.distance > movesLeft){
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), gray);
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), gray);
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), gray);
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), gray);
					}else{
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), green);
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), green);
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), path.getTileAtIndex(path.distance-1).getTileRightEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), green);
						c.drawLine(path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileTopEdge(), path.getTileAtIndex(path.distance-1).getTileLeftEdge(), path.getTileAtIndex(path.distance-1).getTileBottomEdge(), green);
					}
			}
			
			
			switch(unitType){
			case(HERO):
				switch(unitClass){
				case(BARBARIAN):
					//frame = Bitmap.createBitmap(barbarian,spriteIndex*tileSize,direction*tileSize,tileSize,tileSize);
					frame = Bitmap.createBitmap(barbarian,0,0,tileSize,tileSize);
				break;
				case(DWARF):
					//frame = Bitmap.createBitmap(dwarf,spriteIndex*tileSize,direction*tileSize,tileSize,tileSize);
					frame = Bitmap.createBitmap(dwarf,0,0,tileSize,tileSize);
				break;
				case(ELF):
					//frame = Bitmap.createBitmap(elf,spriteIndex*tileSize,direction*tileSize,tileSize,tileSize);
					frame = Bitmap.createBitmap(elf,0,0,tileSize,tileSize);
				break;
				case(WIZARD):
					//frame = Bitmap.createBitmap(wizard,spriteIndex*tileSize,direction*tileSize,tileSize,tileSize);
					frame = Bitmap.createBitmap(wizard,0,0,tileSize,tileSize);
				break;
				}
			break;
			case(MONSTER):
				switch(unitClass){
				case(SKELETON):
					frame = Bitmap.createBitmap(skeleton,0,0,tileSize,tileSize);
					break;
				case(ZOMBIE):					
					frame = Bitmap.createBitmap(zombie,0,0,tileSize,tileSize);
					break;
				case(MUMMY):
					frame = Bitmap.createBitmap(mummy,0,0,tileSize,tileSize);
					break;
				case(GOBLIN):
					frame = Bitmap.createBitmap(goblin,0,0,tileSize,tileSize);
					break;
				case(ORC):
					frame = Bitmap.createBitmap(orc,0,0,tileSize,tileSize);
					break;
				case(FIMIR):
					frame = Bitmap.createBitmap(fimir,0,0,tileSize,tileSize);
					break;
				case(CHAOS):
					frame = Bitmap.createBitmap(chaos,0,0,tileSize,tileSize);
					break;
				case(GARGOYLE):
					frame = Bitmap.createBitmap(gargoyle,0,0,tileSize,tileSize);
					break;
				case(UNIQUE):
					// TODO Unique graphic import.
					frame = Bitmap.createBitmap(gargoyle,0,0,tileSize,tileSize);
					break;
				}
				
				break;
			case(DOODAD):
				switch(unitClass){
				// TODO Height and width
				case(STAIR):
					frame = Bitmap.createBitmap(stair_north,0,0,tileSize,tileSize);
					break;
				case(ALTAR):					
					frame = Bitmap.createBitmap(altar_north,0,0,tileSize,tileSize);
					break;
				case(BOOKCASE):
					frame = Bitmap.createBitmap(bookcase_north,0,0,tileSize,tileSize);
					break;
				case(DESK):
					frame = Bitmap.createBitmap(desk_north,0,0,tileSize,tileSize);
					break;
				case(FIREPLACE):
					frame = Bitmap.createBitmap(fireplace_north,0,0,tileSize,tileSize);
					break;
				case(TABLE):
					frame = Bitmap.createBitmap(table_north,0,0,tileSize,tileSize);
					break;
				case(THRONE):
					frame = Bitmap.createBitmap(throne_north,0,0,tileSize,tileSize);
					break;
				case(TOMB):
					frame = Bitmap.createBitmap(tomb_north,0,0,tileSize,tileSize);
					break;
				case(TORTURE_RACK):
					frame = Bitmap.createBitmap(torture_rack_north,0,0,tileSize,tileSize);
					break;
				case(WEAPON_RACK):
					frame = Bitmap.createBitmap(weapon_rack_north,0,0,tileSize,tileSize);
					break;
				}
				break;
			}
			c.drawBitmap(frame, xPixel, yPixel, null);
		}
		
		// A refresh any time we move anything.
		public void updatePixel(){
			xPixel = existLoc.getTileLeftEdge();
			yPixel = existLoc.getTileTopEdge();
		}
		
		// This might get moved to the Board level since Monsters need it too.
		public void createPath(Tile destTile){
			path = new Path(existLoc, destTile);
			destLoc = new Tile(destTile.yTile, destTile.xTile);
			//TODO May want to update this to isolate between actually moving and planning to move.
			sListener.onMoveChange(unitClass, path.distance);
			//path.printPath();
		}
		
		public boolean pathExists(){
			if(path == null){
				return false;
			}else{
				return true;
			}
		}
		
		// Determines if a monster clicked is within striking range of the active hero.
		public boolean isHittable(Tile t){
			switch(unitType){
			case HERO:
				switch(unitClass){
				case BARBARIAN: case DWARF: case ELF:
					if(existLoc.xTile == t.xTile && (Math.abs(t.yTile-existLoc.yTile) < 2)){
						return true;
					}else if(existLoc.yTile == t.yTile && (Math.abs(t.xTile-existLoc.xTile) < 2)){
						return true;
					}else{
						return false;
					}
				case WIZARD:
					if(Math.abs(existLoc.yTile - t.yTile) < 2 && Math.abs(existLoc.xTile - t.xTile) < 2){
						return true;
					}else{
						return false;
					}
				}
			case MONSTER:
				if(existLoc.xTile == t.xTile && (Math.abs(t.yTile-existLoc.yTile) < 2)){
					return true;
				}else if(existLoc.yTile == t.yTile && (Math.abs(t.xTile-existLoc.xTile) < 2)){
					return true;
				}else{
					return false;
				}
			}
			return false;
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
			makeRollToast(1, rollCounter, Math.round(yPixel), Math.round(xPixel));
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
			if(unitType == HERO){
				makeRollToast(2, rollCounter, Math.round(yPixel), Math.round(xPixel));
			}else if(unitType == MONSTER){
				makeRollToast(3, rollCounter, Math.round(yPixel), Math.round(xPixel));
			}
			return rollCounter;
		}
		
		public int refreshMoves(){
			rollCounter = 0;
			for(int i = 0; i < moveDice; i++){
				rollCounter += Math.ceil((float)Math.random() * 6);
			}
			return rollCounter;
		}
	}

	
	// TODO Continuous AI Improvement.
	public void moveZargon() {
		Log.v(GAME,"The gremlins are creeping...");
		// Determine which monsters share a room with the heroes. Upgrade to just check if a path exists
		// to the heroes using A*. Anything with closed door makes impossible anyway... Cool!

		// May want to put a statement here that waits until all things are inactive so that monsters go one at a time.
		isEnemyTurn = true;
		
		// TODO Lots of things with the AI.
		for(int i = 0; i < monsterCounter; i++){
			int closestHero=-1;
			int closestDistance=256;
			for(int j = 0; j < heroCounter; j++){
				Log.v(GAME, "Hero " + Integer.toString(j) + " is in room " + Integer.toString(heroSet.get(j).room));
				if(monsterSet.get(i).room == heroSet.get(j).room && heroSet.get(j).isEnabled()){
					Log.v(GAME,"Monster " + Integer.toString(i) + " is in same room as hero " + Integer.toString(j));
					monsterSet.get(i).createPath(heroSet.get(j).existLoc);
					if(monsterSet.get(i).pathExists() == false){
						Log.v(GAME,"No path!");
					}else{
						if(monsterSet.get(i).path.distance < closestDistance){
							closestHero = j;
							closestDistance = monsterSet.get(i).path.distance;
						}
					}
				}
				
			}
			// Add && closestDistance < 5 or 6... maybe 10... Things shouldn't rush them right away in the hallway.
			// Maybe an activation flag.
			if(closestHero >= 0){
				if(monsterSet.get(i).isHittable(heroSet.get(closestHero).existLoc) == true){
					monsterSet.get(i).action = ATTACKING;
					combatResult = monsterSet.get(i).attack() - heroSet.get(closestHero).defend();
					if(combatResult > 0){	
						// Did he hit him?
						//TODO Play sword sound.
						
						heroSet.get(closestHero).bodyCurrent -= combatResult; // We hit.
						sListener.onBodyChange(closestHero);
						
						Log.v(GAME, "Ouch! " + Integer.toString(heroSet.get(closestHero).bodyCurrent) + " body points left.");	
						if(heroSet.get(closestHero).bodyCurrent <= 0){
							Log.v(COMBAT,"Oh I dead");
							// Add haptic buzz here for cool effect.
							heroSet.get(closestHero).action = DYING;
							heroSet.remove(closestHero);	// This should get moved into board so the full animation is seen.
							heroCounter--;
						}
					}else{		  // We missed.
						//Play shield clunk or whif sound.
						Log.v(COMBAT, "He hit like a pansy.");
					}
				}else{
					Log.v(GAME,"Monster " + Integer.toString(i) + " is moving to hero " + Integer.toString(closestHero));
					monsterSet.get(i).createPath(heroSet.get(closestHero).existLoc);
					monsterSet.get(i).path.removeTileAtIndex(monsterSet.get(i).path.distance-1);
					monsterSet.get(i).action = MOVING;
				}
			}
		}
		
		isEnemyTurn = false;
	}
}
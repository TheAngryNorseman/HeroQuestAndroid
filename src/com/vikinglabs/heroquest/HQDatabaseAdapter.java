package com.vikinglabs.heroquest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/** The database management class used for all of HeroQuest's persistent data. It is responsible for all CRUD operations
 * on the various tables used. Tables in the database fall into two main categories: those that maintain game progression
 * (such as quest and achievement completions), and those that save the game state when the activity is brought out of
 * focus (such as board state). The former category is populated by file reads only on the first instance of the 
 * application being run on the system, as well as whenever a version update is encountered. The latter is populated
 * entirely by application variables during game operation.
 * 
 * @author Odin */
public class HQDatabaseAdapter {

	// Debug filters.
	private static final String SQL = "HQSQL";
	
	// Database information.
	public static final String DATABASE_NAME = "HQ_DATABASE";
	public static final int DATABASE_VERSION = 1;
	
	// Quest table finals.
	/** The table listing all the quests and their completion progress. */
	public static final String QUEST_TABLE = "QUEST_TABLE";
	public static final String QUEST_COL_ID = "_id";
	public static final String QUEST_COL_TAG = "Tag";
	public static final String QUEST_COL_TITLE = "Title";
	public static final String QUEST_COL_DESCRIPTION = "Description";
	public static final String QUEST_COL_STORY = "Story";
	public static final String QUEST_COL_AVAILABLE = "Available";
	public static final String QUEST_COL_DIFFICULTY_COMPLETED = "DifficultyCompleted";
	public static final int QUEST_ID_COLUMN_OFFSET = 0;
	public static final int QUEST_TAG_COLUMN_OFFSET = 1;
	public static final int QUEST_TITLE_COLUMN_OFFSET = 2;
	public static final int QUEST_DESCRIPTION_COLUMN_OFFSET = 3;
	public static final int QUEST_STORY_COLUMN_OFFSET = 4;
	public static final int QUEST_AVAILABLE_COLUMN_OFFSET = 5;
	public static final int QUEST_DIFFICULTY_COLUMN_OFFSET = 6;
	
	private static final String SCRIPT_CREATE_QUEST_TABLE =
			"CREATE TABLE " + QUEST_TABLE + " ("
					+ QUEST_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ QUEST_COL_TAG + " TEXT NOT NULL, "
					+ QUEST_COL_TITLE + " TEXT NOT NULL, "
					+ QUEST_COL_DESCRIPTION + " TEXT NOT NULL, "
					+ QUEST_COL_STORY + " TEXT NOT NULL, "
					+ QUEST_COL_AVAILABLE + " INTEGER, "
					+ QUEST_COL_DIFFICULTY_COMPLETED + " TEXT NOT NULL);";
	
	// Objective table finals.
	/** The table listing all the quest objectives and their completion progress. This is isolated 
	 * from the quest table in order to support dynamic numbers of objectives for each quest. */
	public static final String OBJECTIVE_TABLE = "OBJECTIVE_TABLE";
	public static final String OBJECTIVE_ID = "_id";
	public static final String OBJECTIVE_QUEST_NUMBER = "QuestNumber";
	public static final String OBJECTIVE_DESCRIPTION = "Description";
	public static final String OBJECTIVE_OPTIONAL = "Optional";
	public static final int OBJECTIVE_ID_COLUMN_OFFSET = 0;
	public static final int OBJECTIVE_QUEST_NUMBER_COLUMN_OFFSET = 1;
	public static final int OBJECTIVE_DESCRIPTION_COLUMN_OFFSET = 2;
	public static final int OBJECTIVE_OPTIONAL_COLUMN_OFFSET = 3;
	
	private static final String SCRIPT_CREATE_OBJECTIVE_TABLE =
			"CREATE TABLE " + OBJECTIVE_TABLE + " ("
					+ OBJECTIVE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ OBJECTIVE_QUEST_NUMBER + " INTEGER, "
					+ OBJECTIVE_DESCRIPTION + " TEXT NOT NULL,"
					+ OBJECTIVE_OPTIONAL + " INTEGER);";
	
	// Achievement table finals.
	/** The table listing all the achievements and their completion progress */
	public static final String ACHIEVEMENT_TABLE = "ACHIEVEMENT_TABLE";
	public static final String ACHIEVEMENT_COL_ID = "_id";
	public static final String ACHIEVEMENT_COL_TITLE = "Title";
	public static final String ACHIEVEMENT_COL_DESCRIPTION = "Description";
	public static final String ACHIEVEMENT_COL_ACHIEVED = "Achieved";
	public static final String ACHIEVEMENT_COL_DATE = "Date";
	public static final int ACHIEVEMENT_ID_COLUMN_OFFSET = 0;
	public static final int ACHIEVEMENT_TITLE_COLUMN_OFFSET = 1;
	public static final int ACHIEVEMENT_DESCRIPTION_COLUMN_OFFSET = 2;
	public static final int ACHIEVEMENT_ACHIEVED_COLUMN_OFFSET = 3;
	public static final int ACHIEVEMENT_DATE_COLUMN_OFFSET = 4;
	
	private static final String SCRIPT_CREATE_ACHIEVEMENT_TABLE =
			"CREATE TABLE " + ACHIEVEMENT_TABLE + " ("
					+ ACHIEVEMENT_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ ACHIEVEMENT_COL_TITLE + " TEXT NOT NULL, "
					+ ACHIEVEMENT_COL_DESCRIPTION + " TEXT NOT NULL, "
					+ ACHIEVEMENT_COL_ACHIEVED + " INTEGER, "
					+ ACHIEVEMENT_COL_DATE + " TEXT NOT NULL);";
	
	
	// Board table finals.
	/** The table containing the state of all tiles on the board. */
	public static final String TILE_TABLE = "TILE_TABLE";
	public static final String TILE_COL_ID = "_id";
	public static final String TILE_COL_X = "X";
	public static final String TILE_COL_Y = "Y";
	public static final String TILE_COL_NORTH_WALL = "NorthWall";
	public static final String TILE_COL_EAST_WALL = "EastWall";
	public static final String TILE_COL_SOUTH_WALL = "SouthWall";
	public static final String TILE_COL_WEST_WALL = "WestWall";
	public static final String TILE_COL_TRAP = "TrapType";
	public static final String TILE_COL_ROOM = "Room";
	public static final String TILE_COL_TRAVELABLE = "Travellable";
	public static final String TILE_COL_BLACK = "Black";
	public static final String TILE_COL_STAIR = "Stair";
	public static final int TILE_ID_COLUMN_OFFSET = 0;
	public static final int TILE_X_COLUMN_OFFSET = 1;
	public static final int TILE_Y_COLUMN_OFFSET = 2;
	public static final int TILE_NORTH_WALL_COLUMN_OFFSET = 3;
	public static final int TILE_EAST_WALL_COLUMN_OFFSET = 4;
	public static final int TILE_SOUTH_WALL_COLUMN_OFFSET = 5;
	public static final int TILE_WEST_WALL_COLUMN_OFFSET = 6;
	public static final int TILE_TRAP_COLUMN_OFFSET = 7;
	public static final int TILE_ROOM_COLUMN_OFFSET = 8;
	public static final int TILE_TRAVELABLE_COLUMN_OFFSET = 9;
	public static final int TILE_BLACK_COLUMN_OFFSET = 10;
	public static final int TILE_STAIR_COLUMN_OFFSET = 11;
	
	private static final String SCRIPT_CREATE_TILE_TABLE =
			"CREATE TABLE " + TILE_TABLE + " ("
					+ TILE_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ TILE_COL_X + " INTEGER, "
					+ TILE_COL_Y + " INTEGER, "
					+ TILE_COL_NORTH_WALL + " INTEGER, "
					+ TILE_COL_EAST_WALL + " INTEGER, "
					+ TILE_COL_SOUTH_WALL + " INTEGER, "
					+ TILE_COL_WEST_WALL + " INTEGER, "
					+ TILE_COL_TRAP + " INTEGER, "
					+ TILE_COL_ROOM + " INTEGER, "
					+ TILE_COL_TRAVELABLE + " INTEGER, "
					+ TILE_COL_BLACK + " INTEGER, "
					+ TILE_COL_STAIR + " INTEGER);";
	

	// Unit table finals.
	/** The table containing the state of all the units (heroes, monsters and doodads) on the board. */
	public static final String UNIT_TABLE = "UNIT_TABLE";
	public static final String UNIT_COL_ID = "_id";
	public static final String UNIT_COL_TYPE = "Type";
	public static final String UNIT_COL_CLASS = "Class";
	public static final String UNIT_COL_ENABLED = "Enabled";
	public static final String UNIT_COL_BODY_CURRENT = "CurrentBody";
	public static final String UNIT_COL_BODY_MAX = "MaxBody";
	public static final String UNIT_COL_MIND_CURRENT = "CurrentMind";
	public static final String UNIT_COL_MIND_MAX = "MaxMind";
	public static final String UNIT_COL_MOVES_LEFT = "MovesLeft";
	public static final String UNIT_COL_MOVE_DICE = "MoveDice";
	public static final String UNIT_COL_ATTACK_DICE = "AttackDice";
	public static final String UNIT_COL_DEFEND_DICE = "DefendDice";
	public static final String UNIT_COL_ATTACKS_PER_ROUND ="AttacksPerRound";
	public static final String UNIT_COL_ACTIONS_LEFT = "ActionsLeft";
	public static final String UNIT_COL_EXIST_X = "ExistXTile";
	public static final String UNIT_COL_EXIST_Y = "ExistYTile";
	public static final String UNIT_COL_DEST_X = "DestXTile";
	public static final String UNIT_COL_DEST_Y = "DestYTile";
	public static final String UNIT_COL_ACTION = "Action";
	public static final String UNIT_COL_TRAVELLING = "Travelling";
	public static final String UNIT_COL_DIRECTION = "Direction";
	public static final int UNIT_ID_COLUMN_OFFSET = 0;
	public static final int UNIT_TYPE_COLUMN_OFFSET = 1;
	public static final int UNIT_CLASS_COLUMN_OFFSET = 2;
	public static final int UNIT_ENABLED_COLUMN_OFFSET = 3;
	public static final int UNIT_BODY_CURRENT_COLUMN_OFFSET = 4;
	public static final int UNIT_BODY_MAX_COLUMN_OFFSET = 5;
	public static final int UNIT_MIND_CURRENT_COLUMN_OFFSET = 6;
	public static final int UNIT_MIND_MAX_COLUMN_OFFSET = 7;
	public static final int UNIT_MOVES_LEFT_COLUMN_OFFSET = 8;
	public static final int UNIT_MOVE_DICE_COLUMN_OFFSET = 9;
	public static final int UNIT_ATTACK_DICE_COLUMN_OFFSET = 10;
	public static final int UNIT_DEFEND_DICE_COLUMN_OFFSET = 11;
	public static final int UNIT_ATTACKS_PER_ROUND_COLUMN_OFFSET = 12;
	public static final int UNIT_ACTIONS_LEFT_COLUMN_OFFSET = 13;
	public static final int UNIT_EXIST_X_COLUMN_OFFSET = 14;
	public static final int UNIT_EXIST_Y_COLUMN_OFFSET = 15;
	public static final int UNIT_DEST_X_COLUMN_OFFSET = 16;
	public static final int UNIT_DEST_Y_COLUMN_OFFSET = 17;
	public static final int UNIT_ACTION_COLUMN_OFFSET = 18;
	public static final int UNIT_TRAVELLING_COLUMN_OFFSET = 19;
	public static final int UNIT_DIRECTION_COLUMN_OFFSET = 20;
	
	private static final String SCRIPT_CREATE_UNIT_TABLE =
			"CREATE TABLE " + UNIT_TABLE + " ("
					+ UNIT_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ UNIT_COL_TYPE + " INTEGER, "
					+ UNIT_COL_CLASS + " INTEGER, "
					+ UNIT_COL_ENABLED + " INTEGER, "
					+ UNIT_COL_BODY_CURRENT + " INTEGER, "
					+ UNIT_COL_BODY_MAX + " INTEGER, "
					+ UNIT_COL_MIND_CURRENT + " INTEGER, "
					+ UNIT_COL_MIND_MAX + " INTEGER, "
					+ UNIT_COL_MOVES_LEFT + " INTEGER, "
					+ UNIT_COL_MOVE_DICE + " INTEGER, "
					+ UNIT_COL_ATTACK_DICE + " INTEGER, "
					+ UNIT_COL_DEFEND_DICE + " INTEGER, "
					+ UNIT_COL_ATTACKS_PER_ROUND + " INTEGER, "
					+ UNIT_COL_ACTIONS_LEFT + " INTEGER, "
					+ UNIT_COL_EXIST_X + " INTEGER, "
					+ UNIT_COL_EXIST_Y + " INTEGER, "
					+ UNIT_COL_DEST_X + " INTEGER, "
					+ UNIT_COL_DEST_Y + " INTEGER, "
					+ UNIT_COL_ACTION + " INTEGER, "
					+ UNIT_COL_TRAVELLING + " INTEGER,"
					+ UNIT_COL_DIRECTION + " INTEGER);";
	
	
	// Inventory finals.
	 /** The table of all the items carried and equipped by the heroes.*/
	public static final String INVENTORY_TABLE = "INVENTORY_TABLE";
	public static final String INVENTORY_COL_ID = "_id";
	public static final String INVENTORY_COL_HERO = "Hero";
	public static final String INVENTORY_COL_WEAPON = "Weapon";
	public static final String INVENTORY_COL_SHIELD = "Shield";
	public static final String INVENTORY_COL_ARMOR = "Armor";
	public static final String INVENTORY_COL_HELM = "Helm";
	public static final String INVENTORY_COL_WRIST = "Wrist";
	public static final String INVENTORY_COL_BACKPACK0 = "Backpack0";
	public static final String INVENTORY_COL_BACKPACK1 = "Backpack1";
	public static final String INVENTORY_COL_BACKPACK2 = "Backpack2";
	public static final String INVENTORY_COL_BACKPACK3 = "Backpack3";
	public static final String INVENTORY_COL_BACKPACK4 = "Backpack4";
	public static final String INVENTORY_COL_BACKPACK5 = "Backpack5";
	public static final String INVENTORY_COL_BACKPACK6 = "Backpack6";
	public static final String INVENTORY_COL_BACKPACK7 = "Backpack7";
	public static final int INVENTORY_ID_COLUMN_OFFSET = 0;
	public static final int	INVENTORY_HERO_COLUMN_OFFSET = 1;
	public static final int INVENTORY_WEAPON_COLUMN_OFFSET = 2;
	public static final int INVENTORY_SHIELD_COLUMN_OFFSET = 3;
	public static final int INVENTORY_ARMOR_COLUMN_OFFSET = 4;
	public static final int INVENTORY_HELM_COLUMN_OFFSET = 5;
	public static final int INVENTORY_WRIST_COLUMN_OFFSET = 6;
	public static final int INVENTORY_BACKPACK0_COLUMN_OFFSET = 7;
	public static final int INVENTORY_BACKPACK1_COLUMN_OFFSET = 8;
	public static final int INVENTORY_BACKPACK2_COLUMN_OFFSET = 9;
	public static final int INVENTORY_BACKPACK3_COLUMN_OFFSET = 10;
	public static final int INVENTORY_BACKPACK4_COLUMN_OFFSET = 11;
	public static final int INVENTORY_BACKPACK5_COLUMN_OFFSET = 12;
	public static final int INVENTORY_BACKPACK6_COLUMN_OFFSET = 13;
	public static final int INVENTORY_BACKPACK7_COLUMN_OFFSET = 14;
	
	private static final String SCRIPT_CREATE_INVENTORY_TABLE =
			"CREATE TABLE " + INVENTORY_TABLE + " ("
					+ INVENTORY_COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ INVENTORY_COL_HERO + " INTEGER, "
					+ INVENTORY_COL_WEAPON + " INTEGER, "
					+ INVENTORY_COL_SHIELD + " INTEGER, "
					+ INVENTORY_COL_ARMOR + " INTEGER, "
					+ INVENTORY_COL_HELM + " INTEGER, "
					+ INVENTORY_COL_WRIST + " INTEGER, "
					+ INVENTORY_COL_BACKPACK0 + " INTEGER, "
					+ INVENTORY_COL_BACKPACK1 + " INTEGER, "
					+ INVENTORY_COL_BACKPACK2 + " INTEGER, "
					+ INVENTORY_COL_BACKPACK3 + " INTEGER, "
					+ INVENTORY_COL_BACKPACK4 + " INTEGER, "
					+ INVENTORY_COL_BACKPACK5 + " INTEGER, "
					+ INVENTORY_COL_BACKPACK6 + " INTEGER, "
					+ INVENTORY_COL_BACKPACK7 + " INTEGER);";
	
	
	// Database variables.
	private static SQLiteHelper sqLiteHelper;
	private static SQLiteDatabase sqLiteDatabase;
	
	private static Cursor cursor;
	private static ContentValues values;
	private static String[] columns;
	private static String selection;
	
	// Context passed in from the calling activity.
	private Context context;
	
	/** Generic constructor takes the context of the calling activity ('this'). */
	public HQDatabaseAdapter(Context c){
		context = c;
		//database_version = version;
		values = new ContentValues();
		sqLiteHelper = new SQLiteHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	/**************************************************************************************************/
	/*********************************GENERAL DATABASE METHODS*****************************************/
	/**************************************************************************************************/
	
	public HQDatabaseAdapter openToRead() throws android.database.SQLException {
		sqLiteDatabase = sqLiteHelper.getReadableDatabase();
		return this;
	}

	public HQDatabaseAdapter openToWrite() throws android.database.SQLException {
		sqLiteHelper = new SQLiteHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		sqLiteDatabase = sqLiteHelper.getWritableDatabase();
		return this;
	}

	public void close(){
		sqLiteHelper.close();
	}

	
	/**************************************************************************************************/
	/***********************************QUEST TABLE METHODS********************************************/
	/**************************************************************************************************/
	
	public Cursor getAllQuests(){
		columns = new String[]{QUEST_COL_ID, QUEST_COL_TAG, QUEST_COL_TITLE, QUEST_COL_DESCRIPTION, QUEST_COL_STORY, QUEST_COL_AVAILABLE, QUEST_COL_DIFFICULTY_COMPLETED};
		cursor = sqLiteDatabase.query(QUEST_TABLE, columns, null, null, null, null, null);
		return cursor;
	}
	
	public Cursor getCompletedQuests(){
		columns = new String[]{QUEST_COL_ID, QUEST_COL_TAG, QUEST_COL_TITLE, QUEST_COL_DESCRIPTION, QUEST_COL_STORY, QUEST_COL_AVAILABLE, QUEST_COL_DIFFICULTY_COMPLETED};
		selection = QUEST_COL_AVAILABLE + " = ?";
		cursor = sqLiteDatabase.query(QUEST_TABLE, columns, selection, new String[]{"1"}, null, null, null);
		return cursor;
	}
	
	public Cursor getQuestById(int index){
		columns = new String[]{QUEST_COL_ID, QUEST_COL_TAG, QUEST_COL_TITLE, QUEST_COL_DESCRIPTION, QUEST_COL_STORY, QUEST_COL_AVAILABLE, QUEST_COL_DIFFICULTY_COMPLETED};
		selection = QUEST_COL_ID + " = ?";
		cursor = sqLiteDatabase.query(QUEST_TABLE, columns, selection, new String[]{Integer.toString(index)}, null, null, null);
		return cursor;
	}
	
	public long completeQuest(String tag, String difficulty){
		Log.v(SQL,"Completing Quest -> " + tag + " on " + difficulty + " difficulty.");
		values.clear();
		values.put(QUEST_COL_DIFFICULTY_COMPLETED, difficulty);
		return sqLiteDatabase.update(QUEST_TABLE, values, QUEST_COL_TAG + "=" + tag, null);
	}
	
	public int resetQuests(){
		Log.v(SQL,"Reseting all Quests and Objectives.");
		resetObjectives();
		return sqLiteDatabase.delete(QUEST_TABLE, null, null);
	}
	
	public int resetObjectives(){
		Log.v(SQL,"Reseting all Objectives.");
		return sqLiteDatabase.delete(OBJECTIVE_TABLE, null, null);
	}
	
	
	
	/**************************************************************************************************/
	/*******************************ACHIEVEMENT TABLE METHODS******************************************/
	/**************************************************************************************************/
	
	public Cursor getAllAchievements(){
		columns = new String[]{ACHIEVEMENT_COL_ID, ACHIEVEMENT_COL_TITLE, ACHIEVEMENT_COL_DESCRIPTION,ACHIEVEMENT_COL_ACHIEVED, ACHIEVEMENT_COL_DATE};
		cursor = sqLiteDatabase.query(ACHIEVEMENT_TABLE, columns, null, null, null, null, null);
		return cursor;
	}
	
	public long completeAchievement(String title){
		Log.v(SQL,"Completing Achievement -> " + title);
		values.clear();
		values.put(ACHIEVEMENT_COL_ACHIEVED, 1);
		values.put(ACHIEVEMENT_COL_DATE, Integer.toString(Calendar.getInstance().get(Calendar.MONTH))
				+ "/" + Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
				+ "/" + Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
		return sqLiteDatabase.update(ACHIEVEMENT_TABLE, values, ACHIEVEMENT_COL_TITLE + "=" + title, null);
	}
	
	public int resetAchievements(){
		Log.v(SQL,"Reseting all Achievements.");
		return sqLiteDatabase.delete(ACHIEVEMENT_TABLE, null, null);
	}

	
	/**************************************************************************************************/
	/***********************************BOARD TABLE METHODS********************************************/
	/**************************************************************************************************/
	
	public Cursor getAllTiles(){
		columns = new String[]{TILE_COL_ID, TILE_COL_X, TILE_COL_Y, 
				TILE_COL_NORTH_WALL, TILE_COL_EAST_WALL, TILE_COL_SOUTH_WALL, TILE_COL_WEST_WALL,
				TILE_COL_TRAP, TILE_COL_ROOM, TILE_COL_TRAVELABLE, 
				TILE_COL_BLACK, TILE_COL_STAIR};
		cursor = sqLiteDatabase.query(TILE_TABLE, columns, null, null, null, null, null);
		return cursor;
	}
	
	public long addTile(int x, int y, int north, int east, int south, int west, int trap, int room, int travel, int black, int stair){
		values.clear();
		values.put(TILE_COL_X, x);
		values.put(TILE_COL_Y, y);
		values.put(TILE_COL_NORTH_WALL, north);
		values.put(TILE_COL_EAST_WALL, east);
		values.put(TILE_COL_SOUTH_WALL, south);
		values.put(TILE_COL_WEST_WALL, west);
		values.put(TILE_COL_TRAP, trap);
		values.put(TILE_COL_ROOM, room);
		values.put(TILE_COL_TRAVELABLE, travel);
		values.put(TILE_COL_BLACK, black);
		values.put(TILE_COL_STAIR, stair);
		
		Log.v(SQL,"Pushed tile " + Integer.toString(y) + ", " + Integer.toString(x));
		return sqLiteDatabase.insert(TILE_TABLE, null, values);
	}
	
	public int clearTiles(){
		return sqLiteDatabase.delete(TILE_TABLE, null, null);
	}
	
	/**************************************************************************************************/
	/**************************************UNIT TABLE METHODS******************************************/
	/**************************************************************************************************/
	
	public Cursor getAllUnits(){
		columns = new String[]{UNIT_COL_ID, UNIT_COL_TYPE, UNIT_COL_CLASS, UNIT_COL_ENABLED,
				UNIT_COL_BODY_CURRENT, UNIT_COL_BODY_MAX, UNIT_COL_MIND_CURRENT, UNIT_COL_MIND_MAX,
				UNIT_COL_MOVES_LEFT, UNIT_COL_MOVE_DICE, UNIT_COL_ATTACK_DICE, UNIT_COL_DEFEND_DICE,
				UNIT_COL_ATTACKS_PER_ROUND, UNIT_COL_ACTIONS_LEFT, UNIT_COL_EXIST_X, UNIT_COL_EXIST_Y,
				UNIT_COL_DEST_X, UNIT_COL_DEST_Y, UNIT_COL_ACTION, UNIT_COL_TRAVELLING, UNIT_COL_DIRECTION};
		cursor = sqLiteDatabase.query(UNIT_TABLE, columns, null, null, null, null, null);
		return cursor;
	}

	public long addUnit(int type, int cla, int enabled, int bodyCurrent, int bodyMax, int mindCurrent, int mindMax, int movesLeft,
			int moveDice, int attackDice, int defendDice, int attacksPerRound, int actionsLeft, int existX, int existY,
			int destX, int destY, int action, int travelling, int direction){
		values.clear();
		values.put(UNIT_COL_TYPE, type);
		values.put(UNIT_COL_CLASS, cla);
		values.put(UNIT_COL_ENABLED, enabled);
		values.put(UNIT_COL_BODY_CURRENT, bodyCurrent);
		values.put(UNIT_COL_BODY_MAX, bodyMax);
		values.put(UNIT_COL_MIND_CURRENT, bodyMax);
		values.put(UNIT_COL_MIND_MAX, mindMax);
		values.put(UNIT_COL_MOVES_LEFT, movesLeft);
		values.put(UNIT_COL_MOVE_DICE, moveDice);
		values.put(UNIT_COL_ATTACK_DICE, attackDice);
		values.put(UNIT_COL_DEFEND_DICE, defendDice);
		values.put(UNIT_COL_ATTACKS_PER_ROUND, attacksPerRound);
		values.put(UNIT_COL_ACTIONS_LEFT, actionsLeft);
		values.put(UNIT_COL_EXIST_X, existX);
		values.put(UNIT_COL_EXIST_Y, existY);
		values.put(UNIT_COL_DEST_X, destX);
		values.put(UNIT_COL_DEST_Y, destY);
		values.put(UNIT_COL_ACTION, action);
		values.put(UNIT_COL_TRAVELLING, travelling);
		values.put(UNIT_COL_DIRECTION, direction);

		switch(type){
		case(Board.Unit.HERO):
			Log.v(SQL,"Pushed Hero of Type " + Integer.toString(cla));
		break;
		case(Board.Unit.MONSTER):
			Log.v(SQL,"Pushed Monster of Type " + Integer.toString(cla));
		break;
		case(Board.Unit.DOODAD):
			Log.v(SQL,"Pushed Doodad of Type " + Integer.toString(cla));
		break;
		}
		return sqLiteDatabase.insert(UNIT_TABLE, null, values);
	}

	public int clearUnits(){
		return sqLiteDatabase.delete(UNIT_TABLE, null, null);

	}
	
	
	/**************************************************************************************************/
	/*********************************INVENTORY TABLE METHODS******************************************/
	/**************************************************************************************************/

	public Cursor getAllItems(){
		columns = new String[]{INVENTORY_COL_ID, INVENTORY_COL_WEAPON, INVENTORY_COL_SHIELD,
				INVENTORY_COL_ARMOR, INVENTORY_COL_HELM, INVENTORY_COL_WRIST,
				INVENTORY_COL_BACKPACK0, INVENTORY_COL_BACKPACK1, INVENTORY_COL_BACKPACK2,
				INVENTORY_COL_BACKPACK3, INVENTORY_COL_BACKPACK4, INVENTORY_COL_BACKPACK5,
				INVENTORY_COL_BACKPACK6, INVENTORY_COL_BACKPACK7};
		cursor = sqLiteDatabase.query(INVENTORY_TABLE, columns, null, null, null, null, null);
		return cursor;
	}
	
	public long addEquipmentSet(int hero, int weapon, int shield, int armor, int helm, int wrist,
			int backpack0, int backpack1, int backpack2, int backpack3, int backpack4,
			int backpack5, int backpack6, int backpack7){
		values.clear();
		values.put(INVENTORY_COL_HERO, hero);
		values.put(INVENTORY_COL_WEAPON, weapon);
		values.put(INVENTORY_COL_SHIELD, weapon);
		values.put(INVENTORY_COL_ARMOR, weapon);
		values.put(INVENTORY_COL_HELM, weapon);
		values.put(INVENTORY_COL_WRIST, weapon);
		values.put(INVENTORY_COL_BACKPACK0, backpack0);
		values.put(INVENTORY_COL_BACKPACK1, backpack1);
		values.put(INVENTORY_COL_BACKPACK2, backpack2);
		values.put(INVENTORY_COL_BACKPACK3, backpack3);
		values.put(INVENTORY_COL_BACKPACK4, backpack4);
		values.put(INVENTORY_COL_BACKPACK5, backpack5);
		values.put(INVENTORY_COL_BACKPACK6, backpack6);
		values.put(INVENTORY_COL_BACKPACK7, backpack7);
		return sqLiteDatabase.insert(INVENTORY_TABLE, null, values);
	}
	
	public int clearInventories(){
		return sqLiteDatabase.delete(INVENTORY_TABLE, null, null);

	}

	/** The custom implementation of SQLiteOpenHelper used to define the actions to take when the tables
	 * need to be initially created and, if necessary, populated. Overrides the onCreate() method, which
	 * is only called if the database does not already exist; as well as onUpgrade() which is called
	 * when the version number has changed.
	 * 
	 * @author Odin	 */
	public class SQLiteHelper extends SQLiteOpenHelper {

		// File readers used for the import.
		private InputStream inputFile;
		private InputStreamReader inputReader;
		private BufferedReader bufferReader;
		private ContentValues values, secondaryValues;
		private String currentLine;
		
		// General purpose variables.
		private int genIndex = 0;
		
		/** SQLiteHelper automatically controls the check for the existence of the database and calls onCreate()
		 * when needed to populate all of the tables needed by the application.
		 * 
		 * @param context The activity context. Typically passed in as 'this'.
		 * @param name The database name.
		 * @param factory 'null' in this application.
		 * @param version Version number used for revision checking. onUpgrade() will be called when the version number is
		 * higher than the existing version. */
		public SQLiteHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			
			Log.v(SQL,"First application run. Creating new database.");
			
			/************************************************/
			/***************CREATE QUEST TABLE***************/
			/************************************************/
			Log.v(SQL,"Creating quest and objective tables.");
			
			database.execSQL(SCRIPT_CREATE_QUEST_TABLE);
			database.execSQL(SCRIPT_CREATE_OBJECTIVE_TABLE);
			
			// Read and parse the available achievements from the input file.
			inputFile = context.getResources().openRawResource(R.raw.quest_list);
			inputReader = new InputStreamReader(inputFile);
			bufferReader = new BufferedReader(inputReader);
			values = new ContentValues();
			secondaryValues = new ContentValues();
			genIndex = 0;
			
			try{
				while((currentLine = bufferReader.readLine()) != null){
					if(currentLine.equals("<Tag>")){
						values.put(QUEST_COL_TAG,bufferReader.readLine());
					}else if(currentLine.equals("<Title>")){
						values.put(QUEST_COL_TITLE, bufferReader.readLine());
					}else if(currentLine.equals("<Description>")){
						values.put(QUEST_COL_DESCRIPTION, bufferReader.readLine());
					}else if(currentLine.equals("<Story>")){
						values.put(QUEST_COL_STORY, bufferReader.readLine());
					}else if(currentLine.equals("<Objective>")){
						secondaryValues.put(OBJECTIVE_QUEST_NUMBER, genIndex);
						secondaryValues.put(OBJECTIVE_DESCRIPTION, bufferReader.readLine());
						secondaryValues.put(OBJECTIVE_OPTIONAL,0);
						database.insert(OBJECTIVE_TABLE, null, secondaryValues);
						secondaryValues.clear();
					}else if(currentLine.equals("<Optional>")){
						secondaryValues.put(OBJECTIVE_QUEST_NUMBER, genIndex);
						secondaryValues.put(OBJECTIVE_DESCRIPTION, bufferReader.readLine());
						secondaryValues.put(OBJECTIVE_OPTIONAL,1);
						database.insert(OBJECTIVE_TABLE, null, secondaryValues);
						secondaryValues.clear();
					}else if(currentLine.equals("</Quest>")){
						if(genIndex==0){
							values.put(QUEST_COL_AVAILABLE, 1);
						}else{
							values.put(QUEST_COL_AVAILABLE, 0);
						}
						values.put(QUEST_COL_DIFFICULTY_COMPLETED, "NONE");
						database.insert(QUEST_TABLE, null, values);
						genIndex++;
					}
				}
			}catch(IOException e){
			}
			Log.v(SQL,"Done loading quest and objective tables.");
			
			
			/************************************************/
			/******** CREATE ACHIEVEMENT TABLE **************/
			/************************************************/
			Log.v(SQL,"Creating achievement database.");
			
			database.execSQL(SCRIPT_CREATE_ACHIEVEMENT_TABLE);
			
			// Read and parse the available achievements from the input file.
			inputFile = context.getResources().openRawResource(R.raw.achievement_list);
			inputReader = new InputStreamReader(inputFile);
			bufferReader = new BufferedReader(inputReader);
			values.clear();
			try{
				while((currentLine = bufferReader.readLine()) != null){
					if(currentLine.equals("<Title>")){
						values.put(ACHIEVEMENT_COL_TITLE, bufferReader.readLine());
					}else if(currentLine.equals("<Description>")){
						values.put(ACHIEVEMENT_COL_DESCRIPTION, bufferReader.readLine());
						values.put(ACHIEVEMENT_COL_ACHIEVED, 0);
						values.put(ACHIEVEMENT_COL_DATE, " ");
						database.insert(ACHIEVEMENT_TABLE, null, values);
						values.clear();
					}
				}
			}catch(IOException e){
			}
			Log.v(SQL,"Done loading achievement table.");
			
			
			/************************************************/
			/***************CREATE BOARD TABLE***************/
			/************************************************/
			Log.v(SQL,"Creating a new empty tile table.");
			
			database.execSQL(SCRIPT_CREATE_TILE_TABLE);
			
			
			/************************************************/
			/***************CREATE HERO TABLE****************/
			/************************************************/		
			Log.v(SQL,"Creating a new empty unit table.");
			
			database.execSQL(SCRIPT_CREATE_UNIT_TABLE);
		
			
			/************************************************/
			/**************CREATE INVENTORY TABLE**************/
			/************************************************/
			Log.v(SQL,"Creating a new empty inventory table.");
			
			database.execSQL(SCRIPT_CREATE_INVENTORY_TABLE);
			
			
			Log.v(SQL,"All tables successfully created.");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			// Reread the file! Update but maintain existing data! (You wouldn't want the nerf bat to hit their equipment, right?)
			Log.v(SQL, "Database upgrade started.");
		}
	}
}
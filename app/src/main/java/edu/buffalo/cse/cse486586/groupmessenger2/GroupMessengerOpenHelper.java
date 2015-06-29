package edu.buffalo.cse.cse486586.groupmessenger2;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by ramyarao on 2/16/15.
 * Provides an SQLite database for the GroupMessenger App.
 */
public class GroupMessengerOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String GROUPMESSENGER_TABLE_CREATE =
            "CREATE TABLE group_messenger (key TEXT, value TEXT);";

    public GroupMessengerOpenHelper(Context context, String name,
                                    SQLiteDatabase.CursorFactory factory, int version) {
        super(context, "group_messenger", factory, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion, int newVersion) {
        // dropping an older version of the table , if it exists
        Log.v("db onUpgrade","group_messenger");
        db.execSQL("DROP TABLE IF EXISTS group_messenger");
        onCreate(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS group_messenger");
        db.execSQL(GROUPMESSENGER_TABLE_CREATE);
        Log.v("db onCreate","group_messenger");
    }


}
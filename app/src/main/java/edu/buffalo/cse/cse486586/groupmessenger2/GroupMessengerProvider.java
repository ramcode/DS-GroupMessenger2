package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    static final String TAG = GroupMessengerProvider.class.getSimpleName();
    static final String PROVIDER_NAME = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
    static final String TABLE_NAME = GroupMessengerDB.TABLE_NAME;
    static final String KEY_COLUM = GroupMessengerDB.TABLE_COL_KEY;
    static final String URL = "content://"+PROVIDER_NAME;
    private static final Uri CONTENT_URI = Uri.parse(URL);
    private GroupMessengerDB groupMessengerDB;
    private static final UriMatcher uriMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);
    static final int TABLE = 1;
    static final int ROW = 2;
    static SQLiteDatabase messengerDB;
    static {
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME, TABLE);
        uriMatcher.addURI(PROVIDER_NAME, TABLE_NAME+"/#",ROW);
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        try{
            //int uriType = uriMatcher.match(uri);
           /* switch(uriType){
                case TABLE:
                    SQLiteDatabase sqlDB = groupMessengerDB.getWritableDatabase();
                    Log.v("inserting values: ", values.toString());
                    long rowId = sqlDB.insert(TABLE_NAME, null, values);
                    if(rowId>0){
                        Log.d(TAG, "Insert row success, RowId: "+rowId);
                        getContext().getContentResolver().notifyChange(uri, null);
                        return Uri.parse(TABLE_NAME + "/" + rowId);
                    }
                    break;

            }*/
            SQLiteDatabase sqlDB = groupMessengerDB.getWritableDatabase();
            Log.v("inserting values: ", values.toString());
            long rowId = sqlDB.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if(rowId>0) {
                Log.d(TAG, "Insert row success, RowId: " + rowId);
                getContext().getContentResolver().notifyChange(uri, null);
                return Uri.parse(TABLE_NAME + "/" + rowId);
            }
            else{
                throw new SQLException("Insert Row failed: "+values.toString());
            }

        }
        catch(Exception ex){
                Log.d(TAG, "Insert Row failed: "+values.toString()+" Exception: "+ex);
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        Log.d(TAG, "Initializing DB in provider...");
        Context context = getContext();
        groupMessengerDB = new GroupMessengerDB(context);
        return groupMessengerDB.getWritableDatabase()==null?false:true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);
        Cursor cursor = null;
        try{
            //int uriType = uriMatcher.match(uri);
           /* switch(uriType){
                case ROW:
                    SQLiteDatabase sqlDB = groupMessengerDB.getWritableDatabase();
                    Log.v(TAG, "Querying Table with column names: " +Arrays.toString(projection) + " ,values: " + selection);
                   cursor =  sqlDB.query(TABLE_NAME, projection, selection, selectionArgs, null, null, null);
                    if(cursor.getCount()>0){
                        Log.d(TAG, "Retrieve row success....");
                        cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    }
                    break;
            }*/
            SQLiteDatabase sqlDB = groupMessengerDB.getWritableDatabase();
            Log.v(TAG, "Querying Table with column names: " +Arrays.toString(projection) + " ,filter columns: " + selection+",filter values: "+Arrays.toString(selectionArgs));
            cursor =  sqlDB.query(TABLE_NAME, projection, KEY_COLUM+"=?", new String[]{selection}, null, null, null);
            if(cursor.getCount()>0){
                Log.d(TAG, "Retrieve row success....");
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
            }
            else{
                Log.d(TAG, "Query failed with Key: "+selection);
            }

        }
        catch(Exception ex){
            Log.d(TAG, "Query failed with key: "+selection+" ,Exception: "+ex);
        }
        return cursor;
    }
}

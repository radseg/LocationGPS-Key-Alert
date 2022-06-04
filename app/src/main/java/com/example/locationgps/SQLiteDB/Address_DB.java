package com.example.locationgps.SQLiteDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class Address_DB {
    public SQLiteDatabase db=null; // 資料庫類別
    private final static String	DATABASE_NAME= "address.db";// 資料庫名稱
    private final static String	TABLE_NAME="address_table"; // 資料表名稱
    private final static String	_ID	= "_id"; // 資料表欄位/
    private final static String	CUSTOM = "custom";
    private final static String	PLACE = "place";
    private final static String ADDRESS = "address";
    private final static String LAT = "lat";
    private final static String LNG = "lng";
    /* 建立資料表的欄位 */
    private final static String	CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + _ID
            + " INTEGER PRIMARY KEY,"+ CUSTOM + " TEXT," + PLACE + " TEXT,"+ ADDRESS + " TEXT,"+ LAT + " DOUBLE,"+ LNG + " DOUBLE)";

    private Context mCtx = null;
    public Address_DB(Context ctx){ // 建構式
        this.mCtx = ctx;      // 傳入 建立物件的 ＭainActivity
    }

    public void open() throws SQLException { // 開啟已經存在的資料庫
        db = mCtx.openOrCreateDatabase(DATABASE_NAME, 0, null);
        try	{
            db.execSQL(CREATE_TABLE);// 建立資料表
        }catch (Exception e) {
        }
    }

    public void close() {  // 關閉資料庫
        db.close();
    }

	public boolean getData() { // 查詢所有資料，判斷有沒有值
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null)!=null;
    }


    public Cursor getAll() { // 查詢所有資料，只取出三個欄位
        return db.query(TABLE_NAME,
                new String[] {_ID,CUSTOM,PLACE,ADDRESS,LAT,LNG},
                null, null, null, null, null,null);
    }

    public Cursor get(long rowId) throws SQLException { // 查詢指定 ID 的資料，只取出三個欄位
        Cursor mCursor = db.query(TABLE_NAME,
                new String[] {_ID,CUSTOM,PLACE,ADDRESS,LAT,LNG},
                _ID +"=" + rowId, null, null, null, null,null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public long append(String custom,String place,String address,Double lat,Double lng) { // 新增一筆資料
        ContentValues args = new ContentValues();
        args.put(CUSTOM, custom);
        args.put(PLACE, place);
        args.put(ADDRESS, address);
        args.put(LAT, lat);
        args.put(LNG, lng);
        return db.insert(TABLE_NAME, null, args);
    }

    public boolean delete(long rowId) {  //刪除指定的資料
        return db.delete(TABLE_NAME, _ID + "=" + rowId, null) > 0;
    }

    public boolean update(long rowId, String custom,String place,String address,Double lat,Double lng) { // 更新指定的資料
        ContentValues args = new ContentValues();
        args.put(CUSTOM, custom);
        args.put(PLACE, place);
        args.put(ADDRESS, address);
        args.put(LAT, lat);
        args.put(LNG, lng);
        return db.update(TABLE_NAME, args,_ID + "=" + rowId, null) > 0;
    }

}

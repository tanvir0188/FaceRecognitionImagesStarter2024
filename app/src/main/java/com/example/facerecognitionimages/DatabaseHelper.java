package com.example.facerecognitionimages;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "faceDatabase.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "Faces";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_IMAGE = "image";
    private static final String COLUMN_EMBEDDING = "embedding";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_NAME + " TEXT," +
                COLUMN_IMAGE + " TEXT," + // Image as Base64 string
                COLUMN_EMBEDDING + " BLOB)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Convert Bitmap to Base64 string
//    public static String bitmapToBase64(Bitmap bitmap) {
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
//        byte[] byteArray = outputStream.toByteArray();
//        return Base64.encodeToString(byteArray, Base64.DEFAULT);
//    }

    // Convert float array to byte array
    public byte[] floatArrayToByteArray(float[] input) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(input.length * Float.BYTES);
        for (float value : input) {
            byteBuffer.putFloat(value);
        }
        return byteBuffer.array();
    }

    public float[] byteArrayToFloatArray(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = byteBuffer.getFloat();
        }
        return floats;
    }





    public List<Friend> getAllFaces() {
        List<Friend> friends = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        int nameIndex = cursor.getColumnIndex(COLUMN_NAME);
        int imageBase64Index = cursor.getColumnIndex(COLUMN_IMAGE);
        int embeddingIndex = cursor.getColumnIndex(COLUMN_EMBEDDING);

        if (nameIndex == -1 || imageBase64Index == -1 || embeddingIndex == -1) {
            throw new IllegalStateException("Column not found in the database");
        }

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(nameIndex);
                String imageBase64 = cursor.getString(imageBase64Index);
                byte[] embeddingBlob = cursor.getBlob(embeddingIndex); // Retrieve as byte[]
                float[] embedding = byteArrayToFloatArray(embeddingBlob); // Convert to float[]

                friends.add(new Friend(name, imageBase64, embedding));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return friends;
    }


    public long saveData(String name, String photo, float[] embedding) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_IMAGE, photo);
        values.put(COLUMN_EMBEDDING, floatArrayToByteArray(embedding));
        long newRowId = db.insert(TABLE_NAME, null, values);

        db.close();
        return newRowId;
    }


}

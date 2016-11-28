package com.example.app1;

import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import com.example.utils.PkgCertWhiteLists;
import java.util.List;

public class MyProvider extends ContentProvider {

    private static final String TAG = MyProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.example.app1.MyProvider";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.myapp.contenttype";
    public static final String CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.com.myapp.contenttype";

    // Expose the interface that the Content Provider provides.
    public interface UserData {
        String PATH = "shareddata";
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
    }
    // add other interfaces that your ContentProvider supported

    // UriMatcher
    private static final int NAME_CODE = 1;
    private static final int NAME_ID_CODE = 2;
    private static UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, UserData.PATH, NAME_CODE);
        sUriMatcher.addURI(AUTHORITY, UserData.PATH + "/#", NAME_ID_CODE);
    }

    private MyDatabaseHelper mDb;
    private static PkgCertWhiteLists sWhiteLists = null;

    private static void buildWhiteLists(Context context) {
        boolean isDebug = BuildConfig.DEBUG;
        sWhiteLists = new PkgCertWhiteLists();
        // Register certificate hash value of partner application com.example.app2
        sWhiteLists.add("com.example.app2", isDebug ?
                // Certificate hash value of "androiddebugkey" in the debug.keystore.
                "719EB599 878F88E1 FCAC5028 E85BFD42 62022E53 94B6D63D A0159470 59DA3A64" :
                // Certificate hash value of "partner key" in the keystore.
                "1F039BB5 7861C27A 3916C778 8E78CE00 690B3974 3EB8259F E2627B8D 4C0EC35A");

        // Add other partner here via add(String pkgName, String sha256)
    }

    private static boolean checkPartner(Context context, String pkgName) {
        if (sWhiteLists == null) {
            buildWhiteLists(context);
        }
        if (context.getPackageName().equals(pkgName)) {
            // current app is in used
            return true;
        }
        return sWhiteLists.test(context, pkgName);
    }

    /**
     * Get the package name of the calling application.
     *
     * @param context: the context
     */

    private String getCallingPackage(Context context) {
        String pkgName = null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procList = am.getRunningAppProcesses();
        int callingPid = Binder.getCallingPid();
        if (procList != null) {
            for (ActivityManager.RunningAppProcessInfo proc : procList) {
                if (proc.pid == callingPid) {
                    pkgName = proc.pkgList[proc.pkgList.length - 1];
                    break;
                }
            }
        }
        return pkgName;
    }

    @Override
    public boolean onCreate() {
        mDb = new MyDatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NAME_CODE:
                return CONTENT_TYPE;
            case NAME_ID_CODE:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Invalid URI：" + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (!checkPartner(getContext(), getCallingPackage(getContext()))) {
            throw new SecurityException("Calling application is not a partner application.");
        }
        if (mDb == null) {
            mDb = new MyDatabaseHelper(getContext());
        }
        int uriType = sUriMatcher.match(uri);
        SQLiteDatabase sqlDB = mDb.getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case NAME_CODE:
                id = sqlDB.insert(MyDatabaseHelper.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        Log.d(TAG, "inserted data " + id);
        return Uri.parse(UserData.PATH + "/" + id);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (!checkPartner(getContext(), getCallingPackage(getContext()))) {
            throw new SecurityException("Calling application is not a partner application.");
        }
        if (mDb == null) {
            mDb = new MyDatabaseHelper(getContext());
        }
        int uriType = sUriMatcher.match(uri);
        SQLiteDatabase sqlDB = mDb.getWritableDatabase();
        int count = 0;
        switch (uriType) {
            case NAME_CODE:
                count = sqlDB.delete(MyDatabaseHelper.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return count;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        if (!checkPartner(getContext(), getCallingPackage(getContext()))) {
            throw new SecurityException("Calling application is not a partner application.");
        }
        if (mDb == null) {
            mDb = new MyDatabaseHelper(getContext());
        }
        SQLiteDatabase db = mDb.getReadableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(MyDatabaseHelper.TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case NAME_CODE:
                break;
            case NAME_ID_CODE:
                String id = uri.getPathSegments().get(1);
                queryBuilder.appendWhere(MyDatabaseHelper.COL_ID + "=" + id);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = MyDatabaseHelper.COL_ID;
        }
        Cursor cursor =
                queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        return cursor;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        if (!checkPartner(getContext(), getCallingPackage(getContext()))) {
            throw new SecurityException("Calling application is not a partner application.");
        }
        if (mDb == null) {
            mDb = new MyDatabaseHelper(getContext());
        }
        int uriType = sUriMatcher.match(uri);
        SQLiteDatabase sqlDB = mDb.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
            case NAME_CODE:
                rowsUpdated =
                        sqlDB.update(MyDatabaseHelper.TABLE_NAME, values, selection, selectionArgs);
                break;
            case NAME_ID_CODE:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(MyDatabaseHelper.TABLE_NAME, values,
                            MyDatabaseHelper.COL_ID + "=" + id, null);
                } else {
                    rowsUpdated = sqlDB.update(MyDatabaseHelper.TABLE_NAME, values,
                            MyDatabaseHelper.COL_ID + "=" + id + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        Log.d(TAG, "updated data " + rowsUpdated);
        return rowsUpdated;
    }
}

package com.example.app1;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private MyDataAdapter mAdapter;
    private ListView mLvData;
    private ArrayList<String> mUserNames = new ArrayList<>();
    private EditText mEdtVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEdtVersion = (EditText) findViewById(R.id.edtVersion);
        mLvData = (ListView) findViewById(R.id.listView);
        try {
            Cursor cursor =
                    getContentResolver().query(MyProvider.UserData.CONTENT_URI, null, null, null,
                            null);
            if (cursor != null) {
                if (!cursor.moveToFirst()) {
                    Toast.makeText(this, "no data yet", Toast.LENGTH_SHORT).show();
                } else {
                    do {
                        String userName = cursor.getString(
                                cursor.getColumnIndex(MyDatabaseHelper.COL_USER_NAME));
                        mUserNames.add(userName);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAdapter = new MyDataAdapter(this, mUserNames);
        mLvData.setAdapter(mAdapter);
        mLvData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int deleted =
                        getContentResolver().delete(MyProvider.UserData.CONTENT_URI, " version = ?",
                                new String[] {
                                        mUserNames.get(position)
                                });
                if (deleted == 1) {
                    mUserNames.remove(position);
                    mAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MainActivity.this, "Can not delete", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onAddNameClick(View view) {
        if (TextUtils.isEmpty(mEdtVersion.getText())) {
            return;
        }
        final String version = mEdtVersion.getText().toString();
        ContentValues values = new ContentValues();
        values.put(MyDatabaseHelper.COL_USER_NAME, version);
        Uri uri = getContentResolver().insert(MyProvider.UserData.CONTENT_URI, values);
        if (uri != null) {
            Toast.makeText(this, "New record inserted !", Toast.LENGTH_LONG).show();
            mUserNames.add(version);
            mAdapter.notifyDataSetChanged();
            mEdtVersion.setText("");
        }
    }
}

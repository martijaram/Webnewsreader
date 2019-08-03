package com.company.myapp.webnewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<String>();

    ArrayList<String> content = new ArrayList<String>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase sqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sqLiteDatabase = this.openOrCreateDatabase("Article", MODE_PRIVATE, null);

        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VAARCHAR, content VARCHAR)");

        DownloadTask downloadTask = new DownloadTask();

        try {

            //downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {

            e.printStackTrace();

        }

        ListView listView = (ListView) findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext(), Main2Activity.class);

                intent.putExtra("content", content.get(position));

                startActivity(intent);

            }
        });

        updateListView();

    }

    public void updateListView() {

        Cursor c = sqLiteDatabase.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");

        int titleIdex = c.getColumnIndex("title");

        if (c.moveToFirst()) {

            titles.clear();

            content.clear();

            do {

                content.add(c.getString(contentIndex));

                titles.add(c.getString(titleIdex));

            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();

        }

    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            String result = "";

            URL url;

            HttpURLConnection httpURLConnection = null;

            try {

                url = new URL(strings[0]);

                httpURLConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = httpURLConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = inputStreamReader.read();

                }

                JSONArray jsonArray = new JSONArray(result);

                int numberItems = 20;

                if (jsonArray.length() < 20) {

                    numberItems = jsonArray.length();

                }

                sqLiteDatabase.execSQL("DELETE FROM articles");

                for (int i = 0; i < numberItems; i++) {

                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ articleId +".json?print=pretty");

                    httpURLConnection = (HttpURLConnection) url.openConnection();

                    inputStream = httpURLConnection.getInputStream();

                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String articleInfo = "";

                    while (data != -1) {

                        char current = (char) data;

                        articleInfo += current;

                        data = inputStreamReader.read();

                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {

                        String articleTitle = jsonObject.getString("title");

                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);

                        httpURLConnection = (HttpURLConnection) url.openConnection();

                        inputStream = httpURLConnection.getInputStream();

                        inputStreamReader = new InputStreamReader(inputStream);

                        data = inputStreamReader.read();

                        String articleContent = "";

                        while (data != -1) {

                            char current = (char) data;

                            articleContent += current;

                            data = inputStreamReader.read();

                        }

                        Log.i("Info if working HTML", articleContent);

                        String sql = "INSERT INTO articles (id, title, content) VALUES (?, ?, ?)";

                        SQLiteStatement statement = sqLiteDatabase.compileStatement(sql);

                        statement.bindString(1, articleId);

                        statement.bindString(2, articleTitle);

                        statement.bindString(3, articleContent);

                        statement.execute();

                    }

                }

                Log.i("URL Information", result);

                return result;

            } catch (Exception e) {

                e.printStackTrace();

            }

            return null;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();

        }
    }

}

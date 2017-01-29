package com.example.android.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ImageAdapter mImageAdapter;
    private final String SORT_POPULAR_KEY = "popular";
    private final String SORT_RATINGS_KEY = "top_rated";
    private String sortMode = new String(SORT_POPULAR_KEY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toolbar Stuff
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //GridView Stuff
        GridView posterGrid = (GridView) findViewById(R.id.poster_gridview);
        mImageAdapter = new ImageAdapter(this);
        posterGrid.setAdapter(mImageAdapter);

        mImageAdapter.movies = new JSONArray();
        mImageAdapter.numElements = 0;
        mImageAdapter.pageNum = 1;
        String[] params = {SORT_POPULAR_KEY, "1"};
        updateMovies(params);

        posterGrid.setOnScrollListener(new AbsListView.OnScrollListener(){
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
            {
                if(firstVisibleItem + visibleItemCount >= totalItemCount) {
                    mImageAdapter.pageNum++;

                    if (sortMode.equals(SORT_POPULAR_KEY)) {
                        String[] params = {SORT_POPULAR_KEY, "" + mImageAdapter.pageNum};
                        updateMovies(params);
                    } else {
                        String[] params = {SORT_RATINGS_KEY, "" + mImageAdapter.pageNum};
                        updateMovies(params);
                    }
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState){

            }
        });

        //On Click
        posterGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(MainActivity.this, MovieDetailsActivity.class);
                try {
                    intent.putExtra(intent.EXTRA_TEXT, mImageAdapter.movies.getJSONObject(position).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                startActivity(intent);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_sort :
                mImageAdapter.movies = new JSONArray();
                mImageAdapter.numElements = 0;
                mImageAdapter.pageNum = 1;
                if (item.getTitle().equals(getString(R.string.change_sort_popular_mode))) {
                    item.setTitle(getString(R.string.change_sort_ratings_mode));
                    String[] params = {SORT_RATINGS_KEY, "1"};
                    sortMode = SORT_RATINGS_KEY;
                    updateMovies(params);
                } else {
                    item.setTitle(getString(R.string.change_sort_popular_mode));
                    String[] params = {SORT_POPULAR_KEY, "1"};
                    sortMode = SORT_POPULAR_KEY;
                    updateMovies(params);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void updateMovies(String[] sortMode) {
        if (isOnline()) {
            FetchMoviesTask moviesTask = new FetchMoviesTask();
            moviesTask.execute(sortMode);
        } else {
            TextView offlineText = new TextView(this);
            offlineText.setText("Error: Device is offline. Please connect to the internet");
            offlineText.setPadding(dp2px(16), dp2px(16), dp2px(16), dp2px(16));
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.main_linearlayout);
            linearLayout.addView(offlineText, 1);
        }
    }

    public int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getApplicationContext().getResources().getDisplayMetrics());
    }

    // checks to see if there is internet access
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private class FetchMoviesTask extends AsyncTask<String[], Void, JSONArray> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        @Override
        protected JSONArray doInBackground(String[]... thePath) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonStr = null;

            try {
                // Construct the URL e.g. http://api.themoviedb.org/3/movie/popular?api_key=
                final String MOVIES_BASE_URL = "http://api.themoviedb.org/3/movie";
                final String PAGE_PARAM = "page";
                final String API_KEY_PARAM = "api_key";
                final String API_KEY = "your key here"; // get a key at https://www.themoviedb.org/documentation/api

                Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                        .appendPath(thePath[0][0])
                        .appendQueryParameter(PAGE_PARAM, thePath[0][1])
                        .appendQueryParameter(API_KEY_PARAM, API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                //Log.v(LOG_TAG, "Build URI: " + url);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            //Log.v(LOG_TAG, "The JSON String: " + moviesJsonStr);

            try {
                return getMoviesDataFromJson(moviesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONArray theArray) {
            //refresh the gridview
            try {
                for (int i = 0; i < theArray.length(); i++) {
                    mImageAdapter.movies.put(theArray.getJSONObject(i));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            mImageAdapter.numElements = mImageAdapter.movies.length();

            GridView posterGrid = (GridView) findViewById(R.id.poster_gridview);
            posterGrid.invalidateViews();
        }

        private JSONArray getMoviesDataFromJson(String moviesJsonString) throws JSONException {

            final String MOVIES_RESULTS_ARRAY = "results";

            JSONObject moviesJSON = new JSONObject(moviesJsonString);
            JSONArray moviesArray = moviesJSON.getJSONArray(MOVIES_RESULTS_ARRAY);

            /*//no need to return an array of JSONObjects when you can return a JSONArray :P
            JSONObject[] movieJSONs = new JSONObject[moviesArray.length()];

            for (int i = 0; i < movieJSONs.length; i++) {
                movieJSONs[i] = moviesArray.getJSONObject(i);
            }

            return movieJSONs;*/

            return moviesArray;
        }
    }

    public class ImageAdapter extends BaseAdapter {

        private final String LOG_TAG = ImageAdapter.class.getSimpleName();

        private Context mContext;
        public int numElements;
        private JSONArray movies;
        public int pageNum;

        //Constructor
        public ImageAdapter(Context c) {
            mContext = c;
            numElements = 0;
            movies = new JSONArray();
            pageNum = 1;
        }

        public int getCount() {
            return numElements;
        }

        public Object getItem (int position) {
            return null;
        }

        public long getItemId (int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.
                        LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                imageView = (ImageView) convertView;
            }

            if (movies != null) {
                try {
                    //Extracting the poster path from JSONObject
                    final String POSTER_PATH_KEY = "poster_path";

                    String posterPath = movies.getJSONObject(position).getString(POSTER_PATH_KEY);

                    //Building image URL
                    final String POSTER_BASE_URL = "http://image.tmdb.org/t/p/";
                    final String POSTER_SIZE_PATH = "w342";

                    Uri builtUri = Uri.parse(POSTER_BASE_URL).buildUpon()
                            .appendPath(POSTER_SIZE_PATH)
                            .build();

                    URL url = new URL(builtUri.toString());

                    //Log.v(LOG_TAG, "the URL: " + url.toString() + "/" + posterPath);
                    //Log.v(LOG_TAG, "the poster path is: " + posterPath);

                    //Display the poster
                    Picasso.with(mContext).load(url.toString() + "/" + posterPath).into(imageView);

                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            } else {
                Picasso.with(mContext).load("http://i.imgur.com/DvpvklR.png").into(imageView);
            }

            return imageView;
        }
    }
}

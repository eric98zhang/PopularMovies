package com.example.android.popularmovies;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class MovieDetailsActivity extends AppCompatActivity {

    private final String LOG_TAG = MovieDetailsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        Intent intent = getIntent();

        // Get JSON data from intent and fill in the layout with values:
        try {
            // JSONObject keys
            final String MOVIE_TITLE = "title";
            final String MOVIE_RATING = "vote_average";
            final String MOVIE_RELEASE_DATE = "release_date";
            final String MOVIE_OVERVIEW = "overview";

            // Get JSONObject from intent
            JSONObject movieDataJSON = new JSONObject(intent.getStringExtra(Intent.EXTRA_TEXT));

            // Setup movie title TextView
            TextView titleText = (TextView) findViewById(R.id.movie_title_textview);
            titleText.setText(movieDataJSON.getString(MOVIE_TITLE));

            // Setup movie poster ImageView (copypasta from MainActivity)
            ImageView posterImage = (ImageView) findViewById(R.id.movie_poster_imageview);

                //Extracting the poster path from JSONObject
                final String POSTER_PATH_KEY = "poster_path";

                String posterPath = movieDataJSON.getString(POSTER_PATH_KEY);

                //Building image URL
                final String POSTER_BASE_URL = "http://image.tmdb.org/t/p/";
                final String POSTER_SIZE_PATH = "w342";

                Uri builtUri = Uri.parse(POSTER_BASE_URL).buildUpon()
                        .appendPath(POSTER_SIZE_PATH)
                        .build();

                URL url = new URL(builtUri.toString());

                //Log.v(LOG_TAG, "the URL: " + url.toString() + "/" + posterPath);
                //Log.v(LOG_TAG, "the poster path is: " + posterPath);

            Picasso.with(this).load(url.toString() + "/" + posterPath).into(posterImage);
            //posterImage.setBackgroundColor(getResources().getColor(R.color.colorAccent));

            // Setup movie rating TextView
            TextView ratingText = (TextView) findViewById(R.id.movie_ratings_textview);
            ratingText.setText("Rating: " + movieDataJSON.getString(MOVIE_RATING) + "/10");

            // Setup movie release date TextView
            TextView releaseDateText = (TextView) findViewById(R.id.movie_release_date_textview);
            releaseDateText.setText("Release Date: " + movieDataJSON.getString(MOVIE_RELEASE_DATE));

            // Setup movie overview TextView
            TextView overviewText = (TextView) findViewById(R.id.movie_overview_textview);
            overviewText.setText(movieDataJSON.getString(MOVIE_OVERVIEW));
            overviewText.setMovementMethod(new ScrollingMovementMethod());

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        // Setup Toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.details_toolbar);
        setSupportActionBar(mToolbar);

        // Get a support ActionBar corresponding to mToolBar to enable Up button
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

    }
}

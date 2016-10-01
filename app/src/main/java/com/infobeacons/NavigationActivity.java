package com.infobeacons;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class NavigationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String NAO_SUPORTA_TTS = "Seu dispositvo não suporta o texto para voz";

    private TextToSpeech mTts;
    final String msg = "mensagem teste";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final TextView mteTextView = (TextView) findViewById(R.id.texto);
        mTts = new TextToSpeech(NavigationActivity.this, NavigationActivity.this);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTts.speak(msg, TextToSpeech.QUEUE_ADD, null);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            mTts.setLanguage(new Locale("pt"));
        } else {
            Toast.makeText(getApplicationContext(), NAO_SUPORTA_TTS, Toast.LENGTH_SHORT).show();
        }
    }
}
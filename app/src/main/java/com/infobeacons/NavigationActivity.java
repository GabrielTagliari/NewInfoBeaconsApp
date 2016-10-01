package com.infobeacons;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NavigationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private String TAG = "NavigationActivity";

    /* Constantes */
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private static final String NAO_SUPORTA_TTS = "Seu dispositvo não suporta o texto para voz";
    private static final String EMPTY = "";

    /* URL */
    private String mUrl = "http://infobeacons.mybluemix.net/listBeacons/";

    /* Volley */
    private RequestQueue mVolleyQueue;
    private JSONObject beaconEncontrado;

    private BeaconManager mBeaconManager;

    private TextToSpeech mTts;
    private String msg = EMPTY;
    private String img = EMPTY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final TextView mTextView = (TextView) findViewById(R.id.texto);
        mTts = new TextToSpeech(NavigationActivity.this, NavigationActivity.this);
        setSupportActionBar(toolbar);

        volleyStart();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTts.speak(msg, TextToSpeech.QUEUE_ADD, null);
            }
        });

        mBeaconManager = new BeaconManager(getApplicationContext());
        mBeaconManager.setForegroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);

        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                mBeaconManager.startMonitoring(ALL_ESTIMOTE_BEACONS_REGION);
            }
        });

        mBeaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                Log.i(TAG, "procurando...");
                if (!list.isEmpty()) {
                    msg = list.get(0).getMacAddress().toStandardString();
                    mTextView.setText(msg);
                } else {
                    
                }
            }
        });

    }

    private void volleyStart() {
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        Network network = new BasicNetwork(new HurlStack());

        mVolleyQueue = new RequestQueue(cache, network);

        mVolleyQueue.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                mBeaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
            }
        });
    }

    @Override
    protected void onPause() {
        mBeaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
        super.onPause();
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

    @Override
    public void onStop() {
        mTts.stop();
        mTts.shutdown();
        super.onStop();
        Log.i(TAG, "onStop");
        mBeaconManager.disconnect();
    }

    @Override
    public void onDestroy() {
        mTts.stop();
        mTts.shutdown();
        super.onDestroy();
        mBeaconManager.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
    }
}
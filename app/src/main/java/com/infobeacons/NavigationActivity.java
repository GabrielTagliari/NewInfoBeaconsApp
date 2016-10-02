package com.infobeacons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NavigationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String AUDIO_PRONTO = "Beacon Encontrado. Para ouvir clique no botão play.";
    private String TAG = "NavigationActivity";

    /* Constantes */
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private static final String NAO_SUPORTA_TTS = "Seu dispositvo não suporta o texto para voz";
    private static final String EMPTY = "";

    /* URL */
    private String mUrl = "http://infobeacons.mybluemix.net/listBeacons/";

    /* Volley */
    private RequestQueue mVolleyQueue;
    private JSONObject mBeaconEncontrado;

    /* Botões */
    private FloatingActionButton play;
    private FloatingActionButton stop;

    /* Toolbar */
    private Toolbar toolbar;

    private TextView mTextView;
    private ImageView mImageView;
    private Vibrator mVibrator;

    private BeaconManager mBeaconManager;

    private String mBeaconMaisProximo = EMPTY;

    private TextToSpeech mTts;
    private String mMsg = EMPTY;
    private String mImg = EMPTY;
    private String mTitle = "InfoBeacons";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mTextView = (TextView) findViewById(R.id.texto);
        mImageView = (ImageView) findViewById(R.id.imagem);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mTts = new TextToSpeech(NavigationActivity.this, NavigationActivity.this);

        setSupportActionBar(toolbar);

        volleyStart();

        play = (FloatingActionButton) findViewById(R.id.play);
        stop = (FloatingActionButton) findViewById(R.id.stop);

        play.setVisibility(View.VISIBLE);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTts.speak(mMsg, TextToSpeech.QUEUE_FLUSH, null);
                play.setVisibility(View.GONE);
                stop.setVisibility(View.VISIBLE);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTts.stop();
                stop.setVisibility(View.GONE);
                play.setVisibility(View.VISIBLE);
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
                    final String nearestBeacon = list.get(0).getMacAddress().toStandardString();

                    if (!mBeaconMaisProximo.equals(nearestBeacon)) {

                        limpaMsgAndImg();

                        mBeaconMaisProximo = nearestBeacon;

                        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                                (Request.Method.GET, mUrl+mBeaconMaisProximo, null, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            mBeaconEncontrado = new JSONObject(String.valueOf(response));
                                            Log.i(TAG, "beaconEncontrado: "+mBeaconEncontrado);

                                            try {
                                                if (mBeaconEncontrado.get("mac").equals(nearestBeacon)) {
                                                    mTitle = (String) mBeaconEncontrado.get("name");
                                                    CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
                                                    collapsingToolbarLayout.setTitle(mTitle);
                                                    mMsg = (String) mBeaconEncontrado.get("text");
                                                    mImg = (String) mBeaconEncontrado.get("img");
                                                }
                                            } catch (JSONException e) {
                                                Log.e(TAG, "Erro json: " + e);
                                            }

                                            if (!mMsg.equals(EMPTY) && !mImg.equals(EMPTY)) {
                                                byte[] decodedString = Base64.decode(mImg, Base64.DEFAULT);
                                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                                mVibrator.vibrate(500);
                                                mImageView.setImageBitmap(decodedByte);
                                                mTextView.setText(mMsg);
                                                Toast.makeText(getApplicationContext(), AUDIO_PRONTO, Toast.LENGTH_LONG).show();
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            mBeaconMaisProximo = EMPTY;
                                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.i(TAG, "Error: " + error);
                                        mBeaconMaisProximo = EMPTY;
                                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        mVolleyQueue.add(jsObjRequest);
                    } else {
                        Log.i(TAG, "Continua o mesmo");
                    }
                } else {
                    mBeaconMaisProximo = EMPTY;
                    Log.i(TAG, "Não encontrado nenhum beacon");
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

    private void limpaMsgAndImg() {
        mMsg = EMPTY;
        mImg= EMPTY;
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
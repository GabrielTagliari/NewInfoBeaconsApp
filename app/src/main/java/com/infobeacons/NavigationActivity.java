package com.infobeacons;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

    private static final String AUDIO_PRONTO = "Dados encontrados.\nPara ouvir clique no botão play.";
    private static final String MSG_INTERNET = "Necessária conexão com internet";
    private static final String ERRO_INESPERADO = "[Erro] Tentando novamente...";
    private String TAG = "NavigationActivity";

    /* Constantes */
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private static final String NAO_SUPORTA_TTS = "Seu dispositvo não suporta o texto p ara voz";
    private static final String EMPTY = "";

    /* URL */
    private String mUrl = "http://infobeacons2.mybluemix.net/listBeacons/";

    /* Volley */
    private RequestQueue mVolleyQueue;
    private JSONObject mBeaconEncontrado;

    /* Botões */
    private FloatingActionButton play;
    private FloatingActionButton stop;

    /* Toolbar */
    private Toolbar toolbar;

    private CollapsingToolbarLayout collapsingToolbarLayout;

    private ProgressBar mProgress;

    private TextView mTextView;
    private ImageView mImageView;
    private Vibrator mVibrator;

    private BeaconManager mBeaconManager;

    private String mBeaconAtual = EMPTY;

    private TextToSpeech mTts;
    private String mMsg = EMPTY;
    private String mImg = EMPTY;
    private String mTitle = "InfoBeacons";

    private AlertDialog alerta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mTextView = (TextView) findViewById(R.id.texto);
        mImageView = (ImageView) findViewById(R.id.imagem);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mTts = new TextToSpeech(NavigationActivity.this, NavigationActivity.this);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);

        mProgress.setVisibility(View.VISIBLE);

        setSupportActionBar(toolbar);
        collapsingToolbarLayout.setTitle("Procurando...");

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

                if (!isOnline()) {
                    Toast.makeText(getApplicationContext(), MSG_INTERNET, Toast.LENGTH_SHORT).show();
                } else {
                    if (!list.isEmpty()) {
                        final String mBeaconProximo = list.get(0).getMacAddress().toStandardString();

                        if (!mBeaconAtual.equals(mBeaconProximo)) {

                            limpaMsgAndImg();
                            mBeaconAtual = mBeaconProximo;

                            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                                    (Request.Method.GET, mUrl + mBeaconAtual, null, new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            try {
                                                mBeaconEncontrado = new JSONObject(String.valueOf(response));
                                                try {
                                                    if (mBeaconEncontrado.get("mac").equals(mBeaconProximo)) {
                                                        mProgress.setVisibility(View.GONE);
                                                        mTitle = (String) mBeaconEncontrado.get("name");
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
                                                mBeaconAtual = EMPTY;
                                            }
                                        }
                                    }, new Response.ErrorListener() {

                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.i(TAG, "Error: " + error);
                                            limpaMsgAndImg();
                                            mImageView.setImageBitmap(null);
                                            mTextView.setText(EMPTY);
                                            collapsingToolbarLayout.setTitle("Procurando...");
                                            mProgress.setVisibility(View.VISIBLE);
                                            mBeaconAtual = EMPTY;
                                        }
                                    });

                            mVolleyQueue.add(jsObjRequest);
                        } else {
                            Log.i(TAG, "Continua o mesmo");
                        }
                    } else {
                        limpaMsgAndImg();
                        mImageView.setImageBitmap(null);
                        mTextView.setText(EMPTY);
                        collapsingToolbarLayout.setTitle("Procurando...");
                        mProgress.setVisibility(View.VISIBLE);
                        mBeaconAtual = EMPTY;
                        Log.i(TAG, "Nenhum beacon encontrado.");
                    }
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
        mImg = EMPTY;
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Ajuda");
            builder.setMessage("Necessário: Bluetooth e conexão com a internet (Wifi ou 3G)\n" +
                    "Ao se aproximar do objeto haverá uma vibração " +
                    "e será carregada a informação.");
            builder.setPositiveButton("Entendi", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                }
            });
            alerta = builder.create();
            alerta.show();
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
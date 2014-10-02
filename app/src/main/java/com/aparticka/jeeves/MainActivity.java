package com.aparticka.jeeves;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.aparticka.jeeves.models.VoiceCommand;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity implements View.OnClickListener, ListView.OnItemClickListener {

    protected SpeechRecognizer mRecognizer;
    protected TextView mTextViewCommand, mTextViewStatus;
    protected RequestQueue mRequestQueue;
    protected int mColorSuccess, mColorFailure, mColorTransparent;
    protected DrawerLayout mLayoutDrawer;
    protected ListView mListViewDrawerLeft;
    protected ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnStartSpeechRecognition = (Button) findViewById(R.id.btnStartVoiceRecognition);
        btnStartSpeechRecognition.setOnClickListener(this);
        mTextViewCommand = (TextView) findViewById(R.id.textViewCommand);
        mTextViewStatus = (TextView) findViewById(R.id.textViewStatus);
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(new MyListener());
        mRequestQueue = Volley.newRequestQueue(this);
        mColorSuccess = Color.parseColor("#aaffaa");
        mColorFailure = Color.parseColor("#ffaaaa");
        mColorTransparent = Color.parseColor("#00ffffff");

        mLayoutDrawer = (DrawerLayout) findViewById(R.id.layoutDrawer);
        mListViewDrawerLeft = (ListView) findViewById(R.id.listViewDrawerLeft);
        mListViewDrawerLeft.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, (String[])VoiceCommand.commandNames.values().toArray(new String[0])));
        mListViewDrawerLeft.setOnItemClickListener(this);

        mDrawerToggle = new ActionBarDrawerToggle(this, mLayoutDrawer, R.drawable.ic_launcher, R.string.drawer_open, R.string.drawer_close) {
        };

        mLayoutDrawer.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    class MyListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle bundle) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onError(int i) {
            mTextViewStatus.setText("error" + i);
        }

        @Override
        public void onResults(Bundle bundle) {
            ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String resultString = (data.size() > 0) ? data.get(0) : "";
            executeVoiceCommand(resultString);
        }

        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    }

    public String getUrl(String action) {
        return getUrl(action, new HashMap<String, String>());
    }

    public String getUrl(String action, Map<String, String> params) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder
                .scheme("http")
                .authority("home-automation-dev.particka.local")
                .appendPath(action);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }
        return uriBuilder.build().toString();
    }

    public void executeVoiceCommand(String speechResults) {
        VoiceCommand command = parseSpeechResults(speechResults);
        if (command != null) {
            mTextViewCommand.setText(command.toString());
            JsonObjectRequest request;
            switch (command.getCommand()) {
                case VoiceCommand.COMMAND_PLAY_TRACK:
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("q", command.getArgument());
                    mTextViewStatus.setBackgroundColor(mColorTransparent);
                    mTextViewStatus.setText("making request");
                    request = new JsonObjectRequest(Request.Method.GET, getUrl("play-track", params), null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            mTextViewStatus.setBackgroundColor(mColorSuccess);
                            mTextViewStatus.setText("request successful");
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            mTextViewStatus.setBackgroundColor(mColorFailure);
                            mTextViewStatus.setText("request failed");
                        }
                    });
                    request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    mRequestQueue.add(request);
                    break;
                case VoiceCommand.COMMAND_PAUSE:
                    mTextViewStatus.setBackgroundColor(mColorTransparent);
                    mTextViewStatus.setText("making request");
                    request = new JsonObjectRequest(Request.Method.GET, getUrl("pause"), null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            mTextViewStatus.setBackgroundColor(mColorSuccess);
                            mTextViewStatus.setText("request successful");
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            mTextViewStatus.setBackgroundColor(mColorFailure);
                            mTextViewStatus.setText("request failed");
                        }
                    });
                    request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    mRequestQueue.add(request);
                    break;
                case VoiceCommand.COMMAND_UNPAUSE:
                    mTextViewStatus.setBackgroundColor(mColorTransparent);
                    mTextViewStatus.setText("making request");
                    request = new JsonObjectRequest(Request.Method.GET, getUrl("unpause"), null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            mTextViewStatus.setBackgroundColor(mColorSuccess);
                            mTextViewStatus.setText("request successful");
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            mTextViewStatus.setBackgroundColor(mColorFailure);
                            mTextViewStatus.setText("request failed");
                        }
                    });
                    request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    mRequestQueue.add(request);
                    break;
                case VoiceCommand.COMMAND_PLAY_ARTIST:
                case VoiceCommand.COMMAND_PLAY_ALBUM:
                case VoiceCommand.COMMAND_PLAY:
                    mTextViewStatus.setBackgroundColor(mColorFailure);
                    mTextViewStatus.setText("command not implemented yet");
                    break;
            }
        } else {
            mTextViewStatus.setBackgroundColor(mColorFailure);
            mTextViewCommand.setText("");
            mTextViewStatus.setText("command not recognized");
        }
    }

    public String formatSpeechResults(String results) {
        results = results.toLowerCase();
        results = results.replace("plate rack", "play track");
        return results;
    }

    public VoiceCommand parseSpeechResults(String results) {
        VoiceCommand command = new VoiceCommand();
        results = formatSpeechResults(results);
        String[] splitResults = results.split(" ");
        int numWords = splitResults.length;
        if (numWords > 0) {
            switch (splitResults[0]) {
                case "play":
                    if (numWords > 1) {
                        int start = 0;
                        switch (splitResults[1]) {
                            case "song":
                            case "track":
                                command.setCommand(VoiceCommand.COMMAND_PLAY_TRACK);
                                break;
                            case "artist":
                            case "band":
                                command.setCommand(VoiceCommand.COMMAND_PLAY_ARTIST);
                                break;
                            default:
                                command.setCommand(VoiceCommand.COMMAND_PLAY);
                                start = 1;
                                break;
                        }
                        if (command.getCommand() != VoiceCommand.COMMAND_PLAY) {
                            if (numWords > 2) {
                                start = 2;
                            } else {
                                return null;
                            }
                        }
                        String argument = "";
                        for (int i = start; i < numWords; i++) {
                            argument += splitResults[i] + " ";
                        }
                        argument = argument.substring(0, argument.length() - 1);
                        command.setArgument(argument);
                    } else {
                        command.setCommand(VoiceCommand.COMMAND_UNPAUSE);
                    }
                    break;
                case "pause":
                case "stop":
                    command.setCommand(VoiceCommand.COMMAND_PAUSE);
                    break;
                case "unpause":
                    command.setCommand(VoiceCommand.COMMAND_UNPAUSE);
                    break;
                default:
                    return null;
            }
        } else {
            return null;
        }
        return command;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnStartVoiceRecognition) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mRecognizer.startListening(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecognizer.destroy();
    }
}

package com.aparticka.jeeves;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
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
import com.aparticka.jeeves.models.Command;
import com.aparticka.jeeves.models.CommandType;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity
        implements View.OnClickListener, ListView.OnItemClickListener {

    protected SpeechRecognizer mRecognizer;
    protected TextView mTextViewCommand, mTextViewStatus;
    protected RequestQueue mRequestQueue;
    protected int mColorSuccess, mColorFailure;
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected ArrayList<String> mCommandTypes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnStartSpeechRecognition = (Button) findViewById(R.id.button_beckon);
        btnStartSpeechRecognition.setOnClickListener(this);
        mTextViewCommand = (TextView) findViewById(R.id.text_view_command);
        mTextViewStatus = (TextView) findViewById(R.id.text_view_status);
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(new MyListener());
        mRequestQueue = Volley.newRequestQueue(this);
        mColorSuccess = Color.parseColor("#aaffaa");
        mColorFailure = Color.parseColor("#ffaaaa");

        mDrawerLayout = (DrawerLayout) findViewById(R.id.layout_drawer);
        mDrawerList = (ListView) findViewById(R.id.list_view_drawer);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mCommandTypes = new ArrayList<>(CommandType.commandTypeNames.values());
        mDrawerList.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mCommandTypes));
        mDrawerList.setOnItemClickListener(this);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
            }
            public void onDrawerOpened(View view) {
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("MainActivity", "clicked on " + mCommandTypes.get(position));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
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
            mTextViewStatus.setBackgroundColor(mColorFailure);
            mTextViewStatus.setText("error " + i);
            mTextViewCommand.setText("");
        }

        @Override
        public void onResults(Bundle bundle) {
            ArrayList<String> data =
                    bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
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
        Command command = parseSpeechResults(speechResults);
        executeCommand(command);
    }

    public void executeRequest(String url,
                               final CommandRequestListener<JSONObject> commandRequestListener) {
        this.executeRequest(
                url,
                commandRequestListener,
                JSONObject.class,
                JsonObjectRequest.class);
    }

    public <T, V extends Request<?>> void executeRequest(
            String url,
            final CommandRequestListener<T> commandRequestListener,
            Class<T> objectClass,
            Class<V> requestClass) {
        mTextViewStatus.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        mTextViewStatus.setText("making request");
        try {
            Class[] argTypes = new Class[] {
                    int.class,
                    String.class,
                    objectClass,
                    Response.Listener.class,
                    Response.ErrorListener.class
            };
            Constructor<V> constructor = requestClass.getDeclaredConstructor(argTypes);
            V request = constructor.newInstance(
                    Request.Method.GET, url, null, new Response.Listener<T>() {
                @Override
                public void onResponse(T element) {
                    mTextViewStatus.setBackgroundColor(mColorSuccess);
                    mTextViewStatus.setText("request successful");
                    if (commandRequestListener != null) {
                        commandRequestListener.onResponse(element);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    mTextViewStatus.setBackgroundColor(mColorFailure);
                    mTextViewStatus.setText("request failed");
                    if (commandRequestListener != null) {
                        commandRequestListener.onErrorResponse(volleyError);
                    }
                }
            });
            request.setRetryPolicy(
                    new DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            mRequestQueue.add((Request<?>)request);
        } catch (InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException e) {
            mTextViewStatus.setBackgroundColor(mColorFailure);
            mTextViewStatus.setText("error occurred");
            Log.e("executeRequest", e.toString());
            e.printStackTrace();
        }
    }

    public void commandPlayTrack(String argument) {
        HashMap<String, String> params = new HashMap<>();
        params.put("q", argument);
        executeRequest(getUrl("play-track", params), new CommandRequestListener<JSONObject>() {
            @Override
            public void onResponse(JSONObject element) {
                // do something with the response
            }
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
    }

    public void commandPauseTrack() {
        executeRequest(getUrl("pause"), null);
    }

    public void commandUnpauseTrack() {
        executeRequest(getUrl("unpause"), null);
    }

    public void executeCommand(Command command) {
        if (command != null) {
            mTextViewCommand.setText(command.toString());
            switch (command.getCommand()) {
                case Command.COMMAND_PLAY_TRACK:
                    commandPlayTrack(command.getArgument());
                    break;
                case Command.COMMAND_PAUSE:
                    commandPauseTrack();
                    break;
                case Command.COMMAND_UNPAUSE:
                    commandUnpauseTrack();
                    break;
                case Command.COMMAND_PLAY_ARTIST:
                case Command.COMMAND_PLAY_ALBUM:
                case Command.COMMAND_PLAY:
                    mTextViewStatus.setBackgroundColor(mColorFailure);
                    mTextViewStatus.setText("command not implemented yet");
                    break;
            }
        } else {
            mTextViewStatus.setBackgroundColor(mColorFailure);
            mTextViewStatus.setText("command not recognized");
            mTextViewCommand.setText("");
        }
    }

    public String formatSpeechResults(String results) {
        results = results.toLowerCase();
        results = results.replace("plate rack", "play track");
        return results;
    }

    public Command parseSpeechResults(String results) {
        Command command = new Command();
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
                            case "songs":
                            case "track":
                                command.setCommand(Command.COMMAND_PLAY_TRACK);
                                break;
                            case "artist":
                            case "band":
                                command.setCommand(Command.COMMAND_PLAY_ARTIST);
                                break;
                            default:
                                command.setCommand(Command.COMMAND_PLAY);
                                start = 1;
                                break;
                        }
                        if (command.getCommand() != Command.COMMAND_PLAY) {
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
                        command.setCommand(Command.COMMAND_UNPAUSE);
                    }
                    break;
                case "pause":
                case "stop":
                    command.setCommand(Command.COMMAND_PAUSE);
                    break;
                case "unpause":
                    command.setCommand(Command.COMMAND_UNPAUSE);
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
        } else if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_beckon) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mRecognizer.startListening(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecognizer.destroy();
    }
}

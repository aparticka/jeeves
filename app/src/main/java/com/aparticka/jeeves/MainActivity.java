package com.aparticka.jeeves;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
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
import com.aparticka.jeeves.models.Mode;

import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity
        implements View.OnClickListener, ListView.OnItemClickListener {

    protected static final String TAG_MODE = "mode";

    protected SpeechRecognizer mRecognizer;
    protected TextView mTextViewCommand, mTextViewStatus;
    protected RequestQueue mRequestQueue;
    protected int mColorSuccess, mColorFailure, mColorBtnNotSpeaking, mColorBtnSpeaking;
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected ArrayList<String> mCommandTypes;
    protected int mCurrentMode;
    protected Menu mMainMenu;
    protected Button mBtnStartSpeechRecognition;
    protected AudioManager mAudioManager;
    protected int mMaxDetachedVolumeLevel;
    protected boolean mIsListening, mDisregardSpeechErrors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnStartSpeechRecognition = (Button) findViewById(R.id.button_beckon);
        mBtnStartSpeechRecognition.setOnClickListener(this);
        mTextViewCommand = (TextView) findViewById(R.id.text_view_command);
        mTextViewStatus = (TextView) findViewById(R.id.text_view_status);
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(new MyListener());
        mRequestQueue = Volley.newRequestQueue(this);
        mColorSuccess = Color.parseColor("#aaffaa");
        mColorFailure = Color.parseColor("#ffaaaa");
        mColorBtnNotSpeaking = Color.parseColor("#ff333333");
        mColorBtnSpeaking = Color.parseColor("#ff666666");

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
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (savedInstanceState != null) {
            mCurrentMode = savedInstanceState.getInt(TAG_MODE);
        } else {
            mCurrentMode = Mode.MODE_HOME;
        }

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mMaxDetachedVolumeLevel = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mIsListening = mDisregardSpeechErrors = false;
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

    public void setRecognizerListening(boolean isListening) {
        mIsListening = isListening;
        mBtnStartSpeechRecognition.setBackgroundColor(
                isListening ? mColorBtnSpeaking : mColorBtnNotSpeaking);
    }

    class MyListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            setRecognizerListening(true);
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
            Log.e("SpeechRecognizer::onError", String.format("%d", i));
            setRecognizerListening(false);
            if (!mDisregardSpeechErrors) {
                String error;
                switch (i) {
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        error = "no speech input";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        error = "speech recognition busy";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        error = "no recognition result";
                        break;
                    case SpeechRecognizer.ERROR_AUDIO:
                        error = "audio input error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        error = "network error";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        error = "recognition server error";
                        break;
                    default:
                        error = "speech recognition error";
                        break;
                }
                changeStatusFailure(error);
                mTextViewCommand.setText("");
            }
            mDisregardSpeechErrors = false;
        }

        @Override
        public void onResults(Bundle bundle) {
            setRecognizerListening(false);
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
        changeStatus("making request");
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
                    changeStatusSuccess("request successful");
                    if (commandRequestListener != null) {
                        commandRequestListener.onResponse(element);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    changeStatusFailure("request failed");
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
            changeStatusFailure("error occurred");
            Log.e("executeRequest", e.toString());
            e.printStackTrace();
        }
    }

    public void changeStatus(String message) {
        changeStatus(message, getResources().getColor(android.R.color.transparent));
    }

    public void changeStatus(String message, int color) {
        mTextViewStatus.setText(message);
        mTextViewStatus.setBackgroundColor(color);
    }

    public void changeStatusSuccess(String message) {
        changeStatus(message, mColorSuccess);
    }

    public void changeStatusFailure(String message) {
        changeStatus(message, mColorFailure);
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

    public void commandPreviousTrack() {
        executeRequest(getUrl("prev"), null);
    }

    public void commandNextTrack() {
        executeRequest(getUrl("next"), null);
    }

    public boolean setMode(int mode) {
        if (!setModeMenuItem(mode)) {
            return false;
        }
        if (mIsListening) {
            mDisregardSpeechErrors = true;
            mRecognizer.stopListening();
        }
        changeStatusSuccess(String.format("mode [%s] set successfully", Mode.getModeName(mode)));
        mCurrentMode = mode;
        return true;
    }

    public boolean setModeMenuItem(int mode) {
        MenuItem menuItem = mMainMenu.findItem(R.id.action_mode);
        Drawable icon;
        String title;
        switch (mode) {
            case Mode.MODE_HOME:
                icon = getResources().getDrawable(R.drawable.ic_fa_home);
                title = getString(R.string.action_mode_home);
                break;
            case Mode.MODE_DETACHED:
                icon = getResources().getDrawable(R.drawable.ic_fa_unlink);
                title = getString(R.string.action_mode_detached);
                break;
            default:
                changeStatusFailure("mode does not exist");
                return false;
        }
        menuItem.setIcon(icon);
        menuItem.setTitle(title);
        return true;
    }

    public void executeCommand(Command command) {
        if (command != null) {
            mTextViewCommand.setText(command.toString());
            if (command.getCommand() == Command.COMMAND_SET_MODE) {
                switch (command.getArgument()) {
                    case "home":
                        setMode(Mode.MODE_HOME);
                        break;
                    case "detached":
                    case "phone":
                    case "mobile":
                        setMode(Mode.MODE_DETACHED);
                        break;
                    default:
                        commandNotRecognized();
                        break;
                }
            } else {
                switch (mCurrentMode) {
                    case Mode.MODE_HOME:
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
                            case Command.COMMAND_PREV:
                                commandPreviousTrack();
                                break;
                            case Command.COMMAND_NEXT:
                                commandNextTrack();
                                break;
                            default:
                                commandNotImplemented();
                                break;
                        }
                        break;
                    case Mode.MODE_DETACHED:
                        switch (command.getCommand()) {
                            case Command.COMMAND_PAUSE:
                                commandDetachedPauseTrack();
                                break;
                            case Command.COMMAND_UNPAUSE:
                                commandDetachedUnpauseTrack();
                                break;
                            case Command.COMMAND_PREV:
                                commandDetachedPrevTrack();
                                break;
                            case Command.COMMAND_NEXT:
                                commandDetachedNextTrack();
                                break;
                            case Command.COMMAND_SET_VOLUME:
                                commandDetachedSetVolume(command.getArgument());
                                break;
                            default:
                                commandNotImplemented();
                                break;
                        }
                }
            }
        } else {
            commandNotRecognized();
        }
    }

    public void commandDetachedPauseTrack() {
        sendMediaIntent(KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    public void commandDetachedUnpauseTrack() {
        sendMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    public void commandDetachedPrevTrack() {
        sendMediaIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    public void commandDetachedNextTrack() {
        sendMediaIntent(KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    public String formatVolumeArgument(String arg) {
        switch (arg) {
            case "won":
                return "1";
            case "to":
            case "too":
                return "2";
            case "free":
                return "3";
            case "for":
                return "4";
            case "sex":
                return "6";
            case "ate":
                return "8";
            case "tin":
                return "10";
            case "maxed":
            case "maximum":
                return "max";
            case "halve":
            case "have":
                return "half";
            case "muted":
            case "off":
                return "mute";
            case "unmuted":
            case "on":
                return "unmute";
            default:
                return arg;
        }
    }

    public void commandDetachedSetVolume(String arg) {
        try {
            int level = Integer.parseInt(arg);
            if ((level >= 0) && (level <= mMaxDetachedVolumeLevel)) {
                setDetachedVolume(level);
            } else {
                changeStatusFailure(
                        String.format("invalid volume level [0-%d]", mMaxDetachedVolumeLevel));
                return;
            }
        } catch (NumberFormatException e) {
            switch (arg) {
                case "max":
                    setDetachedVolume(mMaxDetachedVolumeLevel);
                    break;
                case "half":
                    setDetachedVolume(mMaxDetachedVolumeLevel / 2);
                    break;
                case "mute":
                    setDetachedVolumeMuted();
                    break;
                case "unmute":
                    setDetachedVolumeMuted(false);
                    break;
                default:
                    changeStatusFailure("volume command not recognized");
                    return;
            }
        }
        changeStatusSuccess("volume set successfully");
    }

    public void setDetachedVolume(int level) {
        setDetachedVolumeMuted(false);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level,
                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    public void setDetachedVolumeMuted() {
        setDetachedVolumeMuted(true);
    }

    public void setDetachedVolumeMuted(boolean mute) {
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
    }

    public void sendMediaIntent(int key) {
        final Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, key);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, event);
        this.sendBroadcast(intent);
        changeStatusSuccess("request successful");
    }

    public void commandNotImplemented() {
        changeStatusFailure("command not implemented yet");
    }

    public void commandNotRecognized() {
        changeStatusFailure("command not recognized");
        mTextViewCommand.setText("");
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
                case "prev":
                case "previous":
                case "back":
                    command.setCommand(Command.COMMAND_PREV);
                    break;
                case "next":
                case "forward":
                    command.setCommand(Command.COMMAND_NEXT);
                    break;
                case "set":
                    if (numWords > 1) {
                        switch (splitResults[1]) {
                            case "mode":
                                if (numWords > 2) {
                                    command.setCommand(Command.COMMAND_SET_MODE);
                                    command.setArgument(splitResults[2]);
                                } else {
                                    return null;
                                }
                                break;
                            case "volume":
                                if (numWords > 2) {
                                    command.setCommand(Command.COMMAND_SET_VOLUME);
                                    command.setArgument(formatVolumeArgument(splitResults[2]));
                                } else {
                                    return null;
                                }
                                break;
                        }
                    } else {
                        return null;
                    }
                    break;
                case "mute":
                case "unmute":
                    command.setCommand(Command.COMMAND_SET_VOLUME);
                    command.setArgument(splitResults[0]);
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
        mMainMenu = menu;
        setModeMenuItem(mCurrentMode);
        return true;
    }

    public void toggleMode() {
        if (!setMode(mCurrentMode + 1)) {
            setMode(Mode.MODE_HOME);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mode:
                toggleMode();
                break;
            default:
                if (mDrawerToggle.onOptionsItemSelected(item)) {
                    return true;
                }
                break;
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(TAG_MODE, mCurrentMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        boolean actionTaken = false;
        if (mIsListening) {
            actionTaken = true;
            mRecognizer.stopListening();
        }
        if (mDrawerLayout.isDrawerVisible(mDrawerList)) {
            actionTaken = true;
            mDrawerLayout.closeDrawer(mDrawerList);
        }
        if (!actionTaken) {
            super.onBackPressed();
        }
    }
}

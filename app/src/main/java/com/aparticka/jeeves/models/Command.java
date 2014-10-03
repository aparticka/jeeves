package com.aparticka.jeeves.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Adam on 10/1/2014.
 */
public class Command {

    protected int command;
    protected String argument;
    protected int commandType;

    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PLAY_TRACK = 1;
    public static final int COMMAND_PLAY_ARTIST = 2;
    public static final int COMMAND_PLAY_ALBUM = 3;
    public static final int COMMAND_PAUSE = 20;
    public static final int COMMAND_UNPAUSE = 21;

    public static final Map<Integer, String> commandNames;

    static {
        Map<Integer, String> copyMap = new HashMap<Integer, String>();
        copyMap.put(COMMAND_PLAY, "play any");
        copyMap.put(COMMAND_PLAY_TRACK, "play track");
        copyMap.put(COMMAND_PLAY_ARTIST, "play artist");
        copyMap.put(COMMAND_PLAY_ALBUM, "play album");
        copyMap.put(COMMAND_PAUSE, "pause");
        copyMap.put(COMMAND_UNPAUSE, "unpause");
        commandNames = Collections.unmodifiableMap(copyMap);
    }

    public int getCommand() {
        return this.command;
    }
    public void setCommand(int command) {
        this.command = command;
    }

    public String getArgument() {
        return (this.argument == null) ? "" : this.argument;
    }
    public void setArgument(String argument) {
        this.argument = argument;
    }

    public int getCommandType() {
        return this.commandType;
    }
    public void setCommandType(int commandType) {
        this.commandType = commandType;
    }

    public String getCommandName(int command) {
        return commandNames.containsKey(command) ? commandNames.get(command) : "N/A";
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", getCommandName(getCommand()), getArgument());
    }
}

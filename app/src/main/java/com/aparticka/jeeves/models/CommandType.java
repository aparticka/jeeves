package com.aparticka.jeeves.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Adam on 10/3/2014.
 */
public class CommandType {

    public static final int COMMAND_TYPE_SPOTIFY = 0;

    public static final Map<Integer, String> commandTypeNames;

    static {
        Map<Integer, String> copyMap = new HashMap<Integer, String>();
        copyMap.put(COMMAND_TYPE_SPOTIFY, "Spotify");
        commandTypeNames = Collections.unmodifiableMap(copyMap);
    }

    public static String getCommandTypeName(int commandType) {
        return commandTypeNames.containsKey(commandType) ? commandTypeNames.get(commandType) : "N/A";
    }
}

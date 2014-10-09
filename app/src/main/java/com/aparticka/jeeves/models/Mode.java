package com.aparticka.jeeves.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Adam on 10/8/2014.
 */
public class Mode {

    public static final int MODE_HOME = 0;
    public static final int MODE_DETACHED = 1;

    public static final Map<Integer, String> modeNames;

    static {
        Map<Integer, String> copyMap = new HashMap<Integer, String>();
        copyMap.put(MODE_HOME, "home");
        copyMap.put(MODE_DETACHED, "detached");
        modeNames = Collections.unmodifiableMap(copyMap);
    }

    public static String getModeName(int mode) {
        return modeNames.containsKey(mode) ?
                modeNames.get(mode) : "N/A";
    }
}

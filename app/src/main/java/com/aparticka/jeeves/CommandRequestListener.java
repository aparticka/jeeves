package com.aparticka.jeeves;

import com.android.volley.VolleyError;

/**
 * Created by Adam on 10/3/2014.
 */
public interface CommandRequestListener<T> {
    void onResponse(T element);
    void onErrorResponse(VolleyError error);
}

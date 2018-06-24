package com.stretchsense.ten_channel_ble;

import android.app.Application;
import android.content.Context;

/**
 * Created by r.teotia on 15/01/2018.
 */

public class StretchSenseApplication extends Application {



    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static StretchSenseApplication get(Context context) {
        return (StretchSenseApplication) context.getApplicationContext();
    }
}

package com.zer0.possessor;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public class MainActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final Context context = getApplicationContext();
        MainService.startPossessor(context);

        super.finish();
    }
}

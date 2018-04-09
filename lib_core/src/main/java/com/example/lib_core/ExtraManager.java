package com.example.lib_core;

import android.app.Activity;
import android.os.Bundle;
import android.util.LruCache;

/**
 * Created by janecer on 2018/4/2 0002.
 * email:janecer@sina.cn
 */
public class ExtraManager {


    public static final String SUFFIX_AUTOWIRED = "$$Extra";

    private static ExtraManager instance;
    private LruCache<String, IExtra> classCache;

    public static ExtraManager getInstance() {
        if (instance == null) {
            synchronized (ExtraManager.class) {
                if (instance == null) {
                    instance = new ExtraManager();
                }
            }
        }
        return instance;
    }


    public ExtraManager() {
        classCache = new LruCache<>(66);
    }


    /**
     * 注入
     *
     * @param instance
     */
    public void loadExtras(Activity instance, Bundle bundle) {
        //查找对应activity的缓存
        getIExtra(instance).loadExtra(instance,bundle);
    }

    public void saveExtras(Activity instance,Bundle bundle) {
        getIExtra(instance).saveExtra(instance,bundle);
    }

    private IExtra getIExtra(Activity instance) {
        //查找对应activity的缓存
        String className = instance.getClass().getName();
        IExtra iExtra = classCache.get(className);
        try {
            if (null == iExtra) {
                iExtra = (IExtra) Class.forName(instance.getClass().getName() +
                        SUFFIX_AUTOWIRED).getConstructor().newInstance();
            }
            classCache.put(className, iExtra);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iExtra ;
    }
}

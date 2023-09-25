/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.hapjs.render.jsruntime.serialize.HandlerObject;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;

public class InstanceManager {
    public static final String INST_ID = "instId";
    protected static final String NAME = "name";
    protected static final String NATIVE_TYPE = "_nativeType";
    protected static final String INST_HANDLER = "instHandler";
    private static final String TAG = "InstanceManager";
    private AtomicInteger mIndex = new AtomicInteger(0);
    private ConcurrentHashMap<Integer, InstanceHolder> mCacheInstanceMap =
            new ConcurrentHashMap<>();

    private InstanceManager() {
    }

    public static InstanceManager getInstance() {
        return Holder.instance;
    }

    public <T extends IInstance> T getInstance(int id) {
        InstanceHolder instanceData = mCacheInstanceMap.get(id);
        return instanceData == null ? null : ((T) instanceData.instance);
    }

    public JavaSerializeObject createInstance(HybridManager hybridManager, IInstance instance) {
        int id = mIndex.incrementAndGet();
        mCacheInstanceMap.put(id, new InstanceHolder(hybridManager, instance));
        return createInstanceObject(instance.getFeatureName(), id);
    }

    public void removeInstance(int id) {
        mCacheInstanceMap.remove(id);
    }

    public void dispose(HybridManager hybridManager, boolean force) {
        for (Map.Entry<Integer, InstanceHolder> entry : mCacheInstanceMap.entrySet()) {
            Integer id = entry.getKey();
            InstanceHolder instanceData = entry.getValue();
            if (instanceData.hybridManager == hybridManager) {
                if (instanceData.instance instanceof FeatureExtension) {
                    ((FeatureExtension) instanceData.instance).dispose(force);
                }
                if (force) {
                    removeInstance(id);
                    if (instanceData.instance != null) {
                        instanceData.instance.release();
                    }
                }
            }
        }
    }

    private JavaSerializeObject createInstanceObject(String featureName, int id) {
        JavaSerializeObject javaBridgeObject = new JavaSerializeObject();
        javaBridgeObject.put(NAME, featureName);
        javaBridgeObject.put(INST_ID, id);
        javaBridgeObject.put(NATIVE_TYPE, Extension.NativeType.INSTANCE.ordinal());
        javaBridgeObject.put(INST_HANDLER, new InstanceHandler(id));
        return javaBridgeObject;
    }

    public interface IInstance {
        void release();

        String getFeatureName();
    }

    private static class Holder {
        private static final InstanceManager instance = new InstanceManager();
    }

    public static class InstanceHandler implements HandlerObject {
        private int mId;

        private InstanceHandler() {}

        public InstanceHandler(int id) {
            this.mId = id;
        }

        public int getId() {
            return mId;
        }

        public void setId(int id) {
            this.mId = id;
        }

        @Override
        public V8Object toV8Object(V8 v8) {
            // 内部v8对象，用来监听对象释放情况
            V8Object releaseObj = new InstanceV8Object(v8, mId);
            releaseObj.setWeak();
            return releaseObj;
        }
    }

    private static class InstanceHolder {
        IInstance instance;
        HybridManager hybridManager;

        InstanceHolder(HybridManager hybridManager, IInstance instance) {
            this.hybridManager = hybridManager;
            this.instance = instance;
        }
    }
}

/*
 * Copyright 2013-2019 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.dataContext;

import consulo.application.Application;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.dataContext.*;
import consulo.dataContext.internal.DataManagerEx;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.collection.Maps;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.Promise;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ref.SoftReference;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public abstract class BaseDataManager implements DataManagerEx {
    public static abstract class BaseDataContext<M extends BaseDataManager, C> implements DataContext, UserDataHolder {
        private final M myDataManager;
        private final Reference<C> myRef;
        private Map<Key, Object> myUserData;
        private final Map<Key, Object> myCachedData = Maps.newWeakValueHashMap();

        public BaseDataContext(M manager, C component) {
            myDataManager = manager;
            myRef = component == null ? null : new WeakReference<>(component);
        }

        protected void clearCacheData() {
            myCachedData.clear();
        }

        protected M getDataManager() {
            return myDataManager;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getData(Key<T> dataId) {
            if (ourSafeKeys.contains(dataId)) {
                Object answer = myCachedData.get(dataId);
                if (answer == null) {
                    answer = doGetData(dataId);
                    myCachedData.put(dataId, answer == null ? ObjectUtil.NULL : answer);
                }
                return answer != ObjectUtil.NULL ? (T) answer : null;
            }
            else {
                return doGetData(dataId);
            }
        }

        protected @Nullable C getComponent() {
            return SoftReference.dereference(myRef);
        }

        protected abstract <T> T doGetData(Key<T> key);

        @Override
        public String toString() {
            return "component=" + SoftReference.dereference(myRef);
        }

        @Override
        public <T> T getUserData(Key<T> key) {
            //noinspection unchecked
            return (T) getOrCreateMap().get(key);
        }

        @Override
        public <T> void putUserData(Key<T> key, @Nullable T value) {
            getOrCreateMap().put(key, value);
        }

        private Map<Key, Object> getOrCreateMap() {
            Map<Key, Object> userData = myUserData;
            if (userData == null) {
                myUserData = userData = Maps.newWeakValueHashMap();
            }
            return userData;
        }
    }

    public static class MyUIDataContext extends BaseDataContext<BaseDataManager, consulo.ui.Component> {
        public MyUIDataContext(BaseDataManager dataManager, consulo.ui.Component component) {
            super(dataManager, component);
        }

        @Override
        @Nullable
        @SuppressWarnings("unchecked")
        protected <T> T doGetData(Key<T> dataId) {
            consulo.ui.Component component = getComponent();
            if (PlatformDataKeys.IS_MODAL_CONTEXT == dataId) {
                if (component == null) {
                    return null;
                }
                return (T) (Boolean) false; //FIXME [VISTALL] stub
            }
            if (PlatformDataKeys.CONTEXT_UI_COMPONENT == dataId) {
                return (T) component;
            }
            if (ModalityState.KEY == dataId) {
                return (T) ModalityState.nonModal(); //FIXME [VISTALL] stub
            }
            if (Editor.KEY == dataId || EditorKeys.HOST_EDITOR == dataId) {
                Editor editor = (Editor) getDataManager().getData(dataId, component);
                //return (T)validateEditor(editor);   //FIXME [VISTALL] stub
                return (T) editor;
            }
            return getDataManager().getData(dataId, component);
        }
    }

    protected static final Set<Key> ourSafeKeys = Set.of(
        Project.KEY,
        Editor.KEY,
        PlatformDataKeys.IS_MODAL_CONTEXT,
        UIExAWTDataKey.CONTEXT_COMPONENT,
        PlatformDataKeys.CONTEXT_UI_COMPONENT,
        ModalityState.KEY
    );

    private final Application myApplication;
    protected final Provider<WindowManager> myWindowManager;

    @Inject
    protected BaseDataManager(Application application, Provider<WindowManager> windowManagerProvider) {
        myApplication = application;
        myWindowManager = windowManagerProvider;
    }

    @Override
    public AsyncResult<DataContext> getDataContextFromFocus() {
        AsyncResult<DataContext> context = AsyncResult.undefined();
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> context.setDone(getDataContext()), myApplication.getCurrentModalityState());
        return context;
    }

    @Override
    public Promise<DataContext> getDataContextFromFocusAsync() {
        AsyncPromise<DataContext> result = new AsyncPromise<>();
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> result.setResult(getDataContext()), myApplication.getAnyModalityState());
        return result;
    }

    public @Nullable <T> T getDataFromProvider(DataProvider provider, Key<T> dataId, @Nullable Set<Key> alreadyComputedIds) {
        if (alreadyComputedIds != null && alreadyComputedIds.contains(dataId)) {
            return null;
        }
        try {
            T data = provider.getDataUnchecked(dataId);
            if (data != null) {
                return validated(data, dataId, provider);
            }
            return null;
        }
        finally {
            if (alreadyComputedIds != null) {
                alreadyComputedIds.remove(dataId);
            }
        }
    }

    protected static @Nullable <T> T validated(T data, Key<T> dataId, Object dataSource) {
        T invalidData = DataValidators.findInvalidData(dataId, data, dataSource);
        if (invalidData != null) {
            return null;
        }
        return data;
    }

    public DataContext getDataContextTest(consulo.ui.Component component) {
        DataContext dataContext = getDataContext(component);

        Project project = dataContext.getData(Project.KEY);
        Component focusedComponent = myWindowManager.get().getFocusedComponent(project);
        if (focusedComponent != null) {
            dataContext = getDataContext(focusedComponent);
        }
        return dataContext;
    }

    @Override
    public <T> void saveInDataContext(DataContext dataContext, Key<T> dataKey, @Nullable T data) {
        if (dataContext instanceof UserDataHolder) {
            ((UserDataHolder) dataContext).putUserData(dataKey, data);
        }
    }

    @Override
    public @Nullable <T> T loadFromDataContext(DataContext dataContext, Key<T> dataKey) {
        return dataContext instanceof UserDataHolder ? ((UserDataHolder) dataContext).getUserData(dataKey) : null;
    }

    protected <T> T getData(Key<T> dataId, consulo.ui.@Nullable Component focusedComponent) {
        for (consulo.ui.Component c = focusedComponent; c != null; c = c.getParent()) {
            DataProvider dataProvider = getDataProviderForComponent(c);
            T data = getDataFromProvider(dataProvider, dataId, null);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    protected DataProvider getDataProviderForComponent(consulo.ui.Component component) {
        if (component instanceof UiDataProvider uiProvider) {
            return new UiDataProviderAdapter(uiProvider);
        }
        return component::getUserData;
    }

    @Override
    public AsyncDataContext createAsyncDataContext(DataContext dataContext) {
        consulo.ui.Component component = dataContext.getData(PlatformDataKeys.CONTEXT_UI_COMPONENT);
        List<DataProvider> providers = new ArrayList<>();
        for (consulo.ui.Component c = component; c != null; c = c.getParent()) {
            DataProvider provider = getDataProviderForComponent(c);
            providers.add(PreCachedDataContext.initProviderForAsync(provider));
        }
        return new PreCachedDataContext(this, providers);
    }

    @Override
    public DataContext getDataContext(consulo.ui.@Nullable Component component) {
        return new MyUIDataContext(this, component);
    }
}

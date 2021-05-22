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
package consulo.ide.base;

import com.intellij.ide.DataManager;
import com.intellij.ide.impl.dataRules.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.reference.SoftReference;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.ide.impl.DataValidators;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public abstract class BaseDataManager extends DataManager {
  public static interface DataContextWithEventCount extends DataContext {
    void setEventCount(int eventCount, Object caller);
  }

  public static abstract class BaseDataContext<M extends BaseDataManager, C> implements DataContext, UserDataHolder {
    private final M myDataManager;
    private final Reference<C> myRef;
    private Map<Key, Object> myUserData;
    private final Map<Key, Object> myCachedData = ContainerUtil.createWeakValueMap();

    public BaseDataContext(M manager, C component) {
      myDataManager = manager;
      myRef = component == null ? null : new WeakReference<>(component);
    }

    protected void clearCacheData() {
      myCachedData.clear();
    }

    @Nonnull
    protected M getDataManager() {
      return myDataManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(@Nonnull Key<T> dataId) {
      if (ourSafeKeys.contains(dataId)) {
        Object answer = myCachedData.get(dataId);
        if (answer == null) {
          answer = doGetData(dataId);
          myCachedData.put(dataId, answer == null ? ObjectUtil.NULL : answer);
        }
        return answer != ObjectUtil.NULL ? (T)answer : null;
      }
      else {
        return doGetData(dataId);
      }
    }

    @Nullable
    protected C getComponent() {
      return SoftReference.dereference(myRef);
    }

    protected abstract <T> T doGetData(Key<T> key);

    @NonNls
    public String toString() {
      return "component=" + SoftReference.dereference(myRef);
    }

    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
      //noinspection unchecked
      return (T)getOrCreateMap().get(key);
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
      getOrCreateMap().put(key, value);
    }

    @Nonnull
    private Map<Key, Object> getOrCreateMap() {
      Map<Key, Object> userData = myUserData;
      if (userData == null) {
        myUserData = userData = ContainerUtil.createWeakValueMap();
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
    protected <T> T doGetData(@Nonnull Key<T> dataId) {
      consulo.ui.Component component = getComponent();
      if (PlatformDataKeys.IS_MODAL_CONTEXT == dataId) {
        if (component == null) {
          return null;
        }
        return (T)(Boolean)false; //FIXME [VISTALL] stub
      }
      if (PlatformDataKeys.CONTEXT_UI_COMPONENT == dataId) {
        return (T)component;
      }
      if (PlatformDataKeys.MODALITY_STATE == dataId) {
        return (T)ModalityState.NON_MODAL; //FIXME [VISTALL] stub
      }
      if (CommonDataKeys.EDITOR == dataId || CommonDataKeys.HOST_EDITOR == dataId) {
        Editor editor = (Editor)getDataManager().getData(dataId, component);
        //return (T)validateEditor(editor);   //FIXME [VISTALL] stub
        return (T)editor;
      }
      return getDataManager().getData(dataId, component);
    }
  }

  protected static final Set<Key> ourSafeKeys = ContainerUtil
          .newHashSet(CommonDataKeys.PROJECT, CommonDataKeys.EDITOR, PlatformDataKeys.IS_MODAL_CONTEXT, PlatformDataKeys.CONTEXT_COMPONENT, PlatformDataKeys.CONTEXT_UI_COMPONENT,
                      PlatformDataKeys.MODALITY_STATE);


  protected final ConcurrentMap<Key, GetDataRule> myDataConstantToRuleMap = new ConcurrentHashMap<>();
  protected final Provider<WindowManager> myWindowManager;

  @Inject
  protected BaseDataManager(Provider<WindowManager> windowManagerProvider) {
    myWindowManager = windowManagerProvider;

    registerRules();
  }

  private void registerRules() {
    myDataConstantToRuleMap.put(PlatformDataKeys.COPY_PROVIDER, new CopyProviderRule());
    myDataConstantToRuleMap.put(PlatformDataKeys.CUT_PROVIDER, new CutProviderRule());
    myDataConstantToRuleMap.put(PlatformDataKeys.PASTE_PROVIDER, new PasteProviderRule());
    myDataConstantToRuleMap.put(PlatformDataKeys.FILE_TEXT, new FileTextRule());
    myDataConstantToRuleMap.put(PlatformDataKeys.FILE_EDITOR, new FileEditorRule());
    myDataConstantToRuleMap.put(CommonDataKeys.NAVIGATABLE_ARRAY, new NavigatableArrayRule());
    myDataConstantToRuleMap.put(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE, new InactiveEditorRule());
  }

  @Nullable
  public <T> GetDataRule<T> getDataRule(@Nonnull Key<T> dataId) {
    GetDataRule<T> rule = getRuleFromMap(dataId);
    if (rule != null) {
      return rule;
    }

    final GetDataRule<T> plainRule = getRuleFromMap(AnActionEvent.uninjectedId(dataId));
    if (plainRule != null) {
      return new GetDataRule<T>() {
        @Nonnull
        @Override
        public Key<T> getKey() {
          return plainRule.getKey();
        }

        @Nullable
        @Override
        public T getData(@Nonnull DataProvider dataProvider) {
          return plainRule.getData(key -> dataProvider.getData(AnActionEvent.injectedId(key)));
        }
      };
    }

    return null;
  }

  @Nonnull
  @Override
  public AsyncResult<DataContext> getDataContextFromFocus() {
    AsyncResult<DataContext> context = AsyncResult.undefined();
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> context.setDone(getDataContext()), ModalityState.current());
    return context;
  }

  @Nonnull
  @Override
  public Promise<DataContext> getDataContextFromFocusAsync() {
    AsyncPromise<DataContext> result = new AsyncPromise<>();
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> result.setResult(getDataContext()), ModalityState.any());
    return result;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  protected <T> GetDataRule<T> getRuleFromMap(@Nonnull Key<T> dataId) {
    GetDataRule rule = myDataConstantToRuleMap.get(dataId);
    if (rule == null && !myDataConstantToRuleMap.containsKey(dataId)) {
      final GetDataRule[] eps = GetDataRule.EP_NAME.getExtensions();
      for (GetDataRule<?> getDataRule : eps) {
        if (getDataRule.getKey() == dataId) {
          rule = getDataRule;
        }
      }
      if (rule != null) {
        myDataConstantToRuleMap.putIfAbsent(dataId, rule);
      }
    }
    return rule;
  }

  @Nullable
  public <T> T getDataFromProvider(@Nonnull final DataProvider provider, @Nonnull Key<T> dataId, @Nullable Set<Key> alreadyComputedIds) {
    if (alreadyComputedIds != null && alreadyComputedIds.contains(dataId)) {
      return null;
    }
    try {
      T data = provider.getDataUnchecked(dataId);
      if (data != null) return validated(data, dataId, provider);

      GetDataRule<T> dataRule = getDataRule(dataId);
      if (dataRule != null) {
        final Set<Key> ids = alreadyComputedIds == null ? new HashSet<>() : alreadyComputedIds;
        ids.add(dataId);
        data = dataRule.getData(id -> getDataFromProvider(provider, id, ids));

        if (data != null) return validated(data, dataId, provider);
      }

      return null;
    }
    finally {
      if (alreadyComputedIds != null) alreadyComputedIds.remove(dataId);
    }
  }

  @Nullable
  protected static <T> T validated(@Nonnull T data, @Nonnull Key<T> dataId, @Nonnull Object dataSource) {
    T invalidData = DataValidators.findInvalidData(dataId, data, dataSource);
    if (invalidData != null) {
      return null;
    }
    return data;
  }

  public DataContext getDataContextTest(consulo.ui.Component component) {
    DataContext dataContext = getDataContext(component);

    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    Component focusedComponent = ((WindowManagerEx)myWindowManager.get()).getFocusedComponent(project);
    if (focusedComponent != null) {
      dataContext = getDataContext(focusedComponent);
    }
    return dataContext;
  }

  @Override
  public <T> void saveInDataContext(DataContext dataContext, @Nonnull Key<T> dataKey, @Nullable T data) {
    if (dataContext instanceof UserDataHolder) {
      ((UserDataHolder)dataContext).putUserData(dataKey, data);
    }
  }

  @Override
  @Nullable
  public <T> T loadFromDataContext(@Nonnull DataContext dataContext, @Nonnull Key<T> dataKey) {
    return dataContext instanceof UserDataHolder ? ((UserDataHolder)dataContext).getUserData(dataKey) : null;
  }

  @Nullable
  protected  <T> T getData(@Nonnull Key<T> dataId, final consulo.ui.Component focusedComponent) {
    for (consulo.ui.Component c = focusedComponent; c != null; c = c.getParent()) {
      final DataProvider dataProvider = c::getUserData;
      T data = getDataFromProvider(dataProvider, dataId, null);
      if (data != null) return data;
    }
    return null;
  }

  @Nonnull
  @Override
  public DataContext getDataContext(@Nullable consulo.ui.Component component) {
    return new MyUIDataContext(this, component);
  }

  public DataContext getDataContextTest(java.awt.Component component) {
    throw new UnsupportedOperationException();
  }

  public DataProvider getDataProviderEx(java.awt.Component component) {
    throw new UnsupportedOperationException();
  }
}

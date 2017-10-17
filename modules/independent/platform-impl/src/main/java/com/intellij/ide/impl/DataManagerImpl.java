/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ProhibitAWTEvents;
import com.intellij.ide.impl.dataRules.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.reference.SoftReference;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.WeakValueHashMap;
import consulo.ide.impl.DataValidators;
import consulo.ui.ex.ToolWindowFloatingDecorator;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataManagerImpl extends DataManager {
  private static final Logger LOG = Logger.getInstance(DataManagerImpl.class);

  private final ConcurrentMap<Key, GetDataRule> myDataConstantToRuleMap = new ConcurrentHashMap<>();

  private WindowManagerEx myWindowManager;

  public DataManagerImpl() {
    registerRules();
  }

  @Nullable
  private <T> T getData(@NotNull Key<T> dataId, final Component focusedComponent) {
    try (AccessToken ignored = ProhibitAWTEvents.start("getData")) {
      for (Component c = focusedComponent; c != null; c = c.getParent()) {
        final DataProvider dataProvider = getDataProviderEx(c);
        if (dataProvider == null) continue;
        T data = getDataFromProvider(dataProvider, dataId, null);
        if (data != null) return data;
      }
    }
    return null;
  }

  @Nullable
  private <T> T getData(@NotNull Key<T> dataId, final consulo.ui.Component focusedComponent) {
    for (consulo.ui.Component c = focusedComponent; c != null; c = c.getParentComponent()) {
      final DataProvider dataProvider = c::getUserData;
      T data = getDataFromProvider(dataProvider, dataId, null);
      if (data != null) return data;
    }
    return null;
  }

  @Nullable
  private <T> T getDataFromProvider(@NotNull final DataProvider provider, @NotNull Key<T> dataId, @Nullable Set<Key> alreadyComputedIds) {
    if (alreadyComputedIds != null && alreadyComputedIds.contains(dataId)) {
      return null;
    }
    try {
      T data = provider.getDataUnchecked(dataId);
      if (data != null) return validated(data, dataId, provider);

      GetDataRule<T> dataRule = getDataRule(dataId);
      if (dataRule != null) {
        final Set<Key> ids = alreadyComputedIds == null ? new THashSet<>() : alreadyComputedIds;
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
  public static DataProvider getDataProviderEx(java.awt.Component component) {
    DataProvider dataProvider = null;
    if (component instanceof DataProvider) {
      dataProvider = (DataProvider)component;
    }
    else if (component instanceof TypeSafeDataProvider) {
      dataProvider = new TypeSafeDataProviderAdapter((TypeSafeDataProvider)component);
    }
    else if (component instanceof JComponent) {
      dataProvider = getDataProvider((JComponent)component);
    }

    return dataProvider;
  }

  @Nullable
  public <T> GetDataRule<T> getDataRule(@NotNull Key<T> dataId) {
    GetDataRule<T> rule = getRuleFromMap(dataId);
    if (rule != null) {
      return rule;
    }

    final GetDataRule<T> plainRule = getRuleFromMap(AnActionEvent.uninjectedId(dataId));
    if (plainRule != null) {
      return new GetDataRule<T>() {
        @NotNull
        @Override
        public Key<T> getKey() {
          return plainRule.getKey();
        }

        @Nullable
        @Override
        public T getData(@NotNull DataProvider dataProvider) {
          return plainRule.getData(key -> dataProvider.getData(AnActionEvent.injectedId(key)));
        }
      };
    }

    return null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> GetDataRule<T> getRuleFromMap(@NotNull Key<T> dataId) {
    GetDataRule rule = myDataConstantToRuleMap.get(dataId);
    if (rule == null && !myDataConstantToRuleMap.containsKey(dataId)) {
      final GetDataRule[] eps = Extensions.getExtensions(GetDataRule.EP_NAME);
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
  private static <T> T validated(@NotNull T data, @NotNull Key<T> dataId, @NotNull Object dataSource) {
    T invalidData = DataValidators.findInvalidData(dataId, data, dataSource);
    if (invalidData != null) {
      return null;
    }
    return data;
  }

  @Override
  public DataContext getDataContext(@Nullable Component component) {
    return new MyDataContext(component);
  }

  @NotNull
  @Override
  public DataContext getDataContext(@Nullable consulo.ui.Component component) {
    return new MyDataContext2(component);
  }

  @Override
  public DataContext getDataContext(@NotNull Component component, int x, int y) {
    if (x < 0 || x >= component.getWidth() || y < 0 || y >= component.getHeight()) {
      throw new IllegalArgumentException("wrong point: x=" + x + "; y=" + y);
    }

    // Point inside JTabbedPane has special meaning. If point is inside tab bounds then
    // we construct DataContext by the component which corresponds to the (x, y) tab.
    if (component instanceof JTabbedPane) {
      JTabbedPane tabbedPane = (JTabbedPane)component;
      int index = tabbedPane.getUI().tabForCoordinate(tabbedPane, x, y);
      return getDataContext(index != -1 ? tabbedPane.getComponentAt(index) : tabbedPane);
    }
    else {
      return getDataContext(component);
    }
  }

  public void setWindowManager(WindowManagerEx windowManager) {
    myWindowManager = windowManager;
  }

  @Override
  @NotNull
  public DataContext getDataContext() {
    return getDataContext(getFocusedComponent());
  }

  @NotNull
  @Override
  public AsyncResult<DataContext> getDataContextFromFocus() {
    AsyncResult<DataContext> context = new AsyncResult<>();
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> context.setDone(getDataContext()), ModalityState.current());
    return context;
  }

  public DataContext getDataContextTest(Component component) {
    DataContext dataContext = getDataContext(component);
    if (myWindowManager == null) {
      return dataContext;
    }
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    Component focusedComponent = myWindowManager.getFocusedComponent(project);
    if (focusedComponent != null) {
      dataContext = getDataContext(focusedComponent);
    }
    return dataContext;
  }

  @Nullable
  private Component getFocusedComponent() {
    if (myWindowManager == null) {
      return null;
    }
    Window activeWindow = myWindowManager.getMostRecentFocusedWindow();
    if (activeWindow == null) {
      activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      if (activeWindow == null) {
        activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (activeWindow == null) return null;
      }
    }

    IdeFocusManager fm = IdeFocusManager.findInstanceByComponent(activeWindow);
    if (fm.isFocusBeingTransferred()) {
      return null;
    }

    // In case we have an active floating toolwindow and some component in another window focused,
    // we want this other component to receive key events.
    // Walking up the window ownership hierarchy from the floating toolwindow would have led us to the main IdeFrame
    // whereas we want to be able to type in other frames as well.
    if (activeWindow instanceof ToolWindowFloatingDecorator) {
      IdeFocusManager ideFocusManager = IdeFocusManager.findInstanceByComponent(activeWindow);
      IdeFrame lastFocusedFrame = ideFocusManager.getLastFocusedFrame();
      JComponent frameComponent = lastFocusedFrame != null ? lastFocusedFrame.getComponent() : null;
      Window lastFocusedWindow = frameComponent != null ? SwingUtilities.getWindowAncestor(frameComponent) : null;
      boolean toolWindowIsNotFocused = myWindowManager.getFocusedComponent(activeWindow) == null;
      if (toolWindowIsNotFocused && lastFocusedWindow != null) {
        activeWindow = lastFocusedWindow;
      }
    }

    // try to find first parent window that has focus
    Window window = activeWindow;
    Component focusedComponent = null;
    while (window != null) {
      focusedComponent = myWindowManager.getFocusedComponent(window);
      if (focusedComponent != null) {
        break;
      }
      window = window.getOwner();
    }
    if (focusedComponent == null) {
      focusedComponent = activeWindow;
    }

    return focusedComponent;
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

  @Override
  public <T> void saveInDataContext(DataContext dataContext, @NotNull Key<T> dataKey, @Nullable T data) {
    if (dataContext instanceof UserDataHolder) {
      ((UserDataHolder)dataContext).putUserData(dataKey, data);
    }
  }

  @Override
  @Nullable
  public <T> T loadFromDataContext(@NotNull DataContext dataContext, @NotNull Key<T> dataKey) {
    return dataContext instanceof UserDataHolder ? ((UserDataHolder)dataContext).getUserData(dataKey) : null;
  }

  @Nullable
  public static Editor validateEditor(Editor editor) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner instanceof JComponent) {
      final JComponent jComponent = (JComponent)focusOwner;
      if (jComponent.getClientProperty("AuxEditorComponent") != null) return null; // Hack for EditorSearchComponent
    }

    return editor;
  }

  private static final Set<Key> ourSafeKeys =
          ContainerUtil.newHashSet(CommonDataKeys.PROJECT, CommonDataKeys.EDITOR, PlatformDataKeys.IS_MODAL_CONTEXT, PlatformDataKeys.CONTEXT_COMPONENT, PlatformDataKeys.MODALITY_STATE);

  public static class MyDataContext implements DataContext, UserDataHolder {
    private int myEventCount;
    // To prevent memory leak we have to wrap passed component into
    // the weak reference. For example, Swing often remembers menu items
    // that have DataContext as a field.
    private final Reference<Component> myRef;
    private Map<Key, Object> myUserData;
    private final Map<Key, Object> myCachedData = new WeakValueHashMap<>();

    public MyDataContext(final Component component) {
      myEventCount = -1;
      myRef = component == null ? null : new WeakReference<>(component);
    }

    public void setEventCount(int eventCount, Object caller) {
      assert caller instanceof IdeKeyEventDispatcher : "This method might be accessible from " + IdeKeyEventDispatcher.class.getName() + " only";
      myCachedData.clear();
      myEventCount = eventCount;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(@NotNull Key<T> dataId) {
      int currentEventCount = IdeEventQueue.getInstance().getEventCount();
      if (myEventCount != -1 && myEventCount != currentEventCount) {
        LOG.error("cannot share data context between Swing events; initial event count = " + myEventCount + "; current event count = " + currentEventCount);
        return doGetData(dataId);
      }

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
    @SuppressWarnings("unchecked")
    private <T> T doGetData(@NotNull Key<T> dataId) {
      Component component = SoftReference.dereference(myRef);
      if (PlatformDataKeys.IS_MODAL_CONTEXT == dataId) {
        if (component == null) {
          return null;
        }
        return (T)(Boolean)IdeKeyEventDispatcher.isModalContext(component);
      }
      if (PlatformDataKeys.CONTEXT_COMPONENT == dataId) {
        return (T)component;
      }
      if (PlatformDataKeys.MODALITY_STATE == dataId) {
        return (T)(component != null ? ModalityState.stateForComponent(component) : ModalityState.NON_MODAL);
      }
      if (CommonDataKeys.EDITOR == dataId || CommonDataKeys.HOST_EDITOR == dataId) {
        Editor editor = (Editor)((DataManagerImpl)DataManager.getInstance()).getData(dataId, component);
        return (T)validateEditor(editor);
      }
      return ((DataManagerImpl)DataManager.getInstance()).getData(dataId, component);
    }

    @NonNls
    public String toString() {
      return "component=" + SoftReference.dereference(myRef);
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
      //noinspection unchecked
      return (T)getOrCreateMap().get(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
      getOrCreateMap().put(key, value);
    }

    @NotNull
    private Map<Key, Object> getOrCreateMap() {
      Map<Key, Object> userData = myUserData;
      if (userData == null) {
        myUserData = userData = new WeakValueHashMap<>();
      }
      return userData;
    }
  }

  public static class MyDataContext2 implements DataContext, UserDataHolder {

    private final Reference<consulo.ui.Component> myRef;
    private Map<Key, Object> myUserData;
    private final Map<Key, Object> myCachedData = new WeakValueHashMap<>();

    public MyDataContext2(final consulo.ui.Component component) {
      myRef = component == null ? null : new WeakReference<>(component);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(@NotNull Key<T> dataId) {
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
    @SuppressWarnings("unchecked")
    private <T> T doGetData(@NotNull Key<T> dataId) {
      consulo.ui.Component component = SoftReference.dereference(myRef);
      if (PlatformDataKeys.IS_MODAL_CONTEXT == dataId) {
        if (component == null) {
          return null;
        }
        return (T)(Boolean)false; //FIXME [VISTALL] stub
      }
      //if (PlatformDataKeys.CONTEXT_COMPONENT == dataId) {
      //  return (T)component;
      //}
      if (PlatformDataKeys.MODALITY_STATE == dataId) {
        return (T)ModalityState.NON_MODAL; //FIXME [VISTALL] stub
      }
      if (CommonDataKeys.EDITOR == dataId || CommonDataKeys.HOST_EDITOR == dataId) {
        Editor editor = (Editor)((DataManagerImpl)DataManager.getInstance()).getData(dataId, component);
        return (T)validateEditor(editor);
      }
      return ((DataManagerImpl)DataManager.getInstance()).getData(dataId, component);
    }


    @NonNls
    public String toString() {
      return "component=" + SoftReference.dereference(myRef);
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
      //noinspection unchecked
      return (T)getOrCreateMap().get(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
      getOrCreateMap().put(key, value);
    }

    @NotNull
    private Map<Key, Object> getOrCreateMap() {
      Map<Key, Object> userData = myUserData;
      if (userData == null) {
        myUserData = userData = new WeakValueHashMap<>();
      }
      return userData;
    }
  }
}

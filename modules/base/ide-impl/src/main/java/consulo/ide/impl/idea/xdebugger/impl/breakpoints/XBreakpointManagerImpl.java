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
package consulo.ide.impl.idea.xdebugger.impl.breakpoints;

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.AccessRule;
import consulo.application.ApplicationManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.persist.PersistentStateComponent;
import consulo.disposer.Disposable;
import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.*;
import consulo.execution.debug.event.XBreakpointListener;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ide.impl.idea.xdebugger.impl.XDebuggerManagerImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.ui.image.Image;
import consulo.util.collection.MultiValuesMap;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpFileSystem;
import consulo.virtualFileSystem.http.event.HttpVirtualFileListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.*;

/**
 * @author nik
 */
public class XBreakpointManagerImpl implements XBreakpointManager, PersistentStateComponent<XBreakpointManagerImpl.BreakpointManagerState> {
  private static final Logger LOG = Logger.getInstance(XBreakpointManagerImpl.class);
  public static final SkipDefaultValuesSerializationFilters SERIALIZATION_FILTER = new SkipDefaultValuesSerializationFilters();
  private final MultiValuesMap<XBreakpointType, XBreakpointBase<?, ?, ?>> myBreakpoints = new MultiValuesMap<>(true);
  private final Map<XBreakpointType, XBreakpointBase<?, ?, ?>> myDefaultBreakpoints = new LinkedHashMap<>();
  private final Map<XBreakpointType, BreakpointState<?, ?, ?>> myBreakpointsDefaults = new LinkedHashMap<>();
  private final Set<XBreakpointBase<?, ?, ?>> myAllBreakpoints = new HashSet<>();
  private final Map<XBreakpointType, EventDispatcher<XBreakpointListener>> myDispatchers = new HashMap<>();
  private XBreakpointsDialogState myBreakpointsDialogSettings;
  private final EventDispatcher<XBreakpointListener> myAllBreakpointsDispatcher;
  private final XLineBreakpointManager myLineBreakpointManager;
  private final Project myProject;
  private final XDebuggerManagerImpl myDebuggerManager;
  private final XDependentBreakpointManager myDependentBreakpointManager;
  private long myTime;
  private String myDefaultGroup;

  public XBreakpointManagerImpl(Project project,
                                XDebuggerManagerImpl debuggerManager,
                                StartupManager startupManager,
                                ApplicationConcurrency applicationConcurrency) {
    myProject = project;
    myDebuggerManager = debuggerManager;
    myAllBreakpointsDispatcher = EventDispatcher.create(XBreakpointListener.class);
    myDependentBreakpointManager = new XDependentBreakpointManager(this);
    myLineBreakpointManager = new XLineBreakpointManager(project, myDependentBreakpointManager, startupManager, applicationConcurrency);
    if (!project.isDefault()) {
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        HttpVirtualFileListener httpVirtualFileListener = this::updateBreakpointInFile;
        HttpFileSystem.getInstance().addFileListener(httpVirtualFileListener, project);
      }
      for (XBreakpointType<?, ?> type : XBreakpointUtil.getBreakpointTypes()) {
        addDefaultBreakpoint(type);
      }
    }
  }

  private void updateBreakpointInFile(final VirtualFile file) {
    ApplicationManager.getApplication().invokeLater(() -> {
      for (XBreakpointBase breakpoint : getAllBreakpoints()) {
        XSourcePosition position = breakpoint.getSourcePosition();
        if (position != null && Comparing.equal(position.getFile(), file)) {
          fireBreakpointChanged(breakpoint);
        }
      }
    });
  }

  public XLineBreakpointManager getLineBreakpointManager() {
    return myLineBreakpointManager;
  }

  public XDependentBreakpointManager getDependentBreakpointManager() {
    return myDependentBreakpointManager;
  }

  public XDebuggerManagerImpl getDebuggerManager() {
    return myDebuggerManager;
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  @Nonnull
  public <T extends XBreakpointProperties> XBreakpoint<T> addBreakpoint(final XBreakpointType<XBreakpoint<T>, T> type,
                                                                        @Nullable final T properties) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    XBreakpointBase<?, T, ?> breakpoint = createBreakpoint(type, properties, true, false);
    addBreakpoint(breakpoint, false, true);
    return breakpoint;
  }

  private <T extends XBreakpointProperties> XBreakpointBase<?, T, ?> createBreakpoint(XBreakpointType<XBreakpoint<T>, T> type,
                                                                                      T properties,
                                                                                      final boolean enabled,
                                                                                      boolean defaultBreakpoint) {
    BreakpointState<?, T, ?> state =
      new BreakpointState<>(enabled, type.getId(), defaultBreakpoint ? 0 : myTime++, type.getDefaultSuspendPolicy());
    getBreakpointDefaults(type).applyDefaults(state);
    state.setGroup(myDefaultGroup);
    return new XBreakpointBase<XBreakpoint<T>, T, BreakpointState<?, T, ?>>(type, this, properties, state);
  }

  private <T extends XBreakpointProperties> void addBreakpoint(final XBreakpointBase<?, T, ?> breakpoint,
                                                               final boolean defaultBreakpoint,
                                                               boolean initUI) {
    XBreakpointType type = breakpoint.getType();
    if (defaultBreakpoint) {
      LOG.assertTrue(!myDefaultBreakpoints.containsKey(type), "Cannot have more than one default breakpoint (type " + type.getId() + ")");
      myDefaultBreakpoints.put(type, breakpoint);
    }
    else {
      myBreakpoints.put(type, breakpoint);
    }
    myAllBreakpoints.add(breakpoint);
    if (breakpoint instanceof XLineBreakpointImpl) {
      myLineBreakpointManager.registerBreakpoint((XLineBreakpointImpl)breakpoint, initUI);
    }
    EventDispatcher<XBreakpointListener> dispatcher = myDispatchers.get(type);
    if (dispatcher != null) {
      //noinspection unchecked
      dispatcher.getMulticaster().breakpointAdded(breakpoint);
    }
    getBreakpointDispatcherMulticaster().breakpointAdded(breakpoint);
  }

  private XBreakpointListener<XBreakpoint<?>> getBreakpointDispatcherMulticaster() {
    //noinspection unchecked
    return myAllBreakpointsDispatcher.getMulticaster();
  }

  public void fireBreakpointChanged(XBreakpointBase<?, ?, ?> breakpoint) {
    if (!myAllBreakpoints.contains(breakpoint)) {
      return;
    }

    if (breakpoint instanceof XLineBreakpointImpl) {
      myLineBreakpointManager.breakpointChanged((XLineBreakpointImpl)breakpoint);
    }
    EventDispatcher<XBreakpointListener> dispatcher = myDispatchers.get(breakpoint.getType());
    if (dispatcher != null) {
      //noinspection unchecked
      dispatcher.getMulticaster().breakpointChanged(breakpoint);
    }
    getBreakpointDispatcherMulticaster().breakpointChanged(breakpoint);
  }

  @Override
  public void removeBreakpoint(@Nonnull final XBreakpoint<?> breakpoint) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    doRemoveBreakpoint(breakpoint);
  }

  private void doRemoveBreakpoint(XBreakpoint<?> breakpoint) {
    if (isDefaultBreakpoint(breakpoint)) {
      // removing default breakpoint should just disable it
      breakpoint.setEnabled(false);
    }
    else {
      XBreakpointType type = breakpoint.getType();
      XBreakpointBase<?, ?, ?> breakpointBase = (XBreakpointBase<?, ?, ?>)breakpoint;
      myBreakpoints.remove(type, breakpointBase);
      myAllBreakpoints.remove(breakpointBase);
      if (breakpointBase instanceof XLineBreakpointImpl) {
        myLineBreakpointManager.unregisterBreakpoint((XLineBreakpointImpl)breakpointBase);
      }
      breakpointBase.dispose();
      EventDispatcher<XBreakpointListener> dispatcher = myDispatchers.get(type);
      if (dispatcher != null) {
        //noinspection unchecked
        dispatcher.getMulticaster().breakpointRemoved(breakpoint);
      }
      getBreakpointDispatcherMulticaster().breakpointRemoved(breakpoint);
    }
  }

  @Override
  @Nonnull
  public <T extends XBreakpointProperties> XLineBreakpoint<T> addLineBreakpoint(final XLineBreakpointType<T> type,
                                                                                @Nonnull final String fileUrl,
                                                                                final int line,
                                                                                @Nullable final T properties) {
    return addLineBreakpoint(type, fileUrl, line, properties, false);
  }

  @Override
  @Nonnull
  public <T extends XBreakpointProperties> XLineBreakpoint<T> addLineBreakpoint(final XLineBreakpointType<T> type,
                                                                                @Nonnull final String fileUrl,
                                                                                final int line,
                                                                                @Nullable final T properties,
                                                                                boolean temporary) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    LineBreakpointState<T> state =
      new LineBreakpointState<>(true, type.getId(), fileUrl, line, temporary, myTime++, type.getDefaultSuspendPolicy());
    getBreakpointDefaults(type).applyDefaults(state);
    state.setGroup(myDefaultGroup);
    XLineBreakpointImpl<T> breakpoint = new XLineBreakpointImpl<>(type, this, properties, state);
    addBreakpoint(breakpoint, false, true);
    return breakpoint;
  }

  @Override
  @Nonnull
  public XBreakpointBase<?, ?, ?>[] getAllBreakpoints() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return myAllBreakpoints.toArray(new XBreakpointBase[myAllBreakpoints.size()]);
  }

  @Override
  @SuppressWarnings({"unchecked"})
  @Nonnull
  public <B extends XBreakpoint<?>> Collection<? extends B> getBreakpoints(@Nonnull final XBreakpointType<B, ?> type) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    List<B> result = new ArrayList<>();
    B defaultBreakpoint = getDefaultBreakpoint(type);
    if (defaultBreakpoint != null) {
      result.add(defaultBreakpoint);
    }
    Collection<XBreakpointBase<?, ?, ?>> breakpoints = myBreakpoints.get(type);
    if (breakpoints != null) {
      result.addAll((Collection<? extends B>)breakpoints);
    }
    return Collections.unmodifiableList(result);
  }

  @Nonnull
  @Override
  public <B extends XBreakpoint<?>> Collection<? extends B> getBreakpoints(@Nonnull Class<? extends XBreakpointType<B, ?>> typeClass) {
    XBreakpointType<B, ?> type = XDebuggerUtil.getInstance().findBreakpointType(typeClass);
    LOG.assertTrue(type != null, "Unregistered breakpoint type " + typeClass);
    return getBreakpoints(type);
  }

  @Override
  @Nullable
  public <B extends XBreakpoint<?>> B getDefaultBreakpoint(@Nonnull XBreakpointType<B, ?> type) {
    //noinspection unchecked
    return (B)myDefaultBreakpoints.get(type);
  }

  @Override
  @Nullable
  public <P extends XBreakpointProperties> XLineBreakpoint<P> findBreakpointAtLine(@Nonnull final XLineBreakpointType<P> type,
                                                                                   @Nonnull final VirtualFile file,
                                                                                   final int line) {
    Collection<XBreakpointBase<?, ?, ?>> breakpoints = myBreakpoints.get(type);
    if (breakpoints == null) return null;

    for (XBreakpointBase<?, ?, ?> breakpoint : breakpoints) {
      XLineBreakpoint lineBreakpoint = (XLineBreakpoint)breakpoint;
      if (lineBreakpoint.getFileUrl().equals(file.getUrl()) && lineBreakpoint.getLine() == line) {
        //noinspection unchecked
        return lineBreakpoint;
      }
    }
    return null;
  }

  @Override
  public boolean isDefaultBreakpoint(@Nonnull XBreakpoint<?> breakpoint) {
    //noinspection SuspiciousMethodCalls
    return myDefaultBreakpoints.values().contains(breakpoint);
  }

  private <T extends XBreakpointProperties> EventDispatcher<XBreakpointListener> getOrCreateDispatcher(final XBreakpointType<?, T> type) {
    EventDispatcher<XBreakpointListener> dispatcher = myDispatchers.get(type);
    if (dispatcher == null) {
      dispatcher = EventDispatcher.create(XBreakpointListener.class);
      myDispatchers.put(type, dispatcher);
    }
    return dispatcher;
  }

  @Override
  public <B extends XBreakpoint<P>, P extends XBreakpointProperties> void addBreakpointListener(@Nonnull final XBreakpointType<B, P> type,
                                                                                                @Nonnull final XBreakpointListener<B> listener) {
    getOrCreateDispatcher(type).addListener(listener);
  }

  @Override
  public <B extends XBreakpoint<P>, P extends XBreakpointProperties> void removeBreakpointListener(@Nonnull final XBreakpointType<B, P> type,
                                                                                                   @Nonnull final XBreakpointListener<B> listener) {
    getOrCreateDispatcher(type).removeListener(listener);
  }

  @Override
  public <B extends XBreakpoint<P>, P extends XBreakpointProperties> void addBreakpointListener(@Nonnull final XBreakpointType<B, P> type,
                                                                                                @Nonnull final XBreakpointListener<B> listener,
                                                                                                final Disposable parentDisposable) {
    getOrCreateDispatcher(type).addListener(listener, parentDisposable);
  }

  @Override
  public void addBreakpointListener(@Nonnull final XBreakpointListener<XBreakpoint<?>> listener) {
    myAllBreakpointsDispatcher.addListener(listener);
  }

  @Override
  public void removeBreakpointListener(@Nonnull final XBreakpointListener<XBreakpoint<?>> listener) {
    myAllBreakpointsDispatcher.removeListener(listener);
  }

  @Override
  public void addBreakpointListener(@Nonnull final XBreakpointListener<XBreakpoint<?>> listener,
                                    @Nonnull final Disposable parentDisposable) {
    myAllBreakpointsDispatcher.addListener(listener, parentDisposable);
  }

  @Override
  public void updateBreakpointPresentation(@Nonnull XLineBreakpoint<?> breakpoint, @Nullable Image icon, @Nullable String errorMessage) {
    XLineBreakpointImpl lineBreakpoint = (XLineBreakpointImpl)breakpoint;
    CustomizedBreakpointPresentation presentation = lineBreakpoint.getCustomizedPresentation();
    if (presentation == null) {
      if (icon == null && errorMessage == null) {
        return;
      }
      presentation = new CustomizedBreakpointPresentation();
    }
    else if (Comparing.equal(presentation.getIcon(), icon) && Comparing.strEqual(presentation.getErrorMessage(), errorMessage)) {
      return;
    }

    presentation.setErrorMessage(errorMessage);
    presentation.setIcon(icon);
    lineBreakpoint.setCustomizedPresentation(presentation);
    myLineBreakpointManager.queueBreakpointUpdate(breakpoint);
  }

  @Override
  public BreakpointManagerState getState() {
    myDependentBreakpointManager.saveState();
    BreakpointManagerState state = new BreakpointManagerState();
    for (XBreakpointBase<?, ?, ?> breakpoint : myDefaultBreakpoints.values()) {
      final BreakpointState breakpointState = breakpoint.getState();
      if (differsFromDefault(breakpoint.getType(), breakpointState)) {
        state.getDefaultBreakpoints().add(breakpointState);
      }
    }
    for (XBreakpointBase<?, ?, ?> breakpoint : myBreakpoints.values()) {
      state.getBreakpoints().add(breakpoint.getState());
    }

    for (Map.Entry<XBreakpointType, BreakpointState<?, ?, ?>> entry : myBreakpointsDefaults.entrySet()) {
      if (statesAreDifferent(entry.getValue(), createBreakpointDefaults(entry.getKey()))) {
        state.getBreakpointsDefaults().add(entry.getValue());
      }
    }

    state.setBreakpointsDialogProperties(myBreakpointsDialogSettings);
    state.setTime(myTime);
    state.setDefaultGroup(myDefaultGroup);
    return state;
  }

  private <P extends XBreakpointProperties> boolean differsFromDefault(XBreakpointType<?, P> type, BreakpointState state) {
    final XBreakpoint<P> defaultBreakpoint = createDefaultBreakpoint(type);
    if (defaultBreakpoint == null) {
      return false;
    }

    BreakpointState defaultState = ((XBreakpointBase)defaultBreakpoint).getState();
    return statesAreDifferent(state, defaultState);
  }

  private static boolean statesAreDifferent(BreakpointState state1, BreakpointState state2) {
    Element elem1 = XmlSerializer.serialize(state1, SERIALIZATION_FILTER);
    Element elem2 = XmlSerializer.serialize(state2, SERIALIZATION_FILTER);
    return !JDOMUtil.areElementsEqual(elem1, elem2);
  }

  @Override
  public void loadState(final BreakpointManagerState state) {
    myBreakpointsDialogSettings = state.getBreakpointsDialogProperties();

    myAllBreakpoints.clear();
    myDefaultBreakpoints.clear();
    myBreakpointsDefaults.clear();

    AccessRule.read(() -> {
      for (BreakpointState breakpointState : state.getDefaultBreakpoints()) {
        loadBreakpoint(breakpointState, true);
      }
      for (XBreakpointType<?, ?> type : XBreakpointUtil.getBreakpointTypes()) {
        if (!myDefaultBreakpoints.containsKey(type)) {
          addDefaultBreakpoint(type);
        }
      }

      for (XBreakpointBase<?, ?, ?> breakpoint : myBreakpoints.values()) {
        doRemoveBreakpoint(breakpoint);
      }
      for (BreakpointState breakpointState : state.getBreakpoints()) {
        loadBreakpoint(breakpointState, false);
      }

      for (BreakpointState defaults : state.getBreakpointsDefaults()) {
        XBreakpointType<?, ?> type = XBreakpointUtil.findType(defaults.getTypeId());
        if (type != null) {
          myBreakpointsDefaults.put(type, defaults);
        }
        else {
          LOG.warn("Unknown breakpoint type " + defaults.getTypeId());
        }
      }

      myDependentBreakpointManager.loadState();
    });
    myLineBreakpointManager.updateBreakpointsUI();
    myTime = state.getTime();
    myDefaultGroup = state.getDefaultGroup();
  }

  private <P extends XBreakpointProperties> void addDefaultBreakpoint(XBreakpointType<?, P> type) {
    final XBreakpoint<P> breakpoint = createDefaultBreakpoint(type);
    if (breakpoint != null) {
      addBreakpoint((XBreakpointBase<?, P, ?>)breakpoint, true, false);
    }
  }

  @Nullable
  private <P extends XBreakpointProperties> XBreakpoint<P> createDefaultBreakpoint(final XBreakpointType<? extends XBreakpoint<P>, P> type) {
    return type.createDefaultBreakpoint(properties -> {
      //noinspection unchecked
      return createBreakpoint((XBreakpointType<XBreakpoint<P>, P>)type, properties, false, true);
    });
  }

  private void loadBreakpoint(BreakpointState breakpointState, final boolean defaultBreakpoint) {
    XBreakpointBase<?, ?, ?> breakpoint = createBreakpoint(breakpointState);
    if (breakpoint != null) {
      addBreakpoint(breakpoint, defaultBreakpoint, false);
    }
  }

  public XBreakpointsDialogState getBreakpointsDialogSettings() {
    return myBreakpointsDialogSettings;
  }

  public void setBreakpointsDialogSettings(XBreakpointsDialogState breakpointsDialogSettings) {
    myBreakpointsDialogSettings = breakpointsDialogSettings;
  }

  public Set<String> getAllGroups() {
    HashSet<String> res = new HashSet<>();
    for (XBreakpointBase breakpoint : myAllBreakpoints) {
      String group = breakpoint.getGroup();
      if (group != null) {
        res.add(group);
      }
    }
    return res;
  }

  public String getDefaultGroup() {
    return myDefaultGroup;
  }

  public void setDefaultGroup(String defaultGroup) {
    myDefaultGroup = defaultGroup;
  }

  @Nullable
  private XBreakpointBase<?, ?, ?> createBreakpoint(final BreakpointState breakpointState) {
    XBreakpointType<?, ?> type = XBreakpointUtil.findType(breakpointState.getTypeId());
    if (type == null) {
      LOG.warn("Unknown breakpoint type " + breakpointState.getTypeId());
      return null;
    }
    //noinspection unchecked
    return breakpointState.createBreakpoint(type, this);
  }

  @Nonnull
  public BreakpointState getBreakpointDefaults(@Nonnull XBreakpointType type) {
    BreakpointState defaultState = myBreakpointsDefaults.get(type);
    if (defaultState == null) {
      defaultState = createBreakpointDefaults(type);
      myBreakpointsDefaults.put(type, defaultState);
    }
    return defaultState;
  }

  @Nullable
  @RequiredWriteAction
  <T extends XBreakpointProperties> XLineBreakpoint<T> copyLineBreakpoint(@Nonnull XLineBreakpoint<T> source,
                                                                          @Nonnull String fileUrl,
                                                                          int line) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    if (!(source instanceof XLineBreakpointImpl<?>)) {
      return null;
    }
    myDependentBreakpointManager.saveState();
    final LineBreakpointState sourceState = ((XLineBreakpointImpl<?>)source).getState();

    final LineBreakpointState newState =
      XmlSerializer.deserialize(XmlSerializer.serialize(sourceState, SERIALIZATION_FILTER), LineBreakpointState.class);
    newState.setLine(line);
    newState.setFileUrl(fileUrl);

    //noinspection unchecked
    final XLineBreakpointImpl<T> breakpoint = (XLineBreakpointImpl<T>)createBreakpoint(newState);
    if (breakpoint != null) {
      addBreakpoint(breakpoint, false, true);
      final XBreakpoint<?> masterBreakpoint = myDependentBreakpointManager.getMasterBreakpoint(source);
      if (masterBreakpoint != null) {
        myDependentBreakpointManager.setMasterBreakpoint(breakpoint, masterBreakpoint, sourceState.getDependencyState().isLeaveEnabled());
      }
    }

    return breakpoint;
  }

  @Nonnull
  private static BreakpointState createBreakpointDefaults(@Nonnull XBreakpointType type) {
    BreakpointState state = new BreakpointState();
    state.setTypeId(type.getId());
    state.setSuspendPolicy(type.getDefaultSuspendPolicy());
    return state;
  }

  @Tag("breakpoint-manager")
  public static class BreakpointManagerState {
    private List<BreakpointState> myDefaultBreakpoints = new ArrayList<>();
    private List<BreakpointState> myBreakpoints = new ArrayList<>();
    private List<BreakpointState> myBreakpointsDefaults = new ArrayList<>();
    private XBreakpointsDialogState myBreakpointsDialogProperties;

    private long myTime;
    private String myDefaultGroup;

    @Tag("default-breakpoints")
    @AbstractCollection(surroundWithTag = false)
    public List<BreakpointState> getDefaultBreakpoints() {
      return myDefaultBreakpoints;
    }

    @Tag("breakpoints")
    @AbstractCollection(surroundWithTag = false, elementTypes = {BreakpointState.class, LineBreakpointState.class})
    public List<BreakpointState> getBreakpoints() {
      return myBreakpoints;
    }

    @Tag("breakpoints-defaults")
    @AbstractCollection(surroundWithTag = false, elementTypes = {BreakpointState.class, LineBreakpointState.class})
    public List<BreakpointState> getBreakpointsDefaults() {
      return myBreakpointsDefaults;
    }

    @Tag("breakpoints-dialog")
    public XBreakpointsDialogState getBreakpointsDialogProperties() {
      return myBreakpointsDialogProperties;
    }

    public void setBreakpoints(final List<BreakpointState> breakpoints) {
      myBreakpoints = breakpoints;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setDefaultBreakpoints(List<BreakpointState> defaultBreakpoints) {
      myDefaultBreakpoints = defaultBreakpoints;
    }

    public void setBreakpointsDefaults(List<BreakpointState> breakpointsDefaults) {
      myBreakpointsDefaults = breakpointsDefaults;
    }

    public void setBreakpointsDialogProperties(XBreakpointsDialogState breakpointsDialogProperties) {
      myBreakpointsDialogProperties = breakpointsDialogProperties;
    }

    public long getTime() {
      return myTime;
    }

    public void setTime(long time) {
      myTime = time;
    }

    public String getDefaultGroup() {
      return myDefaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
      myDefaultGroup = defaultGroup;
    }
  }
}

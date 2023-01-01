/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl;

import consulo.execution.debug.event.XDebuggerManagerListener;
import consulo.execution.ui.console.OpenFileHyperlinkInfo;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.ui.AppUIUtil;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.*;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.XDebuggerEditorLinePainter;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.quick.common.ValueLookupManager;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.ide.impl.idea.xdebugger.impl.frame.XWatchesViewImpl;
import consulo.ide.impl.idea.xdebugger.impl.settings.XDebuggerSettingManagerImpl;
import consulo.ide.impl.idea.xdebugger.impl.ui.XDebugSessionData;
import consulo.ide.impl.idea.xdebugger.impl.ui.XDebugSessionTab;
import consulo.application.AccessRule;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ExecutionManager;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.*;
import consulo.execution.debug.breakpoint.*;
import consulo.execution.debug.event.XBreakpointListener;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValueMarkerProvider;
import consulo.execution.debug.step.XSmartStepIntoHandler;
import consulo.execution.debug.step.XSmartStepIntoVariant;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.logging.Logger;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.NotificationType;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.util.collection.SmartHashSet;
import consulo.util.collection.SmartList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author nik
 */
public class XDebugSessionImpl implements XDebugSession {
  private static final Logger LOG = Logger.getInstance(XDebugSessionImpl.class);
  public static final NotificationGroup NOTIFICATION_GROUP = XDebuggerUIConstants.NOTIFICATION_GROUP;
  private XDebugProcess myDebugProcess;
  private final Map<XBreakpoint<?>, CustomizedBreakpointPresentation> myRegisteredBreakpoints = new HashMap<>();
  private final Set<XBreakpoint<?>> myInactiveSlaveBreakpoints = Collections.synchronizedSet(new SmartHashSet<>());
  private boolean myBreakpointsDisabled;
  private final XDebuggerManagerImpl myDebuggerManager;
  private MyBreakpointListener myBreakpointListener;
  private XSuspendContext mySuspendContext;
  private XExecutionStack myCurrentExecutionStack;
  private XStackFrame myCurrentStackFrame;
  private boolean myIsTopFrame;
  private volatile XSourcePosition myTopFramePosition;
  private final AtomicBoolean myPaused = new AtomicBoolean();
  private MyDependentBreakpointListener myDependentBreakpointListener;
  private XValueMarkers<?, ?> myValueMarkers;
  private final String mySessionName;
  private @Nullable
  XDebugSessionTab mySessionTab;
  private final XDebugSessionData mySessionData;
  private XBreakpoint<?> myActiveNonLineBreakpoint;
  private final EventDispatcher<XDebugSessionListener> myDispatcher = EventDispatcher.create(XDebugSessionListener.class);
  private final Project myProject;
  private final @Nullable
  ExecutionEnvironment myEnvironment;
  private final AtomicBoolean myStopped = new AtomicBoolean();
  private boolean myPauseActionSupported;
  private final AtomicBoolean myShowTabOnSuspend;
  private final List<AnAction> myRestartActions = new SmartList<>();
  private final List<AnAction> myExtraStopActions = new SmartList<>();
  private final List<AnAction> myExtraActions = new SmartList<>();
  private ConsoleView myConsoleView;
  private final Image myIcon;

  private volatile boolean breakpointsInitialized;

  public XDebugSessionImpl(@Nonnull ExecutionEnvironment environment, @Nonnull XDebuggerManagerImpl debuggerManager) {
    this(environment, debuggerManager, environment.getRunProfile().getName(), environment.getRunProfile().getIcon(), false, null);
  }

  public XDebugSessionImpl(@Nullable ExecutionEnvironment environment,
                           @Nonnull XDebuggerManagerImpl debuggerManager,
                           @Nonnull String sessionName,
                           @Nullable Image icon,
                           boolean showTabOnSuspend,
                           @Nullable RunContentDescriptor contentToReuse) {
    myEnvironment = environment;
    mySessionName = sessionName;
    myDebuggerManager = debuggerManager;
    myShowTabOnSuspend = new AtomicBoolean(showTabOnSuspend);
    myProject = debuggerManager.getProject();
    ValueLookupManager.getInstance(myProject).startListening();
    myIcon = icon;

    XDebugSessionData oldSessionData = null;
    if (contentToReuse == null) {
      contentToReuse = environment != null ? environment.getContentToReuse() : null;
    }
    if (contentToReuse != null) {
      JComponent component = contentToReuse.getComponent();
      if (component != null) {
        oldSessionData = DataManager.getInstance().getDataContext(component).getData(XDebugSessionData.DATA_KEY);
      }
    }

    String currentConfigurationName = getConfigurationName();
    if (oldSessionData == null || !oldSessionData.getConfigurationName().equals(currentConfigurationName)) {
      oldSessionData = new XDebugSessionData(getWatchExpressions(), currentConfigurationName);
    }
    mySessionData = oldSessionData;
  }

  @Override
  @Nonnull
  public String getSessionName() {
    return mySessionName;
  }

  @Override
  @Nonnull
  public RunContentDescriptor getRunContentDescriptor() {
    assertSessionTabInitialized();
    //noinspection ConstantConditions
    return mySessionTab.getRunContentDescriptor();
  }

  private void assertSessionTabInitialized() {
    if (myShowTabOnSuspend.get()) {
      LOG.error("Debug tool window isn't shown yet because debug process isn't suspended");
    }
    else {
      LOG.assertTrue(mySessionTab != null, "Debug tool window not initialized yet!");
    }
  }

  @Override
  public void setPauseActionSupported(final boolean isSupported) {
    myPauseActionSupported = isSupported;
  }

  @Nonnull
  public List<AnAction> getRestartActions() {
    return myRestartActions;
  }

  public void addRestartActions(AnAction... restartActions) {
    if (restartActions != null) {
      Collections.addAll(myRestartActions, restartActions);
    }
  }

  @Nonnull
  public List<AnAction> getExtraActions() {
    return myExtraActions;
  }

  public void addExtraActions(AnAction... extraActions) {
    if (extraActions != null) {
      Collections.addAll(myExtraActions, extraActions);
    }
  }

  public List<AnAction> getExtraStopActions() {
    return myExtraStopActions;
  }

  public void addExtraStopActions(AnAction... extraStopActions) {
    if (extraStopActions != null) {
      Collections.addAll(myExtraStopActions, extraStopActions);
    }
  }

  @Override
  public void rebuildViews() {
    if (!myShowTabOnSuspend.get() && mySessionTab != null) {
      mySessionTab.rebuildViews();
    }
  }

  @Override
  @Nullable
  public RunProfile getRunProfile() {
    return myEnvironment != null ? myEnvironment.getRunProfile() : null;
  }

  public boolean isPauseActionSupported() {
    return myPauseActionSupported;
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myDebuggerManager.getProject();
  }

  @Override
  @Nonnull
  public XDebugProcess getDebugProcess() {
    return myDebugProcess;
  }

  @Override
  public boolean isSuspended() {
    return myPaused.get() && mySuspendContext != null;
  }

  @Override
  public boolean isPaused() {
    return myPaused.get();
  }

  @Override
  @Nullable
  public XStackFrame getCurrentStackFrame() {
    return myCurrentStackFrame;
  }

  public XExecutionStack getCurrentExecutionStack() {
    return myCurrentExecutionStack;
  }

  @Override
  public XSuspendContext getSuspendContext() {
    return mySuspendContext;
  }

  @Override
  @Nullable
  public XSourcePosition getCurrentPosition() {
    return myCurrentStackFrame != null ? myCurrentStackFrame.getSourcePosition() : null;
  }

  @Nullable
  @Override
  public XSourcePosition getTopFramePosition() {
    return myTopFramePosition;
  }

  XDebugSessionTab init(@Nonnull XDebugProcess process, @Nullable RunContentDescriptor contentToReuse) {
    LOG.assertTrue(myDebugProcess == null);
    myDebugProcess = process;

    if (myDebugProcess.checkCanInitBreakpoints()) {
      initBreakpoints();
    }

    myDebugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(final ProcessEvent event) {
        stopImpl();
        myDebugProcess.getProcessHandler().removeProcessListener(this);
      }
    });
    //todo[nik] make 'createConsole()' method return ConsoleView
    myConsoleView = (ConsoleView)myDebugProcess.createConsole();
    if (!myShowTabOnSuspend.get()) {
      initSessionTab(contentToReuse);
    }

    return mySessionTab;
  }

  public void reset() {
    breakpointsInitialized = false;
  }

  @Override
  public void initBreakpoints() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    LOG.assertTrue(!breakpointsInitialized);
    breakpointsInitialized = true;

    XBreakpointManagerImpl breakpointManager = myDebuggerManager.getBreakpointManager();
    XDependentBreakpointManager dependentBreakpointManager = breakpointManager.getDependentBreakpointManager();
    disableSlaveBreakpoints(dependentBreakpointManager);
    processAllBreakpoints(true, false);

    if (myBreakpointListener == null) {
      myBreakpointListener = new MyBreakpointListener();
      breakpointManager.addBreakpointListener(myBreakpointListener);
    }
    if (myDependentBreakpointListener == null) {
      myDependentBreakpointListener = new MyDependentBreakpointListener();
      dependentBreakpointManager.addListener(myDependentBreakpointListener);
    }
  }

  @Override
  public ConsoleView getConsoleView() {
    return myConsoleView;
  }

  @Nullable
  public XDebugSessionTab getSessionTab() {
    return mySessionTab;
  }

  @Override
  public RunnerLayoutUi getUI() {
    assertSessionTabInitialized();
    assert mySessionTab != null;
    return mySessionTab.getUi();
  }

  private void initSessionTab(@Nullable RunContentDescriptor contentToReuse) {
    mySessionTab = XDebugSessionTab.create(this, myIcon, myEnvironment, contentToReuse);
    myDebugProcess.sessionInitialized();
  }

  public XDebugSessionData getSessionData() {
    return mySessionData;
  }

  private void disableSlaveBreakpoints(final XDependentBreakpointManager dependentBreakpointManager) {
    Set<XBreakpoint<?>> slaveBreakpoints = dependentBreakpointManager.getAllSlaveBreakpoints();
    if (slaveBreakpoints.isEmpty()) {
      return;
    }

    Set<XBreakpointType<?, ?>> breakpointTypes = new HashSet<>();
    for (XBreakpointHandler<?> handler : myDebugProcess.getBreakpointHandlers()) {
      breakpointTypes.add(getBreakpointTypeClass(handler));
    }
    for (XBreakpoint<?> slaveBreakpoint : slaveBreakpoints) {
      if (breakpointTypes.contains(slaveBreakpoint.getType())) {
        myInactiveSlaveBreakpoints.add(slaveBreakpoint);
      }
    }
  }

  public void showSessionTab() {
    RunContentDescriptor descriptor = getRunContentDescriptor();
    ExecutionManager.getInstance(getProject()).getContentManager()
            .showRunContent(DefaultDebugExecutor.getDebugExecutorInstance(), descriptor);
  }

  @Override
  @Nullable
  public XValueMarkers<?, ?> getValueMarkers() {
    if (myValueMarkers == null) {
      XValueMarkerProvider<?, ?> provider = myDebugProcess.createValueMarkerProvider();
      if (provider != null) {
        myValueMarkers = XValueMarkers.createValueMarkers(provider);
      }
    }
    return myValueMarkers;
  }

  @SuppressWarnings("unchecked") //need to compile under 1.8, please do not remove before checking
  private static XBreakpointType getBreakpointTypeClass(final XBreakpointHandler handler) {
    return XDebuggerUtil.getInstance().findBreakpointType(handler.getBreakpointTypeClass());
  }

  private <B extends XBreakpoint<?>> void processBreakpoints(final XBreakpointHandler<B> handler,
                                                             boolean register,
                                                             final boolean temporary) {
    Collection<? extends B> breakpoints = myDebuggerManager.getBreakpointManager().getBreakpoints(handler.getBreakpointTypeClass());
    for (B b : breakpoints) {
      handleBreakpoint(handler, b, register, temporary);
    }
  }

  private <B extends XBreakpoint<?>> void handleBreakpoint(final XBreakpointHandler<B> handler, final B b, final boolean register,
                                                           final boolean temporary) {
    if (register) {
      ThrowableComputable<Boolean,RuntimeException> action = () -> isBreakpointActive(b);
      boolean active = AccessRule.read(action);
      if (active) {
        synchronized (myRegisteredBreakpoints) {
          myRegisteredBreakpoints.put(b, new CustomizedBreakpointPresentation());
        }
        handler.registerBreakpoint(b);
      }
    }
    else {
      boolean removed;
      synchronized (myRegisteredBreakpoints) {
        removed = myRegisteredBreakpoints.remove(b) != null;
      }
      if (removed) {
        handler.unregisterBreakpoint(b, temporary);
      }
    }
  }

  @Nullable
  public CustomizedBreakpointPresentation getBreakpointPresentation(@Nonnull XBreakpoint<?> breakpoint) {
    synchronized (myRegisteredBreakpoints) {
      return myRegisteredBreakpoints.get(breakpoint);
    }
  }

  private void processAllHandlers(final XBreakpoint<?> breakpoint, final boolean register) {
    for (XBreakpointHandler<?> handler : myDebugProcess.getBreakpointHandlers()) {
      processBreakpoint(breakpoint, handler, register);
    }
  }

  private <B extends XBreakpoint<?>> void processBreakpoint(final XBreakpoint<?> breakpoint,
                                                            final XBreakpointHandler<B> handler,
                                                            boolean register) {
    XBreakpointType<?, ?> type = breakpoint.getType();
    if (handler.getBreakpointTypeClass().equals(type.getClass())) {
      //noinspection unchecked
      B b = (B)breakpoint;
      handleBreakpoint(handler, b, register, false);
    }
  }

  public boolean isBreakpointActive(@Nonnull XBreakpoint<?> b) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return !areBreakpointsMuted() && b.isEnabled() && !isInactiveSlaveBreakpoint(b) && !((XBreakpointBase)b).isDisposed();
  }

  @Override
  public boolean areBreakpointsMuted() {
    return mySessionData.isBreakpointsMuted();
  }

  @Override
  public void addSessionListener(@Nonnull XDebugSessionListener listener, @Nonnull Disposable parentDisposable) {
    myDispatcher.addListener(listener, parentDisposable);
  }

  @Override
  public void addSessionListener(@Nonnull final XDebugSessionListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeSessionListener(@Nonnull final XDebugSessionListener listener) {
    myDispatcher.removeListener(listener);
  }

  @Override
  public void setBreakpointMuted(boolean muted) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    if (areBreakpointsMuted() == muted) return;
    mySessionData.setBreakpointsMuted(muted);
    processAllBreakpoints(!muted, muted);
    myDebuggerManager.getBreakpointManager().getLineBreakpointManager().queueAllBreakpointsUpdate();
  }

  @Override
  public void stepOver(final boolean ignoreBreakpoints) {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    if (ignoreBreakpoints) {
      disableBreakpoints();
    }
    myDebugProcess.startStepOver(doResume());
  }

  @Override
  public void stepInto() {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    myDebugProcess.startStepInto(doResume());
  }

  @Override
  public void stepOut() {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    myDebugProcess.startStepOut(doResume());
  }

  @Override
  public <V extends XSmartStepIntoVariant> void smartStepInto(XSmartStepIntoHandler<V> handler, V variant) {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    final XSuspendContext context = doResume();
    handler.startStepInto(variant, context);
  }

  @Override
  public void forceStepInto() {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    myDebugProcess.startForceStepInto(doResume());
  }

  @Override
  public void runToPosition(@Nonnull final XSourcePosition position, final boolean ignoreBreakpoints) {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    if (ignoreBreakpoints) {
      disableBreakpoints();
    }
    myDebugProcess.runToPosition(position, doResume());
  }

  @Override
  public void pause() {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    myDebugProcess.startPausing();
  }

  private void processAllBreakpoints(final boolean register, final boolean temporary) {
    for (XBreakpointHandler<?> handler : myDebugProcess.getBreakpointHandlers()) {
      processBreakpoints(handler, register, temporary);
    }
  }

  private void disableBreakpoints() {
    myBreakpointsDisabled = true;
    processAllBreakpoints(false, true);
  }

  @Override
  public void resume() {
    if (!myDebugProcess.checkCanPerformCommands()) return;

    myDebugProcess.resume(doResume());
  }

  @Nullable
  private XSuspendContext doResume() {
    if (!myPaused.getAndSet(false)) {
      return null;
    }

    myDispatcher.getMulticaster().beforeSessionResume();
    XSuspendContext context = mySuspendContext;
    mySuspendContext = null;
    myCurrentExecutionStack = null;
    myCurrentStackFrame = null;
    myTopFramePosition = null;
    myActiveNonLineBreakpoint = null;
    updateExecutionPosition();
    UIUtil.invokeLaterIfNeeded(() -> {
      if (mySessionTab != null) {
        mySessionTab.getUi().clearAttractionBy(XDebuggerUIConstants.LAYOUT_VIEW_BREAKPOINT_CONDITION);
      }
    });
    myDispatcher.getMulticaster().sessionResumed();
    return context;
  }

  @Override
  public void updateExecutionPosition() {
    // allowed only for the active session
    if (myDebuggerManager.getCurrentSession() == this) {
      boolean isTopFrame = isTopFrameSelected();
      myDebuggerManager.updateExecutionPoint(getCurrentPosition(), !isTopFrame, getPositionIconRenderer(isTopFrame));
    }
  }

  public boolean isTopFrameSelected() {
    return myCurrentExecutionStack != null && myIsTopFrame;
  }


  @Override
  public void showExecutionPoint() {
    if (mySuspendContext != null) {
      XExecutionStack executionStack = mySuspendContext.getActiveExecutionStack();
      if (executionStack != null) {
        XStackFrame topFrame = executionStack.getTopFrame();
        if (topFrame != null) {
          setCurrentStackFrame(executionStack, topFrame, true);
          myDebuggerManager.showExecutionPosition();
        }
      }
    }
  }

  @Override
  public void setCurrentStackFrame(@Nonnull XExecutionStack executionStack, @Nonnull XStackFrame frame, boolean isTopFrame) {
    if (mySuspendContext == null) return;

    boolean frameChanged = myCurrentStackFrame != frame;
    myCurrentExecutionStack = executionStack;
    myCurrentStackFrame = frame;
    myIsTopFrame = isTopFrame;
    activateSession();

    if (frameChanged) {
      myDispatcher.getMulticaster().stackFrameChanged();
    }
  }

  void activateSession() {
    myDebuggerManager.setCurrentSession(this);
    updateExecutionPosition();
  }

  public XBreakpoint<?> getActiveNonLineBreakpoint() {
    if (myActiveNonLineBreakpoint != null) {
      XSourcePosition breakpointPosition = myActiveNonLineBreakpoint.getSourcePosition();
      XSourcePosition position = getTopFramePosition();
      if (breakpointPosition == null ||
          (position != null && !(breakpointPosition.getFile().equals(position.getFile()) && breakpointPosition.getLine() == position.getLine()))) {
        return myActiveNonLineBreakpoint;
      }
    }
    return null;
  }

  @Nullable
  private GutterIconRenderer getPositionIconRenderer(boolean isTopFrame) {
    if (!isTopFrame) {
      return null;
    }
    XBreakpoint<?> activeNonLineBreakpoint = getActiveNonLineBreakpoint();
    if (activeNonLineBreakpoint != null) {
      return ((XBreakpointBase<?, ?, ?>)activeNonLineBreakpoint).createGutterIconRenderer();
    }
    if (myCurrentExecutionStack != null) {
      return myCurrentExecutionStack.getExecutionLineIconRenderer();
    }
    return null;
  }

  @Override
  public void updateBreakpointPresentation(@Nonnull final XLineBreakpoint<?> breakpoint,
                                           @Nullable final Image icon,
                                           @Nullable final String errorMessage) {
    CustomizedBreakpointPresentation presentation;
    synchronized (myRegisteredBreakpoints) {
      presentation = myRegisteredBreakpoints.get(breakpoint);
      if (presentation == null ||
          (Comparing.equal(presentation.getIcon(), icon) && Comparing.strEqual(presentation.getErrorMessage(), errorMessage))) {
        return;
      }

      presentation.setErrorMessage(errorMessage);
      presentation.setIcon(icon);
    }
    myDebuggerManager.getBreakpointManager().getLineBreakpointManager().queueBreakpointUpdate((XLineBreakpointImpl<?>)breakpoint);
  }

  @Override
  public void setBreakpointVerified(@Nonnull XLineBreakpoint<?> breakpoint) {
    updateBreakpointPresentation(breakpoint, XDebuggerUtilImpl.getVerifiedIcon(breakpoint), null);
  }

  @Override
  public void setBreakpointInvalid(@Nonnull XLineBreakpoint<?> breakpoint, @Nullable String errorMessage) {
    updateBreakpointPresentation(breakpoint, AllIcons.Debugger.Db_invalid_breakpoint, errorMessage);
  }

  @Override
  public boolean breakpointReached(@Nonnull final XBreakpoint<?> breakpoint, @Nonnull final XSuspendContext suspendContext) {
    return breakpointReached(breakpoint, null, suspendContext);
  }

  @Override
  public boolean breakpointReached(@Nonnull final XBreakpoint<?> breakpoint, @Nullable String evaluatedLogExpression,
                                   @Nonnull XSuspendContext suspendContext) {
    return breakpointReached(breakpoint, evaluatedLogExpression, suspendContext, true);
  }

  public void breakpointReachedNoProcessing(@Nonnull final XBreakpoint<?> breakpoint, @Nonnull XSuspendContext suspendContext) {
    breakpointReached(breakpoint, null, suspendContext, false);
  }

  private boolean breakpointReached(@Nonnull final XBreakpoint<?> breakpoint, @Nullable String evaluatedLogExpression,
                                    @Nonnull XSuspendContext suspendContext, boolean doProcessing) {
    if (doProcessing) {
      if (breakpoint.isLogMessage()) {
        XSourcePosition position = breakpoint.getSourcePosition();
        OpenFileHyperlinkInfo hyperlinkInfo =
                position != null ? new OpenFileHyperlinkInfo(myProject, position.getFile(), position.getLine()) : null;
        printMessage(XDebuggerBundle.message("xbreakpoint.reached.text") + " ", XBreakpointUtil.getShortText(breakpoint), hyperlinkInfo);
      }

      if (evaluatedLogExpression != null) {
        printMessage(evaluatedLogExpression, null, null);
      }

      processDependencies(breakpoint);

      if (breakpoint.getSuspendPolicy() == SuspendPolicy.NONE) {
        return false;
      }
    }

    myActiveNonLineBreakpoint =
            (!(breakpoint instanceof XLineBreakpoint) || ((XLineBreakpoint)breakpoint).getType().canBeHitInOtherPlaces()) ? breakpoint : null;

    // set this session active on breakpoint, update execution position will be called inside positionReached
    myDebuggerManager.setCurrentSession(this);

    positionReachedInternal(suspendContext, true);

    if (doProcessing && breakpoint instanceof XLineBreakpoint<?> && ((XLineBreakpoint)breakpoint).isTemporary()) {
      handleTemporaryBreakpointHit(breakpoint);
    }
    return true;
  }

  private void handleTemporaryBreakpointHit(final XBreakpoint<?> breakpoint) {
    addSessionListener(new XDebugSessionListener() {
      private void removeBreakpoint() {
        XDebuggerUtil.getInstance().removeBreakpoint(myProject, breakpoint);
        removeSessionListener(this);
      }

      @Override
      public void sessionResumed() {
        removeBreakpoint();
      }

      @Override
      public void sessionStopped() {
        removeBreakpoint();
      }
    });
  }

  public void processDependencies(final XBreakpoint<?> breakpoint) {
    XDependentBreakpointManager dependentBreakpointManager = myDebuggerManager.getBreakpointManager().getDependentBreakpointManager();
    if (!dependentBreakpointManager.isMasterOrSlave(breakpoint)) return;

    List<XBreakpoint<?>> breakpoints = dependentBreakpointManager.getSlaveBreakpoints(breakpoint);
    myInactiveSlaveBreakpoints.removeAll(breakpoints);
    for (XBreakpoint<?> slaveBreakpoint : breakpoints) {
      processAllHandlers(slaveBreakpoint, true);
    }

    if (dependentBreakpointManager.getMasterBreakpoint(breakpoint) != null && !dependentBreakpointManager.isLeaveEnabled(breakpoint)) {
      boolean added = myInactiveSlaveBreakpoints.add(breakpoint);
      if (added) {
        processAllHandlers(breakpoint, false);
        myDebuggerManager.getBreakpointManager().getLineBreakpointManager().queueBreakpointUpdate(breakpoint);
      }
    }
  }

  private void printMessage(final String message, final String hyperLinkText, @Nullable final HyperlinkInfo info) {
    AppUIUtil.invokeOnEdt(() -> {
      myConsoleView.print(message, ConsoleViewContentType.SYSTEM_OUTPUT);
      if (info != null) {
        myConsoleView.printHyperlink(hyperLinkText, info);
      }
      else if (hyperLinkText != null) {
        myConsoleView.print(hyperLinkText, ConsoleViewContentType.SYSTEM_OUTPUT);
      }
      myConsoleView.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    });
  }

  public void unsetPaused() {
    myPaused.set(false);
  }

  private void positionReachedInternal(@Nonnull final XSuspendContext suspendContext, boolean attract) {
    enableBreakpoints();
    mySuspendContext = suspendContext;
    myCurrentExecutionStack = suspendContext.getActiveExecutionStack();
    myCurrentStackFrame = myCurrentExecutionStack != null ? myCurrentExecutionStack.getTopFrame() : null;
    myIsTopFrame = true;
    myTopFramePosition = myCurrentStackFrame != null ? myCurrentStackFrame.getSourcePosition() : null;

    myPaused.set(true);

    updateExecutionPosition();

    final boolean showOnSuspend = myShowTabOnSuspend.compareAndSet(true, false);
    if (showOnSuspend || attract) {
      AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
        if (showOnSuspend) {
          initSessionTab(null);
          showSessionTab();
        }

        // user attractions should only be made if event happens independently (e.g. program paused/suspended)
        // and should not be made when user steps in the code
        if (attract) {
          if (mySessionTab == null) {
            LOG.debug("Cannot request focus because Session Tab is not initialized yet");
            return;
          }

          if (XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().isShowDebuggerOnBreakpoint()) {
            mySessionTab.toFront(true, this::updateExecutionPosition);
          }

          if (myTopFramePosition == null) {
            // if there is no source position available, we should somehow tell the user that session is stopped.
            // the best way is to show the stack frames.
            XDebugSessionTab.showFramesView(this);
          }

          mySessionTab.getUi().attractBy(XDebuggerUIConstants.LAYOUT_VIEW_BREAKPOINT_CONDITION);
        }
      });
    }

    myDispatcher.getMulticaster().sessionPaused();
  }

  @Override
  public void positionReached(@Nonnull XSuspendContext suspendContext, boolean attract) {
    myActiveNonLineBreakpoint = null;
    positionReachedInternal(suspendContext, attract);
  }

  @Override
  public void sessionResumed() {
    doResume();
  }

  private void enableBreakpoints() {
    if (myBreakpointsDisabled) {
      myBreakpointsDisabled = false;
      AccessRule.read(() -> {
        processAllBreakpoints(true, false);
      });
    }
  }

  @Override
  public boolean isStopped() {
    return myStopped.get();
  }

  private void stopImpl() {
    if (!myStopped.compareAndSet(false, true)) {
      return;
    }

    try {
      if (breakpointsInitialized) {
        XBreakpointManagerImpl breakpointManager = myDebuggerManager.getBreakpointManager();
        if (myBreakpointListener != null) {
          breakpointManager.removeBreakpointListener(myBreakpointListener);
        }
        if (myDependentBreakpointListener != null) {
          breakpointManager.getDependentBreakpointManager().removeListener(myDependentBreakpointListener);
        }
      }
    }
    finally {
      //noinspection unchecked
      myDebugProcess.stopAsync().doWhenDone(value -> {
        if (!myProject.isDisposed()) {
          myProject.getMessageBus().syncPublisher(XDebuggerManagerListener.class).processStopped(myDebugProcess);
        }

        if (mySessionTab != null) {
          AppUIUtil.invokeOnEdt(() -> {
            ((XWatchesViewImpl)mySessionTab.getWatchesView()).updateSessionData();
            mySessionTab.detachFromSession();
          });
        }
        else if (myConsoleView != null) {
          AppUIUtil.invokeOnEdt(() -> Disposer.dispose(myConsoleView));
        }

        myTopFramePosition = null;
        myCurrentExecutionStack = null;
        myCurrentStackFrame = null;
        mySuspendContext = null;

        updateExecutionPosition();

        if (myValueMarkers != null) {
          myValueMarkers.clear();
        }
        if (XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().isUnmuteOnStop()) {
          mySessionData.setBreakpointsMuted(false);
        }
        myDebuggerManager.removeSession(this);
        myDispatcher.getMulticaster().sessionStopped();
        myDispatcher.getListeners().clear();

        myProject.putUserData(XDebuggerEditorLinePainter.CACHE, null);

        synchronized (myRegisteredBreakpoints) {
          myRegisteredBreakpoints.clear();
        }
      });
    }
  }

  public boolean isInactiveSlaveBreakpoint(final XBreakpoint<?> breakpoint) {
    return myInactiveSlaveBreakpoints.contains(breakpoint);
  }

  @Override
  public void stop() {
    ProcessHandler processHandler = myDebugProcess == null ? null : myDebugProcess.getProcessHandler();
    if (processHandler == null || processHandler.isProcessTerminated() || processHandler.isProcessTerminating()) return;

    if (processHandler.detachIsDefault()) {
      processHandler.detachProcess();
    }
    else {
      processHandler.destroyProcess();
    }
  }

  @Override
  public void reportMessage(@Nonnull final String message, @Nonnull final NotificationType type, @Nullable final HyperlinkListener listener) {
    NotificationListener notificationListener = listener == null ? null : (notification, event) -> {
      if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        listener.hyperlinkUpdate(event);
      }
    };

    NOTIFICATION_GROUP.createNotification("", message, consulo.project.ui.notification.NotificationType.from(type), notificationListener).notify(myProject);
  }

  private class MyBreakpointListener implements XBreakpointListener<XBreakpoint<?>> {
    @Override
    public void breakpointAdded(@Nonnull final XBreakpoint<?> breakpoint) {
      if (!myBreakpointsDisabled) {
        processAllHandlers(breakpoint, true);
      }
    }

    @Override
    public void breakpointRemoved(@Nonnull final XBreakpoint<?> breakpoint) {
      if (getActiveNonLineBreakpoint() == breakpoint) {
        myActiveNonLineBreakpoint = null;
      }
      processAllHandlers(breakpoint, false);
    }

    @Override
    public void breakpointChanged(@Nonnull final XBreakpoint<?> breakpoint) {
      breakpointRemoved(breakpoint);
      breakpointAdded(breakpoint);
    }
  }

  private class MyDependentBreakpointListener implements XDependentBreakpointListener {
    @Override
    public void dependencySet(final XBreakpoint<?> slave, final XBreakpoint<?> master) {
      boolean added = myInactiveSlaveBreakpoints.add(slave);
      if (added) {
        processAllHandlers(slave, false);
      }
    }

    @Override
    public void dependencyCleared(final XBreakpoint<?> breakpoint) {
      boolean removed = myInactiveSlaveBreakpoints.remove(breakpoint);
      if (removed) {
        processAllHandlers(breakpoint, true);
      }
    }
  }

  @Nonnull
  private String getConfigurationName() {
    if (myEnvironment != null) {
      RunProfile profile = myEnvironment.getRunProfile();
      if (profile instanceof RunConfiguration) {
        return ((RunConfiguration)profile).getType().getId();
      }
    }
    return getSessionName();
  }

  public void setWatchExpressions(@Nonnull XExpression[] watchExpressions) {
    mySessionData.setWatchExpressions(watchExpressions);
    myDebuggerManager.getWatchesManager().setWatches(getConfigurationName(), watchExpressions);
  }

  XExpression[] getWatchExpressions() {
    return myDebuggerManager.getWatchesManager().getWatches(getConfigurationName());
  }

  @Nullable
  public ExecutionEnvironment getExecutionEnvironment() {
    return myEnvironment;
  }
}

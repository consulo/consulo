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
package consulo.execution.debug.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.codeEditor.*;
import consulo.codeEditor.event.*;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.document.util.DocumentUtil;
import consulo.execution.ExecutionManager;
import consulo.execution.debug.*;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.evaluation.ValueLookupManager;
import consulo.execution.debug.event.XBreakpointListener;
import consulo.execution.debug.event.XDebuggerManagerListener;
import consulo.execution.debug.impl.internal.action.ShowBreakpointsOverLineNumbersAction;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointManagerImpl;
import consulo.execution.debug.impl.internal.evaluate.ValueLookupManagerImpl;
import consulo.execution.debug.impl.internal.setting.DebuggerConfigurable;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.execution.debug.impl.internal.ui.ExecutionPointHighlighter;
import consulo.execution.debug.impl.internal.ui.XDebugSessionTab;
import consulo.execution.debug.internal.breakpoint.BreakpointEditorUtil;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.event.RunContentWithExecutorListener;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import consulo.util.lang.ObjectUtil;
import consulo.util.xml.serializer.annotation.Property;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author nik
 */
@State(name = "XDebuggerManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceImpl
public class XDebuggerManagerImpl implements XDebuggerManager, PersistentStateComponent<XDebuggerManagerImpl.XDebuggerState>, Disposable {
    private final class BreakpointPromoterEditorListener implements EditorMouseMotionListener, EditorMouseListener {
        private XSourcePositionImpl myLastPosition = null;
        private Image myLastIcon = null;

        private final XDebuggerLineChangeHandler lineChangeHandler;

        BreakpointPromoterEditorListener() {
            lineChangeHandler = new XDebuggerLineChangeHandler((gutter, position, types) -> {
                myLastIcon = ObjectUtil.doIfNotNull(ContainerUtil.getFirstItem(types), XBreakpointType::getEnabledIcon);
                if (myLastIcon != null) {
                    updateActiveLineNumberIcon(gutter, myLastIcon, position.getLine());
                }
            });
        }

        @RequiredUIAccess
        @Override
        public void mouseMoved(@Nonnull EditorMouseEvent e) {
            if (!ShowBreakpointsOverLineNumbersAction.isSelected()) {
                return;
            }
            Editor editor = e.getEditor();
            if (editor.getProject() != myProject || editor.getEditorKind() != EditorKind.MAIN_EDITOR) {
                return;
            }
            EditorGutter editorGutter = editor.getGutter();
            if (editorGutter instanceof EditorGutterComponentEx gutter) {
                if (e.getArea() == EditorMouseEventArea.LINE_NUMBERS_AREA && BreakpointEditorUtil.isBreakPointsOnLineNumbers()) {
                    int line = consulo.codeEditor.util.EditorUtil.yToLogicalLineNoCustomRenderers(editor, e.getMouseEvent().getY());
                    Document document = editor.getDocument();
                    if (DocumentUtil.isValidLine(line, document)) {
                        XSourcePositionImpl position = XSourcePositionImpl.create(FileDocumentManager.getInstance().getFile(document), line);
                        if (position != null) {
                            if (myLastPosition == null || !myLastPosition.getFile().equals(position.getFile()) || myLastPosition.getLine() != line) {
                                // drop an icon first and schedule the available types calculation
                                clear(gutter);
                                myLastPosition = position;
                                lineChangeHandler.lineChanged(editor, position);
                            }
                            return;
                        }
                    }
                }
                if (myLastIcon != null) {
                    clear(gutter);
                    myLastPosition = null;
                    lineChangeHandler.exitedGutter();
                }
            }
        }

        private void clear(EditorGutterComponentEx gutter) {
            updateActiveLineNumberIcon(gutter, null, null);
            myLastIcon = null;
        }

        private static void updateActiveLineNumberIcon(@Nonnull EditorGutterComponentEx gutter, @Nullable Image icon, @Nullable Integer line) {
            JComponent component = gutter.getComponent();

            if (component.getClientProperty("editor.gutter.context.menu") != null) {
                return;
            }
            boolean requireRepaint = false;
            if (component.getClientProperty("line.number.hover.icon") != icon) {
                component.putClientProperty("line.number.hover.icon", icon);
                component.putClientProperty("line.number.hover.icon.context.menu", icon == null ? null
                    : ActionManager.getInstance().getAction("XDebugger.Hover.Breakpoint.Context.Menu"));
                if (icon != null) {
                    component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Editor updates cursor on MouseMoved, set it explicitly
                }
                requireRepaint = true;
            }
            if (!Objects.equals(component.getClientProperty("active.line.number"), line)) {
                component.putClientProperty("active.line.number", line);
                requireRepaint = true;
            }
            if (requireRepaint) {
                gutter.repaint();
            }
        }
    }

    @Nonnull
    private final Project myProject;
    private final EditorFactory myEditorFactory;
    private final XBreakpointManagerImpl myBreakpointManager;
    private final XDebuggerWatchesManager myWatchesManager;
    private final Map<ProcessHandler, XDebugSessionImpl> mySessions;
    private final ExecutionPointHighlighter myExecutionPointHighlighter;
    private final AtomicReference<XDebugSessionImpl> myActiveSession = new AtomicReference<>();

    @Inject
    public XDebuggerManagerImpl(
        @Nonnull Project project,
        @Nonnull StartupManager startupManager,
        @Nonnull EditorFactory editorFactory,
        @Nonnull ApplicationConcurrency applicationConcurrency
    ) {
        myProject = project;
        myEditorFactory = editorFactory;
        myBreakpointManager = new XBreakpointManagerImpl(project, this, startupManager, applicationConcurrency);
        myWatchesManager = new XDebuggerWatchesManager();
        mySessions = new LinkedHashMap<>();
        myExecutionPointHighlighter = new ExecutionPointHighlighter(project);
    }

    public void projectOpened() {
        MessageBusConnection messageBusConnection = myProject.getMessageBus().connect();
        messageBusConnection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
            @Override
            public void fileContentLoaded(@Nonnull VirtualFile file, @Nonnull Document document) {
                updateExecutionPoint(file, true);
            }

            @Override
            public void fileContentReloaded(@Nonnull VirtualFile file, @Nonnull Document document) {
                updateExecutionPoint(file, true);
            }
        });
        messageBusConnection.subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
                updateExecutionPoint(file, false);
            }
        });
        myBreakpointManager.addBreakpointListener(new XBreakpointListener<>() {
            @Override
            public void breakpointChanged(@Nonnull XBreakpoint<?> breakpoint) {
                if (!(breakpoint instanceof XLineBreakpoint)) {
                    final XDebugSessionImpl session = getCurrentSession();
                    if (session != null && breakpoint.equals(session.getActiveNonLineBreakpoint())) {
                        final XBreakpointBase breakpointBase = (XBreakpointBase) breakpoint;
                        breakpointBase.clearIcon();
                        myExecutionPointHighlighter.updateGutterIcon(breakpointBase.createGutterIconRenderer());
                    }
                }
            }

            @Override
            public void breakpointRemoved(@Nonnull XBreakpoint<?> breakpoint) {
                XDebugSessionImpl session = getCurrentSession();
                if (session != null && breakpoint == session.getActiveNonLineBreakpoint()) {
                    myExecutionPointHighlighter.updateGutterIcon(null);
                }
            }
        });

        messageBusConnection.subscribe(RunContentWithExecutorListener.class, new RunContentWithExecutorListener() {
            @Override
            public void contentSelected(@Nullable RunContentDescriptor descriptor, @Nonnull Executor executor) {
                if (descriptor != null && executor.equals(DefaultDebugExecutor.getDebugExecutorInstance())) {
                    XDebugSessionImpl session = mySessions.get(descriptor.getProcessHandler());
                    if (session != null) {
                        session.activateSession();
                    }
                    else {
                        setCurrentSession(null);
                    }
                }
            }

            @Override
            public void contentRemoved(@Nullable RunContentDescriptor descriptor, @Nonnull Executor executor) {
                if (descriptor != null && executor.equals(DefaultDebugExecutor.getDebugExecutorInstance())) {
                    mySessions.remove(descriptor.getProcessHandler());
                }
            }
        });

        BreakpointPromoterEditorListener bpPromoter = new BreakpointPromoterEditorListener();
        EditorEventMulticaster eventMulticaster = myEditorFactory.getEventMulticaster();
        eventMulticaster.addEditorMouseMotionListener(bpPromoter, this);
    }

    private void updateExecutionPoint(@Nonnull VirtualFile file, boolean navigate) {
        if (file.equals(myExecutionPointHighlighter.getCurrentFile())) {
            myExecutionPointHighlighter.update(navigate);
        }
    }

    @RequiredUIAccess
    @Override
    public void showSettings() {
        ShowConfigurableService service = myProject.getApplication().getInstance(ShowConfigurableService.class);
        service.showAndSelect(myProject, DebuggerConfigurable.class);
    }

    @Override
    public void dispose() {
    }

    @Override
    @Nonnull
    public XBreakpointManagerImpl getBreakpointManager() {
        return myBreakpointManager;
    }

    public XDebuggerWatchesManager getWatchesManager() {
        return myWatchesManager;
    }

    public Project getProject() {
        return myProject;
    }

    @Override
    @Nonnull
    public XDebugSession startSession(@Nonnull ExecutionEnvironment environment,
                                      @Nonnull XDebugProcessStarter processStarter) throws ExecutionException {
        return startSession(environment.getContentToReuse(), processStarter, new XDebugSessionImpl(environment, this));
    }

    @Nonnull
    @Override
    public XDebugSession startSessionAndShowTab(@Nonnull String sessionName,
                                                Image icon,
                                                @Nullable RunContentDescriptor contentToReuse,
                                                boolean showToolWindowOnSuspendOnly,
                                                @Nonnull XDebugProcessStarter starter) throws ExecutionException {
        XDebugSessionImpl session = startSession(contentToReuse,
            starter,
            new XDebugSessionImpl(null,
                this,
                sessionName,
                icon,
                showToolWindowOnSuspendOnly,
                contentToReuse));

        if (!showToolWindowOnSuspendOnly) {
            session.showSessionTab();
        }
        ProcessHandler handler = session.getDebugProcess().getProcessHandler();
        handler.startNotify();
        return session;
    }

    private XDebugSessionImpl startSession(@Nullable RunContentDescriptor contentToReuse,
                                           @Nonnull XDebugProcessStarter processStarter,
                                           @Nonnull XDebugSessionImpl session) throws ExecutionException {
        XDebugProcess process = processStarter.start(session);
        myProject.getMessageBus().syncPublisher(XDebuggerManagerListener.class).processStarted(process);

        // Perform custom configuration of session data for XDebugProcessConfiguratorStarter classes
        if (processStarter.canConfigure()) {
            session.activateSession();
            processStarter.configure(session.getSessionData());
        }

        session.init(process, contentToReuse);

        mySessions.put(session.getDebugProcess().getProcessHandler(), session);

        return session;
    }

    public void removeSession(@Nonnull final XDebugSessionImpl session) {
        XDebugSessionTab sessionTab = session.getSessionTab();
        mySessions.remove(session.getDebugProcess().getProcessHandler());
        if (sessionTab != null &&
            !myProject.isDisposed() &&
            !ApplicationManager.getApplication().isUnitTestMode() &&
            XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().isHideDebuggerOnProcessTermination()) {
            ExecutionManager.getInstance(myProject)
                .getContentManager()
                .hideRunContent(DefaultDebugExecutor.getDebugExecutorInstance(), sessionTab.getRunContentDescriptor());
        }
        if (myActiveSession.compareAndSet(session, null)) {
            onActiveSessionChanged();
        }
    }

    void updateExecutionPoint(@Nullable XSourcePosition position, boolean nonTopFrame, @Nullable GutterIconRenderer gutterIconRenderer) {
        if (position != null) {
            myExecutionPointHighlighter.show(position, nonTopFrame, gutterIconRenderer);
        }
        else {
            myExecutionPointHighlighter.hide();
        }
    }

    private void onActiveSessionChanged() {
        myBreakpointManager.getLineBreakpointManager().queueAllBreakpointsUpdate();
        ApplicationManager.getApplication().invokeLater(() -> ((ValueLookupManagerImpl) ValueLookupManager.getInstance(myProject)).hideHint(), myProject.getDisposed());
    }

    @Override
    @Nonnull
    public XDebugSession[] getDebugSessions() {
        final Collection<XDebugSessionImpl> sessions = mySessions.values();
        return sessions.toArray(new XDebugSessionImpl[sessions.size()]);
    }

    @Override
    @Nullable
    public XDebugSession getDebugSession(@Nonnull ExecutionConsole executionConsole) {
        for (final XDebugSessionImpl debuggerSession : mySessions.values()) {
            XDebugSessionTab sessionTab = debuggerSession.getSessionTab();
            if (sessionTab != null) {
                RunContentDescriptor contentDescriptor = sessionTab.getRunContentDescriptor();
                if (contentDescriptor != null && executionConsole == contentDescriptor.getExecutionConsole()) {
                    return debuggerSession;
                }
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public <T extends XDebugProcess> List<? extends T> getDebugProcesses(Class<T> processClass) {
        List<T> list = null;
        for (XDebugSessionImpl session : mySessions.values()) {
            final XDebugProcess process = session.getDebugProcess();
            if (processClass.isInstance(process)) {
                if (list == null) {
                    list = new SmartList<>();
                }
                list.add(processClass.cast(process));
            }
        }
        return Lists.notNullize(list);
    }

    @Override
    @Nullable
    public XDebugSessionImpl getCurrentSession() {
        return myActiveSession.get();
    }

    void setCurrentSession(@Nullable XDebugSessionImpl session) {
        boolean sessionChanged = myActiveSession.getAndSet(session) != session;
        if (sessionChanged) {
            if (session != null) {
                XDebugSessionTab tab = session.getSessionTab();
                if (tab != null) {
                    tab.select();
                }
            }
            else {
                myExecutionPointHighlighter.hide();
            }
            onActiveSessionChanged();
        }
    }

    @Override
    public XDebuggerState getState() {
        return new XDebuggerState(myBreakpointManager.getState(), myWatchesManager.getState());
    }

    public boolean isFullLineHighlighter() {
        return myExecutionPointHighlighter.isFullLineHighlighter();
    }

    @Override
    public void loadState(final XDebuggerState state) {
        myBreakpointManager.loadState(state.myBreakpointManagerState);
        myWatchesManager.loadState(state.myWatchesManagerState);
    }

    public void showExecutionPosition() {
        myExecutionPointHighlighter.navigateTo();
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class XDebuggerState {
        private XBreakpointManagerImpl.BreakpointManagerState myBreakpointManagerState;
        private XDebuggerWatchesManager.WatchesManagerState myWatchesManagerState;

        public XDebuggerState() {
        }

        public XDebuggerState(final XBreakpointManagerImpl.BreakpointManagerState breakpointManagerState,
                              XDebuggerWatchesManager.WatchesManagerState watchesManagerState) {
            myBreakpointManagerState = breakpointManagerState;
            myWatchesManagerState = watchesManagerState;
        }

        @Property(surroundWithTag = false)
        public XBreakpointManagerImpl.BreakpointManagerState getBreakpointManagerState() {
            return myBreakpointManagerState;
        }

        public void setBreakpointManagerState(final XBreakpointManagerImpl.BreakpointManagerState breakpointManagerState) {
            myBreakpointManagerState = breakpointManagerState;
        }

        @Property(surroundWithTag = false)
        public XDebuggerWatchesManager.WatchesManagerState getWatchesManagerState() {
            return myWatchesManagerState;
        }

        public void setWatchesManagerState(XDebuggerWatchesManager.WatchesManagerState watchesManagerState) {
            myWatchesManagerState = watchesManagerState;
        }
    }
}

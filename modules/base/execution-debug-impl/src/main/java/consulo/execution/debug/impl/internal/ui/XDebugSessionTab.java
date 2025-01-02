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
package consulo.execution.debug.impl.internal.ui;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposer;
import consulo.execution.ExecutionManager;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.frame.*;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.DebuggerContentInfo;
import consulo.execution.debug.ui.XDebugSessionData;
import consulo.execution.debug.ui.XDebugTabLayouter;
import consulo.execution.impl.internal.ui.layout.ViewImpl;
import consulo.execution.internal.layout.RunnerContentUi;
import consulo.execution.internal.layout.RunnerLayoutUiImpl;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.RunContentBuilder;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.RunContentManager;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.layout.PlaceInGrid;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.util.AppUIUtil;
import consulo.project.ui.util.ProjectUIUtil;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.action.*;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.action.ToolWindowActions;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.SystemProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XDebugSessionTab extends DebuggerSessionTabBase {
    public static final Key<XDebugSessionTab> TAB_KEY = Key.create("XDebugSessionTab");

    private XWatchesViewImpl myWatchesView;

    private final Map<String, XDebugView> myViews = new LinkedHashMap<>();

    @Nullable
    private XDebugSessionImpl mySession;
    private XDebugSessionData mySessionData;

    private final Runnable myRebuildWatchesRunnable = new Runnable() {
        @Override
        public void run() {
            if (myWatchesView != null) {
                myWatchesView.computeWatches();
            }
        }
    };

    @Nonnull
    public static XDebugSessionTab create(@Nonnull XDebugSessionImpl session, @Nullable Image icon, @Nullable ExecutionEnvironment environment, @Nullable RunContentDescriptor contentToReuse) {
        if (contentToReuse != null && SystemProperties.getBooleanProperty("xdebugger.reuse.session.tab", false)) {
            JComponent component = contentToReuse.getComponent();
            if (component != null) {
                XDebugSessionTab oldTab = DataManager.getInstance().getDataContext(component).getData(TAB_KEY);
                if (oldTab != null) {
                    oldTab.setSession(session, environment, icon);
                    oldTab.attachToSession(session);
                    return oldTab;
                }
            }
        }
        XDebugSessionTab tab = new XDebugSessionTab(session, icon, environment);
        tab.myRunContentDescriptor.setActivateToolWindowWhenAdded(contentToReuse == null || contentToReuse.isActivateToolWindowWhenAdded());
        return tab;
    }

    @Nonnull
    public RunnerLayoutUi getUi() {
        return myUi;
    }

    private XDebugSessionTab(@Nonnull XDebugSessionImpl session, @Nullable Image icon, @Nullable ExecutionEnvironment environment) {
        super(session.getProject(), "Debug", session.getSessionName(), GlobalSearchScope.allScope(session.getProject()));

        setSession(session, environment, icon);

        myUi.addContent(createFramesContent(), 0, PlaceInGrid.left, false);
        addVariablesAndWatches(session);

        attachToSession(session);

        DefaultActionGroup focus = new DefaultActionGroup();
        focus.add(ActionManager.getInstance().getAction(XDebuggerActions.FOCUS_ON_BREAKPOINT));
        myUi.getOptions().setAdditionalFocusActions(focus);

        myUi.addListener(new ContentManagerListener() {
            @Override
            public void selectionChanged(ContentManagerEvent event) {
                Content content = event.getContent();
                if (mySession != null && content.isSelected() && getWatchesContentId().equals(ViewImpl.ID.get(content))) {
                    myRebuildWatchesRunnable.run();
                }
            }
        }, myRunContentDescriptor);

        rebuildViews();
    }

    private void addVariablesAndWatches(@Nonnull XDebugSessionImpl session) {
        myUi.addContent(createVariablesContent(session), 0, PlaceInGrid.center, false);
    }

    private void setSession(@Nonnull XDebugSessionImpl session, @Nullable ExecutionEnvironment environment, @Nullable Image icon) {
        myEnvironment = environment;
        mySession = session;
        mySessionData = session.getSessionData();
        myConsole = session.getConsoleView();

        AnAction[] restartActions;
        List<AnAction> restartActionsList = session.getRestartActions();
        if (ContainerUtil.isEmpty(restartActionsList)) {
            restartActions = AnAction.EMPTY_ARRAY;
        }
        else {
            restartActions = restartActionsList.toArray(new AnAction[restartActionsList.size()]);
        }

        myRunContentDescriptor =
            new RunContentDescriptor(myConsole, session.getDebugProcess().getProcessHandler(), myUi.getComponent(), session.getSessionName(), icon, myRebuildWatchesRunnable, restartActions);
        Disposer.register(myRunContentDescriptor, this);
        Disposer.register(myProject, myRunContentDescriptor);
    }

    @Nullable
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (XWatchesView.DATA_KEY == dataId) {
            return myWatchesView;
        }
        else if (TAB_KEY == dataId) {
            return this;
        }
        else if (XDebugSessionData.DATA_KEY == dataId) {
            return mySessionData;
        }

        if (mySession != null) {
            if (XDebugSession.DATA_KEY == dataId) {
                return mySession;
            }
            else if (ConsoleView.KEY == dataId) {
                return mySession.getConsoleView();
            }
        }

        return super.getData(dataId);
    }

    private Content createVariablesContent(@Nonnull XDebugSessionImpl session) {
        XVariablesView variablesView;
        variablesView = myWatchesView = new XWatchesViewImpl(session, true);
        registerView(DebuggerContentInfo.VARIABLES_CONTENT, variablesView);

        Content result = myUi.createContent(
            DebuggerContentInfo.VARIABLES_CONTENT,
            variablesView.getPanel(),
            XDebuggerLocalize.debuggerSessionTabVariablesTitle().get(),
            ExecutionDebugIconGroup.nodeValue(),
            null
        );
        result.setCloseable(false);

        ActionGroup group = DebuggerSessionTabBase.getCustomizedActionGroup(XDebuggerActions.VARIABLES_TREE_TOOLBAR_GROUP);
        result.setActions(group, ActionPlaces.DEBUGGER_TOOLBAR, variablesView.getTree());
        return result;
    }

    @Nonnull
    private Content createFramesContent() {
        XFramesView framesView = new XFramesView(myProject);
        registerView(DebuggerContentInfo.FRAME_CONTENT, framesView);
        Content framesContent = myUi.createContent(
            DebuggerContentInfo.FRAME_CONTENT,
            framesView.getMainPanel(),
            XDebuggerLocalize.debuggerSessionTabFramesTitle().get(),
            ExecutionDebugIconGroup.nodeFrame(),
            null
        );
        framesContent.setCloseable(false);
        return framesContent;
    }

    public void rebuildViews() {
        AppUIUtil.invokeLaterIfProjectAlive(myProject, () -> {
            if (mySession != null) {
                for (XDebugView view : myViews.values()) {
                    view.processSessionEvent(XDebugView.SessionEvent.SETTINGS_CHANGED, mySession);
                }
            }
        });
    }

    public XWatchesView getWatchesView() {
        return myWatchesView;
    }

    private void attachToSession(@Nonnull XDebugSessionImpl session) {
        for (XDebugView view : myViews.values()) {
            attachViewToSession(session, view);
        }

        XDebugTabLayouter layouter = session.getDebugProcess().createTabLayouter();
        Content consoleContent = layouter.registerConsoleContent(myUi, myConsole);
        attachNotificationTo(consoleContent);

        layouter.registerAdditionalContent(myUi);
        RunContentBuilder.addAdditionalConsoleEditorActions(myConsole, consoleContent);

        DefaultActionGroup leftToolbar = new DefaultActionGroup();
        if (myEnvironment != null) {
            leftToolbar.add(ActionManager.getInstance().getAction(IdeActions.ACTION_RERUN));
            List<AnAction> additionalRestartActions = session.getRestartActions();
            if (!additionalRestartActions.isEmpty()) {
                leftToolbar.addAll(additionalRestartActions);
                leftToolbar.addSeparator();
            }
            leftToolbar.addAll(session.getExtraActions());
        }
        leftToolbar.addAll(DebuggerSessionTabBase.getCustomizedActionGroup(XDebuggerActions.TOOL_WINDOW_LEFT_TOOLBAR_GROUP));

        for (AnAction action : session.getExtraStopActions()) {
            leftToolbar.add(action, new Constraints(Anchor.AFTER, IdeActions.ACTION_STOP_PROGRAM));
        }

        //group.addSeparator();
        //addAction(group, DebuggerActions.EXPORT_THREADS);
        leftToolbar.addSeparator();

        leftToolbar.add(myUi.getOptions().getLayoutActions());
        final AnAction[] commonSettings = myUi.getOptions().getSettingsActionsList();
        DefaultActionGroup settings = new DefaultActionGroup(ActionLocalize.groupXdebuggerSettingsText(), true);
        settings.getTemplatePresentation().setIcon(myUi.getOptions().getSettingsActions().getTemplatePresentation().getIcon());
        settings.addAll(commonSettings);
        leftToolbar.add(settings);

        leftToolbar.addSeparator();

        leftToolbar.add(ToolWindowActions.getPinAction());

        DefaultActionGroup topToolbar = new DefaultActionGroup();
        topToolbar.addAll(DebuggerSessionTabBase.getCustomizedActionGroup(XDebuggerActions.TOOL_WINDOW_TOP_TOOLBAR_GROUP));

        session.getDebugProcess().registerAdditionalActions(leftToolbar, topToolbar, settings);
        myUi.getOptions().setLeftToolbar(leftToolbar, ActionPlaces.DEBUGGER_TOOLBAR);
        myUi.getOptions().setTopToolbar(topToolbar, ActionPlaces.DEBUGGER_TOOLBAR);

        if (myEnvironment != null) {
            initLogConsoles(myEnvironment.getRunProfile(), myRunContentDescriptor, myConsole);
        }
    }

    private static void attachViewToSession(@Nonnull XDebugSessionImpl session, @Nullable XDebugView view) {
        if (view != null) {
            session.addSessionListener(new XDebugViewSessionListener(view, session), view);
        }
    }

    public void detachFromSession() {
        assert mySession != null;
        mySession = null;
    }

    @Nullable
    public RunContentDescriptor getRunContentDescriptor() {
        return myRunContentDescriptor;
    }

    public static void showWatchesView(@Nonnull XDebugSessionImpl session) {
        XDebugSessionTab tab = session.getSessionTab();
        if (tab != null) {
            showView(session, tab.getWatchesContentId());
        }
    }

    public static void showFramesView(@Nonnull XDebugSessionImpl session) {
        showView(session, DebuggerContentInfo.FRAME_CONTENT);
    }

    private static void showView(@Nullable XDebugSessionImpl session, String viewId) {
        XDebugSessionTab tab = session != null ? session.getSessionTab() : null;
        if (tab != null) {
            tab.toFront(false, null);
            Content content = tab.findOrRestoreContentIfNeeded(viewId);
            // make sure we make it visible to the user
            if (content != null) {
                tab.myUi.selectAndFocus(content, false, false);
            }
        }
    }

    public void toFront(boolean focus, @Nullable final Runnable onShowCallback) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (myRunContentDescriptor != null) {
                RunContentManager manager = ExecutionManager.getInstance(myProject).getContentManager();
                ToolWindow toolWindow = manager.getToolWindowByDescriptor(myRunContentDescriptor);
                if (toolWindow != null) {
                    if (!toolWindow.isVisible()) {
                        toolWindow.show(() -> {
                            if (onShowCallback != null) {
                                onShowCallback.run();
                            }
                            myRebuildWatchesRunnable.run();
                        });
                    }
                    manager.selectRunContent(myRunContentDescriptor);
                }
            }
        });

        if (focus) {
            ApplicationManager.getApplication().invokeLater(() -> {
                boolean focusWnd = XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().isMayBringFrameToFrontOnBreakpoint();
                ProjectUIUtil.focusProjectWindow(myProject, focusWnd);
                if (!focusWnd) {
                    AppIcon.getInstance().requestAttention(myProject, true);
                }
            });
        }
    }

    @Nonnull
    private String getWatchesContentId() {
        return DebuggerContentInfo.VARIABLES_CONTENT;
    }

    private void registerView(String contentId, @Nonnull XDebugView view) {
        myViews.put(contentId, view);
        Disposer.register(myRunContentDescriptor, view);
    }

    private void removeContent(String contentId) {
        myUi.removeContent(findOrRestoreContentIfNeeded(contentId), true);
        unregisterView(contentId);
    }

    protected void unregisterView(String contentId) {
        XDebugView view = myViews.remove(contentId);
        if (view != null) {
            Disposer.dispose(view);
        }
    }

    public @Nullable Content findOrRestoreContentIfNeeded(@Nonnull String contentId) {
        RunnerContentUi contentUi = myUi instanceof RunnerLayoutUiImpl o ? o.getContentUI() : null;
        if (contentUi != null) {
            return contentUi.findOrRestoreContentIfNeeded(contentId);
        }
        return myUi.findContent(contentId);
    }
}

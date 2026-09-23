/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.build;

import consulo.build.ui.BuildContentManager;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.BuildViewSettingsProvider;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.event.FinishBuildEvent;
import consulo.build.ui.event.StartBuildEvent;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ListBox;
import consulo.ui.image.Image;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analog of {@link MultipleBuildsView} for the frontends which render {@link consulo.ui} components rather than
 * swing - the builds the window is holding beside the view of whichever of them is selected, each build with a
 * view of its own so a build which runs again starts from an empty tree.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
public class UnifiedMultipleBuildsView extends BaseMultipleBuildsView {
    private static final Logger LOG = Logger.getInstance(UnifiedMultipleBuildsView.class);

    private static final String TOOLBAR_PLACE = "BuildView";

    private final BuildContentManager myBuildContentManager;

    private final Map<AbstractViewManager.BuildInfo, UnifiedBuildTreeConsoleView> myViewMap = new ConcurrentHashMap<>();
    private final List<AbstractViewManager.BuildInfo> myBuilds = new ArrayList<>();

    private final DefaultActionGroup myToolbarActions = new DefaultActionGroup();

    private AbstractViewManager.BuildInfo myShownBuild;

    /**
     * The icon the tab carries when no build is running - what is put back once one is over.
     */
    private @Nullable Image myContentIcon;

    private volatile @Nullable Content myContent;
    private volatile boolean myDisposed;

    private ListBox<AbstractViewManager.BuildInfo> myBuildsList;
    private TwoComponentSplitLayout mySplit;
    private ActionToolbar myToolbar;
    private DockLayout myRoot;

    public UnifiedMultipleBuildsView(Project project, BuildContentManager buildContentManager, AbstractViewManager viewManager) {
        super(project, viewManager);
        myBuildContentManager = buildContentManager;
    }

    @Override
    protected Map<BuildDescriptor, ?> getBuildsMap() {
        return Collections.unmodifiableMap(myViewMap);
    }

    @Override
    public @Nullable Content getContent() {
        return myContent;
    }

    @Override
    public void onEvent(Object buildId, BuildEvent event) {
        AbstractViewManager.BuildInfo buildInfo;

        if (event instanceof StartBuildEvent startBuildEvent) {
            buildInfo = new AbstractViewManager.BuildInfo(startBuildEvent.getBuildDescriptor());
        }
        else {
            buildInfo = myBuildsMap.get(buildId);
        }

        if (buildInfo == null) {
            LOG.warn("Build can not be found for buildId: '" + buildId + "'");
            return;
        }

        myProject.getUIAccess().giveAndWaitIfNeed(() -> {
            if (myDisposed || myProject.isDisposed()) {
                return;
            }

            ensureUi();

            if (event instanceof StartBuildEvent startBuildEvent) {
                // what the window is done with goes first, and the build which is starting is registered after
                // it - the other way round the new build is cleared along with the old ones
                clearOldBuilds(startBuildEvent);

                myBuildsMap.put(buildId, buildInfo);
                buildInfo.message = event.getMessage();
                myBuilds.add(buildInfo);
                buildsModel().add(buildInfo);

                UnifiedBuildTreeConsoleView view = createBuildView(buildInfo);
                myViewMap.put(buildInfo, view);

                buildInfo.content = myContent;
                selectContent(buildInfo);

                refreshBuildsList();
                // which build is shown once there is more than one of them is the first of the list, the way
                // the toolkit bound one selects its first row and lets the selection carry the view over
                if (myBuilds.size() > 1) {
                    myBuildsList.setValueByIndex(0);
                }
                else {
                    showBuild(buildInfo);
                }

                ((BuildContentManagerImpl) myBuildContentManager)
                    .startBuildNotified(buildInfo, buildInfo.content, buildInfo.getProcessHandler());

                myViewManager.onBuildStart(buildInfo);
            }

            UnifiedBuildTreeConsoleView view = myViewMap.get(buildInfo);
            if (view != null) {
                view.onEvent(buildId, event);
            }

            if (event instanceof FinishBuildEvent finishBuildEvent) {
                buildInfo.endTime = event.getEventTime();
                buildInfo.message = event.getMessage();
                buildInfo.result = finishBuildEvent.getResult();

                ((BuildContentManagerImpl) myBuildContentManager).finishBuildNotified(buildInfo, buildInfo.content);

                myViewManager.onBuildFinish(buildInfo);
            }
            else {
                buildInfo.statusMessage = event.getMessage();
            }

            refreshBuildsList();
        });
    }

    /**
     * The view of a build is built for that build alone, so a build which runs again is not cleared - what it
     * had is dropped and the new run starts from an empty tree.
     */
    @RequiredUIAccess
    private void clearOldBuilds(StartBuildEvent startBuildEvent) {
        ClearDecision decision = decideClearOldBuilds(startBuildEvent, List.copyOf(myBuilds));

        if (decision.clearAll()) {
            myBuildsMap.clear();

            for (UnifiedBuildTreeConsoleView view : myViewMap.values()) {
                Disposer.dispose(view);
            }
            myViewMap.clear();
            myBuilds.clear();
            buildsModel().removeAll();

            mySplit.setSecondComponent(null);
            myToolbarActions.removeAll();
            return;
        }

        for (AbstractViewManager.BuildInfo info : decision.buildsToRemove()) {
            UnifiedBuildTreeConsoleView view = myViewMap.remove(info);
            if (view != null) {
                Disposer.dispose(view);
            }
            myBuilds.remove(info);
            buildsModel().remove(info);
        }
    }

    /**
     * The tab of the view is what a build which is starting is shown in, so it is the one the window selects -
     * the tool window is left as it is, showing or hidden, and only which of its tabs is on top changes.
     */
    @RequiredUIAccess
    private void selectContent(AbstractViewManager.BuildInfo buildInfo) {
        Content content = myContent;
        if (content == null) {
            return;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(BuildContentManager.TOOL_WINDOW_ID);
        ContentManager contentManager = toolWindow == null ? null : toolWindow.getContentManagerIfCreated();
        if (contentManager == null || contentManager.getIndexOfContent(content) == -1) {
            return;
        }

        contentManager.setSelectedContent(content, buildInfo.isAutoFocusContent(), buildInfo.isAutoFocusContent(), false);
    }

    @RequiredUIAccess
    private UnifiedBuildTreeConsoleView createBuildView(AbstractViewManager.BuildInfo buildInfo) {
        BuildViewSettingsProvider settingsProvider = () -> false;

        UnifiedBuildTreeConsoleView view = new UnifiedBuildTreeConsoleView(myProject, buildInfo, null, settingsProvider);
        Disposer.register(this, view);
        return view;
    }

    @RequiredUIAccess
    private void showBuild(AbstractViewManager.BuildInfo buildInfo) {
        UnifiedBuildTreeConsoleView view = myViewMap.get(buildInfo);
        if (view == null) {
            return;
        }

        myShownBuild = buildInfo;
        layoutShownBuild();

        AnAction[] consoleActions = myViewManager.isBuildContentView()
            ? BuildView.createConsoleActions(buildInfo, BuildView.createStopAction(buildInfo))
            : AnAction.EMPTY_ARRAY;

        myViewManager.configureToolbar(myToolbarActions, this, consoleActions);
        // which nodes the tree shows is the business of the same filters the toolkit bound view offers, and a
        // build whose steps all succeeded shows nothing at all without them
        myToolbarActions.add(BuildTreeFilters.createFilteringActionsGroup(view));

        myToolbar.updateActionsImmediately();
    }

    /**
     * A window holding one build gives the whole of itself to it - the list of builds, and the splitter which
     * would carry it, only appear once there is more than one to choose from.
     */
    @RequiredUIAccess
    private void layoutShownBuild() {
        AbstractViewManager.BuildInfo shown = myShownBuild;
        UnifiedBuildTreeConsoleView view = shown == null ? null : myViewMap.get(shown);
        if (view == null) {
            return;
        }

        if (myBuilds.size() > 1) {
            mySplit.setFirstComponent(myBuildsList);
            mySplit.setProportion(25);
            mySplit.setSecondComponent(view.getUIComponent());
            myRoot.center(mySplit);
        }
        else {
            myRoot.center(view.getUIComponent());
        }
    }

    /**
     * The list of builds is what a window holding several of them is switched with, and a window holding one
     * has no need of it.
     */
    @RequiredUIAccess
    private void refreshBuildsList() {
        for (AbstractViewManager.BuildInfo buildInfo : myBuilds) {
            buildsModel().update(buildInfo);
        }

        layoutShownBuild();
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private MutableFlatDataModel<AbstractViewManager.BuildInfo> buildsModel() {
        return (MutableFlatDataModel<AbstractViewManager.BuildInfo>) myBuildsList.getDataModel();
    }

    @RequiredUIAccess
    private void ensureUi() {
        if (myRoot != null) {
            return;
        }

        myBuildsList = ListBox.create(List.<AbstractViewManager.BuildInfo>of());
        myBuildsList.setRender((presentation, item) -> {
            AbstractViewManager.BuildInfo buildInfo = item.getValue();
            if (buildInfo == null) {
                return;
            }
            presentation.withIcon(buildInfo.getIcon());
            presentation.append(LocalizeValue.of(buildInfo.getTitle().get() + ": "));
            presentation.append(buildInfo.message);
        });
        myBuildsList.addValueListener(event -> {
            AbstractViewManager.BuildInfo selected = event.getValue();
            if (selected != null) {
                showBuild(selected);
            }
        });

        mySplit = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);

        myToolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(TOOLBAR_PLACE, myToolbarActions, ActionToolbar.Style.VERTICAL);

        myRoot = DockLayout.create();
        myRoot.putUserData(UiDataProvider.KEY, sink -> sink.set(Project.KEY, myProject));
        myRoot.left(myToolbar.getUIComponent());

        myToolbar.setTargetUIComponent(myRoot);

        Content content = ContentFactory.getInstance().createUIContent(myRoot, myViewManager.getViewName().get(), true);
        myContent = content;

        myContentIcon = myViewManager.getContentIcon();
        if (myContentIcon != null) {
            content.setIcon(myContentIcon);
            content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        }

        // closing the tab is what ends the view and every build it is holding, the way the toolkit bound one
        // registers it
        Disposer.register(content, () -> Disposer.dispose(UnifiedMultipleBuildsView.this));
        Disposer.register(content, () -> myViewManager.onBuildsViewRemove(this));

        // the manager of build content hands the tool window its tab through a modality this frontend does not
        // answer, so the tab is put where it belongs directly
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(BuildContentManager.TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.setAvailable(true);
            toolWindow.getContentManager().addContent(content);
        }
        else {
            LOG.warn("UnifiedMultipleBuildsView: no build tool window to show the output in");
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }
}

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
package consulo.continuousIntegration.impl.internal.toolWindow;

import consulo.application.util.DateFormatUtil;
import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIBuildStatus;
import consulo.continuousIntegration.CIDataKeys;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIJobKind;
import consulo.continuousIntegration.CIManager;
import consulo.continuousIntegration.CIManagerListener;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.impl.internal.CIUtil;
import consulo.continuousIntegration.impl.internal.action.CIPopupActionGroup;
import consulo.continuousIntegration.impl.internal.action.CISettingsAction;
import consulo.continuousIntegration.impl.internal.action.CIToolbarActionGroup;
import consulo.continuousIntegration.impl.internal.monitor.CIBuildMonitorImpl;
import consulo.continuousIntegration.impl.internal.monitor.CIBuildMonitorListener;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Content of the CI tool window: servers, their folders and jobs, and the last builds of each job. Children are loaded
 * asynchronously when a node is opened; the tree shows a loading node meanwhile and is refreshed when the data arrives.
 * Every map below is only touched on the UI thread.
 *
 * @since 2026-11-09
 */
public final class CIToolWindowPanel implements Disposable, UiDataProvider {
    public static final Key<CIToolWindowPanel> KEY = Key.create("ContinuousIntegration.ToolWindowPanel");

    private static final String PLACE = "ContinuousIntegrationToolWindow";
    private static final int BUILDS_LIMIT = 10;

    private sealed interface LoadState<T> {
        record Loading<T>() implements LoadState<T> {
        }

        record Loaded<T>(List<T> items) implements LoadState<T> {
        }

        record Failed<T>(String message) implements LoadState<T> {
        }
    }

    private final Project myProject;
    private final CIManager myManager;
    private final CIBuildMonitorImpl myMonitor;
    private final UIAccess myUIAccess;

    private final Tree<CITreeNode> myTree;
    private final DockLayout myRoot;
    private final DockLayout myContent;
    private final Component myTreeComponent;
    private final Component myEmptyComponent;

    private final Map<String, LoadState<CIJob>> myJobLists = new HashMap<>();
    private final Map<String, LoadState<CIBuild>> myBuildLists = new HashMap<>();
    private final Map<String, CIJob> myJobs = new HashMap<>();
    private final Map<String, CIBuild> myBuilds = new HashMap<>();
    private List<CIServer> myServers = List.of();
    private int myGeneration;
    private boolean myDisposed;

    @RequiredUIAccess
    public CIToolWindowPanel(Project project, CIManager manager, CIBuildMonitorImpl monitor) {
        myProject = project;
        myManager = manager;
        myMonitor = monitor;
        myUIAccess = UIAccess.current();

        myTree = Tree.create(new Model());
        myTree.setSpeedSearchConverter(node -> {
            CITreeNode value = node.getValue();
            return value == null ? "" : getSpeedSearchText(value);
        });
        myTree.addContextMenuListener(this::showContextMenu);
        Disposer.register(this, myTree.destroyHook());

        myTreeComponent = ScrollableLayout.create(myTree);
        myEmptyComponent = createEmptyComponent();

        myContent = DockLayout.create(Space.NONE);
        myRoot = DockLayout.create(Space.NONE);
        if (ActionManager.getInstance().getAction(CIToolbarActionGroup.ID) instanceof ActionGroup toolbarGroup) {
            ActionToolbar toolbar =
                ActionToolbarFactory.getInstance().createActionToolbar(PLACE, toolbarGroup, ActionToolbar.Style.HORIZONTAL);
            // not the tree: the empty state replaces it, and refresh and settings must work there too
            toolbar.setTargetUIComponent(myContent);
            myRoot.top(toolbar.getUIComponent());
        }
        myRoot.center(myContent);
        myRoot.putUserData(UiDataProvider.KEY, this);

        myManager.addListener(new CIManagerListener() {
            @Override
            public void serversChanged() {
                myUIAccess.give(CIToolWindowPanel.this::refresh);
            }
        }, this);
        myMonitor.addListener(new CIBuildMonitorListener() {
            @Override
            public void buildChanged(CIServer server, CIJob job, CIBuild build) {
                myUIAccess.give(() -> onBuildChanged(server, job, build));
            }
        }, this);

        refresh();
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private Component createEmptyComponent() {
        VerticalLayout layout = VerticalLayout.create(Space.SMALL, HorizontalAlignment.CENTER);
        LabelOptions centered = LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build();
        layout.add(Label.create(ContinuousIntegrationImplLocalize.toolwindowEmptyText(), centered));
        Label hint = Label.create(ContinuousIntegrationImplLocalize.toolwindowEmptyHint(), centered);
        hint.setForegroundColor(ComponentColors.DISABLED_TEXT);
        layout.add(hint);
        layout.add(Hyperlink.create(
            ContinuousIntegrationImplLocalize.toolwindowEmptyConfigure(),
            event -> CISettingsAction.showSettings(myProject)
        ));
        layout.paddingBuilder().allSet(Space.LARGE).apply();
        return layout;
    }

    /**
     * Forgets everything loaded and builds the tree again from the current servers; opened nodes stay open and load
     * their children again.
     */
    @RequiredUIAccess
    public void refresh() {
        if (myDisposed) {
            return;
        }

        myGeneration++;
        myJobLists.clear();
        myBuildLists.clear();
        myJobs.clear();
        myBuilds.clear();
        myServers = myManager.getServers();

        myContent.center(myServers.isEmpty() ? myEmptyComponent : myTreeComponent);
        myTree.refreshAll();
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(KEY, this);

        TreeNode<CITreeNode> node = myTree.getSelectedNode();
        CITreeNode value = node == null ? null : node.getValue();
        switch (value) {
            case CITreeNode.Server server -> sink.set(CIDataKeys.SERVER, server.server());
            case CITreeNode.Job job -> {
                sink.set(CIDataKeys.SERVER, job.server());
                sink.set(CIDataKeys.JOB, latestJob(job.key(), job.job()));
            }
            case CITreeNode.Build build -> {
                sink.set(CIDataKeys.SERVER, build.server());
                sink.set(CIDataKeys.JOB, latestJob(CIUtil.jobKey(build.server(), build.job()), build.job()));
                sink.set(CIDataKeys.BUILD, latestBuild(build.key(), build.build()));
            }
            case CITreeNode.Message ignored -> {
            }
            case null -> {
            }
        }
    }

    private CIJob latestJob(String key, CIJob job) {
        CIJob latest = myJobs.get(key);
        return latest == null ? job : latest;
    }

    private CIBuild latestBuild(String key, CIBuild build) {
        CIBuild latest = myBuilds.get(key);
        return latest == null ? build : latest;
    }

    // region children

    @RequiredUIAccess
    private List<CITreeNode> getChildren(@Nullable CITreeNode parent) {
        return switch (parent) {
            case null -> {
                List<CITreeNode> nodes = new ArrayList<>(myServers.size());
                for (CIServer server : myServers) {
                    nodes.add(new CITreeNode.Server(server, CIUtil.serverKey(server)));
                }
                yield nodes;
            }
            case CITreeNode.Server server -> getJobChildren(server.server(), server.key(), null);
            case CITreeNode.Job job -> job.job().kind().isContainer()
                ? getJobChildren(job.server(), job.key(), job.job())
                : getBuildChildren(job);
            case CITreeNode.Build ignored -> List.of();
            case CITreeNode.Message ignored -> List.of();
        };
    }

    @RequiredUIAccess
    private List<CITreeNode> getJobChildren(CIServer server, String parentKey, @Nullable CIJob folder) {
        LoadState<CIJob> state = myJobLists.get(parentKey);
        if (state == null) {
            state = new LoadState.Loading<>();
            myJobLists.put(parentKey, state);
            loadJobs(server, parentKey, folder);
        }

        return switch (state) {
            case LoadState.Loading<CIJob> ignored -> loading(parentKey);
            case LoadState.Failed<CIJob> failed -> failed(parentKey, failed.message());
            case LoadState.Loaded<CIJob> loaded -> {
                if (loaded.items().isEmpty()) {
                    yield empty(parentKey, ContinuousIntegrationImplLocalize.treeNoJobs());
                }
                List<CITreeNode> nodes = new ArrayList<>(loaded.items().size());
                for (CIJob job : loaded.items()) {
                    nodes.add(new CITreeNode.Job(server, job, CIUtil.jobKey(server, job)));
                }
                yield nodes;
            }
        };
    }

    @RequiredUIAccess
    private List<CITreeNode> getBuildChildren(CITreeNode.Job jobNode) {
        String parentKey = jobNode.key();
        LoadState<CIBuild> state = myBuildLists.get(parentKey);
        if (state == null) {
            state = new LoadState.Loading<>();
            myBuildLists.put(parentKey, state);
            loadBuilds(jobNode.server(), jobNode.job(), parentKey);
        }

        return switch (state) {
            case LoadState.Loading<CIBuild> ignored -> loading(parentKey);
            case LoadState.Failed<CIBuild> failed -> failed(parentKey, failed.message());
            case LoadState.Loaded<CIBuild> loaded -> {
                if (loaded.items().isEmpty()) {
                    yield empty(parentKey, ContinuousIntegrationImplLocalize.treeNoBuilds());
                }
                List<CITreeNode> nodes = new ArrayList<>(loaded.items().size());
                for (CIBuild build : loaded.items()) {
                    nodes.add(new CITreeNode.Build(jobNode.server(), jobNode.job(), build, CIUtil.buildKey(jobNode.server(), build)));
                }
                yield nodes;
            }
        };
    }

    private static List<CITreeNode> loading(String parentKey) {
        return List.of(message(parentKey, CITreeNode.MessageKind.LOADING, ContinuousIntegrationImplLocalize.treeLoading()));
    }

    private static List<CITreeNode> failed(String parentKey, String error) {
        return List.of(message(parentKey, CITreeNode.MessageKind.ERROR, ContinuousIntegrationImplLocalize.treeError(error)));
    }

    private static List<CITreeNode> empty(String parentKey, LocalizeValue text) {
        return List.of(message(parentKey, CITreeNode.MessageKind.EMPTY, text));
    }

    private static CITreeNode message(String parentKey, CITreeNode.MessageKind kind, LocalizeValue text) {
        return new CITreeNode.Message(parentKey + "\n#message", kind, text);
    }

    // endregion

    // region loading

    private void loadJobs(CIServer server, String parentKey, @Nullable CIJob folder) {
        int generation = myGeneration;
        call(() -> server.loadJobs(folder)).whenCompleteAsync((jobs, error) -> {
            if (myDisposed || generation != myGeneration) {
                return;
            }
            if (error != null || jobs == null) {
                myJobLists.put(parentKey, new LoadState.Failed<>(error == null ? "" : CIUtil.errorMessage(error)));
            }
            else {
                myJobLists.put(parentKey, new LoadState.Loaded<>(List.copyOf(jobs)));
                for (CIJob job : jobs) {
                    myJobs.put(CIUtil.jobKey(server, job), job);
                }
            }
            refreshChildren(parentKey);
        }, myUIAccess);
    }

    private void loadBuilds(CIServer server, CIJob job, String parentKey) {
        int generation = myGeneration;
        call(() -> server.loadBuilds(job, BUILDS_LIMIT)).whenCompleteAsync((builds, error) -> {
            if (myDisposed || generation != myGeneration) {
                return;
            }
            if (error != null || builds == null) {
                // a background reload keeps the builds shown before
                if (!(myBuildLists.get(parentKey) instanceof LoadState.Loaded)) {
                    myBuildLists.put(parentKey, new LoadState.Failed<>(error == null ? "" : CIUtil.errorMessage(error)));
                }
            }
            else {
                myBuildLists.put(parentKey, new LoadState.Loaded<>(List.copyOf(builds)));
                for (CIBuild build : builds) {
                    myBuilds.put(CIUtil.buildKey(server, build), build);
                }
            }
            refreshChildren(parentKey);
        }, myUIAccess);
    }

    private void reloadJob(CIServer server, CIJob job, String jobKey) {
        int generation = myGeneration;
        call(() -> server.loadJob(job)).whenCompleteAsync((updated, error) -> {
            if (myDisposed || generation != myGeneration || error != null || updated == null) {
                return;
            }
            myJobs.put(jobKey, updated);
            refreshPresentation(jobKey);
        }, myUIAccess);
    }

    /**
     * A build was started, stopped or changed its status: update its row, reload the builds of its job when they are
     * shown, and reload the job for its status.
     */
    @RequiredUIAccess
    private void onBuildChanged(CIServer server, CIJob job, CIBuild build) {
        if (myDisposed) {
            return;
        }

        String serverKey = CIUtil.serverKey(server);
        CIServer current = null;
        for (CIServer candidate : myServers) {
            if (CIUtil.serverKey(candidate).equals(serverKey)) {
                current = candidate;
                break;
            }
        }
        if (current == null) {
            return;
        }

        String buildKey = CIUtil.buildKey(current, build);
        if (myBuilds.containsKey(buildKey)) {
            myBuilds.put(buildKey, build);
            refreshPresentation(buildKey);
        }

        String jobKey = CIUtil.jobKey(current, job);
        if (myBuildLists.get(jobKey) instanceof LoadState.Loaded) {
            loadBuilds(current, job, jobKey);
        }
        if (myJobs.containsKey(jobKey)) {
            reloadJob(current, job, jobKey);
        }
    }

    @RequiredUIAccess
    private void refreshChildren(String key) {
        TreeNode<CITreeNode> node = findLoadedNode(key);
        if (node != null) {
            myTree.refreshItem(node, true);
        }
    }

    @RequiredUIAccess
    private void refreshPresentation(String key) {
        TreeNode<CITreeNode> node = findLoadedNode(key);
        if (node != null) {
            myTree.refreshItem(node, false);
        }
    }

    /**
     * Looks only at the nodes already built: a node which was never shown has nothing to refresh, and building the
     * rest of the tree to find it would load every job of the server.
     */
    private @Nullable TreeNode<CITreeNode> findLoadedNode(String key) {
        TreeNode<CITreeNode> root = myTree.getRootNode();
        if (root == null) {
            return null;
        }

        Deque<TreeNode<CITreeNode>> queue = new ArrayDeque<>(root.getLoadedChildren());
        while (!queue.isEmpty()) {
            TreeNode<CITreeNode> node = queue.poll();
            CITreeNode value = node.getValue();
            if (value != null && value.key().equals(key)) {
                return node;
            }
            queue.addAll(node.getLoadedChildren());
        }
        return null;
    }

    // endregion

    // region presentation

    private void render(CITreeNode value, TextItemPresentation presentation) {
        switch (value) {
            case CITreeNode.Server server -> {
                presentation.withIcon(server.server().getType().getIcon());
                presentation.append(server.server().getName());
                appendDetails(presentation, server.server().getType().getPresentableName());
            }
            case CITreeNode.Job jobNode -> {
                CIJob job = latestJob(jobNode.key(), jobNode.job());
                presentation.withIcon(CIUtil.jobIcon(job));
                boolean disabled = job.kind() == CIJobKind.JOB && !job.buildable();
                presentation.append(job.displayName(), disabled ? TextAttribute.GRAYED : TextAttribute.REGULAR);
                appendDetails(presentation, getJobDetails(job));
            }
            case CITreeNode.Build buildNode -> {
                CIBuild build = latestBuild(buildNode.key(), buildNode.build());
                presentation.withIcon(CIUtil.statusIcon(build.status()));
                presentation.append(build.displayName(), build.status().isActive() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR);
                appendDetails(presentation, getBuildDetails(build));
                String branch = build.branch();
                if (branch != null && !branch.isEmpty()) {
                    appendDetails(presentation, ContinuousIntegrationImplLocalize.treeBuildBranch(branch));
                }
            }
            case CITreeNode.Message message -> {
                if (message.kind() == CITreeNode.MessageKind.ERROR) {
                    presentation.withIcon(PlatformIconGroup.generalError());
                    presentation.append(message.text(), TextAttribute.ERROR);
                }
                else {
                    presentation.append(message.text(), TextAttribute.GRAYED);
                }
            }
        }
    }

    private static void appendDetails(TextItemPresentation presentation, LocalizeValue details) {
        if (!details.isEmpty()) {
            presentation.append(LocalizeValue.space(), TextAttribute.GRAYED);
            presentation.append(details, TextAttribute.GRAYED);
        }
    }

    private static LocalizeValue getJobDetails(CIJob job) {
        if (job.kind().isContainer()) {
            return LocalizeValue.empty();
        }
        if (!job.buildable()) {
            return ContinuousIntegrationImplLocalize.treeJobDisabled();
        }
        if (job.queued()) {
            return ContinuousIntegrationImplLocalize.treeJobQueued();
        }
        CIBuild lastBuild = job.lastBuild();
        if (lastBuild == null) {
            return LocalizeValue.empty();
        }
        Instant startTime = lastBuild.startTime();
        if (startTime == null) {
            return LocalizeValue.of(lastBuild.displayName());
        }
        return ContinuousIntegrationImplLocalize.treeJobLastBuild(lastBuild.displayName(), formatTime(startTime));
    }

    private static LocalizeValue getBuildDetails(CIBuild build) {
        CIBuildStatus status = build.status();
        Instant startTime = build.startTime();
        if (startTime == null) {
            return status.getPresentableName();
        }
        if (status.isActive()) {
            return ContinuousIntegrationImplLocalize.treeBuildStarted(status.getPresentableName(), formatTime(startTime));
        }
        Duration duration = build.duration();
        if (duration == null) {
            return ContinuousIntegrationImplLocalize.treeBuildFinished(status.getPresentableName(), formatTime(startTime));
        }
        return ContinuousIntegrationImplLocalize.treeBuildFinishedDuration(
            status.getPresentableName(),
            formatTime(startTime),
            DateFormatUtil.formatDuration(duration.toMillis())
        );
    }

    private static String formatTime(Instant time) {
        return DateFormatUtil.formatPrettyDateTime(time.toEpochMilli());
    }

    private String getSpeedSearchText(CITreeNode value) {
        return switch (value) {
            case CITreeNode.Server server -> server.server().getName();
            case CITreeNode.Job job -> latestJob(job.key(), job.job()).displayName();
            case CITreeNode.Build build -> latestBuild(build.key(), build.build()).displayName();
            case CITreeNode.Message ignored -> "";
        };
    }

    // endregion

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        AnAction popupGroup = ActionManager.getInstance().getAction(CIPopupActionGroup.ID);
        if (!(popupGroup instanceof ActionGroup group)) {
            return;
        }

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(PLACE, group);
        menu.setTargetComponent(myTree);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    private static <T> CompletableFuture<T> call(Supplier<CompletableFuture<T>> request) {
        try {
            return request.get();
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private final class Model implements TreeModel<CITreeNode> {
        @Override
        public void buildChildren(Function<CITreeNode, TreeNode<CITreeNode>> nodeFactory, @Nullable CITreeNode parentValue) {
            if (myDisposed) {
                return;
            }
            for (CITreeNode child : getChildren(parentValue)) {
                TreeNode<CITreeNode> node = nodeFactory.apply(child);
                node.setLeaf(child.isLeaf());
                node.setRenderer(CIToolWindowPanel.this::render);
            }
        }

        @Override
        public boolean onDoubleClick(Tree<CITreeNode> tree, TreeNode<CITreeNode> node) {
            if (node.getValue() instanceof CITreeNode.Build build) {
                myMonitor.showBuild(build.server(), build.job(), latestBuild(build.key(), build.build()));
                return false;
            }
            return true;
        }
    }
}

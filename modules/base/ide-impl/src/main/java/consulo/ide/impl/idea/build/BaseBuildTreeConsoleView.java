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

import consulo.application.Application;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.UserHomeFileUtil;
import consulo.build.ui.BuildConsoleView;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.FilePosition;
import consulo.build.ui.event.*;
import consulo.build.ui.impl.internal.event.FailureResultImpl;
import consulo.build.ui.impl.internal.event.FileNavigatable;
import consulo.build.ui.impl.internal.event.SkippedResultImpl;
import consulo.build.ui.localize.BuildLocalize;
import consulo.compiler.internal.CompilerWorkspaceConfiguration;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.util.dataholder.Key;
import consulo.navigation.NonNavigatable;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.util.Invoker;
import consulo.ui.ex.util.InvokerFactory;
import consulo.ui.image.Image;
import consulo.util.collection.SmartHashSet;
import consulo.util.io.FileUtil;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static consulo.util.collection.ContainerUtil.addIfNotNull;
import static consulo.util.lang.StringUtil.isEmpty;

/**
 * What a build does to the tree of its nodes, with nothing of the toolkit drawing it - which node an event
 * makes, what it says, and which node's console it is printed to. What shows the tree and the consoles is the
 * frontend, reached through the hooks below.
 *
 * @author Vladislav.Soroka
 */
public abstract class BaseBuildTreeConsoleView implements ConsoleView, BuildConsoleView, Filterable<ExecutionNodeImpl> {
    protected static final Logger LOG = Logger.getInstance(BaseBuildTreeConsoleView.class);

    protected final Project myProject;
    protected final DefaultBuildDescriptor myBuildDescriptor;
    protected final String myWorkingDir;

    protected final Map<Object, ExecutionNodeImpl> nodesMap = new ConcurrentHashMap<>();
    protected final Set<Predicate<? super ExecutionNodeImpl>> myNodeFilters = ConcurrentHashMap.newKeySet();
    protected final Set<BuildEvent> myDeferredEvents = ConcurrentHashMap.newKeySet();

    protected final AtomicBoolean myFinishedBuildEventReceived = new AtomicBoolean();
    protected final AtomicBoolean myDisposed = new AtomicBoolean();
    protected final AtomicBoolean myShownFirstError = new AtomicBoolean();
    protected final AtomicBoolean myExpandedFirstMessage = new AtomicBoolean();

    protected final boolean myNavigateToTheFirstErrorLocation;

    protected final ExecutionNodeImpl myRootNode;
    protected final ExecutionNodeImpl myBuildProgressRootNode;

    /**
     * The nodes are built and changed on one thread of its own, never on the one the build reports from and
     * never on the ui thread - the tree is read from it as well, so every walk of it is free of locking.
     */
    protected final Invoker myInvoker;

    protected BaseBuildTreeConsoleView(Project project, BuildDescriptor buildDescriptor) {
        myProject = project;
        myBuildDescriptor = buildDescriptor instanceof DefaultBuildDescriptor defaultBuildDescriptor
            ? defaultBuildDescriptor : new DefaultBuildDescriptor(buildDescriptor);
        myWorkingDir = FileUtil.toSystemIndependentName(buildDescriptor.getWorkingDir());
        myNavigateToTheFirstErrorLocation = project.getInstance(CompilerWorkspaceConfiguration.class).isAutoShowErrorsInEditor();

        myRootNode = new ExecutionNodeImpl(myProject, null, true, this::isCorrectThread);
        myBuildProgressRootNode = new ExecutionNodeImpl(myProject, myRootNode, true, this::isCorrectThread);
        myRootNode.setFilter(getFilter());
        myRootNode.add(myBuildProgressRootNode);

        // the nodes above are built by whoever is constructing the view, and only what happens after this is
        // held to the thread of the model - the same order the toolkit bound view has always had
        myInvoker = InvokerFactory.getInstance().forBackgroundThreadWithoutReadAction(this);
    }

    // region what the frontend answers for

    /**
     * Tells the tree that a node changed, and with {@code parentStructureChanged} that its parent gained or
     * lost children - which is what makes a node stop being a leaf.
     */
    protected abstract void scheduleUpdate(ExecutionNodeImpl node, boolean parentStructureChanged);

    /**
     * Rebuilds the tree from the root and runs the task afterwards.
     */
    protected abstract void invalidateAll(boolean structure, @Nullable Runnable after);

    protected abstract void makeNodeVisible(ExecutionNodeImpl node);

    protected abstract void selectNode(ExecutionNodeImpl node);

    protected abstract void installContextMenu();

    protected abstract BuildNodeConsoleHandler getConsoleHandler();

    // endregion

    protected boolean isCorrectThread() {
        return myInvoker == null || myInvoker.isValidThread();
    }

    protected void invokeOnModelThread(Runnable task) {
        myInvoker.invoke(task);
    }

    protected void runOrInvokeLaterOnModelThread(Runnable task) {
        myInvoker.runOrInvokeLater(task);
    }

    @Override
    public boolean isFilteringEnabled() {
        return true;
    }

    @Override
    public Predicate<ExecutionNodeImpl> getFilter() {
        return executionNode -> executionNode == getBuildProgressRootNode()
            || executionNode.isRunning()
            || executionNode.isFailed()
            || myNodeFilters.stream().anyMatch(predicate -> predicate.test(executionNode));
    }

    @Override
    public void addFilter(Predicate<? super ExecutionNodeImpl> executionTreeFilter) {
        myNodeFilters.add(executionTreeFilter);
        updateFilter();
    }

    @Override
    public void removeFilter(Predicate<? super ExecutionNodeImpl> filter) {
        myNodeFilters.remove(filter);
        updateFilter();
    }

    @Override
    public boolean contains(Predicate<? super ExecutionNodeImpl> filter) {
        return myNodeFilters.contains(filter);
    }

    protected void updateFilter() {
        ExecutionNodeImpl rootElement = getRootElement();
        runOrInvokeLaterOnModelThread(() -> {
            rootElement.setFilter(getFilter());
            scheduleUpdate(rootElement, true);
        });
    }

    protected ExecutionNodeImpl getRootElement() {
        return myRootNode;
    }

    protected ExecutionNodeImpl getBuildProgressRootNode() {
        return myBuildProgressRootNode;
    }

    public boolean isDisposed() {
        return myDisposed.get();
    }

    @Override
    public void onEvent(Object buildId, BuildEvent event) {
        invokeOnModelThread(() -> onEventInternal(buildId, event));
    }

    private @Nullable ExecutionNodeImpl getOrMaybeCreateParentNode(BuildEvent event) {
        ExecutionNodeImpl parentNode = event.getParentId() == null ? null : nodesMap.get(event.getParentId());
        if (event instanceof MessageEvent messageEvent) {
            parentNode = createMessageParentNodes(messageEvent, parentNode);
            if (parentNode != null) {
                scheduleUpdate(parentNode, true); // To update its parent.
            }
        }
        return parentNode;
    }

    protected void onEventInternal(Object buildId, BuildEvent event) {
        Set<ExecutionNodeImpl> structureChanged = new SmartHashSet<>();
        ExecutionNodeImpl parentNode = getOrMaybeCreateParentNode(event);
        Object eventId = event.getId();
        ExecutionNodeImpl currentNode = nodesMap.get(eventId);
        ExecutionNodeImpl buildProgressRootNode = getBuildProgressRootNode();
        Runnable selectErrorNodeTask = null;
        boolean isMessageEvent = event instanceof MessageEvent;
        BuildNodeConsoleHandler consoleHandler = getConsoleHandler();

        if (event instanceof StartEvent || isMessageEvent) {
            if (currentNode == null) {
                if (event instanceof DuplicateMessageAware) {
                    if (myFinishedBuildEventReceived.get()) {
                        if (parentNode != null && parentNode.findFirstChild(node -> event.getMessage().get().equals(node.getName())) != null) {
                            return;
                        }
                    }
                    else {
                        myDeferredEvents.add(event);
                        return;
                    }
                }
                if (event instanceof StartBuildEvent) {
                    currentNode = buildProgressRootNode;
                    installContextMenu();
                    currentNode.setTitle(myBuildDescriptor.getTitle());
                }
                else {
                    currentNode = new ExecutionNodeImpl(myProject, parentNode, false, this::isCorrectThread);

                    if (isMessageEvent) {
                        currentNode.setAlwaysLeaf(event instanceof FileMessageEvent);
                        MessageEvent messageEvent = (MessageEvent) event;
                        currentNode.setStartTime(messageEvent.getEventTime());
                        addIfNotNull(structureChanged, currentNode.setEndTime(messageEvent.getEventTime()));
                        Navigatable messageEventNavigatable = messageEvent.getNavigatable(myProject);
                        currentNode.setNavigatable(messageEventNavigatable);
                        MessageEventResult messageEventResult = messageEvent.getResult();
                        addIfNotNull(structureChanged, currentNode.setResult(messageEventResult));

                        if (messageEventResult instanceof FailureResult failureResult) {
                            for (Failure failure : failureResult.getFailures()) {
                                selectErrorNodeTask = selectErrorNodeTask != null
                                    ? selectErrorNodeTask
                                    : showErrorIfFirst(currentNode, failure.getNavigatable());
                            }
                        }
                        if (messageEvent.getKind() == MessageEvent.Kind.ERROR) {
                            selectErrorNodeTask = selectErrorNodeTask != null
                                ? selectErrorNodeTask
                                : showErrorIfFirst(currentNode, messageEventNavigatable);
                        }

                        if (parentNode != null) {
                            if (parentNode != buildProgressRootNode) {
                                consoleHandler.addOutput(parentNode, buildId, event);
                                consoleHandler.addOutput(parentNode, "\n", true);
                            }
                            reportMessageKind(messageEvent.getKind(), parentNode);
                        }
                        consoleHandler.addOutput(currentNode, buildId, event);
                    }
                    if (parentNode != null) {
                        structureChanged.add(parentNode);
                        parentNode.add(currentNode);
                    }
                }
                nodesMap.put(eventId, currentNode);
            }
            else {
                LOG.warn("start event id collision found:" + eventId + ", was also in node: " + currentNode.getTitle());
                return;
            }
        }
        else {
            boolean isProgress = event instanceof ProgressBuildEvent;
            currentNode = nodesMap.get(eventId);
            if (currentNode == null) {
                if (isProgress) {
                    currentNode = new ExecutionNodeImpl(
                        myProject,
                        parentNode,
                        parentNode == buildProgressRootNode,
                        this::isCorrectThread
                    );
                    nodesMap.put(eventId, currentNode);
                    if (parentNode != null) {
                        structureChanged.add(parentNode);
                        parentNode.add(currentNode);
                    }
                }
                else if (event instanceof OutputBuildEvent && parentNode != null) {
                    consoleHandler.addOutput(parentNode, buildId, event);
                }
                else if (event instanceof PresentableBuildEvent presentableBuildEvent) {
                    currentNode = addAsPresentableEventNode(
                        presentableBuildEvent,
                        structureChanged,
                        parentNode,
                        eventId,
                        buildProgressRootNode
                    );
                }
            }

            if (isProgress) {
                ProgressBuildEvent progressBuildEvent = (ProgressBuildEvent) event;
                long total = progressBuildEvent.getTotal();
                long progress = progressBuildEvent.getProgress();
                if (currentNode == myBuildProgressRootNode) {
                    consoleHandler.updateProgressBar(total, progress);
                }
            }
        }

        if (currentNode == null) {
            return;
        }

        currentNode.setName(event.getMessage());
        currentNode.setHint(event.getHint());
        if (currentNode.getStartTime() == 0) {
            currentNode.setStartTime(event.getEventTime());
        }

        if (event instanceof FinishEvent finishEvent) {
            EventResult result = finishEvent.getResult();
            if (result instanceof DerivedResult derivedResult) {
                result = calculateDerivedResult(derivedResult, currentNode);
            }
            addIfNotNull(structureChanged, currentNode.setResult(result));
            addIfNotNull(structureChanged, currentNode.setEndTime(event.getEventTime()));
            SkippedResult skippedResult = new SkippedResultImpl();
            finishChildren(structureChanged, currentNode, skippedResult);
            if (result instanceof FailureResult failureResult) {
                for (Failure failure : failureResult.getFailures()) {
                    Runnable task = addChildFailureNode(
                        currentNode,
                        failure,
                        event.getMessage(),
                        event.getEventTime(),
                        structureChanged
                    );
                    if (selectErrorNodeTask == null) {
                        selectErrorNodeTask = task;
                    }
                }
            }
        }

        if (event instanceof FinishBuildEvent) {
            myFinishedBuildEventReceived.set(true);
            LocalizeValue aHint = event.getHint();
            String time = DateFormatUtil.formatDateTime(event.getEventTime());
            aHint = aHint.isEmpty() ? BuildLocalize.buildEventMessageAt(time) : BuildLocalize.buildEventMessage0At1(aHint, time);
            currentNode.setHint(aHint);
            myDeferredEvents.forEach(buildEvent -> onEventInternal(buildId, buildEvent));
            if (!consoleHandler.hasNode()) {
                invokeOnModelThread(() -> consoleHandler.setNode(buildProgressRootNode));
            }
            consoleHandler.stopProgressBar();
        }

        if (structureChanged.isEmpty()) {
            scheduleUpdate(currentNode, false);
        }
        else {
            for (ExecutionNodeImpl node : structureChanged) {
                scheduleUpdate(node, true);
            }
        }

        if (selectErrorNodeTask != null) {
            myExpandedFirstMessage.set(true);
            Runnable finalSelectErrorTask = selectErrorNodeTask;
            invalidateAll(true, finalSelectErrorTask);
        }
        else if (isMessageEvent && myExpandedFirstMessage.compareAndSet(false, true)) {
            ExecutionNodeImpl finalCurrentNode = currentNode;
            invalidateAll(false, () -> makeNodeVisible(finalCurrentNode));
        }
    }

    private ExecutionNodeImpl addAsPresentableEventNode(
        PresentableBuildEvent event,
        Set<ExecutionNodeImpl> structureChanged,
        @Nullable ExecutionNodeImpl parentNode,
        Object eventId,
        ExecutionNodeImpl buildProgressRootNode
    ) {
        ExecutionNodeImpl executionNode = new ExecutionNodeImpl(
            myProject,
            parentNode,
            parentNode == buildProgressRootNode,
            this::isCorrectThread
        );
        BuildEventPresentationData presentationData = event.getPresentationData();
        executionNode.applyFrom(presentationData);
        nodesMap.put(eventId, executionNode);
        if (parentNode != null) {
            structureChanged.add(parentNode);
            parentNode.add(executionNode);
        }
        getConsoleHandler().maybeAddExecutionConsole(executionNode, presentationData);
        return executionNode;
    }

    private static EventResult calculateDerivedResult(DerivedResult result, ExecutionNodeImpl node) {
        if (node.getResult() != null) {
            return node.getResult(); // if another thread set result for child
        }
        if (node.isFailed()) {
            return result.createFailureResult();
        }

        return result.createDefaultResult();
    }

    protected void reportMessageKind(MessageEvent.Kind eventKind, ExecutionNodeImpl parentNode) {
        if (eventKind == MessageEvent.Kind.ERROR
            || eventKind == MessageEvent.Kind.WARNING
            || eventKind == MessageEvent.Kind.INFO) {
            ExecutionNodeImpl executionNode = parentNode;
            do {
                ExecutionNodeImpl updatedRoot = executionNode.reportChildMessageKind(eventKind);
                if (updatedRoot != null) {
                    scheduleUpdate(updatedRoot, true);
                }
                else {
                    scheduleUpdate(executionNode, false);
                }
            }
            while ((executionNode = executionNode.getParent()) != null);
            scheduleUpdate(getRootElement(), false);
        }
    }

    private @Nullable Runnable showErrorIfFirst(ExecutionNodeImpl node, @Nullable Navigatable navigatable) {
        if (myShownFirstError.compareAndSet(false, true)) {
            return () -> {
                selectNode(node);
                if (myNavigateToTheFirstErrorLocation && navigatable != null && navigatable != NonNavigatable.INSTANCE) {
                    Application app = Application.get();
                    app.invokeLater(
                        () -> navigatable.navigate(true),
                        app.getDefaultModalityState(),
                        myProject.getDisposed()
                    );
                }
            };
        }
        return null;
    }

    private @Nullable Runnable addChildFailureNode(
        ExecutionNodeImpl parentNode,
        Failure failure,
        LocalizeValue defaultFailureMessage,
        long eventTime,
        Set<ExecutionNodeImpl> structureChanged
    ) {
        LocalizeValue message = failure.getMessage().orIfEmpty(failure.getDescription());
        if (message.isEmpty()) {
            Throwable error = failure.getError();
            message = error != null ? LocalizeValue.of(error) : defaultFailureMessage;
        }
        LocalizeValue failureNodeName = message.map(BuildConsoleUtils::getMessageTitle);
        Navigatable failureNavigatable = failure.getNavigatable();
        FilePosition filePosition = null;
        if (failureNavigatable instanceof OpenFileDescriptorImpl fileDescriptor) {
            File file = VirtualFileUtil.virtualToIoFile(fileDescriptor.getFile());
            filePosition = new FilePosition(file, fileDescriptor.getLine(), fileDescriptor.getColumn());
            parentNode = createMessageParentNodes(eventTime, filePosition, failureNavigatable, parentNode);
        }
        else if (failureNavigatable instanceof FileNavigatable fileNavigatable) {
            filePosition = fileNavigatable.getFilePosition();
            parentNode = createMessageParentNodes(eventTime, filePosition, failureNavigatable, parentNode);
        }

        ExecutionNodeImpl failureNode = parentNode.findFirstChild(executionNode -> failureNodeName.equals(executionNode.getName()));
        if (failureNode == null) {
            failureNode = new ExecutionNodeImpl(myProject, parentNode, true, this::isCorrectThread);
            failureNode.setName(failureNodeName);
            if (filePosition != null && filePosition.startLine() >= 0) {
                String hint = ":" + (filePosition.startLine() + 1);
                failureNode.setHint(hint);
            }
            parentNode.add(failureNode);
            reportMessageKind(MessageEvent.Kind.ERROR, parentNode);
        }
        if (failureNavigatable != null && failureNavigatable != NonNavigatable.INSTANCE) {
            failureNode.setNavigatable(failureNavigatable);
        }

        List<Failure> failures;
        EventResult result = failureNode.getResult();
        if (result instanceof FailureResult failureResult) {
            failures = new ArrayList<>(failureResult.getFailures());
            failures.add(failure);
        }
        else {
            failures = Collections.singletonList(failure);
        }
        ExecutionNodeImpl updatedRoot = failureNode.setResult(new FailureResultImpl(failures));
        if (updatedRoot == null) {
            updatedRoot = parentNode;
        }
        structureChanged.add(updatedRoot);
        getConsoleHandler().addOutput(failureNode, failure);
        return showErrorIfFirst(failureNode, failureNavigatable);
    }

    private static void finishChildren(
        Set<ExecutionNodeImpl> structureChanged,
        ExecutionNodeImpl node,
        EventResult result
    ) {
        List<ExecutionNodeImpl> childList = node.getChildList();
        if (childList.isEmpty()) {
            return;
        }
        // Make a copy of the list since child.setResult may remove items from the collection.
        for (ExecutionNodeImpl child : new ArrayList<>(childList)) {
            if (!child.isRunning()) {
                continue;
            }
            finishChildren(structureChanged, child, result);
            addIfNotNull(structureChanged, child.setResult(result));
        }
    }

    private @Nullable ExecutionNodeImpl createMessageParentNodes(MessageEvent messageEvent, ExecutionNodeImpl parentNode) {
        Object messageEventParentId = messageEvent.getParentId();
        if (messageEventParentId == null) {
            return null;
        }
        if (messageEvent instanceof FileMessageEvent fileMessageEvent) {
            return createMessageParentNodes(
                messageEvent.getEventTime(),
                fileMessageEvent.getFilePosition(),
                messageEvent.getNavigatable(myProject),
                parentNode
            );
        }
        else {
            return parentNode;
        }
    }

    private ExecutionNodeImpl createMessageParentNodes(
        long eventTime,
        FilePosition filePosition,
        @Nullable Navigatable navigatable,
        ExecutionNodeImpl parentNode
    ) {
        String filePath = FileUtil.toSystemIndependentName(filePosition.getFile().getPath());
        String parentsPath = "";

        String relativePath = FileUtil.getRelativePath(myWorkingDir, filePath, '/');
        if (relativePath != null) {
            if (relativePath.equals(".")) {
                return parentNode;
            }
            if (!relativePath.startsWith("../../")) {
                parentsPath = myWorkingDir;
            }
        }

        if (isEmpty(parentsPath)) {
            File userHomeDir = Platform.current().user().homePath().toFile();
            if (FileUtil.isAncestor(userHomeDir, new File(filePath), true)) {
                relativePath = UserHomeFileUtil.getLocationRelativeToUserHome(filePath, false);
            }
            else {
                relativePath = filePath;
            }
        }
        else {
            relativePath = getRelativePath(parentsPath, filePath);
        }
        Path path = Paths.get(relativePath);
        String nodeName = path.getFileName().toString();
        Path pathParent = path.getParent();
        String pathHint = pathParent == null ? null : pathParent.toString();
        parentNode = getOrCreateMessagesNode(
            eventTime,
            filePath,
            parentNode,
            nodeName,
            pathHint,
            () -> {
                VirtualFile file = VirtualFileUtil.findFileByIoFile(filePosition.getFile(), false);
                if (file != null) {
                    return file.getFileType().getIcon();
                }
                return null;
            },
            navigatable,
            nodesMap,
            myProject
        );
        return parentNode;
    }

    private static String getRelativePath(String basePath, String filePath) {
        String path = ObjectUtil.notNull(FileUtil.getRelativePath(basePath, filePath, '/'), filePath);
        File userHomeDir = Platform.current().user().homePath().toFile();
        if (path.startsWith("..") && FileUtil.isAncestor(userHomeDir, new File(filePath), true)) {
            return UserHomeFileUtil.getLocationRelativeToUserHome(filePath, false);
        }
        return path;
    }

    private ExecutionNodeImpl getOrCreateMessagesNode(
        long eventTime,
        String nodeId,
        ExecutionNodeImpl parentNode,
        String nodeName,
        @Nullable String hint,
        @Nullable Supplier<? extends Image> iconProvider,
        @Nullable Navigatable navigatable,
        Map<Object, ExecutionNodeImpl> nodesMap,
        Project project
    ) {
        ExecutionNodeImpl node = nodesMap.get(nodeId);
        if (node == null) {
            node = new ExecutionNodeImpl(project, parentNode, false, this::isCorrectThread);
            node.setName(nodeName);
            if (hint != null) {
                node.setHint(hint);
            }
            node.setStartTime(eventTime);
            node.setEndTime(eventTime);
            if (iconProvider != null) {
                node.setIconProvider(iconProvider);
            }
            if (navigatable != null) {
                node.setNavigatable(navigatable);
            }
            parentNode.add(node);
            nodesMap.put(nodeId, node);
        }
        return node;
    }

    // region what a console view is asked for and a tree of nodes has no answer to

    @Override
    public void print(String text, ConsoleViewContentType contentType) {
    }

    @Override
    public void scrollTo(int offset) {
    }

    @Override
    public void attachToProcess(ProcessHandler processHandler) {
    }

    @Override
    public boolean isOutputPaused() {
        return false;
    }

    @Override
    public void setOutputPaused(boolean value) {
    }

    @Override
    public boolean hasDeferredOutput() {
        return false;
    }

    @Override
    public void performWhenNoDeferredOutput(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void setHelpId(String helpId) {
    }

    @Override
    public void addMessageFilter(Filter filter) {
    }

    @Override
    public void printHyperlink(String hyperlinkText, @Nullable HyperlinkInfo info) {
    }

    @Override
    public int getContentSize() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public void allowHeavyFilters() {
    }

    @Override
    public void setProcessTextFilter(@Nullable BiPredicate<ProcessEvent, Key> filter) {
    }

    @Override
    public @Nullable BiPredicate<ProcessEvent, Key> getProcessTextFilter() {
        return null;
    }

    @Override
    public void dispose() {
        myDisposed.set(true);
    }

    // endregion
}

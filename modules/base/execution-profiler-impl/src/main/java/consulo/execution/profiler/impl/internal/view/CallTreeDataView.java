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
package consulo.execution.profiler.impl.internal.view;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.profiler.NewCallTreeOnlyProfilerData;
import consulo.execution.profiler.impl.internal.calltree.CallTree;
import consulo.execution.profiler.impl.internal.calltree.CallTreeData;
import consulo.execution.profiler.impl.internal.calltree.CallTreeFlameGraphModel;
import consulo.execution.profiler.impl.internal.calltree.CallTreeNode;
import consulo.execution.profiler.ui.BaseCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.TabbedLayout;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTreeDataView implements Disposable {
    private final NewCallTreeOnlyProfilerData myData;
    private final ProfilerNavigator myNavigator;
    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;
    private final List<ProfilerViewTab> myExtraTabs;
    private final DockLayout myRoot;
    private final DockLayout myFlameGraphHolder = DockLayout.create(Space.NONE);
    private final DockLayout myCallTreeHolder = DockLayout.create(Space.NONE);
    private final DockLayout myBottomUpHolder = DockLayout.create(Space.NONE);
    private final DockLayout myMethodsHolder = DockLayout.create(Space.NONE);

    private @Nullable Disposable myTreeDisposable;
    private @Nullable CallTree myShownTree;

    @RequiredUIAccess
    public CallTreeDataView(
        NewCallTreeOnlyProfilerData data,
        ProfilerNavigator navigator,
        ApplicationTreeExecutorFactory treeExecutorFactory,
        List<ProfilerViewTab> extraTabs,
        Disposable parentDisposable
    ) {
        myData = data;
        myNavigator = navigator;
        myTreeExecutorFactory = treeExecutorFactory;
        myExtraTabs = List.copyOf(extraTabs);

        Disposer.register(parentDisposable, this);

        ProgressIndicator indicator = new EmptyProgressIndicator();
        Disposer.register(this, indicator::cancel);

        DockLayout content = DockLayout.create(Space.NONE);
        LoadingLayout<DockLayout> loading = LoadingLayout.create(content, this);
        loading.setLoadingText(LocalizeValue.localizeTODO("Building the call tree…"));

        myRoot = DockLayout.create(Space.NONE);
        myRoot.center(loading);

        loading.startLoading(() -> CallTreeData.build(data.getBuilder(), indicator), (inner, callTreeData) -> {
            if (!Disposer.isDisposed(this)) {
                showData(inner, callTreeData);
            }
        });
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private void showData(DockLayout content, CallTreeData data) {
        TabbedLayout tabs = TabbedLayout.create();
        Tab firstTab = ProfilerUIUtil.addTab(tabs, LocalizeValue.localizeTODO("Flame Graph"), myFlameGraphHolder);
        ProfilerUIUtil.addTab(tabs, LocalizeValue.localizeTODO("Call Tree"), myCallTreeHolder);
        ProfilerUIUtil.addTab(tabs, LocalizeValue.localizeTODO("Bottom-Up"), myBottomUpHolder);
        ProfilerUIUtil.addTab(tabs, LocalizeValue.localizeTODO("Method List"), myMethodsHolder);
        for (ProfilerViewTab extraTab : myExtraTabs) {
            ProfilerUIUtil.addTab(tabs, extraTab.name(), extraTab.component());
        }

        if (!data.getThreads().isEmpty()) {
            ComboBox<CallTree> threadBox = ComboBox.create(data.getTrees());
            threadBox.setTextRenderer(tree -> tree == null ? LocalizeValue.empty() : CallTreeFlameGraphModel.getTreeName(tree));
            threadBox.setValue(data.getAllThreads(), false);
            threadBox.addValueListener(event -> {
                CallTree tree = event.getValue();
                if (tree != null) {
                    showTree(tree);
                }
            });

            Label threadLabel = Label.create(LocalizeValue.localizeTODO("Thread:"));
            threadLabel.setTarget(threadBox);

            HorizontalLayout threadRow = HorizontalLayout.create(Space.SMALL);
            threadRow.add(threadLabel);
            threadRow.add(threadBox);
            content.top(threadRow);
        }

        content.center(tabs);
        firstTab.select();

        showTree(data.getAllThreads());
    }

    @RequiredUIAccess
    private void showTree(CallTree tree) {
        if (tree == myShownTree) {
            return;
        }
        myShownTree = tree;

        Disposable previous = myTreeDisposable;
        if (previous != null) {
            myTreeDisposable = null;
            Disposer.dispose(previous);
        }

        Disposable treeDisposable = Disposable.newDisposable("ProfilerCallTreeViews");
        Disposer.register(this, treeDisposable);
        myTreeDisposable = treeDisposable;

        TreeExecutor executor = myTreeExecutorFactory.forBackgroundThreadWithoutReadAction(treeDisposable);
        BaseCallStackElementRenderer renderer = myData.getRenderer();
        long total = tree.getTotal();

        ProfilerFlameGraphView flameGraph = new ProfilerFlameGraphView(tree, renderer, myNavigator);
        replace(myFlameGraphHolder, flameGraph.getComponent());

        TreeTable<CallTreeNode> callTree =
            CallTreeTables.create(tree.getTopDown(), total, renderer, myNavigator, executor, treeDisposable);
        callTree.expandAll(2);
        replace(myCallTreeHolder, ScrollableLayout.create(callTree));

        TreeTable<CallTreeNode> bottomUp =
            CallTreeTables.create(tree.getBottomUp(), total, renderer, myNavigator, executor, treeDisposable);
        replace(myBottomUpHolder, ScrollableLayout.create(bottomUp));

        CallTreeMethodsView methods = new CallTreeMethodsView(tree, renderer, myNavigator, executor, treeDisposable);
        replace(myMethodsHolder, methods.getComponent());
    }

    @RequiredUIAccess
    private static void replace(DockLayout holder, Component component) {
        holder.removeAll();
        holder.center(component);
    }

    @Override
    public void dispose() {
        myTreeDisposable = null;
        myShownTree = null;
    }
}

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
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.impl.internal.calltree.CallTree;
import consulo.execution.profiler.impl.internal.calltree.CallTreeMethod;
import consulo.execution.profiler.impl.internal.calltree.CallTreeNode;
import consulo.execution.profiler.ui.BaseCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Space;
import consulo.ui.Table;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeTable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTreeMethodsView {
    private final CallTree myTree;
    private final BaseCallStackElementRenderer myRenderer;
    private final ProfilerNavigator myNavigator;
    private final TreeExecutor myExecutor;
    private final Disposable myParentDisposable;
    private final DockLayout myBackTracesHolder;
    private final DockLayout myMergedCalleesHolder;
    private final TwoComponentSplitLayout myRoot;

    private @Nullable Disposable mySelectionDisposable;
    private @Nullable BaseCallStackElement mySelectedElement;

    @RequiredUIAccess
    public CallTreeMethodsView(
        CallTree tree,
        BaseCallStackElementRenderer renderer,
        ProfilerNavigator navigator,
        TreeExecutor executor,
        Disposable parentDisposable
    ) {
        myTree = tree;
        myRenderer = renderer;
        myNavigator = navigator;
        myExecutor = executor;
        myParentDisposable = parentDisposable;

        long total = tree.getTotal();

        Table<CallTreeMethod> table = Table.create(FlatDataModel.of(tree.getMethods()));
        table.addColumn(LocalizeValue.localizeTODO("Method"), CallTreeMethod::element)
            .setRender((presentation, item) -> {
                BaseCallStackElement element = item.getValue();
                if (element != null) {
                    renderer.render(element, presentation);
                }
            });
        table.addColumn(LocalizeValue.localizeTODO("Self %"), method -> ProfilerFormat.percent(method.self(), total))
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatPercent(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        table.addColumn(LocalizeValue.localizeTODO("Self"), CallTreeMethod::self)
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatCount(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        table.addColumn(LocalizeValue.localizeTODO("Total %"), method -> ProfilerFormat.percent(method.total(), total))
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatPercent(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        table.addColumn(LocalizeValue.localizeTODO("Total"), CallTreeMethod::total)
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatCount(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        table.addColumn(LocalizeValue.localizeTODO("Samples"), CallTreeMethod::count)
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatCount(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());

        table.setSpeedSearchConverter(method -> renderer.getText(method.element()));
        table.addSelectListener(event -> showDetails(event.getValue()));
        table.addDoubleClickListener(event -> {
            CallTreeMethod method = event.getValue();
            if (method != null) {
                navigator.navigate(method.element());
            }
        });

        myBackTracesHolder = DockLayout.create(Space.NONE);
        myMergedCalleesHolder = DockLayout.create(Space.NONE);

        TabbedLayout details = TabbedLayout.create();
        ProfilerUIUtil.addTab(details, LocalizeValue.localizeTODO("Back Traces"), myBackTracesHolder);
        ProfilerUIUtil.addTab(details, LocalizeValue.localizeTODO("Merged Callees"), myMergedCalleesHolder);

        myRoot = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        myRoot.setFirstComponent(ScrollableLayout.create(table));
        myRoot.setSecondComponent(details);
        myRoot.setProportion(60);

        showDetails(null);
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private void showDetails(@Nullable CallTreeMethod method) {
        BaseCallStackElement element = method == null ? null : method.element();
        if (element != null && element.equals(mySelectedElement)) {
            return;
        }
        mySelectedElement = element;

        Disposable previous = mySelectionDisposable;
        if (previous != null) {
            mySelectionDisposable = null;
            Disposer.dispose(previous);
        }

        myBackTracesHolder.removeAll();
        myMergedCalleesHolder.removeAll();

        if (element == null) {
            myBackTracesHolder.center(ProfilerUIUtil.hint(LocalizeValue.localizeTODO("Select a method to see where it is called from")));
            myMergedCalleesHolder.center(ProfilerUIUtil.hint(LocalizeValue.localizeTODO("Select a method to see what it calls")));
            return;
        }

        Disposable selectionDisposable = Disposable.newDisposable("ProfilerMethodDetails");
        Disposer.register(myParentDisposable, selectionDisposable);
        mySelectionDisposable = selectionDisposable;

        long total = myTree.getTotal();

        CallTreeNode backTraces = myTree.getBackTraces(element);
        if (backTraces == null || backTraces.getChildren().isEmpty()) {
            myBackTracesHolder.center(ProfilerUIUtil.hint(LocalizeValue.localizeTODO("The method is a root of every stack it is in")));
        }
        else {
            TreeTable<CallTreeNode> callers =
                CallTreeTables.create(backTraces, total, myRenderer, myNavigator, myExecutor, selectionDisposable);
            callers.expandAll(1);
            myBackTracesHolder.center(ScrollableLayout.create(callers));
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        Disposer.register(selectionDisposable, indicator::cancel);

        DockLayout calleesContent = DockLayout.create(Space.NONE);
        LoadingLayout<DockLayout> loading = LoadingLayout.create(calleesContent, selectionDisposable);
        myMergedCalleesHolder.center(loading);

        loading.startLoading(() -> myTree.buildMergedCallees(element, indicator), (inner, merged) -> {
            if (Disposer.isDisposed(selectionDisposable)) {
                return;
            }

            if (merged.getChildren().isEmpty()) {
                inner.center(ProfilerUIUtil.hint(LocalizeValue.localizeTODO("The method calls nothing that was sampled")));
                return;
            }

            TreeTable<CallTreeNode> callees =
                CallTreeTables.create(merged, total, myRenderer, myNavigator, myExecutor, selectionDisposable);
            callees.expandAll(1);
            inner.center(ScrollableLayout.create(callees));
        });
    }
}

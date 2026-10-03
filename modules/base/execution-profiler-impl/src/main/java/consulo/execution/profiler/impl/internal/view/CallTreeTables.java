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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.impl.internal.calltree.CallTreeNode;
import consulo.execution.profiler.ui.BaseCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.HorizontalAlignment;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeNode;
import consulo.ui.TreeTable;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTreeTables {
    private CallTreeTables() {
    }

    @RequiredUIAccess
    public static TreeTable<CallTreeNode> create(
        CallTreeNode root,
        long total,
        BaseCallStackElementRenderer renderer,
        ProfilerNavigator navigator,
        TreeExecutor executor,
        Disposable parentDisposable
    ) {
        TreeTable<CallTreeNode> table = TreeTable.create(root, new CallTreeNodeModel(renderer, navigator), executor);
        Disposer.register(parentDisposable, table.destroyHook());

        table.setTreeColumnHeader(LocalizeValue.localizeTODO("Method"));

        table.addColumn(LocalizeValue.localizeTODO("Total %"), node -> ProfilerFormat.percent(node.getTotal(), total))
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatPercent(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());

        table.addColumn(LocalizeValue.localizeTODO("Self %"), node -> ProfilerFormat.percent(node.getSelf(), total))
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatPercent(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());

        table.addColumn(LocalizeValue.localizeTODO("Total"), CallTreeNode::getTotal)
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatCount(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());

        table.addColumn(LocalizeValue.localizeTODO("Self"), CallTreeNode::getSelf)
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatCount(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());

        table.addColumn(LocalizeValue.localizeTODO("Samples"), CallTreeNode::getCount)
            .setRender((presentation, item) -> presentation.append(ProfilerFormat.formatCount(item.getValue())))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());

        table.setSpeedSearchConverter(node -> getText(node, renderer));
        return table;
    }

    private static String getText(TreeNode<CallTreeNode> node, BaseCallStackElementRenderer renderer) {
        CallTreeNode value = node.getValue();
        BaseCallStackElement element = value == null ? null : value.getElement();
        return element == null ? "" : renderer.getText(element);
    }
}

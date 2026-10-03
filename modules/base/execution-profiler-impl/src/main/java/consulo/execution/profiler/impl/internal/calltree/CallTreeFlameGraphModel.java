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
package consulo.execution.profiler.impl.internal.calltree;

import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.model.ThreadInfo;
import consulo.execution.profiler.ui.BaseCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.FlameGraphModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTreeFlameGraphModel implements FlameGraphModel<CallTreeNode> {
    private final CallTreeNode myRoot;
    private final BaseCallStackElementRenderer myRenderer;
    private final LocalizeValue myRootName;
    private final ChartUnit myUnit;
    private final boolean myBottomUp;
    private final long myRootWeight;

    private CallTreeFlameGraphModel(
        CallTreeNode root,
        BaseCallStackElementRenderer renderer,
        LocalizeValue rootName,
        ChartUnit unit,
        boolean bottomUp
    ) {
        myRoot = root;
        myRenderer = renderer;
        myRootName = rootName;
        myUnit = unit;
        myBottomUp = bottomUp;

        if (bottomUp) {
            long weight = 0;
            for (CallTreeNode child : root.getChildren()) {
                weight += child.getSelf();
            }
            myRootWeight = weight;
        }
        else {
            myRootWeight = root.getTotal();
        }
    }

    public static CallTreeFlameGraphModel topDown(CallTree tree, BaseCallStackElementRenderer renderer, ChartUnit unit) {
        return new CallTreeFlameGraphModel(tree.getTopDown(), renderer, getTreeName(tree), unit, false);
    }

    public static CallTreeFlameGraphModel bottomUp(CallTree tree, BaseCallStackElementRenderer renderer, ChartUnit unit) {
        return new CallTreeFlameGraphModel(tree.getBottomUp(), renderer, getTreeName(tree), unit, true);
    }

    public static CallTreeFlameGraphModel subtree(CallTreeNode node, BaseCallStackElementRenderer renderer, ChartUnit unit) {
        BaseCallStackElement element = node.getElement();
        LocalizeValue name = element == null ? LocalizeValue.localizeTODO("All threads") : LocalizeValue.of(renderer.getText(element));
        return new CallTreeFlameGraphModel(node, renderer, name, unit, false);
    }

    public static LocalizeValue getTreeName(CallTree tree) {
        ThreadInfo thread = tree.getThread();
        if (thread == null) {
            return LocalizeValue.localizeTODO("All threads");
        }
        String name = thread.getName();
        return name.isEmpty() ? LocalizeValue.localizeTODO("Unnamed thread") : LocalizeValue.of(name);
    }

    public boolean isBottomUp() {
        return myBottomUp;
    }

    @Override
    public CallTreeNode getRoot() {
        return myRoot;
    }

    @Override
    public List<CallTreeNode> getChildren(CallTreeNode node) {
        List<CallTreeNode> children = node.getChildren();
        if (!myBottomUp) {
            return children;
        }

        List<CallTreeNode> withSelf = new ArrayList<>(children.size());
        for (CallTreeNode child : children) {
            if (child.getSelf() > 0) {
                withSelf.add(child);
            }
        }
        return withSelf;
    }

    @Override
    public long getWeight(CallTreeNode node) {
        if (node == myRoot) {
            return myRootWeight;
        }
        return myBottomUp ? node.getSelf() : node.getTotal();
    }

    @Override
    public String getName(CallTreeNode node) {
        BaseCallStackElement element = node.getElement();
        if (node == myRoot || element == null) {
            return myRootName.get();
        }
        return myRenderer.getText(element);
    }

    @Override
    public ChartUnit getWeightUnit() {
        return myUnit;
    }
}

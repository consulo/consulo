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

import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.impl.internal.calltree.CallTree;
import consulo.execution.profiler.impl.internal.calltree.CallTreeFlameGraphModel;
import consulo.execution.profiler.impl.internal.calltree.CallTreeNode;
import consulo.execution.profiler.ui.BaseCallStackElementRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.chart.ChartUnit;
import consulo.ui.chart.FlameGraph;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerFlameGraphView {
    private final CallTree myTree;
    private final BaseCallStackElementRenderer myRenderer;
    private final ProfilerNavigator myNavigator;
    private final DockLayout myRoot;
    private final DockLayout myGraphHolder;
    private final Label myStatusLabel;
    private final TextBox mySearchBox;
    private final Button myJumpButton;

    private CallTreeFlameGraphModel myModel;
    private FlameGraph<CallTreeNode> myGraph;

    @RequiredUIAccess
    public ProfilerFlameGraphView(CallTree tree, BaseCallStackElementRenderer renderer, ProfilerNavigator navigator) {
        myTree = tree;
        myRenderer = renderer;
        myNavigator = navigator;

        CheckBox bottomUpBox = CheckBox.create(LocalizeValue.localizeTODO("Bottom-up"));
        bottomUpBox.addValueListener(event -> rebuild(Boolean.TRUE.equals(event.getValue())));

        Button resetButton = Button.create(LocalizeValue.localizeTODO("Reset Zoom"), event -> myGraph.focus(null));

        myJumpButton = Button.create(LocalizeValue.localizeTODO("Jump to Source"), event -> jumpToSource());
        myJumpButton.setEnabled(false);

        mySearchBox = TextBox.create();
        mySearchBox.setPlaceholder(LocalizeValue.localizeTODO("Search frames"));
        mySearchBox.addValueListener(event -> applyHighlight());

        HorizontalLayout toolbar = HorizontalLayout.create(Space.SMALL);
        toolbar.add(bottomUpBox);
        toolbar.add(resetButton);
        toolbar.add(myJumpButton);

        DockLayout top = DockLayout.create(Space.SMALL);
        top.left(toolbar);
        top.right(mySearchBox);

        myStatusLabel = Label.create(LocalizeValue.localizeTODO("Click a frame to select it, double-click to zoom into it"));

        myGraphHolder = DockLayout.create(Space.NONE);

        myRoot = DockLayout.create(Space.SMALL);
        myRoot.top(top);
        myRoot.center(myGraphHolder);
        myRoot.bottom(myStatusLabel);

        myModel = CallTreeFlameGraphModel.topDown(tree, renderer, ChartUnit.COUNT);
        myGraph = createGraph(myModel);
        myGraphHolder.center(myGraph);
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private void rebuild(boolean bottomUp) {
        myModel = bottomUp
            ? CallTreeFlameGraphModel.bottomUp(myTree, myRenderer, ChartUnit.COUNT)
            : CallTreeFlameGraphModel.topDown(myTree, myRenderer, ChartUnit.COUNT);
        myGraph = createGraph(myModel);

        myGraphHolder.removeAll();
        myGraphHolder.center(myGraph);

        applyHighlight();
        updateSelection(null);
    }

    @RequiredUIAccess
    private FlameGraph<CallTreeNode> createGraph(CallTreeFlameGraphModel model) {
        FlameGraph<CallTreeNode> graph = FlameGraph.create(model);
        graph.addSelectListener(event -> updateSelection(event.getValue()));
        graph.addDoubleClickListener(event -> graph.focus(event.getValue()));
        return graph;
    }

    @RequiredUIAccess
    private void applyHighlight() {
        String text = mySearchBox.getValue();
        String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        CallTreeFlameGraphModel model = myModel;
        if (query.isEmpty()) {
            myGraph.setHighlight(null);
        }
        else {
            myGraph.setHighlight(node -> node.getElement() != null && model.getName(node).toLowerCase(Locale.ROOT).contains(query));
        }
    }

    @RequiredUIAccess
    private void updateSelection(@Nullable CallTreeNode node) {
        BaseCallStackElement element = node == null ? null : node.getElement();
        myJumpButton.setEnabled(element != null && element.isNavigatable());

        if (node == null) {
            myStatusLabel.setText(LocalizeValue.localizeTODO("Click a frame to select it, double-click to zoom into it"));
            return;
        }

        CallTreeFlameGraphModel model = myModel;
        long weight = model.getWeight(node);
        long rootWeight = model.getWeight(model.getRoot());
        String text = model.getName(node)
            + "  "
            + ProfilerFormat.formatCount(weight)
            + " ("
            + ProfilerFormat.formatPercent(ProfilerFormat.percent(weight, rootWeight))
            + ")";
        myStatusLabel.setText(LocalizeValue.of(text));
    }

    @RequiredUIAccess
    private void jumpToSource() {
        CallTreeNode node = myGraph.getSelectedValue();
        BaseCallStackElement element = node == null ? null : node.getElement();
        if (element != null) {
            myNavigator.navigate(element);
        }
    }
}

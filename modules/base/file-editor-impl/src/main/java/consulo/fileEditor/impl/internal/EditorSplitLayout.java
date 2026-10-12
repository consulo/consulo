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
package consulo.fileEditor.impl.internal;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class EditorSplitLayout {
    private static final class SplitView {
        private final TwoComponentSplitLayout myLayout;
        private boolean myVertical;
        private int myPercent;

        private SplitView(TwoComponentSplitLayout layout, boolean vertical, int percent) {
            myLayout = layout;
            myVertical = vertical;
            myPercent = percent;
        }
    }

    private final WrappedLayout myRoot;
    private final Map<EditorSplitBranch, SplitView> mySplits = new IdentityHashMap<>();
    private final Map<Layout<?>, @Nullable Component[]> myOccupants = new IdentityHashMap<>();

    @RequiredUIAccess
    public EditorSplitLayout() {
        myRoot = WrappedLayout.create();
        myOccupants.put(myRoot, new Component[1]);
    }

    public WrappedLayout getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void update(@Nullable EditorSplitNode rootNode, Function<EditorSplitNode, Component> windowComponent) {
        Map<Layout<?>, @Nullable Component[]> wanted = new LinkedHashMap<>();
        Set<EditorSplitBranch> liveBranches = Collections.newSetFromMap(new IdentityHashMap<>());

        Component rootComponent = rootNode == null ? null : collect(rootNode, windowComponent, wanted, liveBranches);
        wanted.put(myRoot, new Component[]{rootComponent});

        for (Map.Entry<Layout<?>, @Nullable Component[]> entry : myOccupants.entrySet()) {
            Layout<?> layout = entry.getKey();
            @Nullable Component[] current = entry.getValue();
            @Nullable Component[] desired = wanted.get(layout);

            for (int slot = current.length - 1; slot >= 0; slot--) {
                Component occupant = current[slot];
                if (occupant != null && (desired == null || desired[slot] != occupant)) {
                    setSlot(layout, slot, null);
                    current[slot] = null;
                }
            }
        }

        mySplits.keySet().removeIf(branch -> !liveBranches.contains(branch));
        myOccupants.keySet().removeIf(layout -> !wanted.containsKey(layout));

        for (Map.Entry<Layout<?>, @Nullable Component[]> entry : wanted.entrySet()) {
            Layout<?> layout = entry.getKey();
            @Nullable Component[] desired = entry.getValue();
            @Nullable Component[] current = myOccupants.computeIfAbsent(layout, it -> new Component[desired.length]);

            for (int slot = 0; slot < desired.length; slot++) {
                Component component = desired[slot];
                if (component != null && current[slot] != component) {
                    setSlot(layout, slot, component);
                    current[slot] = component;
                }
            }
        }
    }

    @RequiredUIAccess
    private Component collect(EditorSplitNode node,
                              Function<EditorSplitNode, Component> windowComponent,
                              Map<Layout<?>, @Nullable Component[]> wanted,
                              Set<EditorSplitBranch> liveBranches) {
        if (!(node instanceof EditorSplitBranch branch)) {
            return windowComponent.apply(node);
        }

        liveBranches.add(branch);

        Component first = collect(branch.getFirst(), windowComponent, wanted, liveBranches);
        Component second = collect(branch.getSecond(), windowComponent, wanted, liveBranches);

        SplitView view = getOrCreateSplit(branch);
        wanted.put(view.myLayout, new Component[]{first, second});
        return view.myLayout;
    }

    @RequiredUIAccess
    private SplitView getOrCreateSplit(EditorSplitBranch branch) {
        int percent = Math.round(branch.getProportion() * 100f);

        SplitView view = mySplits.get(branch);
        if (view == null) {
            TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(toPosition(branch.isVertical()));
            layout.setProportion(percent);

            SplitView newView = new SplitView(layout, branch.isVertical(), percent);
            layout.addSplitProportionChangedListener(event -> {
                newView.myPercent = event.getProportion();
                if (Math.round(branch.getProportion() * 100f) != event.getProportion()) {
                    branch.setProportion(event.getProportion() / 100f);
                }
            });

            mySplits.put(branch, newView);
            return newView;
        }

        if (view.myVertical != branch.isVertical()) {
            view.myVertical = branch.isVertical();
            view.myLayout.setPosition(toPosition(view.myVertical));
        }

        if (view.myPercent != percent) {
            view.myPercent = percent;
            view.myLayout.setProportion(percent);
        }
        return view;
    }

    private static SplitLayoutPosition toPosition(boolean vertical) {
        return vertical ? SplitLayoutPosition.VERTICAL : SplitLayoutPosition.HORIZONTAL;
    }

    @RequiredUIAccess
    private void setSlot(Layout<?> layout, int slot, @Nullable Component component) {
        if (layout == myRoot) {
            myRoot.set(component);
        }
        else if (layout instanceof TwoComponentSplitLayout split) {
            if (slot == 0) {
                split.setFirstComponent(component);
            }
            else {
                split.setSecondComponent(component);
            }
        }
    }
}

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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import consulo.ui.CheckableTree;
import consulo.ui.TreeNode;
import consulo.ui.event.details.InputDetails;
import consulo.ui.impl.tree.CheckBoxTreeState;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.util.lang.ThreeState;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
final class WebTreeCheckBoxes<NODE> {
    private final WebTreeImpl<NODE> myOwner;
    private final CheckBoxTreeState<NODE> myState;

    private final Map<TreeNodeImpl<NODE>, @Nullable Checkbox> myCheckBoxes = new HashMap<>();

    private boolean myApplyScheduled;

    WebTreeCheckBoxes(WebTreeImpl<NODE> owner, CheckableTree<NODE> tree, String keyFilter) {
        myOwner = owner;
        myState = CheckBoxTreeState.attach(tree, owner.getController(), this::scheduleApply);

        WebTreeImpl<NODE>.Vaadin vaadin = owner.toVaadinComponent();
        WebInputDetails.addKeyListener(vaadin.getElement(), "keydown", this::toggleSelection)
            .setFilter("event.key === ' ' && " + WebTreeImpl.OUTSIDE_CHECK_BOX + keyFilter);
        vaadin.addDetachListener(event -> myApplyScheduled = false);
    }

    CheckBoxTreeState<NODE> getState() {
        return myState;
    }

    void decorateRow(WebTreeImpl.VaadinGridTreeToggle toggle, @Nullable TreeNodeImpl<NODE> node) {
        if (node == null) {
            return;
        }

        ThreeState state = myState.getShownState(node);
        if (state == null) {
            myCheckBoxes.put(node, null);
            return;
        }

        Checkbox checkBox = new Checkbox();
        checkBox.addClassName(WebTreeImpl.CHECK_BOX_CLASS);
        apply(checkBox, state, myState.isCheckEnabled(node.getValue()));

        WebInputDetails.addClickListener(checkBox.getElement(), details -> toggleByClick(node, details));
        checkBox.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                scheduleApply();
            }
        });

        myCheckBoxes.put(node, checkBox);
        toggle.add(checkBox);
    }

    private void toggleByClick(TreeNodeImpl<NODE> node, InputDetails details) {
        TreeNodeImpl<NODE> live = myOwner.getController().toNode(node);
        if (live != null) {
            myState.toggle(live, List.of(), details);
        }
        scheduleApply();
    }

    private void toggleSelection(InputDetails details) {
        TreeNode<NODE> lead = myOwner.getSelectedNode();
        if (lead != null) {
            myState.toggle(lead, myOwner.getSelectedNodes(), details);
        }
    }

    private void scheduleApply() {
        if (myApplyScheduled) {
            return;
        }

        WebTreeImpl<NODE>.Vaadin vaadin = myOwner.toVaadinComponent();
        UI ui = vaadin.getUI().orElse(null);
        if (ui == null) {
            return;
        }

        myApplyScheduled = true;
        ui.beforeClientResponse(vaadin, context -> {
            myApplyScheduled = false;
            applyChecks();
        });
    }

    private void applyChecks() {
        for (Map.Entry<TreeNodeImpl<NODE>, @Nullable Checkbox> entry : List.copyOf(myCheckBoxes.entrySet())) {
            TreeNodeImpl<NODE> node = entry.getKey();
            if (node.isRemoved()) {
                myCheckBoxes.remove(node);
                continue;
            }

            ThreeState state = myState.getShownState(node);
            Checkbox checkBox = entry.getValue();
            if (state == null || checkBox == null) {
                if (state != null || checkBox != null) {
                    myOwner.toVaadinComponent().refreshNode(node);
                }
                continue;
            }

            apply(checkBox, state, myState.isCheckEnabled(node.getValue()));
        }
    }

    private static void apply(Checkbox checkBox, ThreeState state, boolean enabled) {
        checkBox.setValue(state == ThreeState.YES);
        checkBox.setIndeterminate(state == ThreeState.UNSURE);
        checkBox.setEnabled(enabled);
    }
}

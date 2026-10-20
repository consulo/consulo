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

import com.vaadin.flow.component.HasComponents;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.internal.PopupTree;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
class WebPopupTreeImpl<NODE> extends WebTreeImpl<NODE> implements PopupTree<NODE> {
    private static final String SEPARATOR_ATTRIBUTE = "separator";

    private static final String OUTSIDE_CHEVRON =
        "!event.composedPath().some(node => node.getAttribute && node.getAttribute('part') === 'toggle')";

    private boolean mySelectOnHover;
    private Predicate<@Nullable NODE> mySeparator = value -> false;
    private @Nullable BiConsumer<TreeNode<NODE>, InputDetails> myActivateListener;

    @RequiredUIAccess
    WebPopupTreeImpl(TreeModel<NODE> model) {
        super(null, model, TreeExecutor.uiThread());
    }

    @Override
    HasComponents decorateRow(VaadinGridTreeToggle toggle, @Nullable TreeNodeImpl<NODE> node) {
        if (node == null) {
            return toggle;
        }

        if (mySeparator.test(node.getValue())) {
            toggle.getElement().setAttribute(SEPARATOR_ATTRIBUTE, true);
            return toggle;
        }

        toggle.getElement().addEventListener("mouseenter", event -> {
            if (mySelectOnHover) {
                getController().select(node);
            }
        });

        WebInputDetails.addClickListener(toggle.getElement(), "click", details -> {
            BiConsumer<TreeNode<NODE>, InputDetails> listener = myActivateListener;
            if (listener != null) {
                listener.accept(node, details);
            }
        }).setFilter(OUTSIDE_CHEVRON);

        return toggle;
    }

    @Override
    boolean isNavigable(@Nullable NODE value) {
        return !mySeparator.test(value);
    }

    @Override
    public void setSelectOnHover(boolean selectOnHover) {
        mySelectOnHover = selectOnHover;
    }

    @Override
    public void setSeparator(Predicate<NODE> separator) {
        mySeparator = separator::test;
    }

    @Override
    public void setActivateListener(BiConsumer<TreeNode<NODE>, InputDetails> listener) {
        myActivateListener = listener;
    }

    @Override
    public void selectNext() {
        moveSelection(true);
    }

    @Override
    public void selectPrevious() {
        moveSelection(false);
    }

    @RequiredUIAccess
    private void moveSelection(boolean forward) {
        getController().selectAdjacent(forward, mySeparator).thenRun(() -> {
            UIAccess uiAccess = getUIAccess();
            if (uiAccess != null) {
                uiAccess.giveIfNeed(() -> scrollTo(getSelectedNode()));
            }
        });
    }

    @Override
    public boolean isExpanded(TreeNode<NODE> node) {
        return getController().isExpanded(node);
    }

    @Override
    public void setExpanded(TreeNode<NODE> node, boolean expanded) {
        if (expanded) {
            expand(node);
        }
        else {
            getController().collapse(node);
        }
    }
}

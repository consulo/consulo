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
package consulo.it.internal.ui;

import consulo.it.internal.HeadlessUIAccess;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public class HeadlessTreeWidget<E> implements TreeWidget<E> {
    private static final Logger LOG = Logger.getInstance(HeadlessTreeWidget.class);

    private final Component myTree;

    public HeadlessTreeWidget(Component tree) {
        myTree = tree;
    }

    @Override
    public @Nullable UIAccess getUIAccess() {
        return myTree.getUIAccess();
    }

    @Override
    public TextItemPresentation createPresentation() {
        return new HeadlessTextItemPresentation();
    }

    @Override
    public void showLoading(TreeNodeImpl<E> node) {
        assertUIThread();
    }

    @Override
    public void hideLoading(TreeNodeImpl<E> node) {
        assertUIThread();
    }

    @Override
    public void setChildren(TreeNodeImpl<E> node, List<TreeNodeImpl<E>> children) {
        assertUIThread();
    }

    @Override
    public void setExpanded(TreeNodeImpl<E> node, boolean expanded) {
        assertUIThread();
    }

    @Override
    public void setSelected(@Nullable TreeNodeImpl<E> node) {
        assertUIThread();
    }

    @Override
    public void update(TreeNodeImpl<E> node) {
        assertUIThread();
    }

    private static void assertUIThread() {
        if (!HeadlessUIAccess.INSTANCE.isUIThread()) {
            LOG.error(new IllegalStateException("Tree widget is touched outside the UI thread"));
        }
    }
}

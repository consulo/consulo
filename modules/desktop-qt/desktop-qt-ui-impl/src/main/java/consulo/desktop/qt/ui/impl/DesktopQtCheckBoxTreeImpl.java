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
package consulo.desktop.qt.ui.impl;

import consulo.ui.CheckBoxTree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.impl.tree.CheckBoxTreeState;
import consulo.ui.impl.tree.CheckBoxTreeStateHolder;
import consulo.ui.impl.tree.TreeNodeImpl;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QTreeWidget;
import io.qt.widgets.QTreeWidgetItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopQtCheckBoxTreeImpl<E> extends DesktopQtTreeImpl<E> implements CheckBoxTree<E>, CheckBoxTreeStateHolder<E> {
    private final DesktopQtTreeCheckBoxes<E> myCheckBoxes;

    public DesktopQtCheckBoxTreeImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);

        myCheckBoxes = new DesktopQtTreeCheckBoxes<>(this, this);
    }

    @Override
    protected QTreeWidget createQt(QWidget parent) {
        QTreeWidget tree = super.createQt(parent);
        myCheckBoxes.installDelegate(tree);
        return tree;
    }

    @Override
    protected void initialize(QTreeWidget tree) {
        super.initialize(tree);

        myCheckBoxes.connect(tree);
    }

    @Override
    protected boolean isPressOnly(QTreeWidget tree, QMouseEvent event) {
        return myCheckBoxes.isCheckBoxPress(tree, event);
    }

    @Override
    protected boolean consumeKeyPress(QTreeWidget tree, QKeyEvent event) {
        return myCheckBoxes.toggleByKey(tree, event);
    }

    @Override
    protected void render(TreeNodeImpl<E> node, QTreeWidgetItem item) {
        sync(() -> {
            super.render(node, item);
            myCheckBoxes.applyCheck(node, item);
        });
    }

    @Override
    public CheckBoxTreeState<E> getCheckBoxState() {
        return myCheckBoxes.getState();
    }
}

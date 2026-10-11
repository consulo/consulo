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
package consulo.desktop.awt.ui.impl;

import consulo.ui.CheckBoxTree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.impl.tree.CheckBoxTreeState;
import consulo.ui.impl.tree.CheckBoxTreeStateHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreeCellRenderer;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopCheckBoxTreeImpl<E> extends DesktopTreeImpl<E> implements CheckBoxTree<E>, CheckBoxTreeStateHolder<E> {
    private final DesktopTreeCheckBoxes<E> myCheckBoxes;

    public DesktopCheckBoxTreeImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);

        myCheckBoxes = new DesktopTreeCheckBoxes<>(this, this);
    }

    @Override
    DesktopAWTTree createTree(javax.swing.tree.TreeModel model) {
        return myCheckBoxes.createTree(model);
    }

    @Override
    TreeCellRenderer createCellRenderer() {
        return myCheckBoxes.getRenderer();
    }

    @Override
    protected DesktopAWTTree createComponent() {
        DesktopAWTTree tree = super.createComponent();
        myCheckBoxes.installToggleKey(tree, tree);
        return tree;
    }

    @Override
    void childrenBuilt(DesktopTreeNodeImpl<E> node) {
        myCheckBoxes.childrenBuilt();
    }

    @Override
    public CheckBoxTreeState<E> getCheckBoxState() {
        return myCheckBoxes.getState();
    }
}

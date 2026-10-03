/*
 * Copyright 2013-2021 consulo.io
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

import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2021-07-14
 */
public class DesktopTreeImpl<E> extends DesktopTreeBase<E, DesktopAWTTree> {
    public DesktopTreeImpl(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);
    }

    @Override
    protected DesktopAWTTree createComponent() {
        DesktopAWTTree tree = createTree(createAsyncTreeModel());
        installTree(tree);
        return tree;
    }

    @Override
    protected DesktopAWTTree getTree() {
        return toAWTComponent();
    }
}

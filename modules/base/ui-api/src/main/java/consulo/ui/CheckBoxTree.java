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
package consulo.ui;

import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Tree} whose rows carry a check box in front of the node presentation - see {@link CheckableTree} for
 * how the check boxes behave.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public interface CheckBoxTree<E> extends CheckableTree<E> {
    static <E> CheckBoxTree<E> create(TreeModel<E> model) {
        return create(null, model);
    }

    static <E> CheckBoxTree<E> create(@Nullable E rootValue, TreeModel<E> model) {
        return create(rootValue, model, TreeExecutor.uiThread());
    }

    static <E> CheckBoxTree<E> create(TreeModel<E> model, TreeExecutor executor) {
        return create(null, model, executor);
    }

    /**
     * @param executor where the model runs - {@link TreeExecutor#uiThread()} is only right for a model that
     *                 computes nothing, see {@link TreeExecutor}
     */
    static <E> CheckBoxTree<E> create(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return UIInternal.get()._Components_checkBoxTree(rootValue, model, executor);
    }
}

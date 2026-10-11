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

import consulo.ui.CheckBoxTreeTable;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.impl.tree.CheckBoxTreeState;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class HeadlessCheckBoxTreeTable<E> extends HeadlessTreeTable<E> implements CheckBoxTreeTable<E>, HeadlessCheckableTree<E> {
    private final CheckBoxTreeState<E> myState;

    public HeadlessCheckBoxTreeTable(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        super(rootValue, model, executor);

        myState = CheckBoxTreeState.attach(this, getController(), () -> {
        });
    }

    @Override
    public CheckBoxTreeState<E> getCheckBoxState() {
        return myState;
    }
}

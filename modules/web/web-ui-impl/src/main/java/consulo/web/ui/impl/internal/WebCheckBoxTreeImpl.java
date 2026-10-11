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

import consulo.ui.CheckBoxTree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.tree.CheckBoxTreeState;
import consulo.ui.impl.tree.CheckBoxTreeStateHolder;
import consulo.ui.impl.tree.TreeNodeImpl;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class WebCheckBoxTreeImpl<NODE> extends WebTreeImpl<NODE> implements CheckBoxTree<NODE>, CheckBoxTreeStateHolder<NODE> {
    private final WebTreeCheckBoxes<NODE> myCheckBoxes;

    @RequiredUIAccess
    public WebCheckBoxTreeImpl(@Nullable NODE rootValue, TreeModel<NODE> model, TreeExecutor executor) {
        super(rootValue, model, executor);

        myCheckBoxes = new WebTreeCheckBoxes<>(this, this, "");
    }

    @Override
    void decorateRow(VaadinGridTreeToggle toggle, @Nullable TreeNodeImpl<NODE> node) {
        myCheckBoxes.decorateRow(toggle, node);
    }

    @Override
    public CheckBoxTreeState<NODE> getCheckBoxState() {
        return myCheckBoxes.getState();
    }
}

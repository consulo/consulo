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
package consulo.ui.ex.toolbar;

import consulo.annotation.DeprecationInfo;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.Table;
import consulo.ui.Tree;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.internal.ToolbarDecoratorBuilderInternal;

import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public sealed interface ToolbarDecoratorBuilder<E> permits ToolbarDecoratorBuilderInternal {
    @Deprecated
    @DeprecationInfo("Use ToolbarDecoratorBuilderFactory")
    static <E> ToolbarDecoratorBuilder<E> newBuilder(Tree<E> tree) {
        return ToolbarDecoratorBuilderFactory.getInstance().create(tree);
    }

    @Deprecated
    @DeprecationInfo("Use ToolbarDecoratorBuilderFactory")
    static <E> ToolbarDecoratorBuilder<E> newBuilder(Table<E> table) {
        return ToolbarDecoratorBuilderFactory.getInstance().create(table);
    }

    @Deprecated
    @DeprecationInfo("Use ToolbarDecoratorBuilderFactory")
    static <E> ToolbarDecoratorBuilder<E> newBuilder(ListBox<E> listBox) {
        return ToolbarDecoratorBuilderFactory.getInstance().create(listBox);
    }

    ToolbarDecoratorBuilder<E> addOrReplaceAction(ToolbarAction<E> action);

    ToolbarDecoratorBuilder<E> disableAction(Class<? extends ToolbarAction> actionClass);

    ToolbarDecoratorBuilder<E> disableAll();

    ToolbarDecoratorBuilder<E> addExtraAction(AnAction action);

    ToolbarDecoratorBuilder<E> addExtraAction(String actionId);

    ToolbarDecoratorBuilder<E> withPlace(String place);

    ToolbarDecoratorBuilder<E> withToolbarPosition(ActionToolbarPosition position);

    ToolbarDecoratorBuilder<E> withActionSorter(Function<List<AnAction>, List<AnAction>> sorter);

    @RequiredUIAccess
    Component build();
}

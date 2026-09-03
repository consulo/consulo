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
package consulo.ui.ex.impl.internal.toolbar;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.ListBox;
import consulo.ui.Table;
import consulo.ui.Tree;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilder;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
@Singleton
@ServiceImpl
public class ToolbarDecoratorBuilderFactoryImpl implements ToolbarDecoratorBuilderFactory {
    @Override
    public <E> ToolbarDecoratorBuilder<E> create(Tree<E> tree) {
        return new ToolbarDecoratorBuilderImpl<>(tree, null);
    }

    @Override
    public <E> ToolbarDecoratorBuilder<E> create(Table<E> table) {
        return new ToolbarDecoratorBuilderImpl<>(table, new TableToolbarExecutor<>(table));
    }

    @Override
    public <E> ToolbarDecoratorBuilder<E> create(ListBox<E> listBox) {
        return new ToolbarDecoratorBuilderImpl<>(listBox, new ListBoxToolbarExecutor<>(listBox));
    }
}

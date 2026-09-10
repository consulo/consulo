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
package consulo.ui.ex.internal;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public interface ToolbarExecutor<E> {
    Key<ToolbarExecutor> KEY = Key.create(ToolbarExecutor.class);

    @RequiredUIAccess
    @Nullable
    E getSelectedValue();

    /**
     * The whole selection, for an action which acts on more than the lead row - a remove over a multi row
     * selection, a rule which has to see every selected row before it can say whether it applies. A widget
     * which only ever holds one selected value answers with that one.
     */
    @RequiredUIAccess
    default List<E> getSelectedValues() {
        E value = getSelectedValue();
        return value == null ? List.of() : List.of(value);
    }

    @RequiredUIAccess
    boolean canAdd();

    @RequiredUIAccess
    void add();

    @RequiredUIAccess
    boolean canEdit();

    @RequiredUIAccess
    void edit();

    @RequiredUIAccess
    boolean canRemove();

    @RequiredUIAccess
    void remove();

    @RequiredUIAccess
    boolean canMoveUp();

    @RequiredUIAccess
    void moveUp();

    @RequiredUIAccess
    boolean canMoveDown();

    @RequiredUIAccess
    void moveDown();
}

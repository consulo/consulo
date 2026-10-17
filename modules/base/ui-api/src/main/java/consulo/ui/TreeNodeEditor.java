/*
 * Copyright 2013-2017 consulo.io
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

import consulo.ui.annotation.RequiredUIAccess;

/**
 * Edits a node of a {@link Tree} in place: while the node is edited, its row shows the component the editor creates
 * instead of the presentation of the node.
 * <p>
 * Editing starts on a click on an already selected editable node, on F2, or by {@link Tree#startEditing}. Enter and
 * leaving the editor stop it, Escape cancels it.
 *
 * @author VISTALL
 * @since 2026-10-17
 */
public interface TreeNodeEditor<E> {
    boolean isEditable(E value);

    /**
     * @return the component shown in the row of the node while it is edited
     */
    @RequiredUIAccess
    Component createEditor(E value);

    /**
     * Called once the editor component is shown.
     */
    @RequiredUIAccess
    default void editingStarted(E value, Component editor) {
    }

    /**
     * Applies what the editor holds.
     *
     * @return false to keep the editor open - for example, when its content is not valid
     */
    @RequiredUIAccess
    default boolean stopEditing(E value, Component editor) {
        return true;
    }

    /**
     * Drops what the editor holds.
     */
    @RequiredUIAccess
    default void cancelEditing(E value, Component editor) {
    }
}

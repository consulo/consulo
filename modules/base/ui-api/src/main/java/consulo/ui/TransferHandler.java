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

import consulo.ui.clipboard.DataTransfer;
import org.jspecify.annotations.Nullable;

/**
 * What a copy of a component is, and what a paste into it does. Dragging and dropping arrive with
 * {@link DragAndDropTransferHandler}, so a component carrying this alone can neither be dragged
 * out of nor dropped into.
 *
 * @param <Item> what the component is a collection of, carried here so that a handler keeps naming
 *               the same items the component does once it takes on more than the clipboard
 * @author VISTALL
 * @since 2026-08-07
 */
public interface TransferHandler<Item> {
    /**
     * The payload a copy of this component produces, {@code null} when there is nothing to copy.
     */
    @Nullable
    DataTransfer createTransfer(Component component);

    default boolean canImport(Component component, DataTransfer transfer) {
        return false;
    }

    default boolean importTransfer(Component component, DataTransfer transfer) {
        return false;
    }
}

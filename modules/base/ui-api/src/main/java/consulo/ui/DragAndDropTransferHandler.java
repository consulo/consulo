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

import java.util.List;

/**
 * A {@link TransferHandler} whose component can also be dragged out of and dropped into. Carrying
 * this is what turns dragging and dropping on, so a component is told what it can do the moment a
 * handler is set on it rather than by asking the handler once a gesture is already under way.
 * <p/>
 * A drag carries the same {@link DataTransfer} a copy carries, so a component can be dropped into
 * from anywhere publishing a type it understands.
 *
 * @param <Item> what the component is a collection of, and therefore what a drop names its target by
 * @author VISTALL
 * @since 2026-08-07
 */
public interface DragAndDropTransferHandler<Item> extends TransferHandler<Item> {
    /**
     * The payload for dragging these items out, {@code null} to refuse the gesture and leave the
     * items where they are.
     *
     * @param move {@code false} when the gesture is a copy, which a source is free to refuse even
     *             though it allows a move
     */
    @Nullable
    DataTransfer createDragTransfer(Component component, List<Item> items, boolean move);

    /**
     * Called on hover with {@link DropContext#isCheckOnly()}, then again to carry the drop out. Both
     * passes run the same decision, so a handler never states its rules twice.
     *
     * @return whether the drop is, or was, possible
     */
    boolean drop(Component component, DropContext<Item> context);

    /**
     * Where a drop lands relative to the item it was made on.
     */
    enum DropPosition {
        ABOVE,
        INTO,
        BELOW
    }

    /**
     * One drop being offered to a component, either to ask whether it is possible or to carry it out.
     */
    interface DropContext<Item> {
        /**
         * The item the drop was made on. A gesture past the last item lands on the last one as
         * {@link DropPosition#BELOW}, so this is never {@code null}.
         */
        Item getTarget();

        DropPosition getPosition();

        /**
         * {@code true} while the drop is only being offered, to decide whether it may happen at all.
         * A handler answering {@code true} to a check is asked again for real, and must not change
         * anything until then.
         */
        boolean isCheckOnly();

        DataTransfer getTransfer();

        /**
         * The dragged items, empty unless the drag started in this same component. A component
         * reordering its own items reads them here rather than off the payload.
         */
        List<Item> getItems();
    }
}

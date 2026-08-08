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
package consulo.desktop.awt.internal.clipboard;

import consulo.ui.DragAndDropTransferHandler;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What {@link DesktopAWTTransferHandlerAdapter} needs of a collection component to turn a toolkit
 * gesture into a drag or a drop stated in items. Reading the drop location is left to the component
 * because each kind of collection is handed a different one by the toolkit, and each already knows
 * how to walk itself.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public interface DesktopAWTTransferTarget<Item> {
    /**
     * The items a drag out of this component takes, normally its selection.
     */
    List<Item> getTransferItems();

    /**
     * Where this drop lands, {@code null} when it lands nowhere and is to be refused.
     */
    @Nullable
    Drop<Item> resolveDrop(javax.swing.TransferHandler.TransferSupport support);

    final class Drop<Item> {
        private final Item myItem;
        private final DragAndDropTransferHandler.DropPosition myPosition;

        public Drop(Item item, DragAndDropTransferHandler.DropPosition position) {
            myItem = item;
            myPosition = position;
        }

        public Item getItem() {
            return myItem;
        }

        public DragAndDropTransferHandler.DropPosition getPosition() {
            return myPosition;
        }
    }
}

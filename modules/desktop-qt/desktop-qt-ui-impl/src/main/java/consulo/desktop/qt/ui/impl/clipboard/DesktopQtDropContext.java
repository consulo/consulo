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
package consulo.desktop.qt.ui.impl.clipboard;

import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.clipboard.DataTransfer;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class DesktopQtDropContext<Item> implements DragAndDropTransferHandler.DropContext<Item> {
    private final DesktopQtDrop<Item> myDrop;
    private final DataTransfer myTransfer;
    private final List<Item> myItems;
    private final boolean myCheckOnly;

    public DesktopQtDropContext(DesktopQtDrop<Item> drop, DataTransfer transfer, List<Item> items, boolean checkOnly) {
        myDrop = drop;
        myTransfer = transfer;
        myItems = items;
        myCheckOnly = checkOnly;
    }

    public DesktopQtDropContext<Item> toPerforming() {
        return new DesktopQtDropContext<>(myDrop, myTransfer, myItems, false);
    }

    @Override
    public Item getTarget() {
        return myDrop.item();
    }

    @Override
    public DragAndDropTransferHandler.DropPosition getPosition() {
        return myDrop.position();
    }

    @Override
    public boolean isCheckOnly() {
        return myCheckOnly;
    }

    @Override
    public DataTransfer getTransfer() {
        return myTransfer;
    }

    @Override
    public List<Item> getItems() {
        return myItems;
    }
}

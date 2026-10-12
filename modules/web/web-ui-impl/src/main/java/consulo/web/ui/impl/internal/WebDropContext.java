/*
 * Copyright 2013-2016 consulo.io
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

import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.clipboard.DataTransfer;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
final class WebDropContext<Item> implements DragAndDropTransferHandler.DropContext<Item> {
    private final Item myTarget;
    private final DragAndDropTransferHandler.DropPosition myPosition;
    private final DataTransfer myTransfer;
    private final List<Item> myItems;
    private final boolean myCheckOnly;

    WebDropContext(Item target, DragAndDropTransferHandler.DropPosition position, DataTransfer transfer, List<Item> items, boolean checkOnly) {
        myTarget = target;
        myPosition = position;
        myTransfer = transfer;
        myItems = items;
        myCheckOnly = checkOnly;
    }

    WebDropContext<Item> toPerforming() {
        return new WebDropContext<>(myTarget, myPosition, myTransfer, myItems, false);
    }

    @Override
    public Item getTarget() {
        return myTarget;
    }

    @Override
    public DragAndDropTransferHandler.DropPosition getPosition() {
        return myPosition;
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

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
package consulo.desktop.awt.ui.impl.clipboard;

import consulo.ui.Component;
import consulo.ui.TransferHandler;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.clipboard.DataTransfer;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.List;

/**
 * Presents a {@link TransferHandler} to this frontend as its own transfer handler, so the component
 * level copy, paste, drag and drop of the toolkit run through it. The toolkit carries all four on
 * one handler, which is why they arrive together here.
 * <p/>
 * Dragging and dropping are only served when the handler is a {@link DragAndDropTransferHandler}
 * and the component knows how to name its own items.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class DesktopAWTTransferHandlerAdapter<Item> extends javax.swing.TransferHandler {
    private final Component myComponent;
    private final TransferHandler<Item> myHandler;
    private final @Nullable DragAndDropTransferHandler<Item> myDragAndDropHandler;
    private final @Nullable DesktopAWTTransferTarget<Item> myTarget;

    private boolean myDragging;
    private boolean myDraggingMove;
    private List<Item> myDraggedItems = List.of();

    public DesktopAWTTransferHandlerAdapter(Component component, TransferHandler<Item> handler) {
        this(component, handler, null);
    }

    public DesktopAWTTransferHandlerAdapter(Component component,
                                            TransferHandler<Item> handler,
                                            @Nullable DesktopAWTTransferTarget<Item> target) {
        myComponent = component;
        myHandler = handler;
        myTarget = target;
        myDragAndDropHandler = target != null && handler instanceof DragAndDropTransferHandler<Item> dragAndDrop ? dragAndDrop : null;
    }

    /**
     * Whether this handler carries the drag and drop half at all, which is what a component asks
     * before it lets the toolkit start gestures on it.
     */
    public boolean isDragAndDropSupported() {
        return myDragAndDropHandler != null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return myDragAndDropHandler == null ? COPY : COPY_OR_MOVE;
    }

    /**
     * The toolkit builds the payload of a drag through the same call it builds the payload of a
     * copy, so which one is under way has to be remembered across it.
     */
    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        myDragging = true;
        myDraggingMove = (action & MOVE) != 0;
        try {
            super.exportAsDrag(comp, e, action);
        }
        finally {
            myDragging = false;
        }
    }

    @Override
    protected @Nullable Transferable createTransferable(JComponent c) {
        DataTransfer transfer = myDragging ? createDragTransfer() : myHandler.createTransfer(myComponent);
        return transfer == null || transfer.isEmpty() ? null : DesktopAWTDataTransfers.toTransferable(transfer);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        DataTransfer transfer = DesktopAWTDataTransfers.fromTransferable(support.getTransferable());
        if (!support.isDrop()) {
            return myHandler.canImport(myComponent, transfer);
        }
        DropContextImpl context = contextOf(support, transfer, true);
        return context != null && myDragAndDropHandler.drop(myComponent, context);
    }

    @Override
    public boolean importData(TransferSupport support) {
        DataTransfer transfer = DesktopAWTDataTransfers.fromTransferable(support.getTransferable());
        if (!support.isDrop()) {
            return myHandler.importTransfer(myComponent, transfer);
        }
        DropContextImpl context = contextOf(support, transfer, false);
        return context != null && myDragAndDropHandler.drop(myComponent, context);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        myDraggedItems = List.of();
    }

    private @Nullable DataTransfer createDragTransfer() {
        if (myDragAndDropHandler == null) {
            return null;
        }

        List<Item> items = myTarget.getTransferItems();
        if (items.isEmpty()) {
            return null;
        }

        DataTransfer transfer = myDragAndDropHandler.createDragTransfer(myComponent, items, myDraggingMove);
        if (transfer == null || transfer.isEmpty()) {
            return null;
        }

        myDraggedItems = items;
        return transfer;
    }

    private @Nullable DropContextImpl contextOf(TransferSupport support, DataTransfer transfer, boolean checkOnly) {
        if (myDragAndDropHandler == null) {
            return null;
        }

        DesktopAWTTransferTarget.Drop<Item> drop = myTarget.resolveDrop(support);
        return drop == null ? null : new DropContextImpl(drop.getItem(), drop.getPosition(), transfer, checkOnly);
    }

    private class DropContextImpl implements DragAndDropTransferHandler.DropContext<Item> {
        private final Item myTargetItem;
        private final DragAndDropTransferHandler.DropPosition myPosition;
        private final DataTransfer myTransfer;
        private final boolean myCheckOnly;

        private DropContextImpl(Item targetItem,
                                DragAndDropTransferHandler.DropPosition position,
                                DataTransfer transfer,
                                boolean checkOnly) {
            myTargetItem = targetItem;
            myPosition = position;
            myTransfer = transfer;
            myCheckOnly = checkOnly;
        }

        @Override
        public Item getTarget() {
            return myTargetItem;
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
            return myDraggedItems;
        }
    }
}

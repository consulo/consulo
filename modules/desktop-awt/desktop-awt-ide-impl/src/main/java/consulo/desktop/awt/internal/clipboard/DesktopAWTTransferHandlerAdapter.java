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

import consulo.ui.Component;
import consulo.ui.TransferHandler;
import consulo.ui.clipboard.DataTransfer;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.Transferable;

/**
 * Presents a {@link TransferHandler} to this frontend as its own transfer handler, so the component
 * level copy and paste of the toolkit run through it.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class DesktopAWTTransferHandlerAdapter extends javax.swing.TransferHandler {
    private final Component myComponent;
    private final TransferHandler myHandler;

    public DesktopAWTTransferHandlerAdapter(Component component, TransferHandler handler) {
        myComponent = component;
        myHandler = handler;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    protected @Nullable Transferable createTransferable(JComponent c) {
        DataTransfer transfer = myHandler.createTransfer(myComponent);
        return transfer == null || transfer.isEmpty() ? null : DesktopAWTDataTransfers.toTransferable(transfer);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return myHandler.canImport(myComponent, DesktopAWTDataTransfers.fromTransferable(support.getTransferable()));
    }

    @Override
    public boolean importData(TransferSupport support) {
        return myHandler.importTransfer(myComponent, DesktopAWTDataTransfers.fromTransferable(support.getTransferable()));
    }
}

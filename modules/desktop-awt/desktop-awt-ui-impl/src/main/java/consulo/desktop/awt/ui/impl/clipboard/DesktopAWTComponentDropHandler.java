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
import consulo.ui.DropHandler;
import consulo.ui.clipboard.DataTransfer;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class DesktopAWTComponentDropHandler extends TransferHandler {
    private final Component myComponent;
    private final DropHandler myHandler;

    public DesktopAWTComponentDropHandler(Component component, DropHandler handler) {
        myComponent = component;
        myHandler = handler;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDrop() && myHandler.drop(myComponent, DesktopAWTDataTransfers.fromTransferable(support.getTransferable()), true);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        DataTransfer transfer = DesktopAWTDataTransfers.fromTransferable(support.getTransferable());
        return myHandler.drop(myComponent, transfer, true) && myHandler.drop(myComponent, transfer, false);
    }
}

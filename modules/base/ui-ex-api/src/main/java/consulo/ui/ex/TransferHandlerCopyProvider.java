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
package consulo.ui.ex;

import consulo.dataContext.DataContext;
import consulo.ui.HasTransferHandler;
import consulo.ui.TransferHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;

/**
 * Copies whatever the {@link TransferHandler} of the component says a copy of it is.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public final class TransferHandlerCopyProvider implements CopyProvider {
    private final HasTransferHandler<?> myComponent;

    public TransferHandlerCopyProvider(HasTransferHandler<?> component) {
        myComponent = component;
    }

    @Override
    @RequiredUIAccess
    public void performCopy(DataContext dataContext) {
        TransferHandler<?> handler = myComponent.getTransferHandler();
        DataTransfer transfer = handler == null ? null : handler.createTransfer(myComponent);
        if (transfer != null && !transfer.isEmpty()) {
            CopyPasteManager.getInstance().setContents(transfer);
        }
    }

    @Override
    public boolean isCopyEnabled(DataContext dataContext) {
        // building the payload just to answer a menu update would be doing the copy to ask about it
        return myComponent.getTransferHandler() != null;
    }

    @Override
    public boolean isCopyVisible(DataContext dataContext) {
        return true;
    }
}

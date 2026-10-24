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
package consulo.desktop.qt.ui.impl.layout;

import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.TransferHandler;
import consulo.ui.layout.LayoutConstraint;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * Drag and drop of the children of {@link DesktopQtTileLayoutImpl} and {@link DesktopQtFlowLayoutImpl}.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public abstract class DesktopQtTileLayoutBase extends DesktopQtLayoutComponent<LayoutConstraint, Object> {
    private final DesktopQtTileDragAndDrop myDragAndDrop = new DesktopQtTileDragAndDrop(this);
    private @Nullable TransferHandler<Component> myTransferHandler;

    @Override
    protected void initialize(QWidget component) {
        super.initialize(component);
        myDragAndDrop.install(component);
    }

    public void setTransferHandler(@Nullable TransferHandler<Component> handler) {
        myTransferHandler = handler;
    }

    public @Nullable TransferHandler<Component> getTransferHandler() {
        return myTransferHandler;
    }

    @Nullable
    DragAndDropTransferHandler<Component> getDragAndDropHandler() {
        return myTransferHandler instanceof DragAndDropTransferHandler<Component> handler ? handler : null;
    }
}

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
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.inject.EditorWindow;
import consulo.ui.Popup;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * Keeps a popup at the caret of an editor while it is open.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class UnifiedCaretAnchor implements Disposable {
    private final Editor myEditor;
    private final Popup myPopup;

    private @Nullable CaretPixelLocation myLocation;
    private @Nullable Rectangle myLocationArea;

    @RequiredUIAccess
    public UnifiedCaretAnchor(Editor editor, Popup popup) {
        myEditor = editor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : editor;
        myPopup = popup;

        myEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                relocate();
            }
        }, this);

        myEditor.getScrollingModel().addVisibleAreaListener(this::scrolled, this);

        if (myEditor instanceof CaretPixelLocationProvider provider) {
            provider.addCaretPixelLocationListener(this::relocate, this);
        }

        popup.addCloseListener(event -> Disposer.dispose(this));
    }

    @RequiredUIAccess
    public void relocate() {
        if (Disposer.isDisposed(this)) {
            return;
        }

        CaretPixelLocation location = myEditor instanceof CaretPixelLocationProvider provider ? provider.getCaretPixelLocation() : null;

        myLocation = location;
        myLocationArea = new Rectangle(myEditor.getScrollingModel().getVisibleArea());

        if (location == null) {
            myPopup.showAt(myEditor.getContentUIComponent(), 0, 0, 0);
        }
        else {
            myPopup.showAt(myEditor.getContentUIComponent(), location.x(), location.y(), location.height());
        }
    }

    @RequiredUIAccess
    private void scrolled(VisibleAreaEvent event) {
        CaretPixelLocation location = myLocation;
        Rectangle locationArea = myLocationArea;
        if (location == null || locationArea == null) {
            return;
        }

        Rectangle visibleArea = event.getNewRectangle();
        int x = location.x() - (visibleArea.x - locationArea.x);
        int y = location.y() - (visibleArea.y - locationArea.y);

        if (y + location.height() <= 0 || y >= visibleArea.height) {
            myPopup.close();
            return;
        }

        myPopup.showAt(myEditor.getContentUIComponent(), x, y, location.height());
    }

    @Override
    public void dispose() {
    }
}

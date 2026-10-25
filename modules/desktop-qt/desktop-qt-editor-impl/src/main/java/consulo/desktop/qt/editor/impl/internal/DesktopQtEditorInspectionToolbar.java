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
package consulo.desktop.qt.editor.impl.internal;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.impl.internal.action.UnifiedActionToolbarImpl;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QWidget;

import java.util.List;

/**
 * The actions of the inspection widget, at the top right corner of the editor.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
final class DesktopQtEditorInspectionToolbar implements Disposable {
    private static final String PLACE = "EditorInspectionsToolbar";
    private static final int GAP = 4;

    private final QWidget myViewport;
    private final QWidget myLayer;
    private final QObject myPlacer;

    private boolean myGone;
    private boolean myDisposed;

    @RequiredUIAccess
    DesktopQtEditorInspectionToolbar(DesktopQtEditorImpl editor, QWidget viewport, List<AnAction> actions) {
        myViewport = viewport;

        myLayer = new QWidget(viewport);
        QHBoxLayout layout = new QHBoxLayout(myLayer);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(0);
        layout.setSizeConstraint(QLayout.SizeConstraint.SetFixedSize);

        UnifiedActionToolbarImpl toolbar = new UnifiedActionToolbarImpl(PLACE, new DefaultActionGroup(actions), ActionToolbar.Style.HORIZONTAL);
        toolbar.setDataContextProvider(editor::getDataContext);

        QtComponentDelegate<?> toolbarComponent = (QtComponentDelegate<?>) toolbar.getUIComponent();
        toolbarComponent.setParent(editor.getContentUIComponent());
        toolbarComponent.bind(myLayer, null);
        QWidget toolbarWidget = toolbarComponent.toQtComponent();
        if (toolbarWidget != null) {
            layout.addWidget(toolbarWidget);
        }

        myPlacer = new QObject(myLayer) {
            @Override
            public boolean eventFilter(QObject watched, QEvent event) {
                QEvent.Type type = event.type();
                if (type == QEvent.Type.Resize || type == QEvent.Type.Show || type == QEvent.Type.LayoutRequest) {
                    place();
                }
                return false;
            }
        };
        viewport.installEventFilter(myPlacer);
        myLayer.installEventFilter(myPlacer);
        myLayer.destroyed.connect(() -> myGone = true);

        toolbar.addActionsUpdatedListener(this, this::place);
        myLayer.show();
        place();
        toolbar.updateActionsAsync();
    }

    private void place() {
        if (!isAlive()) {
            return;
        }
        myLayer.adjustSize();
        myLayer.move(myViewport.width() - myLayer.width() - GAP, GAP);
        myLayer.raise();
    }

    private boolean isAlive() {
        return !myGone && !myDisposed && !myLayer.isDisposed() && !myViewport.isDisposed();
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }
        myDisposed = true;
        if (myGone || myLayer.isDisposed()) {
            return;
        }
        if (!myViewport.isDisposed()) {
            myViewport.removeEventFilter(myPlacer);
        }
        myLayer.hide();
        myLayer.disposeLater();
    }
}

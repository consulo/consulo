// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.desktop.qt.editor.impl.internal.floating;

import consulo.codeEditor.impl.internal.floating.EditorFloatingToolbarInstaller;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.widgets.QApplication;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QWidget;

public final class DesktopQtEditorFloatingToolbar implements Disposable {
    private static final int GAP = 20;

    private static final int POSITION_Y = 20;

    private final QWidget myViewport;

    private final QWidget myLayer;

    private final QHBoxLayout myLayout;

    private final QObject myPlacer;

    private boolean myGone;

    private boolean myDisposed;

    @RequiredUIAccess
    public DesktopQtEditorFloatingToolbar(DesktopQtEditorImpl editor, Project project, QWidget viewport) {
        myViewport = viewport;

        myLayer = new QWidget(viewport);
        myLayer.setMouseTracking(true);

        myLayout = new QHBoxLayout(myLayer);
        myLayout.setContentsMargins(GAP, GAP, GAP, GAP);
        myLayout.setSpacing(GAP);
        myLayout.setSizeConstraint(QLayout.SizeConstraint.SetFixedSize);

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

        myLayer.destroyed.connect(this::layerDestroyed);

        myLayer.show();
        place();

        EditorFloatingToolbarInstaller.install(editor, project, this, (provider, dataContext, providerDisposable) -> {
            DesktopQtFloatingToolbarComponent component = new DesktopQtFloatingToolbarComponent(editor, provider, this, providerDisposable);
            addComponent(component, providerDisposable);
            return component;
        });
    }

    QWidget getLayer() {
        return myLayer;
    }

    boolean isGone() {
        return myGone;
    }

    @RequiredUIAccess
    boolean isComponentOnHold() {
        if (!isAlive()) {
            return false;
        }
        if (myLayer.underMouse()) {
            return true;
        }
        QWidget focusWidget = QApplication.focusWidget();
        return focusWidget != null && (focusWidget == myLayer || myLayer.isAncestorOf(focusWidget));
    }

    @RequiredUIAccess
    private void addComponent(DesktopQtFloatingToolbarComponent component, Disposable parentDisposable) {
        myLayout.addWidget(component);
        Disposer.register(parentDisposable, () -> removeComponent(component));
    }

    @RequiredUIAccess
    private void removeComponent(DesktopQtFloatingToolbarComponent component) {
        if (!isAlive() || !component.isAlive()) {
            return;
        }
        component.hide();
        myLayout.removeWidget(component);
        component.disposeLater();
    }

    @RequiredUIAccess
    private void place() {
        if (!isAlive()) {
            return;
        }
        myLayer.move(myViewport.width() - myLayer.width() - 1, POSITION_Y);
        myLayer.raise();
    }

    private boolean isAlive() {
        return !myGone && !myDisposed && !myLayer.isDisposed() && !myViewport.isDisposed();
    }

    private void layerDestroyed() {
        myGone = true;
        if (!myDisposed) {
            Disposer.dispose(this);
        }
    }

    @Override
    @RequiredUIAccess
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

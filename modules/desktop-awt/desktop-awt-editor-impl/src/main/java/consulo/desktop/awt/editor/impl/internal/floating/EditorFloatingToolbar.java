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
package consulo.desktop.awt.editor.impl.internal.floating;

import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.internal.floating.EditorFloatingToolbarActivation;
import consulo.codeEditor.impl.internal.floating.EditorFloatingToolbarInstaller;
import consulo.codeEditor.toolbar.floating.FloatingToolbarProvider;
import consulo.desktop.awt.editor.impl.internal.DesktopEditorPanelLayer;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class EditorFloatingToolbar extends JPanel implements DesktopEditorPanelLayer, Disposable {
    @RequiredUIAccess
    public EditorFloatingToolbar(Editor editor, Project project) {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        setBorder(BorderFactory.createEmptyBorder());
        setOpaque(false);

        EditorFloatingToolbarInstaller.install(editor, project, this, (provider, dataContext, providerDisposable) -> {
            EditorFloatingToolbarComponent component = new EditorFloatingToolbarComponent(editor, provider, providerDisposable);
            addComponent(component, providerDisposable);
            return component;
        });
    }

    private void addComponent(Component component, Disposable parentDisposable) {
        add(component);
        Disposer.register(parentDisposable, () -> remove(component));
    }

    @Override
    public int getPositionYInLayer() {
        return 20;
    }

    @Override
    public void dispose() {
    }

    private static final class EditorFloatingToolbarComponent extends AbstractFloatingToolbarComponent {
        @RequiredUIAccess
        EditorFloatingToolbarComponent(Editor editor, FloatingToolbarProvider provider, Disposable parentDisposable) {
            super(provider.getActionGroup(), editor.getContentComponent(), parentDisposable);

            EditorFloatingToolbarActivation activation = new EditorFloatingToolbarActivation(editor, provider, this, parentDisposable);

            KeyListener keyListener = new KeyAdapter() {
                @Override
                @RequiredUIAccess
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        activation.escapePressed(getVisibleAreaOnScreen());
                    }
                }
            };
            JComponent contentComponent = editor.getContentComponent();
            contentComponent.addKeyListener(keyListener);
            Disposer.register(parentDisposable, () -> contentComponent.removeKeyListener(keyListener));
        }

        private @Nullable Rectangle2D getVisibleAreaOnScreen() {
            if (!isVisible()) {
                return null;
            }
            Point location = new Point();
            SwingUtilities.convertPointToScreen(location, this);
            return new Rectangle2D(location.x, location.y, getWidth(), getHeight());
        }

        @Override
        protected boolean isComponentOnHold() {
            Container parent = getParent();
            return parent != null && (UIUtil.isComponentUnderMouse(parent) || UIUtil.isFocusAncestor(parent));
        }
    }
}

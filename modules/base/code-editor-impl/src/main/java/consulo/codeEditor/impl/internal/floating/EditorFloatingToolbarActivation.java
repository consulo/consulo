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
package consulo.codeEditor.impl.internal.floating;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.toolbar.floating.FloatingToolbarComponent;
import consulo.codeEditor.toolbar.floating.FloatingToolbarProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import org.jspecify.annotations.Nullable;

public final class EditorFloatingToolbarActivation {
    private final Editor myEditor;

    private final FloatingToolbarProvider myProvider;

    private final FloatingToolbarComponent myComponent;

    private @Nullable Rectangle2D myIgnoreMouseMotionArea;

    @RequiredUIAccess
    public EditorFloatingToolbarActivation(Editor editor,
                                           FloatingToolbarProvider provider,
                                           FloatingToolbarComponent component,
                                           Disposable parentDisposable) {
        myEditor = editor;
        myProvider = provider;
        myComponent = component;

        component.setAutoHideable(provider.isAutoHideable());

        EditorMouseMotionListener mouseMotionListener = new EditorMouseMotionListener() {
            @Override
            @RequiredUIAccess
            public void mouseMoved(EditorMouseEvent e) {
                onMouseMoved(e);
            }
        };
        Disposer.register(parentDisposable, () -> editor.removeEditorMouseMotionListener(mouseMotionListener));
        editor.addEditorMouseMotionListener(mouseMotionListener);
    }

    @RequiredUIAccess
    public void escapePressed(@Nullable Rectangle2D ignoreArea) {
        if (ignoreArea != null) {
            myIgnoreMouseMotionArea = ignoreArea;
        }
        myComponent.hideImmediately();
        myProvider.onHiddenByEsc(myEditor.getDataContext());
    }

    @RequiredUIAccess
    private void onMouseMoved(EditorMouseEvent e) {
        Rectangle2D ignoreArea = myIgnoreMouseMotionArea;
        if (ignoreArea != null && !isInside(ignoreArea, e)) {
            myIgnoreMouseMotionArea = null;
        }
        if (myComponent.isAutoHideable() && myIgnoreMouseMotionArea == null) {
            myComponent.scheduleShow();
        }
    }

    private static boolean isInside(Rectangle2D area, EditorMouseEvent e) {
        InputDetails inputDetails = e.getInputDetails();
        return inputDetails != null && area.contains(inputDetails.getPositionOnScreen());
    }
}

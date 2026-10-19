// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.codeEditor;

import consulo.ui.Component;
import consulo.ui.Length;
import org.jspecify.annotations.Nullable;

/**
 * Open implementation of {@link EditorCustomElementRenderer} for component-based inlays.
 * Can be extended to add context menu and/or gutter associated with inlay.
 * <p>
 * Add it with {@link InlayModel#addBlockElement(int, InlayProperties, EditorCustomElementRenderer)}: every frontend
 * hosts {@link #getComponent()} in the room the inlay takes under its line - a live component, so it takes input and
 * focus like any other - and reports the size the component needs through {@link #setInlaySize}, which is what the
 * inlay takes.
 */
public class ComponentInlayRenderer implements EditorCustomElementRenderer {
    private final Component myComponent;
    private final @Nullable ComponentInlayAlignment myAlignment;

    private @Nullable Length myMaxWidth;

    private int myInlayWidth;
    private int myInlayHeight;

    public ComponentInlayRenderer(Component component) {
        this(component, null);
    }

    public ComponentInlayRenderer(Component component, @Nullable ComponentInlayAlignment alignment) {
        myComponent = component;
        myAlignment = alignment;
    }

    public Component getComponent() {
        return myComponent;
    }

    public @Nullable ComponentInlayAlignment getAlignment() {
        return myAlignment;
    }

    /**
     * The widest the component gets whatever the alignment asks for - text in a component as wide as a wide editor
     * is hard to read.
     */
    public void setMaxWidth(@Nullable Length maxWidth) {
        myMaxWidth = maxWidth;
    }

    public @Nullable Length getMaxWidth() {
        return myMaxWidth;
    }

    /**
     * Called by the frontend hosting the component once it knows the size the component needs; the inlay is updated
     * by the caller afterwards.
     */
    public void setInlaySize(int width, int height) {
        myInlayWidth = Math.max(0, width);
        myInlayHeight = Math.max(0, height);
    }

    @Override
    public final int calcWidthInPixels(Inlay<?> inlay) {
        return myInlayWidth;
    }

    @Override
    public final int calcHeightInPixels(Inlay<?> inlay) {
        return myInlayHeight;
    }
}

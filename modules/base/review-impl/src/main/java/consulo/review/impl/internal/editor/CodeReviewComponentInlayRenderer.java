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
package consulo.review.impl.internal.editor;

import consulo.codeEditor.ComponentInlayAlignment;
import consulo.codeEditor.ComponentInlayRenderer;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class CodeReviewComponentInlayRenderer extends ComponentInlayRenderer {
    private final Component myActualComponent;
    private @Nullable GutterIconRenderer myGutterIconRenderer;

    @RequiredUIAccess
    public CodeReviewComponentInlayRenderer(Component actualComponent) {
        super(DockLayout.create().center(actualComponent), ComponentInlayAlignment.FIT_VIEWPORT_WIDTH);
        myActualComponent = actualComponent;
        // 52 for avatar and gaps
        setMaxWidth(CodeReviewEditorUIConstants.TEXT_CONTENT_WIDTH.plusPixel(52));
    }

    public boolean isVisible() {
        return myActualComponent.isVisible();
    }

    @RequiredUIAccess
    public void setVisible(boolean value) {
        myActualComponent.setVisible(value);
    }

    public @Nullable GutterIconRenderer getGutterIconRenderer() {
        return myGutterIconRenderer;
    }

    @RequiredUIAccess
    public void setGutterIconRenderer(Inlay<?> inlay, @Nullable GutterIconRenderer renderer) {
        if (Objects.equals(myGutterIconRenderer, renderer)) {
            return;
        }
        myGutterIconRenderer = renderer;
        if (inlay.isValid()) {
            inlay.update();
        }
    }

    @Override
    public @Nullable GutterIconRenderer calcGutterIconRenderer(Inlay<?> inlay) {
        return myGutterIconRenderer;
    }
}

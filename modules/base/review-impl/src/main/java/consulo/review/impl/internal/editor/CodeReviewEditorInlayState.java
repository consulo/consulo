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

import consulo.codeEditor.Inlay;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

/**
 * The component of one review inlay, kept while its line is known, and the inlay showing it while it is visible.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
final class CodeReviewEditorInlayState {
    @Nullable Component myComponent;
    @Nullable Disposable myRendererDisposable;
    @Nullable Inlay<?> myInlay;

    void disposeInlay() {
        Inlay<?> inlay = myInlay;
        myInlay = null;
        if (inlay != null) {
            Disposer.dispose(inlay);
        }
    }

    void disposeRenderer() {
        disposeInlay();
        Disposable rendererDisposable = myRendererDisposable;
        myRendererDisposable = null;
        myComponent = null;
        if (rendererDisposable != null) {
            Disposer.dispose(rendererDisposable);
        }
    }
}

/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.codeInsight.preview;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 2025-09-19
 */
@ExtensionImpl
public class ImagePreviewEditorFactoryListener implements EditorFactoryListener {
    private final Provider<ImageOrColorPreviewManager> myImageOrColorPreviewManager;

    @Inject
    public ImagePreviewEditorFactoryListener(Provider<ImageOrColorPreviewManager> imageOrColorPreviewManager) {
        myImageOrColorPreviewManager = imageOrColorPreviewManager;
    }

    @Override
    public void editorCreated(@Nonnull EditorFactoryEvent event) {
        myImageOrColorPreviewManager.get().registerListeners(event.getEditor());
    }

    @Override
    public void editorReleased(@Nonnull EditorFactoryEvent event) {
        myImageOrColorPreviewManager.get().unregisterEditor(event.getEditor());
    }
}

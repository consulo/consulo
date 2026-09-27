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
package consulo.it.internal.editor;

import consulo.codeEditor.Inlay;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorInlayModelBase;
import consulo.codeEditor.impl.EditorLocation;
import org.jspecify.annotations.Nullable;

import java.awt.Point;

/**
 * Inlay model for a headless editor.
 * <p>
 * Inlay bookkeeping is fully inherited from the base model, which works without a rendering surface.
 * Only the pixel hit-testing entry points are neutralized, because they need content component insets
 * and renderer widths that do not exist headlessly.
 *
 * @author VISTALL
 * @since 2026-09-26
 */
public class HeadlessInlayModel extends CodeEditorInlayModelBase {
    public HeadlessInlayModel(CodeEditorBase editor) {
        super(editor);
    }

    @Override
    @Nullable
    public Inlay getElementAt(Point point) {
        return null;
    }

    @Override
    @Nullable
    public Inlay getElementAt(EditorLocation location, boolean ignoreBlockElementWidth) {
        return null;
    }
}

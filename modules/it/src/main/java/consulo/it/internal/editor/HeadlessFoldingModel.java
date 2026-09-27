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

import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorFoldingModelBase;
import consulo.codeEditor.impl.EditorLocation;
import org.jspecify.annotations.Nullable;

import java.awt.Point;

/**
 * Fold region bookkeeping is fully carried by {@link CodeEditorFoldingModelBase}; a headless editor adds
 * nothing on top of it, since the end of a batch has no view to schedule a repaint for and no point on
 * screen can hold a placeholder.
 *
 * @author VISTALL
 */
public class HeadlessFoldingModel extends CodeEditorFoldingModelBase {
    public HeadlessFoldingModel(CodeEditorBase editor) {
        super(editor);
    }

    @Override
    public @Nullable FoldRegion getFoldingPlaceholderAt(Point p) {
        return null;
    }

    @Override
    public FoldRegion getFoldingPlaceholderAt(EditorLocation location, boolean ignoreCustomRegionWidth) {
        return null;
    }
}

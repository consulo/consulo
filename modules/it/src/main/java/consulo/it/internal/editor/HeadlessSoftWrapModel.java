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

import consulo.application.Application;
import consulo.codeEditor.SoftWrapDrawingType;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorSoftWrapModelBase;
import consulo.codeEditor.impl.softwrap.SoftWrapPainter;
import consulo.codeEditor.impl.softwrap.SoftWrapsStorage;
import consulo.codeEditor.impl.softwrap.mapping.CachingSoftWrapDataMapper;
import consulo.codeEditor.impl.softwrap.mapping.SoftWrapApplianceManager;
import consulo.colorScheme.internal.FontPreferencesManager;

import java.awt.Graphics;

/**
 * Soft wrap model for a headless editor. Soft wrapping needs a view area width to wrap against, so it stays
 * off and the whole model degrades to a pass-through: no soft wrap is ever registered, which keeps
 * offset to logical to visual position mapping an identity.
 */
public class HeadlessSoftWrapModel extends CodeEditorSoftWrapModelBase {
    public HeadlessSoftWrapModel(CodeEditorBase editor) {
        super(editor, Application.get().getInstance(FontPreferencesManager.class));
    }

    @Override
    protected SoftWrapApplianceManager createSoftWrapApplianceManager(
        SoftWrapsStorage storage,
        CodeEditorBase editor,
        SoftWrapPainter painter,
        CachingSoftWrapDataMapper dataMapper
    ) {
        return new SoftWrapApplianceManager(storage, editor, painter, dataMapper) {
        };
    }

    @Override
    public boolean isSoftWrappingEnabled() {
        return false;
    }

    @Override
    public void reinitSettings() {
    }

    @Override
    public int doPaint(Graphics g, SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
        return 0;
    }

    @Override
    public int getMinDrawingWidthInPixels(SoftWrapDrawingType drawingType) {
        return 0;
    }

    @Override
    public boolean isDirty() {
        return false;
    }
}

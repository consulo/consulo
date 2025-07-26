// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl.internal.textEditor;

import consulo.codeEditor.*;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class TextComponentInlayModel implements InlayModel {
    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, boolean relatesToPrecedingText, int priority, @Nonnull T renderer) {
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, boolean relatesToPrecedingText, @Nonnull T renderer) {
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, @Nonnull InlayProperties properties, @Nonnull T renderer) {
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset, boolean relatesToPrecedingText, boolean showAbove, int priority, @Nonnull T renderer) {
        return null;
    }

    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset, @Nonnull InlayProperties properties, @Nonnull T renderer) {
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset, boolean relatesToPrecedingText, @Nonnull T renderer) {
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset, @Nonnull InlayProperties properties, @Nonnull T renderer) {
        return null;
    }

    @Nonnull
    @Override
    public List<Inlay<?>> getInlineElementsInRange(int startOffset, int endOffset) {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Inlay<?>> getBlockElementsInRange(int startOffset, int endOffset) {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Inlay<?>> getBlockElementsForVisualLine(int visualLine, boolean above) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasInlineElementAt(int offset) {
        return false;
    }

    @Nullable
    @Override
    public Inlay getInlineElementAt(@Nonnull VisualPosition visualPosition) {
        return null;
    }

    @Nullable
    @Override
    public Inlay getElementAt(@Nonnull Point point) {
        return null;
    }

    @Nonnull
    @Override
    public List<Inlay<?>> getAfterLineEndElementsInRange(int startOffset, int endOffset) {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Inlay<?>> getAfterLineEndElementsForLogicalLine(int logicalLine) {
        return Collections.emptyList();
    }

    @Override
    public void setConsiderCaretPositionOnDocumentUpdates(boolean enabled) {
    }

    @Override
    public void addListener(@Nonnull Listener listener, @Nonnull Disposable disposable) {
    }
}

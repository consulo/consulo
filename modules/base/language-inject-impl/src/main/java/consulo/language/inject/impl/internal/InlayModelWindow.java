// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.codeEditor.*;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

class InlayModelWindow implements InlayModel {
    private static final Logger LOG = Logger.getInstance(InlayModelWindow.class);

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, boolean relatesToPrecedingText, int priority, @Nonnull T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, boolean relatesToPrecedingText, @Nonnull T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, @Nonnull InlayProperties properties, @Nonnull T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset, boolean relatesToPrecedingText, boolean showAbove, int priority, @Nonnull T renderer) {
        logUnsupported();
        return null;
    }

    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset, @Nonnull InlayProperties properties, @Nonnull T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset, boolean relatesToPrecedingText, @Nonnull T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset, @Nonnull InlayProperties properties, @Nonnull T renderer) {
        logUnsupported();
        return null;
    }

    @Nonnull
    @Override
    public List<Inlay<?>> getInlineElementsInRange(int startOffset, int endOffset) {
        logUnsupported();
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Inlay> getBlockElementsInRange(int startOffset, int endOffset) {
        logUnsupported();
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Inlay> getBlockElementsForVisualLine(int visualLine, boolean above) {
        logUnsupported();
        return Collections.emptyList();
    }

    @Override
    public boolean hasInlineElementAt(int offset) {
        logUnsupported();
        return false;
    }

    @Nullable
    @Override
    public Inlay getInlineElementAt(@Nonnull VisualPosition visualPosition) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public Inlay getElementAt(@Nonnull Point point) {
        logUnsupported();
        return null;
    }

    @Nonnull
    @Override
    public List<Inlay<?>> getAfterLineEndElementsInRange(int startOffset, int endOffset) {
        logUnsupported();
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<Inlay> getAfterLineEndElementsForLogicalLine(int logicalLine) {
        logUnsupported();
        return Collections.emptyList();
    }

    @Override
    public void setConsiderCaretPositionOnDocumentUpdates(boolean enabled) {
        logUnsupported();
    }

    @Override
    public void addListener(@Nonnull Listener listener, @Nonnull Disposable disposable) {
        logUnsupported();
    }

    private static void logUnsupported() {
        LOG.error("Inlay operations are not supported for injected editors");
    }
}

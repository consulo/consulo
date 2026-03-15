// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.inject.impl.internal;

import consulo.codeEditor.*;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

class InlayModelWindow implements InlayModel {
    private static final Logger LOG = Logger.getInstance(InlayModelWindow.class);

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, boolean relatesToPrecedingText, int priority, T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, boolean relatesToPrecedingText, T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addInlineElement(int offset, InlayProperties properties, T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset, boolean relatesToPrecedingText, boolean showAbove, int priority, T renderer) {
        logUnsupported();
        return null;
    }

    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addBlockElement(int offset, InlayProperties properties, T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset, boolean relatesToPrecedingText, T renderer) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public <T extends EditorCustomElementRenderer> Inlay<T> addAfterLineEndElement(int offset, InlayProperties properties, T renderer) {
        logUnsupported();
        return null;
    }

    
    @Override
    public List<Inlay<?>> getInlineElementsInRange(int startOffset, int endOffset) {
        logUnsupported();
        return Collections.emptyList();
    }

    
    @Override
    public List<Inlay<?>> getBlockElementsInRange(int startOffset, int endOffset) {
        logUnsupported();
        return Collections.emptyList();
    }

    
    @Override
    public List<Inlay<?>> getBlockElementsForVisualLine(int visualLine, boolean above) {
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
    public Inlay getInlineElementAt(VisualPosition visualPosition) {
        logUnsupported();
        return null;
    }

    @Nullable
    @Override
    public Inlay getElementAt(Point point) {
        logUnsupported();
        return null;
    }

    
    @Override
    public List<Inlay<?>> getAfterLineEndElementsInRange(int startOffset, int endOffset) {
        logUnsupported();
        return Collections.emptyList();
    }

    
    @Override
    public List<Inlay<?>> getAfterLineEndElementsForLogicalLine(int logicalLine) {
        logUnsupported();
        return Collections.emptyList();
    }

    @Override
    public void setConsiderCaretPositionOnDocumentUpdates(boolean enabled) {
        logUnsupported();
    }

    @Override
    public void addListener(Listener listener, Disposable disposable) {
        logUnsupported();
    }

    @Override
    public void execute(boolean batchMode, Runnable operation) {
        logUnsupported();
        operation.run();
    }

    private static void logUnsupported() {
        LOG.error("Inlay operations are not supported for injected editors");
    }
}

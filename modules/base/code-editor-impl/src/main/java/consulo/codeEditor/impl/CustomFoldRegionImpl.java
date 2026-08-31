// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.CustomFoldRegionRenderer;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.event.FoldingListener;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public final class CustomFoldRegionImpl extends FoldRegionImpl implements CustomFoldRegion {
    private final CustomFoldRegionRenderer myRenderer;

    private int myWidthInPixels;
    private int myHeightInPixels;
    private GutterIconRenderer myGutterIconRenderer;

    CustomFoldRegionImpl(CodeEditorBase editor, int startOffset, int endOffset, CustomFoldRegionRenderer renderer) {
        super(editor, startOffset, endOffset, "", null, true);
        myRenderer = renderer;
        doUpdate();
    }

    @Override
    void alignToValidBoundaries() {
        Document document = getDocument();
        int startOffset = intervalStart();
        int endOffset = intervalEnd();
        if (startOffset == DocumentUtil.getLineStartOffset(startOffset, document) &&
            endOffset == DocumentUtil.getLineEndOffset(endOffset, document)) {
            myEditor.getFoldingModel().addAffectedCustomRegions(this);
        }
        else {
            invalidate();
        }
    }

    @Override
    public CustomFoldRegionRenderer getRenderer() {
        return myRenderer;
    }

    @Override
    public int getWidthInPixels() {
        return myWidthInPixels;
    }

    @Override
    public int getHeightInPixels() {
        return myHeightInPixels;
    }

    @Override
    public @Nullable GutterIconRenderer getGutterIconRenderer() {
        return myGutterIconRenderer;
    }

    @Override
    public void update() {
        UIAccess.assertIsUIThread();
        if (myEditor.isDisposed() || !isValid()) {
            return;
        }
        if (myEditor.myDocumentChangeInProgress) {
            throw new IllegalStateException("Custom fold region shouldn't be updated during document change");
        }
        int oldWidth = myWidthInPixels;
        int oldHeight = myHeightInPixels;
        GutterIconRenderer oldIconRenderer = myGutterIconRenderer;
        doUpdate();
        int changeFlags = 0;
        if (oldWidth != myWidthInPixels) {
            changeFlags |= FoldingListener.ChangeFlags.WIDTH_CHANGED;
        }
        if (oldHeight != myHeightInPixels) {
            changeFlags |= FoldingListener.ChangeFlags.HEIGHT_CHANGED;
        }
        if (!Objects.equals(oldIconRenderer, myGutterIconRenderer)) {
            changeFlags |= FoldingListener.ChangeFlags.GUTTER_ICON_PROVIDER_CHANGED;
        }
        if (changeFlags != 0) {
            myEditor.getFoldingModel().onCustomFoldRegionPropertiesChange(this, changeFlags);
        }
        else {
            repaint();
        }
    }

    @Override
    public void repaint() {
        if (isValid() && !myEditor.isDisposed()) {
            if (myEditor.getFoldingModel().isInBatchFoldingOperation()) {
                myEditor.getFoldingModel().setRepaintRequested(true);
            }
            else {
                JComponent component = myEditor.getContentComponent();
                if (component.isShowing()) {
                    Point location = getLocation();
                    if (location != null) {
                        component.repaint(0, location.y, component.getWidth(), myHeightInPixels);
                    }
                }
            }
        }
    }

    private void doUpdate() {
        myWidthInPixels = Math.max(0, myRenderer.calcWidthInPixels(this));
        myHeightInPixels = Math.max(myEditor.getLineHeight(), myRenderer.calcHeightInPixels(this));
        myGutterIconRenderer = myRenderer.calcGutterIconRenderer(this);
    }

    @Override
    public Point getLocation() {
        int startOffset = getStartOffset();
        FoldRegion visibleRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        return visibleRegion == this ? new Point(myEditor.getContentComponent().getInsets().left,
            myEditor.visualLineToY(myEditor.offsetToVisualLine(startOffset)))
            : null;
    }

    @Override
    public String toString() {
        return super.toString() + ", renderer: " + myRenderer + ", size: " + myWidthInPixels + "x" + myHeightInPixels;
    }
}

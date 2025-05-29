// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayProperties;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.markup.GutterIconRenderer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;
import java.util.function.IntSupplier;

public class BlockInlayImpl<R extends EditorCustomElementRenderer> extends InlayImpl<R, BlockInlayImpl> implements IntSupplier {
    public final boolean myShowAbove;
    public final boolean myShowWhenFolded;
    public final int myPriority;
    private int myHeightInPixels;
    private GutterIconRenderer myGutterIconRenderer;

    public BlockInlayImpl(@Nonnull CodeEditorBase editor,
                          int offset,
                          boolean relatesToPrecedingText,
                          boolean showAbove,
                          boolean showWhenFolded,
                          int priority,
                          @Nonnull R renderer) {
        super(editor, offset, relatesToPrecedingText, renderer);
        myShowAbove = showAbove;
        myShowWhenFolded = showWhenFolded;
        myPriority = priority;
    }

    public int getPriority() {
        return myPriority;
    }

    @Override
    MarkerTreeWithPartialSums<BlockInlayImpl> getTree() {
        return myEditor.getInlayModel().myBlockElementsTree;
    }

    @Override
    @Nullable
    public GutterIconRenderer getGutterIconRenderer() {
        return myGutterIconRenderer;
    }

    @Override
    void doUpdate() {
        myWidthInPixels = myRenderer.calcWidthInPixels(this);
        if (myWidthInPixels < 0) {
            throw new IllegalArgumentException("Non-negative width should be defined for a block element");
        }
        int oldHeightInPixels = myHeightInPixels;
        myHeightInPixels = myRenderer.calcHeightInPixels(this);
        if (oldHeightInPixels != myHeightInPixels) {
            getTree().valueUpdated(this);
        }
        if (myHeightInPixels < 0) {
            throw new IllegalArgumentException("Non-negative height should be defined for a block element");
        }
        myGutterIconRenderer = myRenderer.calcGutterIconRenderer(this);
    }

    @Override
    Point getPosition() {
        int visualLine = myEditor.offsetToVisualLine(getOffset());
        int y = myEditor.visualLineToY(visualLine);
        List<Inlay> allInlays = myEditor.getInlayModel().getBlockElementsForVisualLine(visualLine, myShowAbove);
        if (myShowAbove) {
            boolean found = false;
            for (Inlay inlay : allInlays) {
                if (inlay == this) {
                    found = true;
                }
                if (found) {
                    y -= inlay.getHeightInPixels();
                }
            }
        }
        else {
            y += myEditor.getLineHeight();
            for (Inlay inlay : allInlays) {
                if (inlay == this) {
                    break;
                }
                y += inlay.getHeightInPixels();
            }
        }
        return new Point(myEditor.getContentComponent().getInsets().left, y);
    }

    @Override
    public int getHeightInPixels() {
        return myHeightInPixels;
    }

    @Nonnull
    @Override
    public Placement getPlacement() {
        return myShowAbove ? Placement.ABOVE_LINE : Placement.BELOW_LINE;
    }

    @Nonnull
    @Override
    public VisualPosition getVisualPosition() {
        return myEditor.offsetToVisualPosition(getOffset());
    }

    @Override
    public int getAsInt() {
        return myHeightInPixels;
    }

    @Override
    @Nonnull
    public InlayProperties getProperties() {
        return new InlayProperties()
            .relatesToPrecedingText(isRelatedToPrecedingText())
            .showAbove(myShowAbove)
            .showWhenFolded(myShowWhenFolded)
            .priority(myPriority);
    }

    @Override
    public String toString() {
        return "[Block inlay, offset=" + getOffset() + ", width=" + myWidthInPixels + ", height=" + myHeightInPixels + ", renderer=" + myRenderer + "]";
    }
}

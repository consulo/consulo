// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.application.ReadAction;
import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayProperties;
import consulo.codeEditor.VisualPosition;
import consulo.document.impl.RangeMarkerTree;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.List;

public class AfterLineEndInlayImpl<R extends EditorCustomElementRenderer> extends InlayImpl<R, AfterLineEndInlayImpl<?>> {
    private static int ourGlobalCounter = 0;
    final boolean mySoftWrappable;
    final int myPriority;
    final int myOrder;

    AfterLineEndInlayImpl(@Nonnull CodeEditorBase editor,
                          int offset,
                          boolean relatesToPrecedingText,
                          boolean softWrappable,
                          int priority,
                          @Nonnull R renderer) {
        super(editor, offset, relatesToPrecedingText, renderer);
        mySoftWrappable = softWrappable;
        myPriority = priority;
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        myOrder = ourGlobalCounter++;
    }

    @Override
    RangeMarkerTree<AfterLineEndInlayImpl<?>> getTree() {
        return myEditor.getInlayModel().myAfterLineEndElementsTree;
    }

    @Override
    void doUpdate() {
        myWidthInPixels = myRenderer.calcWidthInPixels(this);
        if (myWidthInPixels <= 0) {
            throw new IllegalArgumentException("Positive width should be defined for an after-line-end element");
        }
    }

    @Override
    Point getPosition() {
        VisualPosition pos = ReadAction.compute(() -> getVisualPosition());
        return myEditor.visualPositionToXY(pos);
    }

    @Nonnull
    @Override
    public Placement getPlacement() {
        return Placement.AFTER_LINE_END;
    }

    @Nonnull
    @Override
    public VisualPosition getVisualPosition() {
        int offset = getOffset();
        int logicalLine = myEditor.getDocument().getLineNumber(offset);
        int lineEndOffset = myEditor.getDocument().getLineEndOffset(logicalLine);
        VisualPosition position = myEditor.offsetToVisualPosition(lineEndOffset, true, true);
        if (myEditor.getFoldingModel().isOffsetCollapsed(lineEndOffset)) {
            return position;
        }
        List<Inlay<?>> inlays = myEditor.getInlayModel().getAfterLineEndElementsForLogicalLine(logicalLine);
        int order = inlays.indexOf(this);
        return new VisualPosition(position.line, position.column + 1 + order);
    }

    @Override
    public int getHeightInPixels() {
        return myEditor.getLineHeight();
    }

    @Override
    @Nonnull
    public InlayProperties getProperties() {
        return new InlayProperties()
            .relatesToPrecedingText(isRelatedToPrecedingText())
            .disableSoftWrapping(!mySoftWrappable)
            .priority(myPriority);
    }

    @Override
    public String toString() {
        return "[After-line-end inlay, offset=" + getOffset() + ", width=" + myWidthInPixels + ", renderer=" + myRenderer + "]";
    }
}

// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.codeEditor.*;
import consulo.document.Document;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

public final class EditorLocation {
    private final RealEditor myEditor;
    private final Point myPoint;
    private VisualPosition myVisualPosition;
    private LogicalPosition myLogicalPosition;
    private int myOffset = -1;
    private int[] myVisualLineYRange;
    private FoldRegion myCollapsedRegion = NO_REGION;

    public EditorLocation(@Nonnull RealEditor editor, @Nonnull Point point) {
        myEditor = editor;
        myPoint = point;
    }

    @Nonnull
    public Point getPoint() {
        return myPoint;
    }

    @Nonnull
    public VisualPosition getVisualPosition() {
        if (myVisualPosition == null) {
            myVisualPosition = myEditor.xyToVisualPosition(myPoint);
        }
        return myVisualPosition;
    }

    public int getVisualLineStartY() {
        if (myVisualLineYRange == null) {
            myVisualLineYRange = myEditor.visualLineToYRange(getVisualPosition().line);
        }
        return myVisualLineYRange[0];
    }

    public int getVisualLineEndY() {
        if (myVisualLineYRange == null) {
            myVisualLineYRange = myEditor.visualLineToYRange(getVisualPosition().line);
        }
        return myVisualLineYRange[1];
    }

    @Nonnull
    public LogicalPosition getLogicalPosition() {
        if (myLogicalPosition == null) {
            myLogicalPosition = myEditor.visualToLogicalPosition(getVisualPosition());
        }
        return myLogicalPosition;
    }

    public int getOffset() {
        if (myOffset < 0) {
            myOffset = myEditor.logicalPositionToOffset(getLogicalPosition());
        }
        return myOffset;
    }

    FoldRegion getCollapsedRegion() {
        if (myCollapsedRegion == NO_REGION) {
            myCollapsedRegion = myEditor.getFoldingModel().getCollapsedRegionAtOffset(getOffset());
        }
        return myCollapsedRegion;
    }

    private static final FoldRegion NO_REGION = new FoldRegion() {
        @Override
        public boolean isExpanded() {
            return false;
        }

        @Override
        public void setExpanded(boolean expanded) {
        }

        @Override
        public @Nonnull String getPlaceholderText() {
            return "";
        }

        @Override
        public Editor getEditor() {
            return null;
        }

        @Override
        public @Nullable FoldingGroup getGroup() {
            return null;
        }

        @Override
        public boolean shouldNeverExpand() {
            return false;
        }

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public @Nonnull Document getDocument() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStartOffset() {
            return 0;
        }

        @Override
        public int getEndOffset() {
            return 0;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public void setGreedyToLeft(boolean greedy) {
        }

        @Override
        public void setGreedyToRight(boolean greedy) {
        }

        @Override
        public boolean isGreedyToRight() {
            return false;
        }

        @Override
        public boolean isGreedyToLeft() {
            return false;
        }

        @Override
        public void dispose() {
        }

        @Override
        @Nullable
        public <T> T getUserData(@Nonnull Key<T> key) {
            return null;
        }

        @Override
        public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
        }
    };
}

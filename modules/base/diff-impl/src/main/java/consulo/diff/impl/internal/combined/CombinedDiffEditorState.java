// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.fileEditor.text.TextEditorState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

final class CombinedDiffEditorState implements FileEditorState {
    private final Set<CombinedBlockId> myCurrentBlockIds;
    private final @Nullable CombinedBlockId myActiveBlockId;
    private final List<TextEditorState> myActiveEditorStates;

    CombinedDiffEditorState(Set<CombinedBlockId> currentBlockIds,
                            @Nullable CombinedBlockId activeBlockId,
                            List<TextEditorState> activeEditorStates) {
        myCurrentBlockIds = currentBlockIds;
        myActiveBlockId = activeBlockId;
        myActiveEditorStates = activeEditorStates;
    }

    Set<CombinedBlockId> getCurrentBlockIds() {
        return myCurrentBlockIds;
    }

    @Nullable CombinedBlockId getActiveBlockId() {
        return myActiveBlockId;
    }

    List<TextEditorState> getActiveEditorStates() {
        return myActiveEditorStates;
    }

    @Override
    public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
        if (!(otherState instanceof CombinedDiffEditorState other)) {
            return false;
        }
        if (!myCurrentBlockIds.equals(other.myCurrentBlockIds)) {
            return true;
        }
        if (!Objects.equals(myActiveBlockId, other.myActiveBlockId)) {
            return false;
        }
        int size = Math.min(myActiveEditorStates.size(), other.myActiveEditorStates.size());
        for (int i = 0; i < size; i++) {
            if (!myActiveEditorStates.get(i).canBeMergedWith(other.myActiveEditorStates.get(i), level)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CombinedDiffEditorState that
            && myCurrentBlockIds.equals(that.myCurrentBlockIds)
            && Objects.equals(myActiveBlockId, that.myActiveBlockId)
            && myActiveEditorStates.equals(that.myActiveEditorStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myCurrentBlockIds, myActiveBlockId, myActiveEditorStates);
    }
}

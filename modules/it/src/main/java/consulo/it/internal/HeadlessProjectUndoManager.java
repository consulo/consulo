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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.document.DocumentReference;
import consulo.localize.LocalizeValue;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoableAction;
import consulo.util.lang.Couple;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Records nothing: nothing can be undone headless.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ServiceImpl
@Singleton
public class HeadlessProjectUndoManager implements ProjectUndoManager {
    @Override
    public void undoableActionPerformed(UndoableAction action) {
    }

    @Override
    public void nonundoableActionPerformed(DocumentReference ref, boolean isGlobal) {
    }

    @Override
    public boolean isUndoInProgress() {
        return false;
    }

    @Override
    public boolean isRedoInProgress() {
        return false;
    }

    @Override
    public void undo(@Nullable Object editor) {
    }

    @Override
    public void redo(@Nullable Object editor) {
    }

    @Override
    public boolean isUndoAvailable(@Nullable Object editor) {
        return false;
    }

    @Override
    public boolean isRedoAvailable(@Nullable Object editor) {
        return false;
    }

    @Override
    public Couple<LocalizeValue> getUndoActionNameAndDescription(Object editor) {
        return Couple.of(LocalizeValue.empty(), LocalizeValue.empty());
    }

    @Override
    public Couple<LocalizeValue> getRedoActionNameAndDescription(Object editor) {
        return Couple.of(LocalizeValue.empty(), LocalizeValue.empty());
    }

    @Override
    public void invalidateActionsFor(DocumentReference ref) {
    }

    @Override
    public boolean canMergeGroup(Object groupId, Object lastGroupId) {
        return false;
    }
}

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
package consulo.language.editor.refactoring.safeDelete;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.ConflictsDialogDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.action.DialogOkAction;

import java.util.Collection;

/**
 * Lists the usages which make a safe delete unsafe and asks whether to delete anyway. Where the usage view is
 * available, the usages can be shown there instead.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public class UnsafeUsagesDialogDescriptor extends ConflictsDialogDescriptor {
    private final Runnable myViewUsages;

    public UnsafeUsagesDialogDescriptor(Project project, Collection<LocalizeValue> conflictDescriptions, Runnable viewUsages) {
        super(project, RefactoringLocalize.usagesDetected(), conflictDescriptions);
        myViewUsages = viewUsages;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(RefactoringLocalize.deleteAnywayButton());
    }

    @Override
    protected LocalizeValue getShowInViewText() {
        return RefactoringLocalize.viewUsages();
    }

    @Override
    @RequiredUIAccess
    protected void showInView() {
        myViewUsages.run();
    }
}

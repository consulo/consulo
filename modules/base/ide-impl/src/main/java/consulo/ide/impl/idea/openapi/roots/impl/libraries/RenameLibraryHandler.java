/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.impl.libraries;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.content.library.Library;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.TitledHandler;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.InputValidator;
import consulo.ui.TextBox;
import consulo.undoRedo.*;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class RenameLibraryHandler implements RenameHandler, TitledHandler {
    private static final Logger LOG = Logger.getInstance(RenameLibraryHandler.class);

    @Override
    public boolean isAvailableOnDataContext(DataContext dataContext) {
        Library library = dataContext.getData(Library.KEY);
        return library != null;
    }

    @Override
    public boolean isRenaming(DataContext dataContext) {
        return isAvailableOnDataContext(dataContext);
    }

    @RequiredUIAccess
    @Override
    public void invoke(Project project, Editor editor, PsiFile file, DataContext dataContext) {
        LOG.assertTrue(false);
    }

    @RequiredUIAccess
    @Override
    public void invoke(Project project, PsiElement[] elements, DataContext dataContext) {
        Library library = dataContext.getData(Library.KEY);
        LOG.assertTrue(library != null);
        InputBoxBuilder.text()
            .asQuestion()
            .title(IdeLocalize.titleRenameLibrary())
            .text(IdeLocalize.promptEnterNewLibraryName())
            .value(StringUtil.notNullize(library.getName()))
            .setupComponent(TextBox::selectAll)
            .validator(new LibraryNameValidator(project, library))
            .showAsync(project);
    }

    @Override
    public LocalizeValue getActionTitleValue() {
        return IdeLocalize.titleRenameLibrary();
    }

    private static class LibraryNameValidator implements InputValidator<String> {
        private final Project myProject;
        private final Library myLibrary;

        public LibraryNameValidator(Project project, Library library) {
            myProject = project;
            myLibrary = library;
        }

        @Override
        @RequiredUIAccess
        public @Nullable InputProblem validate(String inputString) {
            if (inputString.isEmpty() || myLibrary.getTable().getLibraryByName(inputString) != null) {
                return InputProblem.error(LocalizeValue.empty());
            }
            return null;
        }

        @Override
        @RequiredUIAccess
        public CompletableFuture<@Nullable InputProblem> confirm(String inputString) {
            String oldName = myLibrary.getName();
            Library.ModifiableModel modifiableModel = renameLibrary(inputString);
            CommandProcessor.getInstance().newCommand()
                .project(myProject)
                .name(IdeLocalize.commandRenamingModule(oldName))
                .run(() -> {
                    UndoableAction action = new BasicUndoableAction() {
                        @Override
                        public void undo() throws UnexpectedUndoException {
                            renameLibrary(oldName).commit();
                        }

                        @Override
                        public void redo() throws UnexpectedUndoException {
                            renameLibrary(inputString).commit();
                        }
                    };
                    ProjectUndoManager.getInstance(myProject).undoableActionPerformed(action);
                    myProject.getApplication().runWriteAction(() -> modifiableModel.commit());
                });
            return CompletableFuture.completedFuture(null);
        }

        private Library.ModifiableModel renameLibrary(String inputString) {
            Library.ModifiableModel modifiableModel = myLibrary.getModifiableModel();
            modifiableModel.setName(inputString);
            return modifiableModel;
        }
    }
}

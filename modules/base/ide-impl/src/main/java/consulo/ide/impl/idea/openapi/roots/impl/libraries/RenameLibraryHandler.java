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
import consulo.ide.impl.idea.ide.TitledHandler;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.*;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        LOG.assertTrue(false);
    }

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull final Project project, @Nonnull PsiElement[] elements, @Nonnull DataContext dataContext) {
        final Library library = dataContext.getData(Library.KEY);
        LOG.assertTrue(library != null);
        Messages.showInputDialog(
            project,
            IdeLocalize.promptEnterNewLibraryName().get(),
            IdeLocalize.titleRenameLibrary().get(),
            UIUtil.getQuestionIcon(),
            library.getName(),
            new MyInputValidator(project, library)
        );
    }

    @Nonnull
    @Override
    public LocalizeValue getActionTitleValue() {
        return IdeLocalize.titleRenameLibrary();
    }

    private static class MyInputValidator implements InputValidator {
        private final Project myProject;
        private final Library myLibrary;

        public MyInputValidator(Project project, Library library) {
            myProject = project;
            myLibrary = library;
        }

        @Override
        @RequiredUIAccess
        public boolean checkInput(String inputString) {
            return inputString != null && !inputString.isEmpty() && myLibrary.getTable().getLibraryByName(inputString) == null;
        }

        @Override
        @RequiredUIAccess
        public boolean canClose(final String inputString) {
            final String oldName = myLibrary.getName();
            final Library.ModifiableModel modifiableModel = renameLibrary(inputString);
            if (modifiableModel == null) {
                return false;
            }
            final Ref<Boolean> success = Ref.create(Boolean.TRUE);
            CommandProcessor.getInstance().executeCommand(
                myProject,
                new Runnable() {
                    @RequiredUIAccess
                    @Override
                    public void run() {
                        UndoableAction action = new BasicUndoableAction() {
                            @Override
                            public void undo() throws UnexpectedUndoException {
                                final Library.ModifiableModel modifiableModel = renameLibrary(oldName);
                                if (modifiableModel != null) {
                                    modifiableModel.commit();
                                }
                            }

                            @Override
                            public void redo() throws UnexpectedUndoException {
                                final Library.ModifiableModel modifiableModel = renameLibrary(inputString);
                                if (modifiableModel != null) {
                                    modifiableModel.commit();
                                }
                            }
                        };
                        ProjectUndoManager.getInstance(myProject).undoableActionPerformed(action);
                        myProject.getApplication().runWriteAction(() -> modifiableModel.commit());
                    }
                },
                IdeLocalize.commandRenamingModule(oldName).get(),
                null
            );
            return success.get();
        }

        @Nullable
        private Library.ModifiableModel renameLibrary(String inputString) {
            final Library.ModifiableModel modifiableModel = myLibrary.getModifiableModel();
            modifiableModel.setName(inputString);
            return modifiableModel;
        }
    }
}

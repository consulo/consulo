/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.application.AllIcons;
import consulo.application.Result;
import consulo.application.RunResult;
import consulo.language.Language;
import consulo.language.editor.WriteCommandAction;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author gregsh
 */
@ExtensionImpl
public final class ScratchRootType extends RootType {
    @Nonnull
    public static ScratchRootType getInstance() {
        return findByClass(ScratchRootType.class);
    }

    @Inject
    ScratchRootType() {
        super("scratches", "Scratches");
    }

    @Override
    public Language substituteLanguage(@Nonnull Project project, @Nonnull VirtualFile file) {
        return ScratchFileService.getInstance().getScratchesMapping().getMapping(file);
    }

    @Nullable
    @Override
    public Image substituteIcon(@Nonnull Project project, @Nonnull VirtualFile file) {
        Image icon = ObjectUtil.chooseNotNull(super.substituteIcon(project, file), AllIcons.FileTypes.Text);
        return ImageEffects.layered(icon, AllIcons.Actions.Scratch);
    }

    @Nullable
    @RequiredUIAccess
    public VirtualFile createScratchFile(Project project, final String fileName, final Language language, final String text) {
        return createScratchFile(project, fileName, language, text, ScratchFileService.Option.create_new_always);
    }

    @Nullable
    @RequiredUIAccess
    public VirtualFile createScratchFile(
        Project project,
        final String fileName,
        final Language language,
        final String text,
        final ScratchFileService.Option option
    ) {
        RunResult<VirtualFile> result =
            new WriteCommandAction<VirtualFile>(project, UILocalize.fileChooserCreateNewFileCommandName().get()) {
                @Override
                protected boolean isGlobalUndoAction() {
                    return true;
                }

                @Override
                protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
                    return UndoConfirmationPolicy.REQUEST_CONFIRMATION;
                }

                @Override
                protected void run(@Nonnull Result<VirtualFile> result) throws Throwable {
                    ScratchFileService fileService = ScratchFileService.getInstance();
                    VirtualFile file = fileService.findFile(ScratchRootType.this, fileName, option);
                    fileService.getScratchesMapping().setMapping(file, language);
                    VfsUtil.saveText(file, text);
                    result.setResult(file);
                }
            }.execute();
        if (result.hasException()) {
            Messages.showMessageDialog(
                UILocalize.createNewFileCouldNotCreateFileErrorMessage(fileName).get(),
                UILocalize.errorDialogTitle().get(),
                UIUtil.getErrorIcon()
            );
            return null;
        }
        return result.getResultObject();
    }
}

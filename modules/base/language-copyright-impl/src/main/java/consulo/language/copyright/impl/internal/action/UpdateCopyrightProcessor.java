/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.copyright.impl.internal.action;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.UpdatePsiFileCopyright;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.copyright.localize.LanguageCopyrightLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.VirtualFile;

public class UpdateCopyrightProcessor extends AbstractFileProcessor {
    private static final Logger logger = Logger.getInstance(UpdateCopyrightProcessor.class);

    public UpdateCopyrightProcessor(Project project, Module module) {
        super(
            project,
            module,
            LanguageCopyrightLocalize.actionUpdateCopyrightText(),
            LanguageCopyrightLocalize.updatingCopyrightsProgressMessage()
        );
    }

    public UpdateCopyrightProcessor(Project project, Module module, PsiDirectory dir, boolean subdirs) {
        super(
            project,
            module,
            dir,
            subdirs,
            LanguageCopyrightLocalize.actionUpdateCopyrightText(),
            LanguageCopyrightLocalize.updatingCopyrightsProgressMessage()
        );
    }

    public UpdateCopyrightProcessor(Project project, Module module, PsiFile file) {
        super(
            project,
            module,
            file,
            LanguageCopyrightLocalize.actionUpdateCopyrightText(),
            LanguageCopyrightLocalize.updatingCopyrightsProgressMessage()
        );
    }

    public UpdateCopyrightProcessor(Project project, Module module, PsiFile[] files) {
        super(
            project,
            module,
            files,
            LanguageCopyrightLocalize.actionUpdateCopyrightText(),
            LanguageCopyrightLocalize.updatingCopyrightsProgressMessage()
        );
    }

    @Override
    protected Runnable preprocessFile(PsiFile file) throws IncorrectOperationException {
        VirtualFile vfile = file.getVirtualFile();
        if (vfile == null) {
            return EmptyRunnable.getInstance();
        }
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (progressIndicator != null) {
            progressIndicator.setText2Value(LocalizeValue.of(vfile.getPresentableUrl()));
        }
        Module mod = myModule;
        if (myModule == null) {
            mod = ProjectRootManager.getInstance(myProject).getFileIndex().getModuleForFile(vfile);
        }

        if (mod == null) {
            return EmptyRunnable.getInstance();
        }

        UpdateCopyrightsProvider updateCopyrightsProvider = UpdateCopyrightsProvider.forFileType(file.getFileType());
        if (updateCopyrightsProvider == null) {
            return EmptyRunnable.getInstance();
        }

        CopyrightProfile copyrightProfile = CopyrightManager.getInstance(myProject).getCopyrightOptions(file);
        if (copyrightProfile != null && UpdateCopyrightsProvider.hasExtension(file)) {
            logger.debug("process " + file);
            UpdatePsiFileCopyright<?> updateCopyright = updateCopyrightsProvider.createInstance(file, copyrightProfile);

            return () -> {
                try {
                    updateCopyright.process();

                }
                catch (Exception e) {
                    logger.error(e);
                }
            };
        }
        else {
            return EmptyRunnable.getInstance();
        }
    }
}

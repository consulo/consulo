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
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.VirtualFile;

public class UpdateCopyrightProcessor extends AbstractFileProcessor {
  private static final Logger logger = Logger.getInstance(UpdateCopyrightProcessor.class);

  public static final String TITLE = "Update Copyright";
  public static final String DESCRIPTION = "Generate/Update the copyright notice.";
  public static final String MESSAGE = "Updating copyrights...";

  private Project project;
  private Module module;

  public UpdateCopyrightProcessor(Project project, Module module) {
    super(project, module, TITLE, MESSAGE);
    setup(project, module);
  }

  public UpdateCopyrightProcessor(Project project, Module module, PsiDirectory dir, boolean subdirs) {
    super(project, dir, subdirs, TITLE, MESSAGE);
    setup(project, module);
  }

  public UpdateCopyrightProcessor(Project project, Module module, PsiFile file) {
    super(project, file, TITLE, MESSAGE);
    setup(project, module);
  }

  public UpdateCopyrightProcessor(Project project, Module module, PsiFile[] files) {
    super(project, files, TITLE, MESSAGE, null);
    setup(project, module);
  }

  @Override
  protected Runnable preprocessFile(final PsiFile file) throws IncorrectOperationException {
    VirtualFile vfile = file.getVirtualFile();
    if (vfile == null) {
      return EmptyRunnable.getInstance();
    }
    final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    if (progressIndicator != null) {
      progressIndicator.setText2(vfile.getPresentableUrl());
    }
    Module mod = module;
    if (module == null) {
      mod = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(vfile);
    }

    if (mod == null) {
      return EmptyRunnable.getInstance();
    }

    UpdateCopyrightsProvider updateCopyrightsProvider = UpdateCopyrightsProvider.forFileType(file.getFileType());
    if(updateCopyrightsProvider == null) {
      return EmptyRunnable.getInstance();
    }

    CopyrightProfile copyrightProfile = CopyrightManager.getInstance(project).getCopyrightOptions(file);
    if (copyrightProfile != null && UpdateCopyrightsProvider.hasExtension(file)) {
      logger.debug("process " + file);
      final UpdatePsiFileCopyright<?> updateCopyright = updateCopyrightsProvider.createInstance(file, copyrightProfile);

      return new Runnable() {
        @Override
        public void run() {
          try {
            updateCopyright.process();

          }
          catch (Exception e) {
            logger.error(e);
          }
        }
      };
    }
    else {
      return EmptyRunnable.getInstance();
    }
  }

  private void setup(Project project, Module module) {
    this.project = project;
    this.module = module;
  }
}

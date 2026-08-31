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
package consulo.language.editor.impl.internal.daemon;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.FileDocumentManager;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactoryWithContext;
import consulo.language.editor.internal.SilentChangeVetoer;
import consulo.language.editor.Pass;
import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiFile;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import org.jspecify.annotations.Nullable;

/**
 * @author Alexey
 * @author cdr
 * @since 2006-11-25
 */
@ExtensionImpl
public class ShowAutoImportPassFactory implements TextEditorHighlightingPassFactoryWithContext<CanISilentlyChange.Result> {
  private final Project myProject;
  private final FileDocumentManager myFileDocumentManager;

  @Inject
  public ShowAutoImportPassFactory(Project project, FileDocumentManager fileDocumentManager) {
    myProject = project;
    myFileDocumentManager = fileDocumentManager;
  }

  @Override
  public void register(Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, new int[]{Pass.UPDATE_ALL,}, null, false, -1);
  }

  /**
   * Called on the EDT to capture only the undo-manager state (the cheap, EDT-required stage of the
   * "can I silently change" computation). The expensive VCS check runs later on a background thread
   * inside {@link #createHighlightingPass}.
   */
  @RequiredUIAccess
  @Override
  public CanISilentlyChange.@Nullable Result getContextFromUI(Editor editor) {
    VirtualFile virtualFile = myFileDocumentManager.getFile(editor.getDocument());
    return CanISilentlyChange.thisFile(myProject, virtualFile);
  }

  @Override
  public @Nullable TextEditorHighlightingPass createHighlightingPass(PsiFile file, Editor editor, CanISilentlyChange.@Nullable Result context) {
    // PsiCodeFragment is always allowed — this PSI check is safe under the read action held on BGT.
    if (file instanceof PsiCodeFragment) {
      return new ShowAutoImportPass(file.getProject(), file, editor);
    }
    if (context == null) {
      return null;
    }
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    // VCS dirty-scope query and index access run here on the background thread, not on the EDT.
    ThreeState extensionsAllow = SilentChangeVetoer.extensionsAllowToChangeFileSilently(file.getProject(), virtualFile);
    boolean isInContent = ModuleUtilCore.projectContainsFile(file.getProject(), virtualFile, false);
    return context.canIReally(isInContent, extensionsAllow) ? new ShowAutoImportPass(file.getProject(), file, editor) : null;
  }
}

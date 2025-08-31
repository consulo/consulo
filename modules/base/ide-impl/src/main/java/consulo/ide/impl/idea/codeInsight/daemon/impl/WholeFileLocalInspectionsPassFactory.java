/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposer;
import consulo.document.util.ProperTextRange;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.Pass;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.impl.highlight.VisibleHighlightingPassFactory;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.impl.internal.highlight.DefaultHighlightInfoProcessor;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfileWrapper;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.inspection.scheme.event.ProfileChangeAdapter;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cdr
 */
@ExtensionImpl
public class WholeFileLocalInspectionsPassFactory implements TextEditorHighlightingPassFactory {
  private final Map<PsiFile, Boolean> myFileToolsCache = ContainerUtil.createConcurrentWeakMap();
  private final Project myProject;

  private volatile long myPsiModificationCount;

  @Inject
  public WholeFileLocalInspectionsPassFactory(Project project) {
    myProject = project;

    myProject.getMessageBus().connect().subscribe(ProfileChangeAdapter.class, new ProfileChangeAdapter() {
      @Override
      public void profileChanged(@Nullable Profile profile) {
        myFileToolsCache.clear();
      }

      @Override
      public void profileActivated(@Nullable Profile oldProfile, @Nullable Profile profile) {
        myFileToolsCache.clear();
      }
    });

    Disposer.register(myProject, myFileToolsCache::clear);
  }

  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(
      this,
      null,
      new int[]{Pass.LOCAL_INSPECTIONS},
      true,
      Pass.WHOLE_FILE_LOCAL_INSPECTIONS
    );
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    long psiModificationCount = PsiManager.getInstance(myProject).getModificationTracker().getModificationCount();
    if (psiModificationCount == myPsiModificationCount) {
      return null; //optimization
    }

    if (myFileToolsCache.containsKey(file) && !myFileToolsCache.get(file)) {
      return null;
    }
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new LocalInspectionsPass(
      file,
      editor.getDocument(),
      0,
      file.getTextLength(),
      visibleRange,
      true,
      new DefaultHighlightInfoProcessor()
    ) {
      @Nonnull
      @Override
      List<LocalInspectionToolWrapper> getInspectionTools(@Nonnull InspectionProfileWrapper profile) {
        List<LocalInspectionToolWrapper> tools = super.getInspectionTools(profile);
        List<LocalInspectionToolWrapper> result = tools.stream()
          .filter(LocalInspectionToolWrapper::runForWholeFile)
          .collect(Collectors.toList());
        myFileToolsCache.put(file, !result.isEmpty());
        return result;
      }

      @Override
      public String getPresentableName() {
        return DaemonLocalize.passWholeInspections().get();
      }

      @Override
      void inspectInjectedPsi(
        @Nonnull List<PsiElement> elements,
        boolean onTheFly,
        @Nonnull ProgressIndicator indicator,
        @Nonnull InspectionManager iManager,
        boolean inVisibleRange,
        @Nonnull List<LocalInspectionToolWrapper> wrappers
      ) {
        // already inspected in LIP
      }

      @Override
      protected void applyInformationWithProgress() {
        super.applyInformationWithProgress();
        myPsiModificationCount = PsiManager.getInstance(myProject).getModificationTracker().getModificationCount();
      }
    };
  }
}

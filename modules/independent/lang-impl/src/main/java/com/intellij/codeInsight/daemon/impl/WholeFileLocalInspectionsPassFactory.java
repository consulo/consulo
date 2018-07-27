/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ex.InspectionProfileWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.profile.Profile;
import com.intellij.profile.ProfileChangeAdapter;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ObjectIntMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author cdr
 */
public class WholeFileLocalInspectionsPassFactory implements TextEditorHighlightingPassFactory {
  private static final Key<Set<PsiFile>> ourSetKey = Key.create("Set<PsiFile>");
  private static final Key<ObjectIntMap<PsiFile>> ourPsiModificationKey = Key.create("ObjectIntMap<PsiFile>");

  @Override
  public void register(@Nonnull Project project, @Nonnull TextEditorHighlightingPassRegistrar registrar) {
    // can run in the same time with LIP, but should start after it, since I believe whole-file inspections would run longer
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.LOCAL_INSPECTIONS}, true, Pass.WHOLE_FILE_LOCAL_INSPECTIONS);

    // guarded by mySkipWholeInspectionsCache
    project.putUserData(ourSetKey, ContainerUtil.createWeakSet());
    // guarded by myPsiModificationCount
    project.putUserData(ourPsiModificationKey, ContainerUtil.createWeakKeyIntValueMap());

    project.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project p) {
        if (project == p) {
          WholeFileLocalInspectionsPassFactory.this.projectOpened(p);
        }
      }
    });
  }

  private void projectOpened(Project project) {
    InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(project);
    profileManager.addProfileChangeListener(new ProfileChangeAdapter() {
      @Override
      public void profileChanged(Profile profile) {
        clearSkipCache(project);
      }

      @Override
      public void profileActivated(Profile oldProfile, @Nullable Profile profile) {
        clearSkipCache(project);
      }
    }, project);

    Disposer.register(project, () -> {
      clearSkipCache(project);
    });
  }

  private void clearSkipCache(Project project) {
    Set<PsiFile> skipWholeInspectionsCache = project.getUserData(ourSetKey);
    assert skipWholeInspectionsCache != null;

    synchronized (skipWholeInspectionsCache) {
      skipWholeInspectionsCache.clear();
    }
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    Project project = file.getProject();

    ObjectIntMap<PsiFile> psiModificationCount = project.getUserData(ourPsiModificationKey);
    Set<PsiFile> skipWholeInspectionsCache = project.getUserData(ourSetKey);

    assert psiModificationCount != null;
    assert skipWholeInspectionsCache != null;

    long actualCount = PsiManager.getInstance(project).getModificationTracker().getModificationCount();
    synchronized (psiModificationCount) {
      if (psiModificationCount.get(file) == (int)actualCount) {
        return null; //optimization
      }
    }

    if (!ProblemHighlightFilter.shouldHighlightFile(file)) {
      return null;
    }

    synchronized (skipWholeInspectionsCache) {
      if (skipWholeInspectionsCache.contains(file)) {
        return null;
      }
    }
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new LocalInspectionsPass(file, editor.getDocument(), 0, file.getTextLength(), visibleRange, true, new DefaultHighlightInfoProcessor()) {
      @Nonnull
      @Override
      List<LocalInspectionToolWrapper> getInspectionTools(@Nonnull InspectionProfileWrapper profile) {
        List<LocalInspectionToolWrapper> tools = super.getInspectionTools(profile);
        List<LocalInspectionToolWrapper> result = ContainerUtil.filter(tools, LocalInspectionToolWrapper::runForWholeFile);
        if (result.isEmpty()) {
          synchronized (skipWholeInspectionsCache) {
            skipWholeInspectionsCache.add(file);
          }
        }
        return result;
      }

      @Override
      protected String getPresentableName() {
        return DaemonBundle.message("pass.whole.inspections");
      }

      @Override
      void inspectInjectedPsi(@Nonnull List<PsiElement> elements,
                              boolean onTheFly,
                              @Nonnull ProgressIndicator indicator,
                              @Nonnull InspectionManager iManager,
                              boolean inVisibleRange,
                              @Nonnull List<LocalInspectionToolWrapper> wrappers) {
        // already inspected in LIP
      }

      @Override
      protected void applyInformationWithProgress() {
        super.applyInformationWithProgress();
        long modificationCount = PsiManager.getInstance(myProject).getModificationTracker().getModificationCount();
        synchronized (psiModificationCount) {
          psiModificationCount.put(file, (int)modificationCount);
        }
      }
    };
  }
}
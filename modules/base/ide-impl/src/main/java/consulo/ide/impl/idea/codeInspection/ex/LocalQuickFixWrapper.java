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

package consulo.ide.impl.idea.codeInspection.ex;

import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.QuickFix;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.BatchQuickFix;
import consulo.language.editor.intention.IntentionWrapper;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public class LocalQuickFixWrapper extends QuickFixAction {
  private final QuickFix myFix;
  private String myText;

  public LocalQuickFixWrapper(@Nonnull QuickFix fix, @Nonnull InspectionToolWrapper toolWrapper) {
    super(fix.getName(), toolWrapper);
    myFix = fix;
    myText = myFix.getName();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    getTemplatePresentation().setText(myText);
    e.getPresentation().setText(myText);
  }

  @Override
  public String getText(RefEntity where) {
    return myText;
  }

  public void setText(@Nonnull String text) {
    myText = text;
  }


  @Override
  protected boolean isProblemDescriptorsAcceptable() {
    return true;
  }

  @Nonnull
  public QuickFix getFix() {
    return myFix;
  }

  @Nullable
  protected QuickFix getWorkingQuickFix(@Nonnull QuickFix[] fixes) {
    for (QuickFix fix : fixes) {
      if (!myFix.getClass().isInstance(fix)) continue;
      if (myFix instanceof IntentionWrapper && fix instanceof IntentionWrapper &&
          !((IntentionWrapper)myFix).getAction().getClass().isInstance(((IntentionWrapper)fix).getAction())) {
        continue;
      }
      return fix;
    }
    return null;
  }

  @Override
  protected boolean applyFix(@Nonnull RefEntity[] refElements) {
    return true;
  }

  @Override
  protected void applyFix(@Nonnull final Project project,
                          @Nonnull final GlobalInspectionContextImpl context,
                          @Nonnull final CommonProblemDescriptor[] descriptors,
                          @Nonnull final Set<PsiElement> ignoredElements) {
    final PsiModificationTracker tracker = PsiManager.getInstance(project).getModificationTracker();
    if (myFix instanceof BatchQuickFix) {
      final List<PsiElement> collectedElementsToIgnore = new ArrayList<PsiElement>();
      final Runnable refreshViews = new Runnable() {
        @Override
        public void run() {
          DaemonCodeAnalyzer.getInstance(project).restart();
          for (CommonProblemDescriptor descriptor : descriptors) {
            ignore(ignoredElements, descriptor, getWorkingQuickFix(descriptor.getFixes()), context);
          }

          final RefManager refManager = context.getRefManager();
          final RefElement[] refElements = new RefElement[collectedElementsToIgnore.size()];
          for (int i = 0, collectedElementsToIgnoreSize = collectedElementsToIgnore.size(); i < collectedElementsToIgnoreSize; i++) {
            refElements[i] = refManager.getReference(collectedElementsToIgnore.get(i));
          }

          removeElements(refElements, project, myToolWrapper);
        }
      };

      ((BatchQuickFix)myFix).applyFix(project, descriptors, collectedElementsToIgnore, refreshViews);
      return;
    }

    boolean restart = false;
    for (CommonProblemDescriptor descriptor : descriptors) {
      if (descriptor == null) continue;
      final QuickFix[] fixes = descriptor.getFixes();
      if (fixes != null) {
        final QuickFix fix = getWorkingQuickFix(fixes);
        if (fix != null) {
          final long startCount = tracker.getModificationCount();
          //CCE here means QuickFix was incorrectly inherited, is there a way to signal (plugin) it is wrong?
          fix.applyFix(project, descriptor);
          if (startCount != tracker.getModificationCount()) {
            restart = true;
            ignore(ignoredElements, descriptor, fix, context);
          }
        }
      }
    }
    if (restart) {
      DaemonCodeAnalyzer.getInstance(project).restart();
    }
  }

  private void ignore(@Nonnull Set<PsiElement> ignoredElements,
                      @Nonnull CommonProblemDescriptor descriptor,
                      @Nullable QuickFix fix,
                      @Nonnull GlobalInspectionContextImpl context) {
    if (fix != null) {
      InspectionToolPresentation presentation = context.getPresentation(myToolWrapper);
      presentation.ignoreProblem(descriptor, fix);
    }
    if (descriptor instanceof ProblemDescriptor) {
      ignoredElements.add(((ProblemDescriptor)descriptor).getPsiElement());
    }
  }
}
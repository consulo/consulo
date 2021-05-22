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

package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefManagerImpl;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.codeInspection.ui.InspectionTree;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.util.SequentialModalProgressTask;
import com.intellij.util.SequentialTask;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.*;

/**
 * @author max
 */
public class QuickFixAction extends AnAction {
  protected final InspectionToolWrapper myToolWrapper;

  public static InspectionResultsView getInvoker(AnActionEvent e) {
    return e.getData(InspectionResultsView.DATA_KEY);
  }

  protected QuickFixAction(String text, @Nonnull InspectionToolWrapper toolWrapper) {
    this(text, AllIcons.Actions.CreateFromUsage, null, toolWrapper);
  }

  protected QuickFixAction(String text, Image icon, KeyStroke keyStroke, @Nonnull InspectionToolWrapper toolWrapper) {
    super(text, null, icon);
    myToolWrapper = toolWrapper;
    if (keyStroke != null) {
      registerCustomShortcutSet(new CustomShortcutSet(keyStroke), null);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    final InspectionResultsView view = getInvoker(e);
    if (view == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    e.getPresentation().setVisible(false);
    e.getPresentation().setEnabled(false);

    final InspectionTree tree = view.getTree();
    final InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper();
    if (!view.isSingleToolInSelection() || toolWrapper != myToolWrapper) {
      return;
    }

    if (!isProblemDescriptorsAcceptable() && tree.getSelectedElements().length > 0 ||
        isProblemDescriptorsAcceptable() && tree.getSelectedDescriptors().length > 0) {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(true);
    }
  }

  protected boolean isProblemDescriptorsAcceptable() {
    return false;
  }

  public String getText(RefEntity where) {
    return getTemplatePresentation().getText();
  }

  @Override
  public void actionPerformed(final AnActionEvent e) {
    final InspectionResultsView view = getInvoker(e);
    final InspectionTree tree = view.getTree();
    if (isProblemDescriptorsAcceptable()) {
      final CommonProblemDescriptor[] descriptors = tree.getSelectedDescriptors();
      if (descriptors.length > 0) {
        doApplyFix(view.getProject(), descriptors, tree.getContext());
        return;
      }
    }

    doApplyFix(getSelectedElements(e), view);
  }


  protected void applyFix(@Nonnull Project project,
                          @Nonnull GlobalInspectionContextImpl context,
                          @Nonnull CommonProblemDescriptor[] descriptors,
                          @Nonnull Set<PsiElement> ignoredElements) {
  }

  private void doApplyFix(@Nonnull final Project project,
                          @Nonnull final CommonProblemDescriptor[] descriptors,
                          @Nonnull final GlobalInspectionContextImpl context) {
    final Set<VirtualFile> readOnlyFiles = new HashSet<VirtualFile>();
    for (CommonProblemDescriptor descriptor : descriptors) {
      final PsiElement psiElement = descriptor instanceof ProblemDescriptor ? ((ProblemDescriptor)descriptor).getPsiElement() : null;
      if (psiElement != null && !psiElement.isWritable()) {
        readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
      }
    }

    if (!FileModificationService.getInstance().prepareVirtualFilesForWrite(project, readOnlyFiles)) return;

    final RefManagerImpl refManager = (RefManagerImpl)context.getRefManager();

    final boolean initial = refManager.isInProcess();

    refManager.inspectionReadActionFinished();

    try {
      final Set<PsiElement> ignoredElements = new HashSet<PsiElement>();

      CommandProcessor.getInstance().executeCommand(project, new Runnable() {
        @Override
        public void run() {
          CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
              final SequentialModalProgressTask progressTask =
                new SequentialModalProgressTask(project, getTemplatePresentation().getText(), false);
              progressTask.setMinIterationTime(200);
              progressTask.setTask(new PerformFixesTask(project, descriptors, ignoredElements, progressTask, context));
              ProgressManager.getInstance().run(progressTask);
            }
          });
        }
      }, getTemplatePresentation().getText(), null);

      refreshViews(project, ignoredElements, myToolWrapper);
    }
    finally { //to make offline view lazy
      if (initial) refManager.inspectionReadActionStarted();
    }
  }

  public void doApplyFix(@Nonnull final RefEntity[] refElements, @Nonnull InspectionResultsView view) {
    final RefManagerImpl refManager = (RefManagerImpl)view.getGlobalInspectionContext().getRefManager();

    final boolean initial = refManager.isInProcess();

    refManager.inspectionReadActionFinished();

    try {
      final boolean[] refreshNeeded = {false};
      if (refElements.length > 0) {
        final Project project = refElements[0].getRefManager().getProject();
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            CommandProcessor.getInstance().markCurrentCommandAsGlobal(project);
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                refreshNeeded[0] = applyFix(refElements);
              }
            });
          }
        }, getTemplatePresentation().getText(), null);
      }
      if (refreshNeeded[0]) {
        refreshViews(view.getProject(), refElements, myToolWrapper);
      }
    }
    finally {  //to make offline view lazy
      if (initial) refManager.inspectionReadActionStarted();
    }
  }

  public static void removeElements(@Nonnull RefEntity[] refElements, @Nonnull Project project, @Nonnull InspectionToolWrapper toolWrapper) {
    refreshViews(project, refElements, toolWrapper);
    final ArrayList<RefElement> deletedRefs = new ArrayList<RefElement>(1);
    for (RefEntity refElement : refElements) {
      if (!(refElement instanceof RefElement)) continue;
      refElement.getRefManager().removeRefElement((RefElement)refElement, deletedRefs);
    }
  }

  private static Set<VirtualFile> getReadOnlyFiles(@Nonnull RefEntity[] refElements) {
    Set<VirtualFile> readOnlyFiles = new HashSet<VirtualFile>();
    for (RefEntity refElement : refElements) {
      PsiElement psiElement = refElement instanceof RefElement ? ((RefElement)refElement).getPsiElement() : null;
      if (psiElement == null || psiElement.getContainingFile() == null) continue;
      readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
    }
    return readOnlyFiles;
  }

  private static RefEntity[] getSelectedElements(AnActionEvent e) {
    final InspectionResultsView invoker = getInvoker(e);
    if (invoker == null) return new RefElement[0];
    List<RefEntity> selection = new ArrayList<RefEntity>(Arrays.asList(invoker.getTree().getSelectedElements()));
    PsiDocumentManager.getInstance(invoker.getProject()).commitAllDocuments();
    Collections.sort(selection, new Comparator<RefEntity>() {
      @Override
      public int compare(RefEntity o1, RefEntity o2) {
        if (o1 instanceof RefElement && o2 instanceof RefElement) {
          RefElement r1 = (RefElement)o1;
          RefElement r2 = (RefElement)o2;
          final PsiElement element1 = r1.getPsiElement();
          final PsiElement element2 = r2.getPsiElement();
          final PsiFile containingFile1 = element1.getContainingFile();
          final PsiFile containingFile2 = element2.getContainingFile();
          if (containingFile1 == containingFile2) {
            int i1 = element1.getTextOffset();
            int i2 = element2.getTextOffset();
            if (i1 < i2) {
              return 1;
            } else if (i1 > i2){
              return -1;
            }
            return 0;
          }
          return containingFile1.getName().compareTo(containingFile2.getName());
        }
        if (o1 instanceof RefElement) {
          return 1;
        }
        if (o2 instanceof RefElement) {
          return -1;
        }
        return o1.getName().compareTo(o2.getName());
      }
    });

    return selection.toArray(new RefEntity[selection.size()]);
  }

  private static void refreshViews(@Nonnull Project project, @Nonnull Set<PsiElement> selectedElements, @Nonnull InspectionToolWrapper toolWrapper) {
    InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final Set<GlobalInspectionContextImpl> runningContexts = managerEx.getRunningContexts();
    for (GlobalInspectionContextImpl context : runningContexts) {
      for (PsiElement element : selectedElements) {
        context.ignoreElement(toolWrapper.getTool(), element);
      }
      context.refreshViews();
    }
  }

  private static void refreshViews(@Nonnull Project project, @Nonnull RefEntity[] refElements, @Nonnull InspectionToolWrapper toolWrapper) {
    final Set<PsiElement> ignoredElements = new HashSet<PsiElement>();
    for (RefEntity element : refElements) {
      final PsiElement psiElement = element instanceof RefElement ? ((RefElement)element).getPsiElement() : null;
      if (psiElement != null && psiElement.isValid()) {
        ignoredElements.add(psiElement);
      }
    }
    refreshViews(project, ignoredElements, toolWrapper);
  }

  /**
   * @return true if immediate UI update needed.
   */
  protected boolean applyFix(@Nonnull RefEntity[] refElements) {
    Set<VirtualFile> readOnlyFiles = getReadOnlyFiles(refElements);
    if (!readOnlyFiles.isEmpty()) {
      final Project project = refElements[0].getRefManager().getProject();
      final ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(
        VfsUtilCore.toVirtualFileArray(readOnlyFiles));
      if (operationStatus.hasReadonlyFiles()) return false;
    }
    return true;
  }

  private class PerformFixesTask implements SequentialTask {
    @Nonnull
    private final Project myProject;
    private final CommonProblemDescriptor[] myDescriptors;
    @Nonnull
    private final Set<PsiElement> myIgnoredElements;
    private final SequentialModalProgressTask myTask;
    @Nonnull
    private final GlobalInspectionContextImpl myContext;
    private int myCount = 0;

    public PerformFixesTask(@Nonnull Project project,
                            @Nonnull CommonProblemDescriptor[] descriptors,
                            @Nonnull Set<PsiElement> ignoredElements,
                            @Nonnull SequentialModalProgressTask task,
                            @Nonnull GlobalInspectionContextImpl context) {
      myProject = project;
      myDescriptors = descriptors;
      myIgnoredElements = ignoredElements;
      myTask = task;
      myContext = context;
    }

    @Override
    public void prepare() {
    }

    @Override
    public boolean isDone() {
      return myCount > myDescriptors.length - 1;
    }

    @Override
    public boolean iteration() {
      final CommonProblemDescriptor descriptor = myDescriptors[myCount++];
      ProgressIndicator indicator = myTask.getIndicator();
      if (indicator != null) {
        indicator.setFraction((double)myCount / myDescriptors.length);
        if (descriptor instanceof ProblemDescriptor) {
          final PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
          if (psiElement != null) {
            indicator.setText("Processing " + SymbolPresentationUtil.getSymbolPresentableText(psiElement));
          }
        }
      }
      applyFix(myProject, myContext, new CommonProblemDescriptor[]{descriptor}, myIgnoredElements);
      return isDone();
    }

    @Override
    public void stop() {
    }
  }
}

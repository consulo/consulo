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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.ide.impl.idea.codeInspection.ui.InspectionTree;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.application.progress.SequentialModalProgressTask;
import consulo.application.progress.SequentialTask;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.impl.inspection.reference.RefManagerImpl;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

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

    protected QuickFixAction(LocalizeValue text, @Nonnull InspectionToolWrapper toolWrapper) {
        this(text, PlatformIconGroup.actionsIntentionbulb(), null, toolWrapper);
    }

    protected QuickFixAction(LocalizeValue text, Image icon, KeyStroke keyStroke, @Nonnull InspectionToolWrapper toolWrapper) {
        super(text, text, icon);
        myToolWrapper = toolWrapper;
        if (keyStroke != null) {
            registerCustomShortcutSet(new CustomShortcutSet(keyStroke), null);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        InspectionResultsView view = getInvoker(e);
        if (view == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setEnabledAndVisible(false);

        InspectionTree tree = view.getTree();
        InspectionToolWrapper toolWrapper = tree.getSelectedToolWrapper();
        if (!view.isSingleToolInSelection() || toolWrapper != myToolWrapper) {
            return;
        }

        if (!isProblemDescriptorsAcceptable() && tree.getSelectedElements().length > 0 ||
            isProblemDescriptorsAcceptable() && tree.getSelectedDescriptors().length > 0) {
            e.getPresentation().setEnabledAndVisible(true);
        }
    }

    protected boolean isProblemDescriptorsAcceptable() {
        return false;
    }

    public LocalizeValue getText(RefEntity where) {
        return getTemplatePresentation().getTextValue();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        InspectionResultsView view = getInvoker(e);
        InspectionTree tree = view.getTree();
        if (isProblemDescriptorsAcceptable()) {
            CommonProblemDescriptor[] descriptors = tree.getSelectedDescriptors();
            if (descriptors.length > 0) {
                doApplyFix(view.getProject(), descriptors, tree.getContext());
                return;
            }
        }

        doApplyFix(getSelectedElements(e), view);
    }

    protected void applyFix(
        @Nonnull Project project,
        @Nonnull GlobalInspectionContextImpl context,
        @Nonnull CommonProblemDescriptor[] descriptors,
        @Nonnull Set<PsiElement> ignoredElements
    ) {
    }

    @RequiredUIAccess
    private void doApplyFix(
        @Nonnull Project project,
        @Nonnull CommonProblemDescriptor[] descriptors,
        @Nonnull GlobalInspectionContextImpl context
    ) {
        Set<VirtualFile> readOnlyFiles = new HashSet<>();
        for (CommonProblemDescriptor descriptor : descriptors) {
            PsiElement psiElement = descriptor instanceof ProblemDescriptor problemDescriptor
                ? problemDescriptor.getPsiElement()
                : null;
            if (psiElement != null && !psiElement.isWritable()) {
                readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
            }
        }

        if (!FileModificationService.getInstance().prepareVirtualFilesForWrite(project, readOnlyFiles)) {
            return;
        }

        RefManagerImpl refManager = (RefManagerImpl)context.getRefManager();

        boolean initial = refManager.isInProcess();

        refManager.inspectionReadActionFinished();

        try {
            Set<PsiElement> ignoredElements = new HashSet<>();

            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(getTemplatePresentation().getTextValue())
                .inWriteAction()
                .inGlobalUndoAction()
                .run(() -> {
                    SequentialModalProgressTask progressTask =
                        new SequentialModalProgressTask(project, getTemplatePresentation().getText(), false);
                    progressTask.setMinIterationTime(200);
                    progressTask.setTask(new PerformFixesTask(project, descriptors, ignoredElements, progressTask, context));
                    ProgressManager.getInstance().run(progressTask);
                });

            refreshViews(project, ignoredElements, myToolWrapper);
        }
        finally { //to make offline view lazy
            if (initial) {
                refManager.inspectionReadActionStarted();
            }
        }
    }

    @RequiredUIAccess
    public void doApplyFix(@Nonnull RefEntity[] refElements, @Nonnull InspectionResultsView view) {
        RefManagerImpl refManager = (RefManagerImpl)view.getGlobalInspectionContext().getRefManager();

        boolean initial = refManager.isInProcess();

        refManager.inspectionReadActionFinished();

        try {
            SimpleReference<Boolean> refreshNeeded = new SimpleReference<>(false);
            if (refElements.length > 0) {
                Project project = refElements[0].getRefManager().getProject();
                CommandProcessor.getInstance().newCommand()
                    .project(project)
                    .name(getTemplatePresentation().getTextValue())
                    .inWriteAction()
                    .inGlobalUndoAction()
                    .run(() -> refreshNeeded.set(applyFix(refElements)));
            }
            if (refreshNeeded.get()) {
                refreshViews(view.getProject(), refElements, myToolWrapper);
            }
        }
        finally {  //to make offline view lazy
            if (initial) {
                refManager.inspectionReadActionStarted();
            }
        }
    }

    public static void removeElements(
        @Nonnull RefEntity[] refElements,
        @Nonnull Project project,
        @Nonnull InspectionToolWrapper toolWrapper
    ) {
        refreshViews(project, refElements, toolWrapper);
        ArrayList<RefElement> deletedRefs = new ArrayList<>(1);
        for (RefEntity refElement : refElements) {
            if (!(refElement instanceof RefElement)) {
                continue;
            }
            refElement.getRefManager().removeRefElement((RefElement)refElement, deletedRefs);
        }
    }

    private static Set<VirtualFile> getReadOnlyFiles(@Nonnull RefEntity[] refElements) {
        Set<VirtualFile> readOnlyFiles = new HashSet<>();
        for (RefEntity refEntity : refElements) {
            PsiElement psiElement = refEntity instanceof RefElement refElement ? refElement.getPsiElement() : null;
            if (psiElement == null || psiElement.getContainingFile() == null) {
                continue;
            }
            readOnlyFiles.add(psiElement.getContainingFile().getVirtualFile());
        }
        return readOnlyFiles;
    }

    private static RefEntity[] getSelectedElements(AnActionEvent e) {
        InspectionResultsView invoker = getInvoker(e);
        if (invoker == null) {
            return new RefElement[0];
        }
        List<RefEntity> selection = new ArrayList<>(Arrays.asList(invoker.getTree().getSelectedElements()));
        PsiDocumentManager.getInstance(invoker.getProject()).commitAllDocuments();
        Collections.sort(
            selection,
            (o1, o2) -> {
                if (o1 instanceof RefElement r1 && o2 instanceof RefElement r2) {
                    PsiElement element1 = r1.getPsiElement();
                    PsiElement element2 = r2.getPsiElement();
                    PsiFile containingFile1 = element1.getContainingFile();
                    PsiFile containingFile2 = element2.getContainingFile();
                    if (containingFile1 == containingFile2) {
                        int i1 = element1.getTextOffset();
                        int i2 = element2.getTextOffset();
                        if (i1 < i2) {
                            return 1;
                        }
                        else if (i1 > i2) {
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
        );

        return selection.toArray(new RefEntity[selection.size()]);
    }

    private static void refreshViews(
        @Nonnull Project project,
        @Nonnull Set<PsiElement> selectedElements,
        @Nonnull InspectionToolWrapper toolWrapper
    ) {
        InspectionManagerImpl managerEx = (InspectionManagerImpl)InspectionManager.getInstance(project);
        Set<GlobalInspectionContextImpl> runningContexts = managerEx.getRunningContexts();
        for (GlobalInspectionContextImpl context : runningContexts) {
            for (PsiElement element : selectedElements) {
                context.ignoreElement(toolWrapper.getTool(), element);
            }
            context.refreshViews();
        }
    }

    private static void refreshViews(
        @Nonnull Project project,
        @Nonnull RefEntity[] refElements,
        @Nonnull InspectionToolWrapper toolWrapper
    ) {
        Set<PsiElement> ignoredElements = new HashSet<>();
        for (RefEntity element : refElements) {
            PsiElement psiElement = element instanceof RefElement refElement ? refElement.getPsiElement() : null;
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
            Project project = refElements[0].getRefManager().getProject();
            ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(
                VfsUtilCore.toVirtualFileArray(readOnlyFiles));
            if (operationStatus.hasReadonlyFiles()) {
                return false;
            }
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

        public PerformFixesTask(
            @Nonnull Project project,
            @Nonnull CommonProblemDescriptor[] descriptors,
            @Nonnull Set<PsiElement> ignoredElements,
            @Nonnull SequentialModalProgressTask task,
            @Nonnull GlobalInspectionContextImpl context
        ) {
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
        @RequiredReadAction
        public boolean iteration() {
            CommonProblemDescriptor descriptor = myDescriptors[myCount++];
            ProgressIndicator indicator = myTask.getIndicator();
            if (indicator != null) {
                indicator.setFraction((double)myCount / myDescriptors.length);
                if (descriptor instanceof ProblemDescriptor problemDescriptor) {
                    PsiElement psiElement = problemDescriptor.getPsiElement();
                    if (psiElement != null) {
                        indicator.setTextValue(
                            InspectionLocalize.processingProgressText(SymbolPresentationUtil.getSymbolPresentableText(psiElement))
                        );
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

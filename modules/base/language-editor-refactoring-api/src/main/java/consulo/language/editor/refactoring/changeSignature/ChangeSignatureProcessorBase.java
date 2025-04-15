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
package consulo.language.editor.refactoring.changeSignature;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.RefactoringTransaction;
import consulo.language.editor.refactoring.ResolveSnapshotProvider;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.event.UndoRefactoringElementListener;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoableAction;
import consulo.usage.MoveRenameUsageInfo;
import consulo.usage.UsageInfo;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author Maxim.Medvedev
 */
public abstract class ChangeSignatureProcessorBase extends BaseRefactoringProcessor {
    private static final Logger LOG = Logger.getInstance(ChangeSignatureProcessorBase.class);

    protected final ChangeInfo myChangeInfo;
    protected final PsiManager myManager;

    protected ChangeSignatureProcessorBase(Project project, ChangeInfo changeInfo) {
        super(project);
        myChangeInfo = changeInfo;
        myManager = PsiManager.getInstance(project);
    }

    protected ChangeSignatureProcessorBase(Project project, @Nullable Runnable prepareSuccessfulCallback, ChangeInfo changeInfo) {
        super(project, prepareSuccessfulCallback);
        myChangeInfo = changeInfo;
        myManager = PsiManager.getInstance(project);
    }

    @Override
    @Nonnull
    protected UsageInfo[] findUsages() {
        List<UsageInfo> infos = new ArrayList<>();

        for (ChangeSignatureUsageProcessor processor : ChangeSignatureUsageProcessor.EP_NAME.getExtensionList()) {
            ContainerUtil.addAll(infos, processor.findUsages(myChangeInfo));
        }
        infos = filterUsages(infos);
        return infos.toArray(new UsageInfo[infos.size()]);
    }

    protected List<UsageInfo> filterUsages(List<UsageInfo> infos) {
        Map<PsiElement, MoveRenameUsageInfo> moveRenameInfos = new HashMap<>();
        Set<PsiElement> usedElements = new HashSet<>();

        List<UsageInfo> result = new ArrayList<>(infos.size() / 2);
        for (UsageInfo info : infos) {
            LOG.assertTrue(info != null, getClass());
            PsiElement element = info.getElement();
            if (info instanceof MoveRenameUsageInfo moveRenameUsageInfo) {
                if (usedElements.contains(element)) {
                    continue;
                }
                moveRenameInfos.put(element, moveRenameUsageInfo);
            }
            else {
                moveRenameInfos.remove(element);
                usedElements.add(element);
                if (!(info instanceof PossiblyIncorrectUsage possiblyIncorrectUsage && !possiblyIncorrectUsage.isCorrect())) {
                    result.add(info);
                }
            }
        }
        result.addAll(moveRenameInfos.values());
        return result;
    }


    @Override
    protected boolean isPreviewUsages(@Nonnull UsageInfo[] usages) {
        for (ChangeSignatureUsageProcessor processor : ChangeSignatureUsageProcessor.EP_NAME.getExtensionList()) {
            if (processor.shouldPreviewUsages(myChangeInfo, usages)) {
                return true;
            }
        }
        return super.isPreviewUsages(usages);
    }

    @Nullable
    @Override
    protected String getRefactoringId() {
        return "refactoring.changeSignature";
    }

    @Nullable
    @Override
    protected RefactoringEventData getBeforeData() {
        RefactoringEventData data = new RefactoringEventData();
        data.addElement(getChangeInfo().getMethod());
        return data;
    }

    @Nullable
    @Override
    protected RefactoringEventData getAfterData(@Nonnull UsageInfo[] usages) {
        RefactoringEventData data = new RefactoringEventData();
        data.addElement(getChangeInfo().getMethod());
        return data;
    }

    @Override
    @RequiredReadAction
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        RefactoringTransaction transaction = getTransaction();
        RefactoringElementListener elementListener =
            transaction == null ? null : transaction.getElementListener(myChangeInfo.getMethod());
        String fqn = QualifiedNameProviderUtil.elementToFqn(myChangeInfo.getMethod(), null);
        if (fqn != null) {
            UndoableAction action = new BasicUndoableAction() {
                @Override
                public void undo() {
                    if (elementListener instanceof UndoRefactoringElementListener undoRefactoringElementListener) {
                        undoRefactoringElementListener.undoElementMovedOrRenamed(myChangeInfo.getMethod(), fqn);
                    }
                }

                @Override
                public void redo() {
                }
            };
            ProjectUndoManager.getInstance(myProject).undoableActionPerformed(action);
        }
        try {
            List<ChangeSignatureUsageProcessor> processors = ChangeSignatureUsageProcessor.EP_NAME.getExtensionList();

            ResolveSnapshotProvider resolveSnapshotProvider =
                myChangeInfo.isParameterNamesChanged() ? ResolveSnapshotProvider.forLanguage(myChangeInfo.getMethod().getLanguage()) : null;
            List<ResolveSnapshotProvider.ResolveSnapshot> snapshots = new ArrayList<>();
            for (ChangeSignatureUsageProcessor processor : processors) {
                if (resolveSnapshotProvider != null) {
                    processor.registerConflictResolvers(snapshots, resolveSnapshotProvider, usages, myChangeInfo);
                }
            }

            for (UsageInfo usage : usages) {
                for (ChangeSignatureUsageProcessor processor : processors) {
                    if (processor.processUsage(myChangeInfo, usage, true, usages)) {
                        break;
                    }
                }
            }

            LOG.assertTrue(myChangeInfo.getMethod().isValid());
            for (ChangeSignatureUsageProcessor processor : processors) {
                if (processor.processPrimaryMethod(myChangeInfo)) {
                    break;
                }
            }

            for (UsageInfo usage : usages) {
                for (ChangeSignatureUsageProcessor processor : processors) {
                    if (processor.processUsage(myChangeInfo, usage, false, usages)) {
                        break;
                    }
                }
            }

            if (!snapshots.isEmpty()) {
                for (ParameterInfo parameterInfo : myChangeInfo.getNewParameters()) {
                    for (ResolveSnapshotProvider.ResolveSnapshot snapshot : snapshots) {
                        snapshot.apply(parameterInfo.getName());
                    }
                }
            }
            PsiElement method = myChangeInfo.getMethod();
            LOG.assertTrue(method.isValid());
            if (elementListener != null && myChangeInfo.isNameChanged()) {
                elementListener.elementRenamed(method);
            }
        }
        catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    @Nonnull
    @Override
    @RequiredReadAction
    protected String getCommandName() {
        return RefactoringLocalize.changingSignatureOf0(DescriptiveNameUtil.getDescriptiveName(myChangeInfo.getMethod())).get();
    }

    public ChangeInfo getChangeInfo() {
        return myChangeInfo;
    }
}

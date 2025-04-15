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

package consulo.language.editor.refactoring.rename;

import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.event.RefactoringEventListener;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.ConflictsDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.RelatedUsageInfo;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.usage.*;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.util.*;

public class RenameProcessor extends BaseRefactoringProcessor {
    private static final Logger LOG = Logger.getInstance(RenameProcessor.class);

    protected final LinkedHashMap<PsiElement, String> myAllRenames = new LinkedHashMap<>();

    private @Nonnull
    PsiElement myPrimaryElement;
    private String myNewName = null;

    private boolean mySearchInComments;
    private boolean mySearchTextOccurrences;
    protected boolean myForceShowPreview;

    private String myCommandName;

    private NonCodeUsageInfo[] myNonCodeUsages = new NonCodeUsageInfo[0];
    private final List<AutomaticRenamerFactory> myRenamerFactories = new ArrayList<>();
    private final List<AutomaticRenamer> myRenamers = new ArrayList<>();
    private final List<UnresolvableCollisionUsageInfo> mySkippedUsages = new ArrayList<>();

    public RenameProcessor(
        Project project,
        @Nonnull PsiElement element,
        @Nonnull @NonNls String newName,
        boolean isSearchInComments,
        boolean isSearchTextOccurrences
    ) {
        super(project);
        myPrimaryElement = element;

        assertNonCompileElement(element);
        //assertValidName(element, newName);

        mySearchInComments = isSearchInComments;
        mySearchTextOccurrences = isSearchTextOccurrences;

        setNewName(newName);
    }

    public Set<PsiElement> getElements() {
        return Collections.unmodifiableSet(myAllRenames.keySet());
    }

    public String getNewName(PsiElement element) {
        return myAllRenames.get(element);
    }

    public void addRenamerFactory(AutomaticRenamerFactory factory) {
        if (!myRenamerFactories.contains(factory)) {
            myRenamerFactories.add(factory);
        }
    }

    public void removeRenamerFactory(AutomaticRenamerFactory factory) {
        myRenamerFactories.remove(factory);
    }

    @Override
    @RequiredUIAccess
    public void doRun() {
        if (!myPrimaryElement.isValid()) {
            return;
        }
        prepareRenaming(myPrimaryElement, myNewName, myAllRenames);

        super.doRun();
    }

    public void prepareRenaming(
        @Nonnull final PsiElement element,
        final String newName,
        final LinkedHashMap<PsiElement, String> allRenames
    ) {
        final List<RenamePsiElementProcessor> processors = RenamePsiElementProcessor.allForElement(element);
        myForceShowPreview = false;
        for (RenamePsiElementProcessor processor : processors) {
            if (processor.canProcessElement(element)) {
                processor.prepareRenaming(element, newName, allRenames);
                myForceShowPreview |= processor.forcesShowPreview();
            }
        }
    }

    @Nullable
    private String getHelpID() {
        return RenamePsiElementProcessor.forElement(myPrimaryElement).getHelpID(myPrimaryElement);
    }

    @Override
    @RequiredUIAccess
    public boolean preprocessUsages(@Nonnull SimpleReference<UsageInfo[]> refUsages) {
        UsageInfo[] usagesIn = refUsages.get();
        MultiMap<PsiElement, String> conflicts = new MultiMap<>();

        RenameUtil.addConflictDescriptions(usagesIn, conflicts);
        RenamePsiElementProcessor.forElement(myPrimaryElement)
            .findExistingNameConflicts(myPrimaryElement, myNewName, conflicts, myAllRenames);
        if (!conflicts.isEmpty()) {

            final RefactoringEventData conflictData = new RefactoringEventData();
            conflictData.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values());
            myProject.getMessageBus().syncPublisher(RefactoringEventListener.class)
                .conflictsDetected("refactoring.rename", conflictData);

            if (myProject.getApplication().isUnitTestMode()) {
                throw new ConflictsInTestsException(conflicts.values());
            }
            ConflictsDialog conflictsDialog = prepareConflictsDialog(conflicts, refUsages.get());
            if (!conflictsDialog.showAndGet()) {
                if (conflictsDialog.isShowConflicts()) {
                    prepareSuccessful();
                }
                return false;
            }
        }

        final List<UsageInfo> variableUsages = new ArrayList<>();
        if (!myRenamers.isEmpty()) {
            if (!findRenamedVariables(variableUsages)) {
                return false;
            }
            final LinkedHashMap<PsiElement, String> renames = new LinkedHashMap<>();
            for (final AutomaticRenamer renamer : myRenamers) {
                final List<? extends PsiNamedElement> variables = renamer.getElements();
                for (final PsiNamedElement variable : variables) {
                    final String newName = renamer.getNewName(variable);
                    if (newName != null) {
                        addElement(variable, newName);
                        prepareRenaming(variable, newName, renames);
                    }
                }
            }
            if (!renames.isEmpty()) {
                for (PsiElement element : renames.keySet()) {
                    assertNonCompileElement(element);
                }
                myAllRenames.putAll(renames);
                final Runnable runnable = () -> {
                    for (final Map.Entry<PsiElement, String> entry : renames.entrySet()) {
                        ThrowableComputable<UsageInfo[], RuntimeException> action =
                            () -> RenameUtil.findUsages(
                                entry.getKey(),
                                entry.getValue(),
                                mySearchInComments,
                                mySearchTextOccurrences,
                                myAllRenames
                            );
                        final UsageInfo[] usages = AccessRule.read(action);
                        Collections.addAll(variableUsages, usages);
                    }
                };
                if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    runnable,
                    RefactoringLocalize.searchingForVariables().get(),
                    true,
                    myProject
                )) {
                    return false;
                }
            }
        }

        final int[] choice = myAllRenames.size() > 1 ? new int[]{-1} : null;
        try {
            for (Iterator<Map.Entry<PsiElement, String>> iterator = myAllRenames.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<PsiElement, String> entry = iterator.next();
                if (entry.getKey() instanceof PsiFile file) {
                    final PsiDirectory containingDirectory = file.getContainingDirectory();
                    if (CommonRefactoringUtil.checkFileExist(containingDirectory, choice, file, entry.getValue(), "Rename")) {
                        iterator.remove();
                        continue;
                    }
                }
                RenameUtil.checkRename(entry.getKey(), entry.getValue());
            }
        }
        catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringLocalize.renameTitle().get(), e.getMessage(), getHelpID(), myProject);
            return false;
        }

        final Set<UsageInfo> usagesSet = new LinkedHashSet<>(Arrays.asList(usagesIn));
        usagesSet.addAll(variableUsages);
        final List<UnresolvableCollisionUsageInfo> conflictUsages = RenameUtil.removeConflictUsages(usagesSet);
        if (conflictUsages != null) {
            mySkippedUsages.addAll(conflictUsages);
        }
        refUsages.set(usagesSet.toArray(new UsageInfo[usagesSet.size()]));

        prepareSuccessful();
        return PsiElementRenameHandler.canRename(myProject, null, myPrimaryElement);
    }

    public static void assertNonCompileElement(PsiElement element) {
        LOG.assertTrue(!(element instanceof PsiCompiledElement), element);
    }

    private void assertValidName(PsiElement element, String newName) {
        LOG.assertTrue(RenameUtil.isValidName(myProject, element, newName), "element: " + element + ", newName: " + newName);
    }

    private boolean findRenamedVariables(final List<UsageInfo> variableUsages) {
        for (Iterator<AutomaticRenamer> iterator = myRenamers.iterator(); iterator.hasNext(); ) {
            AutomaticRenamer automaticVariableRenamer = iterator.next();
            if (!automaticVariableRenamer.hasAnythingToRename()) {
                continue;
            }
            if (!showAutomaticRenamingDialog(automaticVariableRenamer)) {
                iterator.remove();
            }
        }

        final Runnable runnable = () -> myProject.getApplication().runReadAction(() -> {
            for (final AutomaticRenamer renamer : myRenamers) {
                renamer.findUsages(variableUsages, mySearchInComments, mySearchTextOccurrences, mySkippedUsages, myAllRenames);
            }
        });

        return ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(runnable, RefactoringLocalize.searchingForVariables().get(), true, myProject);
    }

    @RequiredUIAccess
    protected boolean showAutomaticRenamingDialog(AutomaticRenamer automaticVariableRenamer) {
        if (myProject.getApplication().isUnitTestMode()) {
            for (PsiNamedElement element : automaticVariableRenamer.getElements()) {
                automaticVariableRenamer.setRename(element, automaticVariableRenamer.getNewName(element));
            }
            return true;
        }
        final AutomaticRenamingDialog dialog = new AutomaticRenamingDialog(myProject, automaticVariableRenamer);
        return dialog.showAndGet();
    }

    public void addElement(@Nonnull PsiElement element, @Nonnull String newName) {
        assertNonCompileElement(element);
        myAllRenames.put(element, newName);
    }

    private void setNewName(@Nonnull String newName) {
        myNewName = newName;
        myAllRenames.put(myPrimaryElement, newName);
        myCommandName = RefactoringLocalize.renaming01To2(
            UsageViewUtil.getType(myPrimaryElement),
            DescriptiveNameUtil.getDescriptiveName(myPrimaryElement),
            newName
        ).get();
    }

    @Override
    @Nonnull
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new RenameViewDescriptor(myAllRenames);
    }

    @Override
    @Nonnull
    public UsageInfo[] findUsages() {
        myRenamers.clear();
        ArrayList<UsageInfo> result = new ArrayList<>();

        List<PsiElement> elements = new ArrayList<>(myAllRenames.keySet());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < elements.size(); i++) {
            PsiElement element = elements.get(i);
            if (element == null) {
                LOG.error("primary: " + myPrimaryElement + "; renamers: " + myRenamers);
                continue;
            }
            final String newName = myAllRenames.get(element);
            final UsageInfo[] usages = RenameUtil.findUsages(element, newName, mySearchInComments, mySearchTextOccurrences, myAllRenames);
            final List<UsageInfo> usagesList = Arrays.asList(usages);
            result.addAll(usagesList);

            for (AutomaticRenamerFactory factory : myRenamerFactories) {
                if (factory.isApplicable(element)) {
                    myRenamers.add(factory.createRenamer(element, newName, usagesList));
                }
            }

            for (AutomaticRenamerFactory factory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
                if (factory.getOptionName() == LocalizeValue.of() && factory.isApplicable(element)) {
                    myRenamers.add(factory.createRenamer(element, newName, usagesList));
                }
            }
        }
        UsageInfo[] usageInfos = result.toArray(new UsageInfo[result.size()]);
        usageInfos = UsageViewUtil.removeDuplicatedUsages(usageInfos);
        return usageInfos;
    }

    @Override
    protected void refreshElements(@Nonnull PsiElement[] elements) {
        LOG.assertTrue(elements.length > 0);
        myPrimaryElement = elements[0];

        final Iterator<String> newNames = myAllRenames.values().iterator();
        LinkedHashMap<PsiElement, String> newAllRenames = new LinkedHashMap<>();
        for (PsiElement resolved : elements) {
            newAllRenames.put(resolved, newNames.next());
        }
        myAllRenames.clear();
        myAllRenames.putAll(newAllRenames);
    }

    @Override
    protected boolean isPreviewUsages(@Nonnull UsageInfo[] usages) {
        return myForceShowPreview || super.isPreviewUsages(usages) || UsageViewUtil.reportNonRegularUsages(usages, myProject);
    }

    @Nullable
    @Override
    protected String getRefactoringId() {
        return "refactoring.rename";
    }

    @Nullable
    @Override
    protected RefactoringEventData getBeforeData() {
        final RefactoringEventData data = new RefactoringEventData();
        data.addElement(myPrimaryElement);
        return data;
    }

    @Nullable
    @Override
    protected RefactoringEventData getAfterData(@Nonnull UsageInfo[] usages) {
        final RefactoringEventData data = new RefactoringEventData();
        data.addElement(myPrimaryElement);
        return data;
    }

    @Override
    public void performRefactoring(@Nonnull UsageInfo[] usages) {
        List<Runnable> postRenameCallbacks = new ArrayList<>();

        final MultiMap<PsiElement, UsageInfo> classified = classifyUsages(myAllRenames.keySet(), usages);
        for (final PsiElement element : myAllRenames.keySet()) {
            String newName = myAllRenames.get(element);

            final RefactoringElementListener elementListener = getTransaction().getElementListener(element);
            final RenamePsiElementProcessor renamePsiElementProcessor = RenamePsiElementProcessor.forElement(element);
            Runnable postRenameCallback = renamePsiElementProcessor.getPostRenameCallback(element, newName, elementListener);
            final Collection<UsageInfo> infos = classified.get(element);
            try {
                RenameUtil.doRename(element, newName, infos.toArray(new UsageInfo[infos.size()]), myProject, elementListener);
            }
            catch (final IncorrectOperationException e) {
                RenameUtil.showErrorMessage(e, element, myProject);
                return;
            }
            if (postRenameCallback != null) {
                postRenameCallbacks.add(postRenameCallback);
            }
        }

        for (Runnable runnable : postRenameCallbacks) {
            runnable.run();
        }

        List<NonCodeUsageInfo> nonCodeUsages = new ArrayList<>();
        for (UsageInfo usage : usages) {
            if (usage instanceof NonCodeUsageInfo nonCodeUsageInfo) {
                nonCodeUsages.add(nonCodeUsageInfo);
            }
        }
        myNonCodeUsages = nonCodeUsages.toArray(new NonCodeUsageInfo[nonCodeUsages.size()]);
        if (!mySkippedUsages.isEmpty()) {
            final Application application = myProject.getApplication();
            if (!application.isUnitTestMode() && !application.isHeadlessEnvironment()) {
                application.invokeLater(() -> {
                    final IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(myProject);
                    if (ideFrame != null) {

                        StatusBar statusBar = ideFrame.getStatusBar();
                        HyperlinkListener listener = e -> {
                            if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                                return;
                            }
                            Messages.showMessageDialog(
                                "<html>Following usages were safely skipped:<br>" +
                                    StringUtil.join(
                                        mySkippedUsages,
                                        unresolvableCollisionUsageInfo -> unresolvableCollisionUsageInfo.getDescription(),
                                        "<br>"
                                    ) +
                                    "</html>",
                                "Not All Usages Were Renamed",
                                null
                            );
                        };
                        statusBar.notifyProgressByBalloon(
                            NotificationType.WARNING,
                            "<html><body>Unable to rename certain usages. <a href=\"\">Browse</a></body></html>",
                            null,
                            listener
                        );
                    }
                }, Application.get().getNoneModalityState());
            }
        }
    }

    @Override
    protected void performPsiSpoilingRefactoring() {
        RenameUtil.renameNonCodeUsages(myProject, myNonCodeUsages);
    }

    @Nonnull
    @Override
    protected String getCommandName() {
        return myCommandName;
    }

    public static MultiMap<PsiElement, UsageInfo> classifyUsages(Collection<? extends PsiElement> elements, UsageInfo[] usages) {
        final MultiMap<PsiElement, UsageInfo> result = new MultiMap<>();
        for (UsageInfo usage : usages) {
            LOG.assertTrue(usage instanceof MoveRenameUsageInfo);
            if (usage.getReference() instanceof LightweightPsiElement) {
                continue; //filter out implicit references (e.g. from derived class to super class' default constructor)
            }
            MoveRenameUsageInfo usageInfo = (MoveRenameUsageInfo)usage;
            if (usage instanceof RelatedUsageInfo relatedUsageInfo) {
                final PsiElement relatedElement = relatedUsageInfo.getRelatedElement();
                if (elements.contains(relatedElement)) {
                    result.putValue(relatedElement, usage);
                }
            }
            else {
                PsiElement referenced = usageInfo.getReferencedElement();
                if (elements.contains(referenced)) {
                    result.putValue(referenced, usage);
                }
                else if (referenced != null) {
                    PsiElement indirect = referenced.getNavigationElement();
                    if (elements.contains(indirect)) {
                        result.putValue(indirect, usage);
                    }
                }

            }
        }
        return result;
    }

    public Collection<String> getNewNames() {
        return myAllRenames.values();
    }

    public void setSearchInComments(boolean value) {
        mySearchInComments = value;
    }

    public void setSearchTextOccurrences(boolean searchTextOccurrences) {
        mySearchTextOccurrences = searchTextOccurrences;
    }

    public boolean isSearchInComments() {
        return mySearchInComments;
    }

    public boolean isSearchTextOccurrences() {
        return mySearchTextOccurrences;
    }

    public void setCommandName(final String commandName) {
        myCommandName = commandName;
    }
}

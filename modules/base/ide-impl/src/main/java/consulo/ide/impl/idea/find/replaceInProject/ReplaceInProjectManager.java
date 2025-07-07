// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.find.replaceInProject;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.find.actions.FindInPathAction;
import consulo.ide.impl.idea.find.findInProject.FindInProjectManager;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.ide.impl.idea.usages.impl.UsageViewImpl;
import consulo.ide.impl.idea.util.AdapterProcessor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.*;
import consulo.usage.rule.UsageInFile;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.function.Supplier;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ReplaceInProjectManager {
    private static final NotificationGroup NOTIFICATION_GROUP = FindInPathAction.NOTIFICATION_GROUP;

    private final Project myProject;
    private final NotificationService myNotificationService;
    private boolean myIsFindInProgress;

    public static ReplaceInProjectManager getInstance(Project project) {
        return ServiceManager.getService(project, ReplaceInProjectManager.class);
    }

    @Inject
    public ReplaceInProjectManager(Project project, NotificationService notificationService) {
        myProject = project;
        myNotificationService = notificationService;
    }

    private static boolean hasReadOnlyUsages(Collection<? extends Usage> usages) {
        for (Usage usage : usages) {
            if (usage.isReadOnly()) {
                return true;
            }
        }

        return false;
    }

    static class ReplaceContext {
        private final UsageView usageView;
        private final FindModel findModel;
        private Set<Usage> excludedSet;

        ReplaceContext(@Nonnull UsageView usageView, @Nonnull FindModel findModel) {
            this.usageView = usageView;
            this.findModel = findModel;
        }

        @Nonnull
        public FindModel getFindModel() {
            return findModel;
        }

        @Nonnull
        public UsageView getUsageView() {
            return usageView;
        }

        @Nonnull
        Set<Usage> getExcludedSetCached() {
            if (excludedSet == null) {
                excludedSet = usageView.getExcludedUsages();
            }
            return excludedSet;
        }

        void invalidateExcludedSetCache() {
            excludedSet = null;
        }
    }

    /**
     * @param model would be used for replacing if not null, otherwise shared (project-level) model would be used
     */
    @RequiredUIAccess
    public void replaceInProject(@Nonnull DataContext dataContext, @Nullable FindModel model) {
        FindManager findManager = FindManager.getInstance(myProject);
        FindModel findModel;

        boolean isOpenInNewTabEnabled;
        boolean toOpenInNewTab;
        Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
        if (selectedContent != null && selectedContent.isPinned()) {
            toOpenInNewTab = true;
            isOpenInNewTabEnabled = false;
        }
        else {
            toOpenInNewTab = FindSettings.getInstance().isShowResultsInSeparateView();
            isOpenInNewTabEnabled = UsageViewContentManager.getInstance(myProject).getReusableContentsCount() > 0;
        }
        if (model == null) {

            findModel = findManager.getFindInProjectModel().clone();
            findModel.setReplaceState(true);
            findModel.setOpenInNewTabEnabled(isOpenInNewTabEnabled);
            findModel.setOpenInNewTab(toOpenInNewTab);
            FindInProjectUtil.setDirectoryName(findModel, dataContext);
            FindInProjectUtil.initStringToFindFromDataContext(findModel, dataContext);
        }
        else {
            findModel = model;
            findModel.setOpenInNewTabEnabled(isOpenInNewTabEnabled);
        }

        findManager.showFindDialog(
            findModel,
            () -> {
                if (findModel.isReplaceState()) {
                    replaceInPath(findModel);
                }
                else {
                    FindInProjectManager.getInstance(myProject).findInPath(findModel);
                }
            }
        );
    }

    public void replaceInPath(@Nonnull FindModel findModel) {
        FindManager findManager = FindManager.getInstance(myProject);
        if (!findModel.isProjectScope() && FindInProjectUtil.getDirectory(findModel) == null && findModel.getModuleName() == null && findModel.getCustomScope() == null) {
            return;
        }

        UsageViewManager manager = UsageViewManager.getInstance(myProject);

        if (manager == null) {
            return;
        }
        findManager.getFindInProjectModel().copyFrom(findModel);
        FindModel findModelCopy = findModel.clone();

        UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModelCopy);
        FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation(myProject, true, presentation);
        processPresentation.setShowFindOptionsPrompt(findModel.isPromptOnReplace());

        UsageSearcherFactory factory = new UsageSearcherFactory(findModelCopy, processPresentation);
        searchAndShowUsages(manager, factory, findModelCopy, presentation, processPresentation);
    }

    private static class ReplaceInProjectTarget extends FindInProjectUtil.StringUsageTarget {
        ReplaceInProjectTarget(@Nonnull Project project, @Nonnull FindModel findModel) {
            super(project, findModel);
        }

        @Nonnull
        @Override
        public String getLongDescriptiveName() {
            UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(myFindModel);
            return "Replace " + StringUtil.decapitalize(presentation.getToolwindowTitle()) + " with '" + myFindModel.getStringToReplace() + "'";
        }

        @Override
        public KeyboardShortcut getShortcut() {
            return ActionManager.getInstance().getKeyboardShortcut("ReplaceInPath");
        }

        @Override
        @RequiredUIAccess
        public void showSettings() {
            Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
            JComponent component = selectedContent == null ? null : selectedContent.getComponent();
            ReplaceInProjectManager findInProjectManager = getInstance(myProject);
            findInProjectManager.replaceInProject(DataManager.getInstance().getDataContext(component), myFindModel);
        }
    }

    public void searchAndShowUsages(
        @Nonnull UsageViewManager manager,
        @Nonnull Supplier<UsageSearcher> usageSearcherFactory,
        @Nonnull final FindModel findModelCopy,
        @Nonnull UsageViewPresentation presentation,
        @Nonnull FindUsagesProcessPresentation processPresentation
    ) {
        presentation.setMergeDupLinesAvailable(false);
        ReplaceInProjectTarget target = new ReplaceInProjectTarget(myProject, findModelCopy);
        ((FindManagerImpl)FindManager.getInstance(myProject)).getFindUsagesManager().addToHistory(target);
        final ReplaceContext[] context = new ReplaceContext[1];
        manager.searchAndShowUsages(
            new UsageTarget[]{target},
            usageSearcherFactory,
            processPresentation,
            presentation,
            new UsageViewManager.UsageViewStateListener() {
                @Override
                public void usageViewCreated(@Nonnull UsageView usageView) {
                    context[0] = new ReplaceContext(usageView, findModelCopy);
                    addReplaceActions(context[0]);
                    usageView.setRerunAction(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            UsageViewPresentation rerunPresentation = presentation.copy();
                            rerunPresentation.setOpenInNewTab(false);
                            searchAndShowUsages(manager, usageSearcherFactory, findModelCopy, rerunPresentation, processPresentation);
                        }
                    });
                }

                @Override
                @RequiredUIAccess
                public void findingUsagesFinished(UsageView usageView) {
                    if (context[0] != null && !processPresentation.isShowFindOptionsPrompt()) {
                        replaceUsagesUnderCommand(context[0], usageView.getUsages());
                        context[0].invalidateExcludedSetCache();
                    }
                }
            }
        );
    }

    public boolean showReplaceAllConfirmDialog(
        @Nonnull String usagesCount,
        @Nonnull String stringToFind,
        @Nonnull String filesCount,
        @Nonnull String stringToReplace
    ) {
        return Messages.YES == MessageDialogBuilder.yesNo(
                FindLocalize.findReplaceAllConfirmationTitle().get(),
                FindLocalize.findReplaceAllConfirmation(
                    usagesCount,
                    StringUtil.escapeXmlEntities(stringToFind),
                    filesCount,
                    StringUtil.escapeXmlEntities(stringToReplace)
                ).get()
            )
            .yesText(FindLocalize.findReplaceCommand().get())
            .project(myProject)
            .noText(CommonLocalize.buttonCancel().get())
            .show();
    }

    private static Set<VirtualFile> getFiles(@Nonnull ReplaceContext replaceContext, boolean selectedOnly) {
        Set<Usage> usages = selectedOnly ? replaceContext.getUsageView().getSelectedUsages() : replaceContext.getUsageView().getUsages();
        if (usages.isEmpty()) {
            return Collections.emptySet();
        }

        Set<VirtualFile> files = new HashSet<>();
        for (Usage usage : usages) {
            if (usage instanceof UsageInfo2UsageAdapter usageAdapter) {
                files.add(usageAdapter.getFile());
            }
        }
        return files;
    }

    private static Set<Usage> getAllUsagesForFile(@Nonnull ReplaceContext replaceContext, @Nonnull VirtualFile file) {
        Set<Usage> usages = replaceContext.getUsageView().getUsages();
        Set<Usage> result = new LinkedHashSet<>();
        for (Usage usage : usages) {
            if (usage instanceof UsageInfo2UsageAdapter usageAdapter && Objects.equals(usageAdapter.getFile(), file)) {
                result.add(usage);
            }
        }
        return result;
    }

    private void addReplaceActions(final ReplaceContext replaceContext) {
        replaceContext.getUsageView().addButtonToLowerPane(new DumbAwareAction(FindLocalize.findReplaceAllAction()) {
            {
                setShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)));
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                Set<Usage> usages = replaceContext.getUsageView().getUsages();
                if (usages.isEmpty()) {
                    return;
                }
                Set<VirtualFile> files = getFiles(replaceContext, false);
                if (files.size() < 2
                    || showReplaceAllConfirmDialog(
                    String.valueOf(usages.size()),
                    replaceContext.getFindModel().getStringToFind(),
                    String.valueOf(files.size()),
                    replaceContext.getFindModel().getStringToReplace()
                )) {
                    replaceUsagesUnderCommand(replaceContext, usages);
                }
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(!replaceContext.getUsageView().getUsages().isEmpty());
            }
        });

        replaceContext.getUsageView().addButtonToLowerPane(new AnAction() {
            {
                setShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK)));
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                replaceUsagesUnderCommand(replaceContext, replaceContext.getUsageView().getSelectedUsages());
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                presentation.setTextValue(FindLocalize.findReplaceSelectedAction(replaceContext.getUsageView().getSelectedUsages().size()));
                presentation.setEnabled(!replaceContext.getUsageView().getSelectedUsages().isEmpty());
            }
        });
    }

    @RequiredUIAccess
    private boolean replaceUsages(@Nonnull ReplaceContext replaceContext, @Nonnull Collection<Usage> usages) {
        if (!ensureUsagesWritable(replaceContext, usages)) {
            return true;
        }

        int[] replacedCount = {0};
        boolean[] success = {true};

        success[0] &= ((ApplicationEx)Application.get()).runWriteActionWithCancellableProgressInDispatchThread(
            FindLocalize.findReplaceAllConfirmationTitle().get(),
            myProject,
            null,
            indicator -> {
                indicator.setIndeterminate(false);
                int processed = 0;
                VirtualFile lastFile = null;

                for (Usage usage : usages) {
                    ++processed;
                    indicator.checkCanceled();
                    indicator.setFraction((float)processed / usages.size());

                    if (usage instanceof UsageInFile usageInFile) {
                        VirtualFile virtualFile = usageInFile.getFile();
                        if (virtualFile != null && !virtualFile.equals(lastFile)) {
                            indicator.setText2(virtualFile.getPresentableUrl());
                            lastFile = virtualFile;
                        }
                    }

                    ProgressManager.getInstance().executeNonCancelableSection(() -> {
                        try {
                            if (replaceUsage(usage, replaceContext.getFindModel(), replaceContext.getExcludedSetCached(), false)) {
                                replacedCount[0]++;
                            }
                        }
                        catch (FindManager.MalformedReplacementStringException ex) {
                            markAsMalformedReplacement(replaceContext, usage);
                            success[0] = false;
                        }
                    });
                }
            }
        );

        replaceContext.getUsageView().removeUsagesBulk(usages);
        reportNumberReplacedOccurrences(myProject, replacedCount[0]);
        return success[0];
    }

    private static void markAsMalformedReplacement(ReplaceContext replaceContext, Usage usage) {
        replaceContext.getUsageView().excludeUsages(new Usage[]{usage});
    }

    @Deprecated
    public static void reportNumberReplacedOccurrences(Project project, int occurrences) {
    }

    public boolean replaceUsage(
        @Nonnull Usage usage,
        @Nonnull FindModel findModel,
        @Nonnull Set<Usage> excludedSet,
        boolean justCheck
    ) throws FindManager.MalformedReplacementStringException {
        SimpleReference<FindManager.MalformedReplacementStringException> exceptionResult = SimpleReference.create();
        boolean result = WriteAction.compute(() -> {
            if (excludedSet.contains(usage)) {
                return false;
            }

            Document document = ((UsageInfo2UsageAdapter)usage).getDocument();
            if (!document.isWritable()) {
                return false;
            }

            return ((UsageInfo2UsageAdapter)usage).processRangeMarkers(segment -> {
                int textOffset = segment.getStartOffset();
                int textEndOffset = segment.getEndOffset();
                SimpleReference<String> stringToReplace = SimpleReference.create();
                try {
                    if (!getStringToReplace(textOffset, textEndOffset, document, findModel, stringToReplace)) {
                        return true;
                    }
                    if (!stringToReplace.isNull() && !justCheck) {
                        document.replaceString(textOffset, textEndOffset, stringToReplace.get());
                    }
                }
                catch (FindManager.MalformedReplacementStringException e) {
                    exceptionResult.set(e);
                    return false;
                }
                return true;
            });
        });

        if (!exceptionResult.isNull()) {
            throw exceptionResult.get();
        }
        return result;
    }

    private boolean getStringToReplace(
        int textOffset,
        int textEndOffset,
        Document document,
        FindModel findModel,
        SimpleReference<? super String> stringToReplace
    ) throws FindManager.MalformedReplacementStringException {
        if (textOffset < 0 || textOffset >= document.getTextLength()) {
            return false;
        }
        if (textEndOffset < 0 || textEndOffset > document.getTextLength()) {
            return false;
        }
        FindManager findManager = FindManager.getInstance(myProject);
        CharSequence foundString = document.getCharsSequence().subSequence(textOffset, textEndOffset);
        PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        FindResult findResult =
            findManager.findString(document.getCharsSequence(), textOffset, findModel, file != null ? file.getVirtualFile() : null);
        if (!findResult.isStringFound() ||
            // find result should be in needed range
            !(findResult.getStartOffset() >= textOffset && findResult.getEndOffset() <= textEndOffset)) {
            return false;
        }

        stringToReplace.set(FindManager.getInstance(myProject)
            .getStringToReplace(foundString.toString(), findModel, textOffset, document.getText()));

        return true;
    }

    @RequiredUIAccess
    private void replaceUsagesUnderCommand(@Nonnull ReplaceContext replaceContext, @Nonnull Set<? extends Usage> usagesSet) {
        if (usagesSet.isEmpty()) {
            return;
        }

        List<Usage> usages = new ArrayList<>(usagesSet);
        Collections.sort(usages, UsageViewImpl.USAGE_COMPARATOR);

        if (!ensureUsagesWritable(replaceContext, usages)) {
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .name(FindLocalize.findReplaceCommand())
            .run(() -> {
                boolean success = replaceUsages(replaceContext, usages);
                UsageView usageView = replaceContext.getUsageView();

                if (closeUsageViewIfEmpty(usageView, success)) {
                    return;
                }
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
                    () -> IdeFocusManager.getGlobalInstance().requestFocus(usageView.getPreferredFocusableComponent(), true)
                );
            });

        replaceContext.invalidateExcludedSetCache();
    }

    @RequiredUIAccess
    private boolean ensureUsagesWritable(ReplaceContext replaceContext, Collection<? extends Usage> selectedUsages) {
        Set<VirtualFile> readOnlyFiles = null;
        for (Usage usage : selectedUsages) {
            VirtualFile file = ((UsageInFile)usage).getFile();

            if (file != null && !file.isWritable()) {
                if (readOnlyFiles == null) {
                    readOnlyFiles = new HashSet<>();
                }
                readOnlyFiles.add(file);
            }
        }

        if (readOnlyFiles != null) {
            ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(readOnlyFiles);
        }

        if (hasReadOnlyUsages(selectedUsages)) {
            int result = Messages.showOkCancelDialog(
                replaceContext.getUsageView().getComponent(),
                FindLocalize.findReplaceOccurrencesInReadOnlyFilesPrompt().get(),
                FindLocalize.findReplaceOccurrencesInReadOnlyFilesTitle().get(),
                UIUtil.getWarningIcon()
            );
            if (result != Messages.OK) {
                return false;
            }
        }
        return true;
    }

    private boolean closeUsageViewIfEmpty(UsageView usageView, boolean success) {
        if (usageView.getUsages().isEmpty()) {
            usageView.close();
            return true;
        }
        if (!success) {
            myNotificationService.newError(NOTIFICATION_GROUP)
                .content(LocalizeValue.localizeTODO("One or more malformed replacement strings"))
                .notify(myProject);
        }
        return false;
    }

    public boolean isWorkInProgress() {
        return myIsFindInProgress;
    }

    public boolean isEnabled() {
        return !myIsFindInProgress && !FindInProjectManager.getInstance(myProject).isWorkInProgress();
    }

    private class UsageSearcherFactory implements Supplier<UsageSearcher> {
        private final FindModel myFindModelCopy;
        private final FindUsagesProcessPresentation myProcessPresentation;

        private UsageSearcherFactory(@Nonnull FindModel findModelCopy, @Nonnull FindUsagesProcessPresentation processPresentation) {
            myFindModelCopy = findModelCopy;
            myProcessPresentation = processPresentation;
        }

        @Override
        public UsageSearcher get() {
            return processor -> {
                try {
                    myIsFindInProgress = true;

                    FindInProjectUtil.findUsages(
                        myFindModelCopy,
                        myProject,
                        new AdapterProcessor<>(processor, UsageInfo2UsageAdapter.CONVERTER),
                        myProcessPresentation
                    );
                }
                finally {
                    myIsFindInProgress = false;
                }
            };
        }
    }
}

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
import consulo.find.*;
import consulo.find.localize.FindLocalize;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.find.actions.FindInPathAction;
import consulo.ide.impl.idea.find.findInProject.FindInProjectManager;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.usages.impl.UsageViewImpl;
import consulo.ide.impl.idea.util.AdapterProcessor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.undoRedo.CommandProcessor;
import consulo.usage.*;
import consulo.usage.rule.UsageInFile;
import consulo.util.lang.ref.Ref;
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
    private boolean myIsFindInProgress;

    public static ReplaceInProjectManager getInstance(Project project) {
        return ServiceManager.getService(project, ReplaceInProjectManager.class);
    }

    @Inject
    public ReplaceInProjectManager(Project project) {
        myProject = project;
    }

    private static boolean hasReadOnlyUsages(final Collection<? extends Usage> usages) {
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
    public void replaceInProject(@Nonnull DataContext dataContext, @Nullable FindModel model) {
        final FindManager findManager = FindManager.getInstance(myProject);
        final FindModel findModel;

        final boolean isOpenInNewTabEnabled;
        final boolean toOpenInNewTab;
        final Content selectedContent = UsageViewContentManager.getInstance(myProject).getSelectedContent(true);
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

        findManager.showFindDialog(findModel, () -> {
            if (findModel.isReplaceState()) {
                replaceInPath(findModel);
            }
            else {
                FindInProjectManager.getInstance(myProject).findInPath(findModel);
            }
        });
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
        final FindModel findModelCopy = findModel.clone();

        final UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModelCopy);
        final FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation(myProject, true, presentation);
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
        final ReplaceInProjectTarget target = new ReplaceInProjectTarget(myProject, findModelCopy);
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
                public void findingUsagesFinished(final UsageView usageView) {
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
            if (usage instanceof UsageInfo2UsageAdapter) {
                files.add(((UsageInfo2UsageAdapter)usage).getFile());
            }
        }
        return files;
    }

    private static Set<Usage> getAllUsagesForFile(@Nonnull ReplaceContext replaceContext, @Nonnull VirtualFile file) {
        Set<Usage> usages = replaceContext.getUsageView().getUsages();
        Set<Usage> result = new LinkedHashSet<>();
        for (Usage usage : usages) {
            if (usage instanceof UsageInfo2UsageAdapter usageAdapter && Comparing.equal(usageAdapter.getFile(), file)) {
                result.add(usage);
            }
        }
        return result;
    }

    private void addReplaceActions(final ReplaceContext replaceContext) {
        final AbstractAction replaceAllAction = new AbstractAction(FindLocalize.findReplaceAllAction().get()) {
            {
                KeyStroke altShiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
                putValue(ACCELERATOR_KEY, altShiftEnter);
                putValue(SHORT_DESCRIPTION, KeymapUtil.getKeystrokeText(altShiftEnter));
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
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
            public boolean isEnabled() {
                return !replaceContext.getUsageView().getUsages().isEmpty();
            }
        };
        replaceContext.getUsageView().addButtonToLowerPane(replaceAllAction);

        final AbstractAction replaceSelectedAction = new AbstractAction() {
            {
                KeyStroke altEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
                putValue(ACCELERATOR_KEY, altEnter);
                putValue(LONG_DESCRIPTION, KeymapUtil.getKeystrokeText(altEnter));
                putValue(SHORT_DESCRIPTION, KeymapUtil.getKeystrokeText(altEnter));
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                replaceUsagesUnderCommand(replaceContext, replaceContext.getUsageView().getSelectedUsages());
            }

            @Override
            public Object getValue(String key) {
                return Action.NAME.equals(key)
                    ? FindLocalize.findReplaceSelectedAction(replaceContext.getUsageView().getSelectedUsages().size()).get()
                    : super.getValue(key);
            }

            @Override
            public boolean isEnabled() {
                return !replaceContext.getUsageView().getSelectedUsages().isEmpty();
            }
        };

        replaceContext.getUsageView().addButtonToLowerPane(replaceSelectedAction);

        final AbstractAction replaceAllInThisFileAction = new AbstractAction() {
            {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(ActionEvent e) {
                Set<VirtualFile> files = getFiles(replaceContext, true);
                if (files.size() == 1) {
                    replaceUsagesUnderCommand(replaceContext, getAllUsagesForFile(replaceContext, files.iterator().next()));
                }
            }

            @Override
            public Object getValue(String key) {
                return Action.NAME.equals(key)
                    ? FindBundle.message(
                    "find.replace.this.file.action",
                    replaceContext.getUsageView().getSelectedUsages().size()
                )
                    : super.getValue(key);
            }

            @Override
            public boolean isEnabled() {
                return getFiles(replaceContext, true).size() == 1;
            }
        };

        //replaceContext.getUsageView().addButtonToLowerPane(replaceAllInThisFileAction);

        final AbstractAction skipThisFileAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Set<VirtualFile> files = getFiles(replaceContext, true);
                if (files.size() != 1) {
                    return;
                }
                VirtualFile selectedFile = files.iterator().next();
                Set<Usage> toSkip = getAllUsagesForFile(replaceContext, selectedFile);
                Usage usageToSelect = ((UsageViewImpl)replaceContext.getUsageView()).getNextToSelect(toSkip);
                replaceContext.getUsageView().excludeUsages(toSkip.toArray(Usage.EMPTY_ARRAY));
                if (usageToSelect != null) {
                    replaceContext.getUsageView().selectUsages(new Usage[]{usageToSelect});
                }
                else {
                    replaceContext.getUsageView().selectUsages(Usage.EMPTY_ARRAY);
                }
            }

            @Override
            public Object getValue(String key) {
                return Action.NAME.equals(key)
                    ? FindBundle.message(
                    "find.replace.skip.this.file.action",
                    replaceContext.getUsageView().getSelectedUsages().size()
                )
                    : super.getValue(key);
            }

            @Override
            public boolean isEnabled() {
                Set<VirtualFile> files = getFiles(replaceContext, true);
                if (files.size() != 1) {
                    return false;
                }
                VirtualFile selectedFile = files.iterator().next();
                Set<Usage> toSkip = getAllUsagesForFile(replaceContext, selectedFile);
                return ((UsageViewImpl)replaceContext.getUsageView()).getNextToSelect(toSkip) != null;
            }
        };

        //replaceContext.getUsageView().addButtonToLowerPane(skipThisFileAction);
    }

    @RequiredUIAccess
    private boolean replaceUsages(@Nonnull ReplaceContext replaceContext, @Nonnull Collection<Usage> usages) {
        if (!ensureUsagesWritable(replaceContext, usages)) {
            return true;
        }

        int[] replacedCount = {0};
        final boolean[] success = {true};

        success[0] &= ((ApplicationEx)Application.get()).runWriteActionWithCancellableProgressInDispatchThread(
            FindLocalize.findReplaceAllConfirmationTitle().get(),
            myProject,
            null,
            indicator -> {
                indicator.setIndeterminate(false);
                int processed = 0;
                VirtualFile lastFile = null;

                for (final Usage usage : usages) {
                    ++processed;
                    indicator.checkCanceled();
                    indicator.setFraction((float)processed / usages.size());

                    if (usage instanceof UsageInFile) {
                        VirtualFile virtualFile = ((UsageInFile)usage).getFile();
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

    public static void reportNumberReplacedOccurrences(Project project, int occurrences) {
        if (occurrences != 0) {
            final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(FindLocalize.zeroOccurrencesReplaced(occurrences).get());
            }
        }
    }

    public boolean replaceUsage(
        @Nonnull final Usage usage,
        @Nonnull final FindModel findModel,
        @Nonnull final Set<Usage> excludedSet,
        final boolean justCheck
    ) throws FindManager.MalformedReplacementStringException {
        final Ref<FindManager.MalformedReplacementStringException> exceptionResult = Ref.create();
        final boolean result = WriteAction.compute(() -> {
            if (excludedSet.contains(usage)) {
                return false;
            }

            final Document document = ((UsageInfo2UsageAdapter)usage).getDocument();
            if (!document.isWritable()) {
                return false;
            }

            return ((UsageInfo2UsageAdapter)usage).processRangeMarkers(segment -> {
                final int textOffset = segment.getStartOffset();
                final int textEndOffset = segment.getEndOffset();
                final Ref<String> stringToReplace = Ref.create();
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
        Ref<? super String> stringToReplace
    ) throws FindManager.MalformedReplacementStringException {
        if (textOffset < 0 || textOffset >= document.getTextLength()) {
            return false;
        }
        if (textEndOffset < 0 || textEndOffset > document.getTextLength()) {
            return false;
        }
        FindManager findManager = FindManager.getInstance(myProject);
        final CharSequence foundString = document.getCharsSequence().subSequence(textOffset, textEndOffset);
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
    private void replaceUsagesUnderCommand(@Nonnull final ReplaceContext replaceContext, @Nonnull final Set<? extends Usage> usagesSet) {
        if (usagesSet.isEmpty()) {
            return;
        }

        final List<Usage> usages = new ArrayList<>(usagesSet);
        Collections.sort(usages, UsageViewImpl.USAGE_COMPARATOR);

        if (!ensureUsagesWritable(replaceContext, usages)) {
            return;
        }

        CommandProcessor.getInstance().executeCommand(
            myProject,
            () -> {
                final boolean success = replaceUsages(replaceContext, usages);
                final UsageView usageView = replaceContext.getUsageView();

                if (closeUsageViewIfEmpty(usageView, success)) {
                    return;
                }
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
                    () -> IdeFocusManager.getGlobalInstance().requestFocus(usageView.getPreferredFocusableComponent(), true)
                );
            },
            FindLocalize.findReplaceCommand().get(),
            null
        );

        replaceContext.invalidateExcludedSetCache();
    }

    @RequiredUIAccess
    private boolean ensureUsagesWritable(ReplaceContext replaceContext, Collection<? extends Usage> selectedUsages) {
        Set<VirtualFile> readOnlyFiles = null;
        for (final Usage usage : selectedUsages) {
            final VirtualFile file = ((UsageInFile)usage).getFile();

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
            NOTIFICATION_GROUP.createNotification("One or more malformed replacement strings", NotificationType.ERROR).notify(myProject);
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

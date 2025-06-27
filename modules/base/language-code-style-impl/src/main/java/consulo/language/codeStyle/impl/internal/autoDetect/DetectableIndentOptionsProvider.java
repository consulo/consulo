// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.impl.internal.autoDetect;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.document.Document;
import consulo.language.codeStyle.*;
import consulo.language.codeStyle.impl.internal.TimeStampedIndentOptions;
import consulo.language.codeStyle.internal.CodeStyleInternalHelper;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiBinaryFile;
import consulo.language.psi.PsiCompiledFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.*;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.Maps;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static consulo.language.codeStyle.CommonCodeStyleSettings.IndentOptions;

/**
 * @author Rustam Vishnyakov
 */
@ExtensionImpl(order = "last")
public class DetectableIndentOptionsProvider extends FileIndentOptionsProvider {
    static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup(
        "automaticIndentDetection",
        ApplicationLocalize.notificationGroupAutomaticIndentDetection(),
        NotificationDisplayType.STICKY_BALLOON,
        true
    );

    private boolean myIsEnabledInTest;
    private final Map<VirtualFile, IndentOptions> myDiscardedOptions = Maps.newWeakHashMap();

    @Nullable
    @Override
    @RequiredReadAction
    public IndentOptions getIndentOptions(@Nonnull CodeStyleSettings settings, @Nonnull PsiFile file) {
        if (!isEnabled(settings, file)) {
            return null;
        }

        Project project = file.getProject();
        PsiDocumentManager psiManager = PsiDocumentManager.getInstance(project);
        Document document = psiManager.getDocument(file);
        if (document == null) {
            return null;
        }

        TimeStampedIndentOptions options;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (document) {
            options = getValidCachedIndentOptions(file, document);

            if (options != null) {
                return options;
            }

            options = DetectAndAdjustIndentOptionsTask.getDefaultIndentOptions(file, document);
            options.associateWithDocument(document);
        }

        scheduleDetectionInBackground(project, document, options);

        return options;
    }

    protected void scheduleDetectionInBackground(
        @Nonnull Project project,
        @Nonnull Document document,
        @Nonnull TimeStampedIndentOptions options
    ) {
        new DetectAndAdjustIndentOptionsTask(project, document, options).scheduleInBackgroundForCommittedDocument();
    }

    @Override
    public boolean useOnFullReformat() {
        return false;
    }

    @TestOnly
    public void setEnabledInTest(boolean isEnabledInTest) {
        myIsEnabledInTest = isEnabledInTest;
    }

    @RequiredReadAction
    private boolean isEnabled(@Nonnull CodeStyleSettings settings, @Nonnull PsiFile file) {
        if (!file.isValid() || !file.isWritable() || file instanceof PsiBinaryFile
            || file instanceof PsiCompiledFile || ScratchUtil.isScratch(file.getVirtualFile())) {
            return false;
        }
        if (Application.get().isUnitTestMode()) {
            return myIsEnabledInTest;
        }
        VirtualFile vFile = file.getVirtualFile();
        return !(vFile == null || vFile instanceof LightVirtualFile || myDiscardedOptions.containsKey(vFile))
            && FormattingModelBuilder.forContext(file) != null && settings.AUTODETECT_INDENTS;
    }

    @TestOnly
    @Nullable
    public static DetectableIndentOptionsProvider getInstance() {
        return FileIndentOptionsProvider.EP_NAME.findExtension(DetectableIndentOptionsProvider.class);
    }

    private void disableForFile(@Nonnull VirtualFile file, @Nonnull IndentOptions indentOptions) {
        myDiscardedOptions.put(file, indentOptions);
    }

    public TimeStampedIndentOptions getValidCachedIndentOptions(PsiFile file, Document document) {
        if (IndentOptions.retrieveFromAssociatedDocument(file) instanceof TimeStampedIndentOptions cachedInDocument) {
            IndentOptions defaultIndentOptions = DetectAndAdjustIndentOptionsTask.getDefaultIndentOptions(file, document);
            if (!cachedInDocument.isOutdated(document, defaultIndentOptions)) {
                return cachedInDocument;
            }
        }
        return null;
    }

    private static void showDisabledDetectionNotification(@Nonnull Project project) {
        NotificationService.getInstance()
            .newInfo(NOTIFICATION_GROUP)
            .title(ApplicationLocalize.codeStyleIndentDetectorNotificationContent())
            .addClosingAction(
                ApplicationLocalize.codeStyleIndentProviderNotificationReEnable(),
                () -> {
                    CodeStyle.getSettings(project).AUTODETECT_INDENTS = true;
                    notifyIndentOptionsChanged(project, null);
                }
            )
            .addClosingAction(
                ApplicationLocalize.codeStyleIndentProviderNotificationSettings(),
                () -> CodeStyleInternalHelper.getInstance().showDetectIndentSettings(project)
            )
            .notify(project);
    }

    private static boolean areDetected(@Nonnull IndentOptions indentOptions) {
        return indentOptions instanceof TimeStampedIndentOptions timeStampedIndentOptions && timeStampedIndentOptions.isDetected();
    }

    @Nullable
    @Override
    public IndentStatusBarUIContributor getIndentStatusBarUiContributor(@Nonnull IndentOptions indentOptions) {
        return new MyUIContributor(indentOptions);
    }

    private final class MyUIContributor extends IndentStatusBarUIContributor {
        private MyUIContributor(IndentOptions options) {
            super(options);
        }

        @Override
        public AnAction[] getActions(@Nonnull PsiFile file) {
            IndentOptions indentOptions = getIndentOptions();
            List<AnAction> actions = new ArrayList<>();
            VirtualFile virtualFile = file.getVirtualFile();
            Project project = file.getProject();
            IndentOptions projectOptions = CodeStyle.getSettings(project).getIndentOptions(file.getFileType());
            LocalizeValue projectOptionsTip = IndentStatusBarUIContributor.getIndentInfo(projectOptions);
            if (indentOptions instanceof TimeStampedIndentOptions timeStampedIndentOptions) {
                if (timeStampedIndentOptions.isDetected()) {
                    actions.add(DumbAwareAction.create(
                        ApplicationLocalize.codeStyleIndentDetectorReject(projectOptionsTip).get(),
                        e -> {
                            disableForFile(virtualFile, indentOptions);
                            notifyIndentOptionsChanged(project, file);
                        }
                    ));
                    actions.add(DumbAwareAction.create(
                        ApplicationLocalize.codeStyleIndentDetectorReindent(projectOptionsTip).get(),
                        e -> {
                            disableForFile(virtualFile, indentOptions);
                            notifyIndentOptionsChanged(project, file);
                            CommandProcessor.getInstance()
                                .runUndoTransparentAction(() -> Application.get()
                                    .runWriteAction(() -> CodeStyleManager.getInstance(project)
                                        .adjustLineIndent(file, file.getTextRange())));
                            myDiscardedOptions.remove(virtualFile);
                        }
                    ));
                    actions.add(AnSeparator.getInstance());
                }
            }
            else if (myDiscardedOptions.containsKey(virtualFile)) {
                IndentOptions discardedOptions = myDiscardedOptions.get(virtualFile);
                Document document = PsiDocumentManager.getInstance(project).getDocument(file);
                if (document != null) {
                    actions.add(DumbAwareAction.create(
                        ApplicationLocalize.codeStyleIndentDetectorApply(
                            IndentStatusBarUIContributor.getIndentInfo(discardedOptions),
                            ColorUtil.toHex(JBColor.GRAY)
                        ),
                        LocalizeValue.empty(),
                        null,
                        e -> {
                            myDiscardedOptions.remove(virtualFile);
                            discardedOptions.associateWithDocument(document);
                            notifyIndentOptionsChanged(project, file);
                        }
                    ));
                    actions.add(AnSeparator.getInstance());
                }
            }
            return actions.toArray(AnAction.EMPTY_ARRAY);
        }

        @Override
        public
        @Nonnull
        AnAction createDisableAction(@Nonnull Project project) {
            return DumbAwareAction.create(
                ApplicationLocalize.codeStyleIndentDetectorDisable().get(),
                e -> {
                    CodeStyle.getSettings(project).AUTODETECT_INDENTS = false;
                    myDiscardedOptions.clear();
                    notifyIndentOptionsChanged(project, null);
                    showDisabledDetectionNotification(project);
                }
            );
        }

        @Override
        public String getHint() {
            if (areDetected(getIndentOptions())) {
                return LanguageLocalize.indentOptionDetected().get();
            }
            return null;
        }

        @Override
        public boolean areActionsAvailable(@Nonnull VirtualFile file) {
            return areDetected(getIndentOptions()) || myDiscardedOptions.containsKey(file);
        }
    }
}

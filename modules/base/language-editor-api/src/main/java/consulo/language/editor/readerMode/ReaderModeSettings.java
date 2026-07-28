// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.readerMode;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSchemes;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.content.FileIndexFacade;
import consulo.language.editor.readerMode.ReaderModeProvider.ReaderMode;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public interface ReaderModeSettings {
    record Scheme(@Nullable String name, boolean isProjectLevel) {
        public Scheme() {
            this(CodeStyleScheme.DEFAULT_SCHEME_NAME, false);
        }
    }

    Key<Boolean> MATCHES_READER_MODE_KEY = Key.create("readerMode.matches");

    static ReaderModeSettings getInstance(Project project) {
        return project.getInstance(ReaderModeSettings.class);
    }

    @RequiredUIAccess
    static void applyReaderMode(Project project, Editor editor, @Nullable VirtualFile file) {
        applyReaderMode(project, editor, file, false, false);
    }

    @RequiredUIAccess
    static void applyReaderMode(
        Project project,
        Editor editor,
        @Nullable VirtualFile file,
        boolean fileIsOpenAlready,
        boolean forceUpdate
    ) {
        if (file == null || !file.isValid()) {
            return;
        }

        if (isBlockingApplication(project.getApplication())) {
            boolean matchMode = ReadAction.compute(() -> matchMode(project, file, editor));
            if (matchMode || forceUpdate) {
                applyModeChanged(project, editor, matchMode, fileIsOpenAlready);
            }
            return;
        }

        // caching is required for instant reopening of file with the previously computed mode without irritating file UI changes
        Boolean matchCachedValue = file.getUserData(MATCHES_READER_MODE_KEY);
        if (!forceUpdate && matchCachedValue != null) {
            if (matchCachedValue) {
                applyModeChanged(project, editor, true, fileIsOpenAlready);
            }
            return;
        }

        ReadAction.<Boolean>nonBlocking(() -> {
                if (!file.isValid()) {
                    return Boolean.FALSE;
                }
                boolean value = matchMode(project, file, editor);
                file.putUserData(MATCHES_READER_MODE_KEY, value);
                return value;
            })
            .finishOnUiThread(
                Application::getDefaultModalityState,
                matchMode -> {
                    if (matchMode || forceUpdate) {
                        applyModeChanged(project, editor, matchMode, fileIsOpenAlready);
                    }
                }
            )
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    @RequiredUIAccess
    private static void applyModeChanged(Project project, Editor editor, boolean matchMode, boolean fileIsOpenAlready) {
        if (editor.isDisposed()) {
            return;
        }

        boolean modeEnabledForFile = getInstance(project).isEnabled() && matchMode;
        project.getApplication()
            .getExtensionPoint(ReaderModeProvider.class)
            .forEach(provider -> provider.applyModeChanged(project, editor, modeEnabledForFile, fileIsOpenAlready));
    }

    @RequiredReadAction
    static boolean matchMode(Project project, VirtualFile file) {
        return matchMode(project, file, null);
    }

    @RequiredReadAction
    static boolean matchMode(Project project, VirtualFile file, @Nullable Editor editor) {
        if (PsiManager.getInstance(project).findFile(file) == null || editor != null && editor.isDisposed()) {
            return false;
        }
        return matchMode(project, file, editor, getInstance(project).getMode());
    }

    @RequiredReadAction
    private static boolean matchMode(Project project, VirtualFile file, @Nullable Editor editor, ReaderMode mode) {
        Application application = project.getApplication();
        if (application.isHeadlessEnvironment()) {
            return false;
        }

        for (ReaderModeMatcher matcher : application.getExtensionList(ReaderModeMatcher.class)) {
            Boolean matched = matcher.matches(project, file, editor, mode);
            if (matched != null) {
                return matched;
            }
        }

        boolean isWritable = file.isWritable();

        return switch (mode) {
            case LIBRARIES_AND_READ_ONLY -> !isWritable || FileIndexFacade.getInstance(project).isInLibrary(file);
            case LIBRARIES -> FileIndexFacade.getInstance(project).isInLibrary(file);
            case READ_ONLY -> !isWritable;
        };
    }

    private static boolean isBlockingApplication(Application application) {
        return application.isHeadlessEnvironment() || application.isUnitTestMode();
    }

    boolean isEnabled();

    void setEnabled(boolean enabled);

    ReaderMode getMode();

    void setMode(ReaderMode mode);

    boolean isShowRenderedDocs();

    void setShowRenderedDocs(boolean showRenderedDocs);

    boolean isShowWarnings();

    void setShowWarnings(boolean showWarnings);

    boolean isShowLigatures();

    void setShowLigatures(boolean showLigatures);

    boolean isIncreaseLineSpacing();

    void setIncreaseLineSpacing(boolean increaseLineSpacing);

    boolean isShowInlaysHints();

    void setShowInlaysHints(boolean showInlaysHints);

    boolean isEnableVisualFormatting();

    void setEnableVisualFormatting(boolean enableVisualFormatting);

    boolean isUseActiveSchemeForVisualFormatting();

    void setUseActiveSchemeForVisualFormatting(boolean useActiveSchemeForVisualFormatting);

    Scheme getVisualFormattingChosenScheme();

    void setVisualFormattingChosenScheme(Scheme scheme);

    @Nullable
    default CodeStyleSettings getVisualFormattingCodeStyleSettings(Project project) {
        if (!isEnableVisualFormatting()) {
            return null;
        }
        if (isUseActiveSchemeForVisualFormatting()) {
            return CodeStyle.getSettings(project);
        }
        CodeStyleSchemes codeStyleSchemes = CodeStyleSchemes.getInstance();
        Scheme chosenScheme = getVisualFormattingChosenScheme();
        CodeStyleSettings settings;
        if (CodeStyleScheme.PROJECT_SCHEME_NAME.equals(chosenScheme.name()) && chosenScheme.isProjectLevel()) {
            settings = CodeStyleSettingsManager.getInstance(project).PER_PROJECT_SETTINGS;
        }
        else {
            String name = chosenScheme.name();
            CodeStyleScheme scheme = name == null ? null : codeStyleSchemes.findSchemeByName(name);
            settings = scheme == null ? null : scheme.getCodeStyleSettings();
        }
        return settings != null ? settings : codeStyleSchemes.getDefaultScheme().getCodeStyleSettings();
    }
}

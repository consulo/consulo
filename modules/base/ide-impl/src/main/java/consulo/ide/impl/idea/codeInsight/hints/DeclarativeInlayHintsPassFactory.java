// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.ide.impl.idea.codeInsight.hints.settings.DeclarativeInlayHintsSettings;
import consulo.ide.impl.idea.codeInsight.hints.settings.InlayHintsSettings;
import consulo.language.editor.Pass;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * Factory for creating declarative inlay hints passes.
 */
@ExtensionImpl
public class DeclarativeInlayHintsPassFactory
    implements TextEditorHighlightingPassFactory, DumbAware {

    private static final Key<Long> PSI_MODIFICATION_STAMP =
        Key.create("declarative.inlays.psi.modification.stamp");

    @RequiredReadAction
    public static DeclarativeInlayHintsPass createPassForPreview(
        PsiFile file,
        Editor editor,
        DeclarativeInlayHintsProvider provider,
        String providerId,
        Map<String, Boolean> optionsToEnabled,
        boolean isDisabled
    ) {
        List<InlayProviderPassInfo> list = Collections.singletonList(
            new InlayProviderPassInfo(provider, providerId, optionsToEnabled)
        );
        return new DeclarativeInlayHintsPass(file, editor, list, true, isDisabled);
    }

    public static List<InlayProviderInfo> getSuitableToFileProviders(PsiFile file) {
        List<InlayProviderInfo> infos = InlayHintsProviderFactory.findProvidersForLanguage(file.getLanguage());
        if (!DumbService.isDumb(file.getProject())) {
            return infos;
        }
        List<InlayProviderInfo> result = new ArrayList<>();
        for (InlayProviderInfo info : infos) {
            if (DumbService.isDumbAware(info.getProvider())) {
                result.add(info);
            }
        }
        return result;
    }

    public static void updateModificationStamp(Editor editor, PsiFile file) {
        updateModificationStamp(editor, file.getProject());
    }

    public static void scheduleRecompute(Editor editor, Project project) {
        resetModificationStamp(editor);
        DaemonCodeAnalyzerEx.getInstanceEx(project)
            .restart("DeclarativeInlayHintsPassFactory.scheduleRecompute");
    }

    static void updateModificationStamp(Editor editor, Project project) {
        long current = PsiModificationTracker.getInstance(project).getModificationCount();
        editor.putUserData(PSI_MODIFICATION_STAMP, current);
    }

    public static void resetModificationStamp() {
        for (Editor e : EditorFactory.getInstance().getAllEditors()) {
            resetModificationStamp(e);
        }
    }

    static void resetModificationStamp(Editor editor) {
        editor.putUserData(PSI_MODIFICATION_STAMP, null);
    }

    @Override
    public DeclarativeInlayHintsPass createHighlightingPass(PsiFile psiFile, Editor editor) {
        if (editor.isOneLineMode()) {
            return null;
        }
        if (!HighlightingLevelManager.getInstance(psiFile.getProject()).shouldHighlight(psiFile)) {
            return null;
        }

        Long stamp = editor.getUserData(PSI_MODIFICATION_STAMP);
        long current = PsiModificationTracker.getInstance(psiFile.getProject()).getModificationCount();
        if (stamp != null && stamp == current) {
            return null;
        }

        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        boolean enabledGlobally = InlayHintsSettings.getInstance().hintsEnabledGlobally();
        List<InlayProviderPassInfo> passProviders;

        if (enabledGlobally) {
            passProviders = new ArrayList<>();
            for (InlayProviderInfo info : getSuitableToFileProviders(psiFile)) {
                boolean providerEnabled = settings.isProviderEnabled(info.getProviderId());
                if (providerEnabled || (!providerEnabled && info.isEnabledByDefault())) {
                    Map<String, Boolean> optionsToEnabled = new HashMap<>();
                    for (var optionInfo : info.getOptions()) {
                        Boolean opt = settings.isOptionEnabled(optionInfo.getId(), info.getProviderId());
                        boolean isOptEnabled = (opt != null) ? opt : optionInfo.isEnabledByDefault();
                        if (optionsToEnabled.put(optionInfo.getId(), isOptEnabled) != null) {
                            throw new IllegalArgumentException("Duplicate option key: " + optionInfo.getId());
                        }
                    }
                    passProviders.add(new InlayProviderPassInfo(
                        info.getProvider(), info.getProviderId(), optionsToEnabled));
                }
            }
        }
        else {
            passProviders = Collections.emptyList();
        }

        return new DeclarativeInlayHintsPass(psiFile, editor, passProviders, false);
    }

    @Override
    public void register(@Nonnull Registrar registrar) {
        int[] deps = new int[]{Pass.UPDATE_ALL};
        registrar.registerTextEditorHighlightingPass(
            this, deps, null, false, -1);
    }
}

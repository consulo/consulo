// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.ContributedReferencesAnnotator;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.HighlightedReference;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.impl.psi.path.WebReference;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceService;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HyperlinkAnnotator implements Annotator, DumbAware {
    private static final Key<LocalizeValue> MESSAGE_KEY = Key.create("hyperlink.message");

    @Override
    @RequiredReadAction
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (holder.isBatchMode()) {
            return;
        }

        if (WebReference.isWebReferenceWorthy(element)) {
            annotateContributedReferences(element, holder);
        }
    }

    @RequiredReadAction
    private static void annotateContributedReferences(PsiElement element, AnnotationHolder holder) {
        List<PsiReference> references = getReferences(element);

        if (!annotateHyperlinks(element, holder, references)) {
            return;
        }

        List<ContributedReferencesAnnotator> annotators =
            ContributedReferencesAnnotator.allForLanguageOrAny(holder.getCurrentAnnotationSession().getFile().getLanguage());

        for (ContributedReferencesAnnotator annotator : annotators) {
            annotator.annotate(element, references, holder);
        }
    }

    @RequiredReadAction
    public static List<PsiReference> calculateReferences(PsiElement element) {
        return PsiReferenceService.getService().getReferences(element, PsiReferenceService.Hints.HIGHLIGHTED_REFERENCES);
    }

    private static final Key<ParameterizedCachedValue<List<PsiReference>, PsiElement>> REFS_KEY = Key.create("HyperlinkAnnotator");
    private static final ParameterizedCachedValueProvider<List<PsiReference>, PsiElement> REFS_PROVIDER = element -> {
        List<PsiReference> references;
        try {
            references = calculateReferences(element);
        }
        catch (IndexNotReadyException ignored) {
            return CachedValueProvider.Result.create(List.of(), DumbService.getInstance(element.getProject()).getModificationTracker());
        }

        if (references.isEmpty()) {
            references = List.of();
        }
        return CachedValueProvider.Result.create(references, PsiModificationTracker.MODIFICATION_COUNT);
    };

    private static List<PsiReference> getReferences(PsiElement element) {
        return CachedValuesManager.getManager(element.getProject())
            .getParameterizedCachedValue(element, REFS_KEY, REFS_PROVIDER, false, element);
    }

    @RequiredReadAction
    private static boolean annotateHyperlinks(PsiElement element, AnnotationHolder holder, List<PsiReference> references) {
        boolean hasUnprocessedReferences = false;
        Set<TextRange> highlighted = new HashSet<>();
        for (PsiReference reference : references) {
            if (reference instanceof WebReference webReference) {
                LocalizeValue message = holder.getCurrentAnnotationSession().getUserData(MESSAGE_KEY);
                if (message == null) {
                    message = getMessage();
                    holder.getCurrentAnnotationSession().putUserData(MESSAGE_KEY, message);
                }
                TextRange range = reference.getRangeInElement().shiftRight(element.getTextRange().getStartOffset());
                if (webReference.getHighlight() && highlighted.add(range)) {
                    holder.newAnnotation(HighlightSeverity.INFORMATION, message)
                        .range(range)
                        .textAttributes(CodeInsightColors.INACTIVE_HYPERLINK_ATTRIBUTES)
                        .create();
                }
            }
            else if (reference instanceof HighlightedReference highlightedReference) {
                if (reference.isSoft() && !highlightedReference.isHighlightedWhenSoft()) {
                    continue;
                }

                TextRange rangeInElement = reference.getRangeInElement();
                if (rangeInElement.isEmpty()) {
                    continue;
                }

                TextRange range = rangeInElement.shiftRight(element.getTextRange().getStartOffset());
                holder.newSilentAnnotation(HighlightInfoType.HIGHLIGHTED_REFERENCE_SEVERITY)
                    .range(range)
                    .textAttributes(DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE)
                    .create();
            }
            else {
                hasUnprocessedReferences = true;
            }
        }
        return hasUnprocessedReferences;
    }

    public static LocalizeValue getMessage() {
        LocalizeValue message = CodeInsightLocalize.openUrlInBrowserTooltip();
        String shortcutsText = getGoToDeclarationShortcutsText();
        if (!shortcutsText.isEmpty()) {
            return LocalizeValue.join(message, LocalizeValue.of(" (" + shortcutsText + ")"));
        }
        return message;
    }

    /**
     * Returns a comma-separated list of shortcuts assigned to the 'Go To Declaration' action.
     * This list includes up to two shortcuts: at most one mouse shortcut and one keyboard shortcut.
     * If no shortcuts are assigned, this method returns an empty string.
     */
    public static String getGoToDeclarationShortcutsText() {
        Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_GOTO_DECLARATION);
        String shortcutText = "";
        Shortcut mouseShortcut = ContainerUtil.find(shortcuts, shortcut -> !shortcut.isKeyboard());
        if (mouseShortcut != null) {
            shortcutText += KeymapUtil.getShortcutText(mouseShortcut);
            shortcutText = shortcutText.replace("Button1 ", "");
        }
        Shortcut keyboardShortcut = ContainerUtil.find(shortcuts, shortcut -> shortcut.isKeyboard());
        if (keyboardShortcut != null) {
            if (!shortcutText.isEmpty()) {
                shortcutText += ", ";
            }
            shortcutText += KeymapUtil.getShortcutText(keyboardShortcut);
        }
        if (!shortcutText.isEmpty()) {
            return shortcutText;
        }
        return "";
    }
}

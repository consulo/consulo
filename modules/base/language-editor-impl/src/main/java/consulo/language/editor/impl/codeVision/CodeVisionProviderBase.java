// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.codeVision;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.FileHighlightingSetting;
import consulo.language.editor.codeVision.*;
import consulo.language.editor.internal.HighlightingSettingsPerFile;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CodeVisionProviderBase implements DaemonBoundCodeVisionProvider {

    /**
     * WARNING: This method is executed also before the file is open. It must be fast!
     *
     * @return true iff this provider may provide lenses for this file.
     */
    public abstract boolean acceptsFile(PsiFile file);

    /**
     * WARNING: This method is executed also before the file is open. It must be fast!
     *
     * @return true iff this provider may provide lenses for this element.
     */
    public abstract boolean acceptsElement(PsiElement element);

    /**
     * @return text that the user sees for a given element as a code lens
     */
    public abstract @Nullable String getHint(PsiElement element, PsiFile file);

    /**
     * @return vision info (text + optional count) for a given element, or null if no lens
     */
    public @Nullable CodeVisionInfo getVisionInfo(PsiElement element, PsiFile file) {
        String hint = getHint(element, file);
        if (hint == null) return null;
        return new CodeVisionInfo(hint);
    }

    /**
     * Called when user clicks the code vision entry.
     */
    public abstract void handleClick(Editor editor, PsiElement element, @Nullable MouseEvent event);

    /**
     * Override to log feature usage statistics on click.
     */
    public void logClickToFUS(PsiElement element, String hint) {
    }

    @Override
    @RequiredReadAction
    public List<Pair<TextRange, CodeVisionEntry>> computeForEditor(Editor editor, PsiFile file) {
        Project project = file.getProject();
        if (project.isDefault()) return Collections.emptyList();
        if (!acceptsFile(file)) return Collections.emptyList();

        HighlightingSettingsPerFile settings = HighlightingSettingsPerFile.getInstance(project);
        if (settings.getHighlightingSettingForRoot(file) != FileHighlightingSetting.FORCE_HIGHLIGHTING) {
            return Collections.emptyList();
        }

        if (ProjectFileIndex.getInstance(project).isInLibrarySource(file.getViewProvider().getVirtualFile())) {
            return Collections.emptyList();
        }

        List<Pair<TextRange, CodeVisionEntry>> lenses = new ArrayList<>();
        SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(file);
        for (PsiElement element : traverser) {
            if (!acceptsElement(element)) continue;
            if (!isFirstInLine(element)) continue;
            String hint = getHint(element, file);
            if (hint == null) continue;
            ClickHandler handler = new ClickHandler(SmartPointerManager.createPointer(element), hint, this);
            TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(element);
            lenses.add(Pair.create(range, new ClickableTextCodeVisionEntry(hint, getId(), handler)));
        }
        return lenses;
    }

    @Override
    public @Nullable CodeVisionPlaceholderCollector getPlaceholderCollector(Editor editor, @Nullable PsiFile psiFile) {
        if (psiFile == null || !acceptsFile(psiFile)) return null;
        return (BypassBasedPlaceholderCollector) (element, e) -> {
            if (!acceptsElement(element)) return Collections.emptyList();
            TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(element);
            return List.of(range);
        };
    }

    @Override
    public CodeVisionAnchorKind getDefaultAnchor() {
        return CodeVisionAnchorKind.Default;
    }

    // -------------------------------------------------------------------------
    // PSI utilities (equivalent to InlayHintsUtils in ide-impl)
    // -------------------------------------------------------------------------

    @RequiredReadAction
    private static TextRange getTextRangeWithoutLeadingCommentsAndWhitespaces(PsiElement element) {
        PsiElement start = element;
        for (PsiElement child : SyntaxTraverser.psiApi().children(element)) {
            if (!(child instanceof PsiComment) && !(child instanceof PsiWhiteSpace)) {
                start = child;
                break;
            }
        }
        return TextRange.create(start.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
    }

    @RequiredReadAction
    private static boolean isFirstInLine(PsiElement element) {
        PsiElement prev = PsiTreeUtil.prevLeaf(element, true);
        if (prev == null) {
            return true;
        }
        while (prev instanceof PsiWhiteSpace ws) {
            String text = ws.getText();
            if (text.contains("\n") || ws.getTextRange().getStartOffset() == 0) {
                return true;
            }
            prev = PsiTreeUtil.prevLeaf(prev, true);
        }
        return false;
    }

    // -------------------------------------------------------------------------

    private record ClickHandler(
        SmartPsiElementPointer<PsiElement> elementPointer,
        String hint,
        CodeVisionProviderBase provider
    ) implements ClickableTextCodeVisionEntry.CodeVisionClickHandler {
        @Override
        public void onClick(@Nullable MouseEvent event, Editor editor) {
            PsiElement element = elementPointer().getElement();
            if (element == null) return;
            provider().logClickToFUS(element, hint());
            provider().handleClick(editor, element, event);
        }
    }

    /**
     * Code vision item information.
     *
     * @param text          Label of the item displayed in the interline
     * @param count         If the item represents a counter, the count; null otherwise
     * @param countIsExact  Whether the counter is exact or a lower-bound estimate
     */
    public record CodeVisionInfo(String text, @Nullable Integer count, boolean countIsExact) {
        public CodeVisionInfo(String text) {
            this(text, null, true);
        }

        public CodeVisionInfo(String text, int count) {
            this(text, count, true);
        }
    }
}

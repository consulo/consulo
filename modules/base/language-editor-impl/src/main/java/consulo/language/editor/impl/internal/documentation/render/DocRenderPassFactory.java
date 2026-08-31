// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.editor.documentation.InlineDocumentation;
import consulo.language.editor.documentation.InlineDocumentationProvider;
import consulo.language.editor.Pass;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.impl.highlight.EditorBoundHighlightingPass;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.CharArrayUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class DocRenderPassFactory implements TextEditorHighlightingPassFactory, DumbAware {
    private static final Logger LOG = Logger.getInstance(DocRenderPassFactory.class);

    private static final Key<Long> MODIFICATION_STAMP = Key.create("doc.render.modification.stamp");
    private static final Key<Boolean> RESET_TO_DEFAULT = Key.create("doc.render.reset.to.default");
    private static final Key<Boolean> ICONS_ENABLED = Key.create("doc.render.icons.enabled");

    @Override
    public void register(Registrar registrar) {
        registrar.registerTextEditorHighlightingPass(this, Registrar.Anchor.AFTER, Pass.UPDATE_FOLDING, false);
    }

    @Override
    public @Nullable TextEditorHighlightingPass createHighlightingPass(PsiFile psiFile, Editor editor) {
        long current = PsiModificationTracker.getInstance(psiFile.getProject()).getModificationCount();
        boolean iconsEnabled = DocRenderDummyLineMarkerProvider.isGutterIconEnabled();
        Long existing = editor.getUserData(MODIFICATION_STAMP);
        Boolean iconsWereEnabled = editor.getUserData(ICONS_ENABLED);
        return editor.getProject() == null
            || existing != null && existing == current && iconsWereEnabled != null && iconsWereEnabled == iconsEnabled
            ? null : new DocRenderPass(editor, psiFile);
    }

    public static void forceRefreshOnNextPass(Editor editor) {
        editor.putUserData(MODIFICATION_STAMP, null);
        editor.putUserData(RESET_TO_DEFAULT, Boolean.TRUE);
    }

    private static final class DocRenderPass extends EditorBoundHighlightingPass implements DumbAware {
        private volatile Items myItems;

        DocRenderPass(Editor editor, PsiFile psiFile) {
            super(editor, psiFile, false);
        }

        @Override
        public void doCollectInformation(ProgressIndicator progress) {
            myItems = calculateItemsToRender(myEditor, myFile);
        }

        @Override
        public void doApplyInformationToEditor() {
            boolean resetToDefault =
                myEditor.getUserData(MODIFICATION_STAMP) == null || myEditor.getUserData(RESET_TO_DEFAULT) != null;
            myEditor.putUserData(RESET_TO_DEFAULT, null);
            applyItemsToRender(myEditor, myProject, myItems, resetToDefault && DocRenderManager.isDocRenderingEnabled(myEditor));
        }
    }

    @RequiredReadAction
    public static Items calculateItemsToRender(Editor editor, PsiFile psiFile) {
        boolean enabled = DocRenderManager.isDocRenderingEnabled(editor);
        return calculateItemsToRender(editor.getDocument(), psiFile, enabled);
    }

    @RequiredReadAction
    static Items calculateItemsToRender(Document document, PsiFile psiFile, boolean enabled) {
        Items items = new Items();
        for (InlineDocumentation documentation : inlineDocumentationItems(psiFile)) {
            TextRange range = documentation.getDocumentationRange();
            if (isValidRange(document, range)) {
                String textToRender = enabled ? calcText(documentation) : null;
                items.addItem(new Item(range, textToRender));
            }
        }
        return items;
    }

    @RequiredReadAction
    private static Collection<InlineDocumentation> inlineDocumentationItems(PsiFile psiFile) {
        List<InlineDocumentation> result = new ArrayList<>();
        psiFile.getProject().getApplication()
            .getExtensionPoint(InlineDocumentationProvider.class)
            .forEach(provider -> result.addAll(provider.inlineDocumentationItems(psiFile)));
        return result;
    }

    private static boolean isValidRange(Document document, TextRange range) {
        int startOffset = range.getStartOffset();
        int endOffset = range.getEndOffset();
        int textLength = document.getTextLength();
        if (startOffset >= textLength || endOffset > textLength) {
            LOG.error("Invalid range: " + range + " while document length is " + textLength);
            return false;
        }

        CharSequence text = document.getImmutableCharSequence();
        int startLine = document.getLineNumber(startOffset);
        int endLine = document.getLineNumber(endOffset);
        if (!CharArrayUtil.containsOnlyWhiteSpaces(text.subSequence(document.getLineStartOffset(startLine), startOffset)) ||
            !CharArrayUtil.containsOnlyWhiteSpaces(text.subSequence(endOffset, document.getLineEndOffset(endLine)))) {
            return false;
        }
        return startLine < endLine || document.getLineStartOffset(startLine) < document.getLineEndOffset(endLine);
    }

    @RequiredReadAction
    static String calcText(@Nullable InlineDocumentation documentation) {
        String text = documentation == null ? null : documentation.renderText();
        return text == null ? "Documentation is not available" : text;
    }

    public static void applyItemsToRender(Editor editor, Project project, Items items, boolean collapseNewRegions) {
        editor.putUserData(MODIFICATION_STAMP, PsiModificationTracker.getInstance(project).getModificationCount());
        editor.putUserData(ICONS_ENABLED, DocRenderDummyLineMarkerProvider.isGutterIconEnabled());
        DocRenderItemManager.getInstance().setItemsToEditor(editor, items, collapseNewRegions);
    }

    public static final class Items implements Iterable<Item> {
        private final Map<TextRange, Item> myItems = new LinkedHashMap<>();

        public boolean isEmpty() {
            return myItems.isEmpty();
        }

        private void addItem(Item item) {
            myItems.put(item.textRange, item);
        }

        @Nullable
        Item removeItem(Segment textRange) {
            return myItems.remove(TextRange.create(textRange));
        }

        @Override
        public Iterator<Item> iterator() {
            return myItems.values().iterator();
        }
    }

    public static final class Item {
        public final TextRange textRange;
        public final String textToRender;

        public Item(TextRange textRange, @Nullable String textToRender) {
            this.textRange = textRange;
            this.textToRender = textToRender;
        }
    }
}

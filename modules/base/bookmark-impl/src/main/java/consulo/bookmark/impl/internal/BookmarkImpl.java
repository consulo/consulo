/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.bookmark.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.bookmark.Bookmark;
import consulo.bookmark.internal.BookmarkIcon;
import consulo.bookmark.localize.BookmarkLocalize;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.*;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class BookmarkImpl implements Bookmark {
    private final VirtualFile myFile;
    @Nonnull
    private final OpenFileDescriptor myTarget;
    private final Project myProject;

    private String myDescription;
    private char myMnemonic = 0;

    public BookmarkImpl(
        @Nonnull Project project,
        @Nonnull VirtualFile file,
        int line,
        @Nonnull String description,
        boolean addHighlighter
    ) {
        myFile = file;
        myProject = project;
        myDescription = description;

        myTarget = OpenFileDescriptorFactory.getInstance(project).newBuilder(file).line(line).persist().build();

        if (addHighlighter) {
            addHighlighter();
        }
    }

    public void updateHighlighter() {
        release();
        addHighlighter();
    }

    void addHighlighter() {
        Document document = FileDocumentManager.getInstance().getCachedDocument(getFile());
        if (document != null) {
            createHighlighter(DocumentMarkupModel.forDocument(document, myProject, true));
        }
    }

    public RangeHighlighter createHighlighter(@Nonnull MarkupModelEx markup) {
        RangeHighlighterEx highlighter;
        int line = getLine();
        if (line >= 0) {
            highlighter = markup.addPersistentLineHighlighter(line, HighlighterLayer.ERROR + 1, null);
            if (highlighter != null) {
                highlighter.setGutterIconRenderer(new MyGutterIconRenderer(this));
                highlighter.setTextAttributesKey(CodeInsightColors.BOOKMARKS_ATTRIBUTES);
            }
        }
        else {
            highlighter = null;
        }
        return highlighter;
    }

    @Override
    public Document getDocument() {
        return FileDocumentManager.getInstance().getDocument(getFile());
    }

    public void release() {
        int line = getLine();
        if (line < 0) {
            return;
        }
        Document document = getDocument();
        if (document == null) {
            return;
        }
        MarkupModelEx markup = DocumentMarkupModel.forDocument(document, myProject, true);
        Document markupDocument = markup.getDocument();
        if (markupDocument.getLineCount() <= line) {
            return;
        }
        int startOffset = markupDocument.getLineStartOffset(line);
        int endOffset = markupDocument.getLineEndOffset(line);

        SimpleReference<RangeHighlighterEx> found = new SimpleReference<>();
        markup.processRangeHighlightersOverlappingWith(
            startOffset,
            endOffset,
            highlighter -> {
                GutterMark renderer = highlighter.getGutterIconRenderer();
                if (renderer instanceof MyGutterIconRenderer iconRenderer && iconRenderer.myBookmark == BookmarkImpl.this) {
                    found.set(highlighter);
                    return false;
                }
                return true;
            }
        );
        if (!found.isNull()) {
            found.get().dispose();
        }
    }

    @Nonnull
    @Override
    public Image getIcon(boolean gutter) {
        if (myMnemonic == 0) {
            return BookmarkIcon.getDefaultIcon(gutter);
        }
        return BookmarkIcon.getMnemonicIcon(myMnemonic, gutter);
    }

    @Override
    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        myDescription = description;
    }

    @Override
    public char getMnemonic() {
        return myMnemonic;
    }

    public void setMnemonic(char mnemonic) {
        myMnemonic = Character.toUpperCase(mnemonic);
    }

    @Override
    @Nonnull
    public VirtualFile getFile() {
        return myFile;
    }

    @Nullable
    public String getNotEmptyDescription() {
        return StringUtil.isEmpty(myDescription) ? null : myDescription;
    }

    @Override
    public boolean isValid() {
        return getFile().isValid() && myTarget.isValid();
    }

    @Override
    public boolean canNavigate() {
        return myTarget.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return myTarget.canNavigateToSource();
    }

    @Override
    public void navigate(boolean requestFocus) {
        myTarget.navigate(requestFocus);
    }

    @Override
    public int getLine() {
        RangeMarker marker = myTarget.getUserData(RangeMarker.KEY);
        if (marker != null && marker.isValid()) {
            Document document = marker.getDocument();
            return document.getLineNumber(marker.getStartOffset());
        }
        return myTarget.getLine();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(getQualifiedName());
        String description = StringUtil.escapeXml(getNotEmptyDescription());
        if (description != null) {
            result.append(": ").append(description);
        }
        return result.toString();
    }

    @Override
    @RequiredReadAction
    @Nonnull
    public String getQualifiedName() {
        String presentableUrl = myFile.getPresentableUrl();
        if (myFile.isDirectory()) {
            return presentableUrl;
        }

        PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);

        if (psiFile == null) {
            return presentableUrl;
        }

        StructureViewBuilder builder = PsiStructureViewFactory.createBuilderForFile(psiFile);
        if (builder instanceof TreeBasedStructureViewBuilder viewBuilder) {
            StructureViewModel model = viewBuilder.createStructureViewModel(null);
            Object element;
            try {
                element = model.getCurrentEditorElement();
            }
            finally {
                model.dispose();
            }
            if (element instanceof NavigationItem navItem) {
                ItemPresentation presentation = navItem.getPresentation();
                if (presentation != null) {
                    presentableUrl = navItem.getName() + " " + presentation.getLocationString();
                }
            }
        }

        return BookmarkLocalize.bookmarkFileXLineY(presentableUrl, getLine() + 1).get();
    }

    private LocalizeValue getBookmarkTooltip() {
        String description = StringUtil.escapeXml(getNotEmptyDescription());
        if (myMnemonic != 0) {
            return description != null
                ? BookmarkLocalize.tooltipBookmark0WithDescription(myMnemonic, description)
                : BookmarkLocalize.tooltipBookmark0(myMnemonic);
        }
        else {
            return description != null
                ? BookmarkLocalize.tooltipBookmarkWithDescription(description)
                : BookmarkLocalize.tooltipBookmark();
        }
    }

    private static class MyGutterIconRenderer extends GutterIconRenderer {
        private final BookmarkImpl myBookmark;

        public MyGutterIconRenderer(@Nonnull BookmarkImpl bookmark) {
            myBookmark = bookmark;
        }

        @Override
        @Nonnull
        public Image getIcon() {
            return myBookmark.getIcon(true);
        }

        @Nonnull
        @Override
        public LocalizeValue getTooltipValue() {
            return myBookmark.getBookmarkTooltip();
        }

        @Nullable
        @Override
        public ActionGroup getPopupMenuActions() {
            return (ActionGroup) ActionManager.getInstance().getAction("popup@BookmarkContextMenu");
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MyGutterIconRenderer iconRenderer
                && Comparing.equal(getTooltipValue(), iconRenderer.getTooltipValue())
                && Comparing.equal(getIcon(), iconRenderer.getIcon());
        }

        @Override
        public int hashCode() {
            return getIcon().hashCode();
        }
    }
}

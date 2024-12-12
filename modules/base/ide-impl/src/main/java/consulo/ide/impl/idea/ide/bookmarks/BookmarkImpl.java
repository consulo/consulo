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

package consulo.ide.impl.idea.ide.bookmarks;

import consulo.annotation.access.RequiredReadAction;
import consulo.bookmark.Bookmark;
import consulo.bookmark.icon.BookmarkIconGroup;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

public class BookmarkImpl implements Bookmark {
    //0..9  + A..Z
    // Gutter + Action icon
    @SuppressWarnings("unchecked")
    private static final Couple<Image>[] ourMnemonicImageCache = new Couple[36];

    @Nonnull
    public static Image getDefaultIcon(boolean gutter) {
        return gutter ? BookmarkIconGroup.gutterBookmark() : BookmarkIconGroup.actionBookmark();
    }

    @Nonnull
    private static Image getMnemonicIcon(char mnemonic, boolean gutter) {
        int index = mnemonic - 48;
        if (index > 9) {
            index -= 7;
        }
        if (index < 0 || index > ourMnemonicImageCache.length - 1) {
            return createMnemonicIcon(mnemonic, gutter);
        }

        if (ourMnemonicImageCache[index] == null) {
            // its not mistake about using gutter icon as default icon for named bookmarks, too big icon
            ourMnemonicImageCache[index] = Couple.of(createMnemonicIcon(mnemonic, true), createMnemonicIcon(mnemonic, true));
        }
        Couple<Image> couple = ourMnemonicImageCache[index];
        return gutter ? couple.getFirst() : couple.getSecond();
    }

    @Nonnull
    private static Image createMnemonicIcon(char cha, boolean gutter) {
        ImageKey base = PlatformIconGroup.gutterMnemonic();

        return ImageEffects.layered(PlatformIconGroup.gutterMnemonic(), ImageEffects.canvas(base.getWidth(), base.getHeight(), c -> {
            c.setFillStyle(ComponentColors.TEXT);

            EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
            int editorFontSize = scheme.getEditorFontSize();

            c.setFont(FontManager.get().createFont(scheme.getEditorFontName(), editorFontSize, consulo.ui.font.Font.STYLE_PLAIN));
            c.setTextAlign(Canvas2D.TextAlign.center);
            c.setTextBaseline(Canvas2D.TextBaseline.middle);

            c.fillText(Character.toString(cha), base.getWidth() / 2 - 1, base.getHeight() / 2 - 1);
        }));
    }

    private final VirtualFile myFile;
    @Nonnull
    private final OpenFileDescriptorImpl myTarget;
    private final Project myProject;

    private String myDescription;
    private char myMnemonic = 0;
    public static final Font MNEMONIC_FONT = new Font("Monospaced", 0, 11);

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

        myTarget = new OpenFileDescriptorImpl(project, file, line, -1, true);

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
        final RangeHighlighterEx myHighlighter;
        int line = getLine();
        if (line >= 0) {
            myHighlighter = markup.addPersistentLineHighlighter(line, HighlighterLayer.ERROR + 1, null);
            if (myHighlighter != null) {
                myHighlighter.setGutterIconRenderer(new MyGutterIconRenderer(this));

                TextAttributes textAttributes =
                    EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.BOOKMARKS_ATTRIBUTES);

                ColorValue stripeColor = textAttributes.getErrorStripeColor();
                myHighlighter.setErrorStripeMarkColor(stripeColor != null ? stripeColor : StandardColors.BLACK);
                myHighlighter.setErrorStripeTooltip(getBookmarkTooltip());

                TextAttributes attributes = myHighlighter.getTextAttributes();
                if (attributes == null) {
                    attributes = new TextAttributes();
                }
                attributes.setBackgroundColor(textAttributes.getBackgroundColor());
                attributes.setForegroundColor(textAttributes.getForegroundColor());
                myHighlighter.setTextAttributes(attributes);
            }
        }
        else {
            myHighlighter = null;
        }
        return myHighlighter;
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
        final Document document = getDocument();
        if (document == null) {
            return;
        }
        MarkupModelEx markup = DocumentMarkupModel.forDocument(document, myProject, true);
        final Document markupDocument = markup.getDocument();
        if (markupDocument.getLineCount() <= line) {
            return;
        }
        final int startOffset = markupDocument.getLineStartOffset(line);
        final int endOffset = markupDocument.getLineEndOffset(line);

        final SimpleReference<RangeHighlighterEx> found = new SimpleReference<>();
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
            return getDefaultIcon(gutter);
        }
        return getMnemonicIcon(myMnemonic, gutter);
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
        RangeMarker marker = myTarget.getRangeMarker();
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

        final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);

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

        return IdeLocalize.bookmarkFileXLineY(presentableUrl, getLine() + 1).get();
    }

    private LocalizeValue getBookmarkTooltip() {
        StringBuilder result = new StringBuilder("BookmarkImpl");
        if (myMnemonic != 0) {
            result.append(" ").append(myMnemonic);
        }
        String description = StringUtil.escapeXml(getNotEmptyDescription());
        if (description != null) {
            result.append(": ").append(description);
        }
        return LocalizeValue.of(result.toString());
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

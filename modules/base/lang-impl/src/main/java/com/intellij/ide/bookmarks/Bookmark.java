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

package com.intellij.ide.bookmarks;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.ex.util.LightDarkColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.lang.Couple;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

public class Bookmark implements Navigatable {
  //0..9  + A..Z
  // Gutter + Action icon
  @SuppressWarnings("unchecked")
  private static final Couple<Image>[] ourMnemonicImageCache = new Couple[36];

  @Nonnull
  public static Image getDefaultIcon(boolean gutter) {
    return gutter ? PlatformIconGroup.actionsBookmarkSmall() : PlatformIconGroup.actionsBookmark();
  }

  @Nonnull
  private static Image getMnemonicIcon(char mnemonic, boolean gutter) {
    int index = mnemonic - 48;
    if (index > 9) index -= 7;
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
    int width = gutter ? 12 : 16;
    int height = gutter ? 12 : 16;
    int fontSize = gutter ? 11 : 13;

    return ImageEffects.canvas(width, height, c -> {
      c.setFillStyle(new LightDarkColorValue(new RGBColor(255, 255, 204), new RGBColor(103, 81, 51)));
      c.fillRect(0, 0, width, height);

      c.setStrokeStyle(StandardColors.GRAY);
      c.rect(0, 0, width, height);
      c.stroke();

      c.setFillStyle(ComponentColors.TEXT);
      EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
      c.setFont(FontManager.get().createFont(scheme.getEditorFontName(), fontSize, consulo.ui.font.Font.STYLE_PLAIN));
      c.setTextAlign(Canvas2D.TextAlign.center);
      c.setTextBaseline(Canvas2D.TextBaseline.middle);

      c.fillText(Character.toString(cha), width / 2, height / 2 - 1);
    });
  }

  private final VirtualFile myFile;
  @Nonnull
  private final OpenFileDescriptor myTarget;
  private final Project myProject;

  private String myDescription;
  private char myMnemonic = 0;
  public static final Font MNEMONIC_FONT = new Font("Monospaced", 0, 11);

  public Bookmark(@Nonnull Project project, @Nonnull VirtualFile file, int line, @Nonnull String description, boolean addHighlighter) {
    myFile = file;
    myProject = project;
    myDescription = description;

    myTarget = new OpenFileDescriptor(project, file, line, -1, true);

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
      createHighlighter((MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true));
    }
  }

  public RangeHighlighter createHighlighter(@Nonnull MarkupModelEx markup) {
    final RangeHighlighterEx myHighlighter;
    int line = getLine();
    if (line >= 0) {
      myHighlighter = markup.addPersistentLineHighlighter(line, HighlighterLayer.ERROR + 1, null);
      if (myHighlighter != null) {
        myHighlighter.setGutterIconRenderer(new MyGutterIconRenderer(this));

        TextAttributes textAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.BOOKMARKS_ATTRIBUTES);

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

  public Document getDocument() {
    return FileDocumentManager.getInstance().getDocument(getFile());
  }

  public void release() {
    int line = getLine();
    if (line < 0) {
      return;
    }
    final Document document = getDocument();
    if (document == null) return;
    MarkupModelEx markup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true);
    final Document markupDocument = markup.getDocument();
    if (markupDocument.getLineCount() <= line) return;
    final int startOffset = markupDocument.getLineStartOffset(line);
    final int endOffset = markupDocument.getLineEndOffset(line);

    final SimpleReference<RangeHighlighterEx> found = new SimpleReference<>();
    markup.processRangeHighlightersOverlappingWith(startOffset, endOffset, highlighter -> {
      GutterMark renderer = highlighter.getGutterIconRenderer();
      if (renderer instanceof MyGutterIconRenderer && ((MyGutterIconRenderer)renderer).myBookmark == Bookmark.this) {
        found.set(highlighter);
        return false;
      }
      return true;
    });
    if (!found.isNull()) found.get().dispose();
  }

  public Image getIcon(boolean gutter) {
    if (myMnemonic == 0) {
      return getDefaultIcon(gutter);
    }
    return getMnemonicIcon(myMnemonic, gutter);
  }

  @Nonnull
  @Deprecated(forRemoval = true)
  public Image getIcon() {
    return getIcon(true);
  }

  public String getDescription() {
    return myDescription;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  public char getMnemonic() {
    return myMnemonic;
  }

  public void setMnemonic(char mnemonic) {
    myMnemonic = Character.toUpperCase(mnemonic);
  }

  @Nonnull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nullable
  public String getNotEmptyDescription() {
    return StringUtil.isEmpty(myDescription) ? null : myDescription;
  }

  public boolean isValid() {
    if (!getFile().isValid()) {
      return false;
    }

    // There is a possible case that target document line that is referenced by the current bookmark is removed. We assume
    // that corresponding range marker becomes invalid then.
    RangeMarker rangeMarker = myTarget.getRangeMarker();
    return rangeMarker == null || rangeMarker.isValid();
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

  @RequiredReadAction
  @Nonnull
  public String getQualifiedName() {
    String presentableUrl = myFile.getPresentableUrl();
    if (myFile.isDirectory()) return presentableUrl;

    final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);

    if (psiFile == null) return presentableUrl;

    StructureViewBuilder builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(psiFile);
    if (builder instanceof TreeBasedStructureViewBuilder) {
      StructureViewModel model = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(null);
      Object element;
      try {
        element = model.getCurrentEditorElement();
      }
      finally {
        model.dispose();
      }
      if (element instanceof NavigationItem) {
        ItemPresentation presentation = ((NavigationItem)element).getPresentation();
        if (presentation != null) {
          presentableUrl = ((NavigationItem)element).getName() + " " + presentation.getLocationString();
        }
      }
    }

    return IdeBundle.message("bookmark.file.X.line.Y", presentableUrl, getLine() + 1);
  }

  private String getBookmarkTooltip() {
    StringBuilder result = new StringBuilder("Bookmark");
    if (myMnemonic != 0) {
      result.append(" ").append(myMnemonic);
    }
    String description = StringUtil.escapeXml(getNotEmptyDescription());
    if (description != null) {
      result.append(": ").append(description);
    }
    return result.toString();
  }

  private static class MyGutterIconRenderer extends GutterIconRenderer {
    private final Bookmark myBookmark;

    public MyGutterIconRenderer(@Nonnull Bookmark bookmark) {
      myBookmark = bookmark;
    }

    @Override
    @Nonnull
    public Image getIcon() {
      return myBookmark.getIcon(true);
    }

    @Override
    public String getTooltipText() {
      return myBookmark.getBookmarkTooltip();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MyGutterIconRenderer &&
             Comparing.equal(getTooltipText(), ((MyGutterIconRenderer)obj).getTooltipText()) &&
             Comparing.equal(getIcon(), ((MyGutterIconRenderer)obj).getIcon());
    }

    @Override
    public int hashCode() {
      return getIcon().hashCode();
    }
  }
}

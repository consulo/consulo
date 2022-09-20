// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.util.lang.ObjectUtil;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SyntaxTraverser;
import consulo.project.Project;
import consulo.util.collection.TreeTraversal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class ChameleonSyntaxHighlightingPass extends GeneralHighlightingPass {

  ChameleonSyntaxHighlightingPass(@Nonnull Project project,
                                  @Nonnull PsiFile file,
                                  @Nonnull Document document,
                                  @Nonnull ProperTextRange restrictRange,
                                  @Nonnull ProperTextRange priorityRange,
                                  @Nullable Editor editor,
                                  @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    super(project, file, document, restrictRange.getStartOffset(), restrictRange.getEndOffset(), true, priorityRange, editor, highlightInfoProcessor);
  }

  @Override
  public void collectInformationWithProgress(@Nonnull ProgressIndicator progress) {
    SyntaxTraverser<PsiElement> s = SyntaxTraverser.psiTraverser(myFile).filter(o -> {
      IElementType type = PsiUtilCore.getElementType(o);
      return type instanceof ILazyParseableElementType && !(type instanceof IFileElementType);
    });

    List<PsiElement> lazyOutside = new ArrayList<>(100);
    List<PsiElement> lazyInside = new ArrayList<>(100);
    List<HighlightInfo> outside = new ArrayList<>(100);
    List<HighlightInfo> inside = new ArrayList<>(100);

    for (PsiElement e : s) {
      (e.getTextRange().intersects(myPriorityRange) ? lazyInside : lazyOutside).add(e);
    }
    for (PsiElement e : lazyInside) {
      collectHighlights(e, inside, outside, myPriorityRange);
    }
    myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), inside, myPriorityRange, myRestrictRange, getId());
    for (PsiElement e : lazyOutside) {
      collectHighlights(e, inside, outside, myPriorityRange);
    }
    myHighlightInfoProcessor.highlightsOutsideVisiblePartAreProduced(myHighlightingSession, getEditor(), outside, myPriorityRange, myRestrictRange, getId());
    myHighlights.addAll(inside);
    myHighlights.addAll(outside);
  }

  private void collectHighlights(@Nonnull PsiElement element, @Nonnull List<? super HighlightInfo> inside, @Nonnull List<? super HighlightInfo> outside, @Nonnull ProperTextRange priorityRange) {
    EditorColorsScheme scheme = ObjectUtil.notNull(getColorsScheme(), EditorColorsManager.getInstance().getGlobalScheme());
    TextAttributes defaultAttrs = scheme.getAttributes(HighlighterColors.TEXT);

    Language language = ILazyParseableElementType.LANGUAGE_KEY.get(element.getNode());
    if (language == null) return;

    SyntaxHighlighter syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, myProject, myFile.getVirtualFile());
    for (PsiElement token : SyntaxTraverser.psiTraverser(element).traverse(TreeTraversal.LEAVES_DFS)) {
      TextRange tr = token.getTextRange();
      if (tr.isEmpty()) continue;
      IElementType type = PsiUtilCore.getElementType(token);
      TextAttributesKey[] keys = syntaxHighlighter.getTokenHighlights(type);

      // force attribute colors to override host' ones
      TextAttributes attributes = null;
      for (TextAttributesKey key : keys) {
        TextAttributes attrs2 = scheme.getAttributes(key);
        if (attrs2 != null) {
          attributes = attributes == null ? attrs2 : TextAttributes.merge(attributes, attrs2);
        }
      }
      TextAttributes forcedAttributes;
      List<? super HighlightInfo> result = priorityRange.contains(tr) ? inside : outside;
      if (attributes == null || attributes.isEmpty() || attributes.equals(defaultAttrs)) {
        forcedAttributes = TextAttributes.ERASE_MARKER;
      }
      else {
        HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT).
                range(tr).
                textAttributes(TextAttributes.ERASE_MARKER).
                createUnconditionally();
        result.add(info);

        forcedAttributes = new TextAttributes(attributes.getForegroundColor(), attributes.getBackgroundColor(), attributes.getEffectColor(), attributes.getEffectType(), attributes.getFontType());
      }

      HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT).
              range(tr).
              textAttributes(forcedAttributes).
              createUnconditionally();
      result.add(info);
    }
  }

  @Override
  protected void applyInformationWithProgress() {
  }

  @Nullable
  @Override
  protected String getPresentableName() {
    return null; // do not show progress for
  }
}

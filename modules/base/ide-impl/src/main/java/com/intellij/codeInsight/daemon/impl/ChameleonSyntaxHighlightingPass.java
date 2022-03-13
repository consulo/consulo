// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.MainHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import consulo.ide.impl.psi.SyntaxTraverser;
import com.intellij.util.ObjectUtils;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.editor.Pass;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.util.collection.TreeTraversal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class ChameleonSyntaxHighlightingPass extends GeneralHighlightingPass {
  final static class Factory implements MainHighlightingPassFactory {
    @Override
    public void register(@Nonnull Registrar registrar) {
      registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, -1);
    }

    @Nonnull
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
      Project project = file.getProject();
      TextRange restrict = FileStatusMap.getDirtyTextRange(editor, Pass.UPDATE_ALL);
      if (restrict == null) return new ProgressableTextEditorHighlightingPass.EmptyPass(project, editor.getDocument());
      ProperTextRange priority = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
      return new ChameleonSyntaxHighlightingPass(project, file, editor.getDocument(), ProperTextRange.create(restrict), priority, editor, new DefaultHighlightInfoProcessor());
    }

    @Nonnull
    @Override
    public TextEditorHighlightingPass createMainHighlightingPass(@Nonnull PsiFile file, @Nonnull Document document, @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
      ProperTextRange range = ProperTextRange.from(0, document.getTextLength());
      return new ChameleonSyntaxHighlightingPass(file.getProject(), file, document, range, range, null, highlightInfoProcessor);
    }
  }

  private ChameleonSyntaxHighlightingPass(@Nonnull Project project,
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
    EditorColorsScheme scheme = ObjectUtils.notNull(getColorsScheme(), EditorColorsManager.getInstance().getGlobalScheme());
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

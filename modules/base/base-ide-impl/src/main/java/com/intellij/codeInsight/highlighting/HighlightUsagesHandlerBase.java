/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import consulo.document.util.TextRange;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotation.access.RequiredReadAction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public abstract class HighlightUsagesHandlerBase<T extends PsiElement> {
  protected final Editor myEditor;
  protected final PsiFile myFile;

  protected List<TextRange> myReadUsages = new ArrayList<TextRange>();
  protected List<TextRange> myWriteUsages = new ArrayList<TextRange>();
  protected String myStatusText;
  protected String myHintText;

  protected HighlightUsagesHandlerBase(final Editor editor, final PsiFile file) {
    myEditor = editor;
    myFile = file;
  }

  public void highlightUsages() {
    List<T> targets = getTargets();
    if (targets == null) {
      return;
    }
    selectTargets(targets, new Consumer<List<T>>() {
      @Override
      public void consume(final List<T> targets) {
        computeUsages(targets);
        performHighlighting();
      }
    });
  }

  protected void performHighlighting() {
    boolean clearHighlights = HighlightUsagesHandler.isClearHighlights(myEditor);
    EditorColorsManager manager = EditorColorsManager.getInstance();
    TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    TextAttributes writeAttributes = manager.getGlobalScheme().getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
    HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(myEditor.getProject()),
                                           myEditor, attributes, clearHighlights, myReadUsages);
    HighlightUsagesHandler.highlightRanges(HighlightManager.getInstance(myEditor.getProject()),
                                           myEditor, writeAttributes, clearHighlights, myWriteUsages);
    if (!clearHighlights) {
      WindowManager.getInstance().getStatusBar(myEditor.getProject()).setInfo(myStatusText);

      HighlightHandlerBase.setupFindModel(myEditor.getProject()); // enable f3 navigation
    }
    if (myHintText != null) {
      HintManager.getInstance().showInformationHint(myEditor, myHintText);
    }
  }

  public void buildStatusText(@Nullable String elementName, int refCount) {
    if (refCount > 0) {
      myStatusText = CodeInsightBundle.message(elementName != null ?
                                               "status.bar.highlighted.usages.message" :
                                               "status.bar.highlighted.usages.no.target.message", refCount, elementName,
                                               HighlightUsagesHandler.getShortcutText());
    }
    else {
      myHintText = CodeInsightBundle.message(elementName != null ?
                                             "status.bar.highlighted.usages.not.found.message" :
                                             "status.bar.highlighted.usages.not.found.no.target.message", elementName);
    }
  }

  public abstract List<T> getTargets();

  @Nullable
  public String getFeatureId() {
    return null;
  }

  protected abstract void selectTargets(List<T> targets, Consumer<List<T>> selectionConsumer);

  public abstract void computeUsages(List<T> targets);

  @RequiredReadAction
  public void addOccurrence(@Nonnull PsiElement element) {
    TextRange range = element.getTextRange();
    if (range != null) {
      range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
      myReadUsages.add(range);
    }
  }

  public List<TextRange> getReadUsages() {
    return myReadUsages;
  }

  public List<TextRange> getWriteUsages() {
    return myWriteUsages;
  }

  /**
   * In case of egoistic handler (highlightReferences = true) IdentifierHighlighterPass applies information only from this particular handler.
   * Otherwise additional information would be collected from reference search as well.
   */
  public boolean highlightReferences() {
    return false;
  }
}

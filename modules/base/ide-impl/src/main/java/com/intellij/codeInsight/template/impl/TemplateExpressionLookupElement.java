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
package com.intellij.codeInsight.template.impl;

import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.OffsetMap;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementDecorator;
import com.intellij.codeInsight.template.TemplateLookupSelectionHandler;
import consulo.application.Result;
import consulo.language.editor.WriteCommandAction;
import consulo.codeEditor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author peter
 */
class TemplateExpressionLookupElement extends LookupElementDecorator<LookupElement> {
  private final TemplateStateImpl myState;

  public TemplateExpressionLookupElement(final TemplateStateImpl state, LookupElement element, int index) {
    super(PrioritizedLookupElement.withPriority(element, Integer.MAX_VALUE - 10 - index));
    myState = state;
  }

  private static InsertionContext createInsertionContext(LookupElement item,
                                                         PsiFile psiFile,
                                                         List<? extends LookupElement> elements,
                                                         Editor editor, final char completionChar) {
    final OffsetMap offsetMap = new OffsetMap(editor.getDocument());
    final InsertionContext context = new InsertionContext(offsetMap, completionChar, elements.toArray(new LookupElement[elements.size()]), psiFile, editor, false);
    context.setTailOffset(editor.getCaretModel().getOffset());
    offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, context.getTailOffset() - item.getLookupString().length());
    offsetMap.addOffset(CompletionInitializationContext.SELECTION_END_OFFSET, context.getTailOffset());
    offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, context.getTailOffset());
    return context;
  }

  void handleTemplateInsert(List<? extends LookupElement> elements, final char completionChar) {
    final InsertionContext context = createInsertionContext(this, myState.getPsiFile(), elements, myState.getEditor(), completionChar);
    new WriteCommandAction(context.getProject()) {
      @Override
      protected void run(@Nonnull Result result) throws Throwable {
        doHandleInsert(context);
      }
    }.execute();
    Disposer.dispose(context.getOffsetMap());

    if (handleCompletionChar(context) && !myState.isFinished()) {
      myState.calcResults(true);
      myState.considerNextTabOnLookupItemSelected(getDelegate());
    }
  }

  @Override
  public void handleInsert(final InsertionContext context) {
    doHandleInsert(context);
    handleCompletionChar(context);
  }

  private void doHandleInsert(InsertionContext context) {
    LookupElement item = getDelegate();
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();

    TextRange range = myState.getCurrentVariableRange();
    final TemplateLookupSelectionHandler handler = item.getUserData(TemplateLookupSelectionHandler.KEY_IN_LOOKUP_ITEM);
    if (handler != null && range != null) {
      handler.itemSelected(item, context.getFile(), context.getDocument(), range.getStartOffset(), range.getEndOffset());
    }
    else {
      super.handleInsert(context);
    }
  }

  private static boolean handleCompletionChar(InsertionContext context) {
    if (context.getCompletionChar() == '.') {
      EditorModificationUtil.insertStringAtCaret(context.getEditor(), ".");
      AutoPopupController.getInstance(context.getProject()).autoPopupMemberLookup(context.getEditor(), null);
      return false;
    }
    return true;
  }
}
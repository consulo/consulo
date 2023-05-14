/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.matcher.PlainPrefixMatcher;
import consulo.language.Language;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.*;
import consulo.document.Document;
import consulo.application.dumb.DumbAware;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.editor.ui.awt.StringComboboxEditor;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author peter
 */
@ExtensionImpl(id = "comboEditor", order = "first")
public class ComboEditorCompletionContributor extends CompletionContributor implements DumbAware {

  public static final Key<Boolean> CONTINUE_RUN_COMPLETION = Key.create("CONTINUE_RUN_COMPLETION");

  @RequiredReadAction
  @Override
  public void fillCompletionVariants(@Nonnull final CompletionParameters parameters, @Nonnull final CompletionResultSet result) {
    if (parameters.getInvocationCount() == 0) {
      return;
    }

    final PsiFile file = parameters.getOriginalFile();
    final Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document != null) {
      JComboBox comboBox = document.getUserData(StringComboboxEditor.COMBO_BOX_KEY);
      if (comboBox != null) {
        String substring = document.getText().substring(0, parameters.getOffset());
        boolean plainPrefixMatcher = Boolean.TRUE.equals(document.getUserData(StringComboboxEditor.USE_PLAIN_PREFIX_MATCHER));
        final CompletionResultSet resultSet = plainPrefixMatcher ?
                                              result.withPrefixMatcher(new PlainPrefixMatcher(substring)) :
                                              result.withPrefixMatcher(substring);
        final int count = comboBox.getItemCount();
        for (int i = 0; i < count; i++) {
          final Object o = comboBox.getItemAt(i);
          if (o instanceof String) {
            resultSet.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create((String)o).withInsertHandler(
                    new InsertHandler<LookupElement>() {
                      @Override
                      public void handleInsert(final InsertionContext context, final LookupElement item) {
                        final Document document = context.getEditor().getDocument();
                        document.deleteString(context.getEditor().getCaretModel().getOffset(), document.getTextLength());
                      }
                    }), count-i));
          }
        }
        if (!Boolean.TRUE.equals(document.getUserData(CONTINUE_RUN_COMPLETION))) {
          result.stopHere();
        }
      }
    }
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}

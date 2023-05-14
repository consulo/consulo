/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.textCompletion;

import consulo.language.editor.AutoPopupController;
import consulo.language.editor.hint.HintManager;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.project.DumbService;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.Ref;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.psi.PsiFile;
import consulo.language.editor.ui.awt.LanguageTextField;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class TextCompletionUtil {
  public static final Key<TextCompletionProvider> COMPLETING_TEXT_FIELD_KEY = Key.create("COMPLETING_TEXT_FIELD_KEY");
  public static final Key<Boolean> AUTO_POPUP_KEY = Key.create("AUTOPOPUP_TEXT_FIELD_KEY");

  public static void installProvider(@Nonnull PsiFile psiFile, @Nonnull TextCompletionProvider provider, boolean autoPopup) {
    psiFile.putUserData(COMPLETING_TEXT_FIELD_KEY, provider);
    psiFile.putUserData(AUTO_POPUP_KEY, autoPopup);
  }

  @Nullable
  public static TextCompletionProvider getProvider(@Nonnull PsiFile file) {
    TextCompletionProvider provider = file.getUserData(COMPLETING_TEXT_FIELD_KEY);

    if (provider == null || (DumbService.isDumb(file.getProject()) && !DumbService.isDumbAware(provider))) {
      return null;
    }
    return provider;
  }

  public static void installCompletionHint(@Nonnull EditorEx editor) {
    String completionShortcutText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));
    if (!StringUtil.isEmpty(completionShortcutText)) {

      final Ref<Boolean> toShowHintRef = new Ref<>(true);
      editor.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        public void documentChanged(DocumentEvent e) {
          toShowHintRef.set(false);
        }
      });

      editor.addFocusListener(new FocusChangeListener() {
        @Override
        public void focusGained(final Editor editor) {
          if (Boolean.TRUE.equals(editor.getUserData(AutoPopupController.AUTO_POPUP_ON_FOCUS_GAINED))) {
            AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);
            return;
          }

          if (toShowHintRef.get() && editor.getDocument().getText().isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() -> HintManager.getInstance().showInformationHint(editor, "Code completion available ( " + completionShortcutText + " )"));
          }
        }

        @Override
        public void focusLost(Editor editor) {
          // Do nothing
        }
      });
    }
  }

  public static class DocumentWithCompletionCreator extends LanguageTextField.SimpleDocumentCreator {
    @Nonnull
    private final TextCompletionProvider myProvider;
    private final boolean myAutoPopup;

    public DocumentWithCompletionCreator(@Nonnull TextCompletionProvider provider, boolean autoPopup) {
      myProvider = provider;
      myAutoPopup = autoPopup;
    }

    @Override
    public void customizePsiFile(@Nonnull PsiFile file) {
      installProvider(file, myProvider, myAutoPopup);
    }
  }
}

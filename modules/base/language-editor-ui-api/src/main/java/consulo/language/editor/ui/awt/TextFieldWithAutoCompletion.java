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

package consulo.language.editor.ui.awt;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.language.editor.hint.HintManager;
import consulo.language.plain.PlainTextLanguage;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * <p/>
 * It is text field with autocompletion from list of values.
 * <p/>
 * Autocompletion is implemented via {@code TextFieldWithAutoCompletionContributor}.
 * Use {@code setVariants} set list of values for autocompletion.
 *
 * @author Roman Chernyatchik
 */
public class TextFieldWithAutoCompletion<T> extends LanguageTextField {

  public static final TextFieldWithAutoCompletionListProvider EMPTY_COMPLETION = new StringsCompletionProvider(null, null);
  private final boolean myShowAutocompletionIsAvailableHint;
  private final TextFieldWithAutoCompletionListProvider<T> myProvider;

  @SuppressWarnings("unchecked")
  public TextFieldWithAutoCompletion() {
    this(null, EMPTY_COMPLETION, false, null);
  }


  public TextFieldWithAutoCompletion(Project project,
                                     @Nonnull TextFieldWithAutoCompletionListProvider<T> provider,
                                     boolean showAutocompletionIsAvailableHint, @Nullable String text) {
    super(PlainTextLanguage.INSTANCE, project, text == null ? "" : text);

    myShowAutocompletionIsAvailableHint = showAutocompletionIsAvailableHint;
    myProvider = provider;

    TextFieldWithAutoCompletionContributor.installCompletion(getDocument(), project, provider, true);
  }

  public static TextFieldWithAutoCompletion<String> create(Project project,
                                                           @Nonnull Collection<String> items,
                                                           boolean showAutocompletionIsAvailableHint,
                                                           @Nullable String text) {
    return create(project, items, null, showAutocompletionIsAvailableHint, text);
  }

  public static TextFieldWithAutoCompletion<String> create(Project project,
                                                           @Nonnull Collection<String> items,
                                                           @Nullable Image icon,
                                                           boolean showAutocompletionIsAvailableHint,
                                                           @Nullable String text) {
    return new TextFieldWithAutoCompletion<String>(project, new StringsCompletionProvider(items, icon), showAutocompletionIsAvailableHint,
                                                   text);
  }

  public void setVariants(@Nonnull Collection<T> variants) {
    myProvider.setItems(variants);
  }

  public <T> void installProvider(@Nonnull TextFieldWithAutoCompletionListProvider<T> provider) {
    TextFieldWithAutoCompletionContributor.installCompletion(getDocument(), getProject(), provider, true);
  }

  @Override
  protected EditorEx createEditor() {
    EditorEx editor = super.createEditor();

    if (!myShowAutocompletionIsAvailableHint) {
      return editor;
    }

    final String completionShortcutText =
      KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));
    if (StringUtil.isEmpty(completionShortcutText)) {
      return editor;
    }

    final Ref<Boolean> toShowHintRef = new Ref<Boolean>(true);
    editor.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void documentChanged(DocumentEvent e) {
        toShowHintRef.set(false);
      }
    });

    editor.addFocusListener(new FocusChangeListener() {
      @Override
      public void focusGained(final Editor editor) {
        if (toShowHintRef.get() && getText().length() == 0) {
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              HintManager.getInstance().showInformationHint(editor, "Code completion available ( " + completionShortcutText + " )");
            }
          });
        }
      }

      @Override
      public void focusLost(Editor editor) {
        // Do nothing
      }
    });
    return editor;
  }

  public static class StringsCompletionProvider extends TextFieldWithAutoCompletionListProvider<String> {
    @Nullable private final Image myIcon;

    public StringsCompletionProvider(@Nullable Collection<String> variants,
                                     @Nullable Image icon) {
      super(variants);
      myIcon = icon;
    }

    @Override
    public int compare(String item1, String item2) {
      return StringUtil.compare(item1, item2, false);
    }

    @Override
    protected Image getIcon(@Nonnull String item) {
      return myIcon;
    }

    @Nonnull
    @Override
    protected String getLookupString(@Nonnull String item) {
      return item;
    }

    @Override
    protected String getTailText(@Nonnull String item) {
      return null;
    }

    @Override
    protected String getTypeText(@Nonnull String item) {
      return null;
    }
  }
}

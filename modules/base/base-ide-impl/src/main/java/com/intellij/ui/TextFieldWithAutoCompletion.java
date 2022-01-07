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

package com.intellij.ui;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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


  public TextFieldWithAutoCompletion(final Project project,
                                     @Nonnull final TextFieldWithAutoCompletionListProvider<T> provider,
                                     final boolean showAutocompletionIsAvailableHint, @Nullable final String text) {
    super(PlainTextLanguage.INSTANCE, project, text == null ? "" : text);

    myShowAutocompletionIsAvailableHint = showAutocompletionIsAvailableHint;
    myProvider = provider;

    TextFieldWithAutoCompletionContributor.installCompletion(getDocument(), project, provider, true);
  }

  public static TextFieldWithAutoCompletion<String> create(final Project project,
                                                           @Nonnull final Collection<String> items,
                                                           final boolean showAutocompletionIsAvailableHint,
                                                           @Nullable final String text) {
    return create(project, items, null, showAutocompletionIsAvailableHint, text);
  }

  public static TextFieldWithAutoCompletion<String> create(final Project project,
                                                           @Nonnull final Collection<String> items,
                                                           @Nullable final Image icon,
                                                           final boolean showAutocompletionIsAvailableHint,
                                                           @Nullable final String text) {
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
    final EditorEx editor = super.createEditor();

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

    public StringsCompletionProvider(@Nullable final Collection<String> variants,
                                     @Nullable final Image icon) {
      super(variants);
      myIcon = icon;
    }

    @Override
    public int compare(final String item1, final String item2) {
      return StringUtil.compare(item1, item2, false);
    }

    @Override
    protected Image getIcon(@Nonnull final String item) {
      return myIcon;
    }

    @Nonnull
    @Override
    protected String getLookupString(@Nonnull final String item) {
      return item;
    }

    @Override
    protected String getTailText(@Nonnull final String item) {
      return null;
    }

    @Override
    protected String getTypeText(@Nonnull final String item) {
      return null;
    }
  }
}

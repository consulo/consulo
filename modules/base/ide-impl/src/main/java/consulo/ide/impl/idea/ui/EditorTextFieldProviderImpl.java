/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.impl.internal.action.TextComponentEditorAction;
import consulo.language.Language;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.ui.EditorCustomization;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.LanguageTextField;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Provides default implementation for {@link EditorTextFieldProvider} service and applies available
 * {@link EditorCustomization customizations} if necessary.
 *
 * @author Denis Zhdanov
 * @since 2010-08-20
 */
@Singleton
@ServiceImpl
public class EditorTextFieldProviderImpl implements EditorTextFieldProvider {

  /**
   * Encapsulates sorting rule that defines what editor actions have precedence to non-editor actions. Current approach is that
   * we want to process text processing-oriented editor actions with higher priority than non-editor actions and all
   * other editor actions with lower priority.
   * <p/>
   * Rationale: there is at least one commit-specific action that is mapped to the editor action by default
   * (<code>'show commit messages history'</code> vs <code>'scroll to center'</code>). We want to process the former on target
   * short key triggering. Another example is that {@code 'Ctrl+Shift+Right/Left Arrow'} shortcut is bound to
   * <code>'expand/reduce selection by word'</code> editor action and <code>'change dialog width'</code> non-editor action
   * and we want to use the first one.
   */
  private static final Comparator<? super AnAction> ACTIONS_COMPARATOR = (o1, o2) -> {
    if (o1 instanceof EditorAction && o2 instanceof EditorAction) {
      return 0;
    }
    if (o1 instanceof TextComponentEditorAction) {
      return -1;
    }
    if (o2 instanceof TextComponentEditorAction) {
      return 1;
    }
    if (o1 instanceof EditorAction) {
      return 1;
    }
    if (o2 instanceof EditorAction) {
      return -1;
    }
    return 0;
  };

  @Nonnull
  @Override
  public EditorTextField getEditorField(@Nonnull Language language, @Nonnull Project project,
                                        @Nonnull Iterable<Consumer<EditorEx>> features) {
    return new MyEditorTextField(language, project, features);
  }

  private static class MyEditorTextField extends LanguageTextField {

    @Nonnull
    private final Iterable<Consumer<EditorEx>> myCustomizations;

    MyEditorTextField(@Nonnull Language language, @Nonnull Project project, @Nonnull Iterable<Consumer<EditorEx>> customizations) {
      super(language, project, "", false);
      myCustomizations = customizations;
    }

    @Override
    protected EditorEx createEditor() {
      EditorEx ex = super.createEditor();
      ex.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      ex.setHorizontalScrollbarVisible(true);
      applyDefaultSettings(ex);
      applyCustomizations(ex);
      return ex;
    }

    private static void applyDefaultSettings(EditorEx ex) {
      EditorSettings settings = ex.getSettings();
      settings.setAdditionalColumnsCount(3);
      settings.setVirtualSpace(false);
    }

    private void applyCustomizations(@Nonnull EditorEx editor) {
      for (Consumer<EditorEx> customization : myCustomizations) {
        customization.accept(editor);
      }
    }

    @Override
    public boolean isOneLineMode() {
      return false;
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      if (PlatformDataKeys.ACTIONS_SORTER == dataId) {
        return ACTIONS_COMPARATOR;
      }
      return super.getData(dataId);
    }
  }
}

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
package consulo.language.editor.refactoring;

import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class IntroduceTargetChooser {
  private IntroduceTargetChooser() {
  }

  public static <T extends PsiElement> void showChooser(final Editor editor, final List<T> expressions, final Consumer<T> callback, final Function<T, String> renderer) {
    showChooser(editor, expressions, callback, renderer, "Expressions");
  }

  public static <T extends PsiElement> void showChooser(final Editor editor, final List<T> expressions, final Consumer<T> callback, final Function<T, String> renderer, String title) {
    showChooser(editor, expressions, callback, renderer, title, ScopeHighlighter.NATURAL_RANGER);
  }

  public static <T extends PsiElement> void showChooser(final Editor editor,
                                                        final List<T> expressions,
                                                        final Consumer<T> callback,
                                                        final Function<T, String> renderer,
                                                        String title,
                                                        Function<PsiElement, TextRange> ranger) {
    showChooser(editor, expressions, callback, renderer, title, -1, ranger);
  }

  public static <T extends PsiElement> void showChooser(final Editor editor,
                                                        final List<T> expressions,
                                                        final Consumer<T> callback,
                                                        final Function<T, String> renderer,
                                                        String title,
                                                        int selection,
                                                        Function<PsiElement, TextRange> ranger) {
    final ScopeHighlighter highlighter = new ScopeHighlighter(editor, ranger);

    IPopupChooserBuilder<T> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(expressions);
    builder.setSelectedValue(expressions.get(selection > -1 ? selection : 0), true);
    builder.setTitle(title);
    builder.setMovable(false);
    builder.setResizable(false);
    builder.setRequestFocus(true);
    builder.setRenderer(new DefaultListCellRenderer() {

      @Override
      public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        final T expr = (T)value;
        if (expr.isValid()) {
          String text = renderer.apply(expr);
          int firstNewLinePos = text.indexOf('\n');
          String trimmedText = text.substring(0, firstNewLinePos != -1 ? firstNewLinePos : Math.min(100, text.length()));
          if (trimmedText.length() != text.length()) trimmedText += " ...";
          setText(trimmedText);
        }
        return rendererComponent;
      }
    });
    builder.setItemSelectedCallback(expr -> {
      highlighter.dropHighlight();
      final ArrayList<PsiElement> toExtract = new ArrayList<PsiElement>();
      toExtract.add(expr);
      highlighter.highlight(expr, toExtract);
    });
    builder.setItemChosenCallback(callback::accept);
    builder.addListener(new JBPopupAdapter() {
      @Override
      public void onClosed(LightweightWindowEvent event) {
        highlighter.dropHighlight();
      }
    });

    JBPopup popup = builder.createPopup();
    editor.showPopupInBestPositionFor(popup);
  }
}

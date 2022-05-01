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
package consulo.ide.impl.idea.refactoring;

import consulo.ide.impl.idea.codeInsight.unwrap.ScopeHighlighter;
import consulo.ide.impl.idea.util.Function;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IntroduceTargetChooser {
  private IntroduceTargetChooser() {
  }

  public static <T extends PsiElement> void showChooser(final Editor editor, final List<T> expressions, final Consumer<T> callback,
                                                        final Function<T, String> renderer) {
    showChooser(editor, expressions, callback, renderer, "Expressions");
  }

  public static <T extends PsiElement> void showChooser(final Editor editor,
                                                        final List<T> expressions,
                                                        final Consumer<T> callback,
                                                        final Function<T, String> renderer,
                                                        String title) {
    showChooser(editor, expressions, callback, renderer, title, ScopeHighlighter.NATURAL_RANGER);
  }

  public static <T extends PsiElement> void showChooser(final Editor editor,
                                                        final List<T> expressions,
                                                        final Consumer<T> callback,
                                                        final Function<T, String> renderer,
                                                        String title,
                                                        NotNullFunction<PsiElement, TextRange> ranger) {
    showChooser(editor, expressions, callback, renderer, title, -1, ranger);
  }

  public static <T extends PsiElement> void showChooser(final Editor editor,
                                                        final List<T> expressions,
                                                        final Consumer<T> callback,
                                                        final Function<T, String> renderer,
                                                        String title,
                                                        int selection,
                                                        NotNullFunction<PsiElement, TextRange> ranger) {
    final ScopeHighlighter highlighter = new ScopeHighlighter(editor, ranger);
    final DefaultListModel model = new DefaultListModel();
    for (T expr : expressions) {
      model.addElement(expr);
    }
    final JList list = new JBList(model);
    if (selection > -1) list.setSelectedIndex(selection);
    list.setCellRenderer(new DefaultListCellRenderer() {

      @Override
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index,
                                                    final boolean isSelected,
                                                    final boolean cellHasFocus) {
        final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        final T expr = (T)value;
        if (expr.isValid()) {
          String text = renderer.fun(expr);
          int firstNewLinePos = text.indexOf('\n');
          String trimmedText = text.substring(0, firstNewLinePos != -1 ? firstNewLinePos : Math.min(100, text.length()));
          if (trimmedText.length() != text.length()) trimmedText += " ...";
          setText(trimmedText);
        }
        return rendererComponent;
      }
    });

    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        highlighter.dropHighlight();
        final int index = list.getSelectedIndex();
        if (index < 0 ) return;
        final T expr = (T)model.get(index);
        final ArrayList<PsiElement> toExtract = new ArrayList<PsiElement>();
        toExtract.add(expr);
        highlighter.highlight(expr, toExtract);
      }
    });

    JBPopup popup = new PopupChooserBuilder<>(list).setTitle(title).setMovable(false).setResizable(false).setRequestFocus(true).setItemChoosenCallback(new Runnable() {
      @Override
      public void run() {
        callback.accept((T)list.getSelectedValue());
      }
    }).addListener(new JBPopupAdapter() {
      @Override
      public void onClosed(LightweightWindowEvent event) {
        highlighter.dropHighlight();
      }
    }).createPopup();

    editor.showPopupInBestPositionFor(popup);
  }
}

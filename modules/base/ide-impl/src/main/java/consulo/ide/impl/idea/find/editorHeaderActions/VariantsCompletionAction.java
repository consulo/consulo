/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.NameUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.colorScheme.EditorFontType;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.psi.stub.IdTableBuilding;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class VariantsCompletionAction extends AnAction {
    private final JTextComponent myTextField;

    public VariantsCompletionAction(JTextComponent textField) {
        myTextField = textField;
        AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION);
        if (action != null) {
            registerCustomShortcutSet(action.getShortcutSet(), myTextField);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
        if (editor == null) {
            return;
        }
        String prefix = myTextField.getText().substring(0, myTextField.getCaretPosition());
        if (StringUtil.isEmpty(prefix)) {
            return;
        }

        final String[] array = calcWords(prefix, editor);
        if (array.length == 0) {
            return;
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed("find.completion");
        JList list = new JBList(array) {
            @Override
            protected void paintComponent(Graphics g) {
                GraphicsUtil.setupAntialiasing(g);
                super.paintComponent(g);
            }
        };
        list.setBackground(new JBColor(new Color(235, 244, 254), new Color(0x4C4F51)));
        list.setFont(editor.getColorsScheme().getFont(EditorFontType.PLAIN));

        Utils.showCompletionPopup(
            e.getInputEvent() instanceof MouseEvent ? myTextField : null,
            list, null, myTextField, null
        );
    }

    private static String[] calcWords(String prefix, Editor editor) {
        Matcher matcher = NameUtil.buildMatcher(prefix, 0, true, true);
        Set<String> words = new HashSet<>();
        CharSequence chars = editor.getDocument().getCharsSequence();

        IdTableBuilding.scanWords(
            (chars1, charsArray, start, end) -> {
                String word = chars1.subSequence(start, end).toString();
                if (matcher.matches(word)) {
                    words.add(word);
                }
            },
            chars,
            0,
            chars.length()
        );


        List<String> sortedWords = new ArrayList<>(words);
        Collections.sort(sortedWords);

        return ArrayUtil.toStringArray(sortedWords);
    }
}

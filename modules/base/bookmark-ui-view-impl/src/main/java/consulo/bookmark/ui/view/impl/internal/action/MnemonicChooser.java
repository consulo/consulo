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
package consulo.bookmark.ui.view.impl.internal.action;

import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.HorizontalLayout;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author max
 */
public class MnemonicChooser extends JPanel {
    public MnemonicChooser() {
        super(new HorizontalLayout(20));
        setBorder(JBUI.Borders.empty(10));

        JPanel numbers = new NonOpaquePanel(new GridLayout(2, 5, 8, 8));
        for (char i = '1'; i <= '9'; i++) {
            numbers.add(wrapLabel(i));
        }
        numbers.add(wrapLabel('0'));


        JPanel letters = new NonOpaquePanel(new GridLayout(5, 6, 8, 8));
        for (char c = 'A'; c <= 'Z'; c++) {
            letters.add(wrapLabel(c));
        }

        // just ignore vertical expand
        add(new BorderLayoutPanel().addToTop(numbers));
        // just ignore vertical expand
        add(new BorderLayoutPanel().addToTop(letters));

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    onCancelled();
                }
                else if (e.getModifiersEx() == 0) {
                    char typed = Character.toUpperCase(e.getKeyChar());
                    if ('0' <= typed && typed <= '9' || 'A' <= typed && typed <= 'Z') {
                        onMnemonicChosen(typed);
                    }
                }
            }
        });

        setFocusable(true);
    }

    protected boolean isOccupied(char c) {
        return false;
    }

    protected void onMnemonicChosen(char c) {
    }

    protected void onCancelled() {
    }

    private Component wrapLabel(char c) {
        Button button = Button.create(LocalizeValue.of("&" + c));
        button.addClickListener(e -> onMnemonicChosen(c));
        if (isOccupied(c)) {
            button.addStyle(ButtonStyle.PRIMARY);
        }
        return TargetAWT.to(button);
    }
}

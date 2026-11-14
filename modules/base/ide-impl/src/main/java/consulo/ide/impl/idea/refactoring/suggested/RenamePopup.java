// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.ui.ex.awt.JBUI;

import javax.swing.*;
import java.awt.*;

class RenamePopup extends JPanel {
    private final JButton myButton = new JButton(RefactoringLocalize.suggestedRefactoringRenameButtonText().get()) {
        @Override
        public boolean isDefaultButton() {
            return true;
        }
    };

    private Runnable myOnRefactor = () -> {
    };

    RenamePopup(String oldName, String newName) {
        super(new BorderLayout());

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(myButton, BorderLayout.EAST);

        JLabel label = new JLabel(RefactoringLocalize.suggestedRefactoringRenamePopupText(oldName, newName).get());
        label.setBorder(JBUI.Borders.empty(0, 0, 0, 24));
        add(label, BorderLayout.NORTH);
        add(Box.createVerticalStrut(18));
        add(buttonPanel, BorderLayout.SOUTH);

        setBorder(JBUI.Borders.empty(5, 2));

        myButton.addActionListener(e -> onRefactor());
    }

    void setOnRefactor(Runnable onRefactor) {
        myOnRefactor = onRefactor;
    }

    void onRefactor() {
        myOnRefactor.run();
    }
}

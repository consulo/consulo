// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.action;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.util.lang.StringUtil;

import javax.swing.*;

public abstract class ToolbarLabelAction extends DumbAwareAction implements CustomComponentAction {
    protected ToolbarLabelAction(LocalizeValue text) {
        super(text);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(false);
    }

    @RequiredUIAccess
    @Override
    public final void actionPerformed(AnActionEvent e) {
        //do nothing
    }

    
    @Override
    public JComponent createCustomComponent(Presentation presentation, String place) {
        return new MyLabel(presentation).withFont(JBUI.Fonts.toolbarFont()).withBorder(JBUI.Borders.empty(0, 6, 0, 5));
    }

    private static class MyLabel extends JBLabel {
        
        private final Presentation myPresentation;

        MyLabel(Presentation presentation) {
            myPresentation = presentation;

            presentation.addPropertyChangeListener(e -> {
                String propertyName = e.getPropertyName();
                if (Presentation.PROP_TEXT.equals(propertyName)
                    || Presentation.PROP_DESCRIPTION.equals(propertyName)
                    || Presentation.PROP_ICON.equals(propertyName)) {
                    updatePresentation();
                }
            });
            updatePresentation();
        }

        private void updatePresentation() {
            setText(StringUtil.notNullize(myPresentation.getText()));
            setToolTipText(StringUtil.nullize(myPresentation.getDescription()));
            setIcon(myPresentation.getIcon());
        }
    }
}

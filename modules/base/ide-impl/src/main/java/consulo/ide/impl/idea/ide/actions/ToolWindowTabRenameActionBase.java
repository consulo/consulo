/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.wm.ToolWindowContextMenuActionBase;
import consulo.ide.impl.idea.util.BooleanFunction;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * from kotlin
 */
public class ToolWindowTabRenameActionBase extends ToolWindowContextMenuActionBase {
    private final String myToolWindowId;
    private final String myLabeltext;

    public ToolWindowTabRenameActionBase(String toolWindowId, String labeltext) {
        myToolWindowId = toolWindowId;
        myLabeltext = labeltext;
    }

    @Override
    public void update(@Nonnull AnActionEvent e, @Nonnull ToolWindow toolWindow, @Nullable Content content) {
        String id = toolWindow.getId();
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY) && myToolWindowId.equals(id) && content != null);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull ToolWindow toolWindow, @Nullable Content content) {
        Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (component == null || content == null || !e.hasData(Content.KEY)) {
            return;
        }

        showContentRenamePopup(component, content);
    }

    private void showContentRenamePopup(Component baseLabel, Content content) {
        JBTextField textField = new JBTextField(content.getDisplayName());
        textField.getEmptyText().setText(content.getDisplayName());
        textField.putClientProperty("StatusVisibleFunction", (BooleanFunction<JBTextField>)o -> o.getText().isEmpty());
        textField.selectAll();

        JBLabel label = new JBLabel(myLabeltext);
        label.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));

        JPanel panel = new JPanel(new VerticalFlowLayout());
        panel.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                IdeFocusManager.findInstance().requestFocus(textField, false);
            }
        });
        panel.add(label);
        panel.add(textField);

        Balloon balloon = JBPopupFactory.getInstance().createDialogBalloonBuilder(panel, null)
            .setShowCallout(true)
            .setCloseButtonEnabled(false)
            .setAnimationCycle(0)
            .setDisposable(content)
            .setHideOnKeyOutside(true)
            .setHideOnClickOutside(true)
            .setRequestFocus(true)
            .setBlockClicksThroughBalloon(true)
            .createBalloon();

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (!Disposer.isDisposed(content)) {
                        String text = textField.getText();
                        if (!text.isEmpty()) {
                            content.setDisplayName(text);
                        }
                        balloon.hide();
                    }
                }
            }
        });
        balloon.show(new RelativePoint(baseLabel, new Point(baseLabel.getWidth() / 2, 0)), Balloon.Position.above);
    }
}

/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.welcomeScreen;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.ide.impl.welcomeScreen.RecentProjectItemRender;
import consulo.project.ProjectGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author Konstantin Bulenkov
 */
public class NewRecentProjectPanel extends RecentProjectPanel {

    @RequiredUIAccess
    public NewRecentProjectPanel(Disposable parentDisposable, boolean welcomeScreen) {
        super(parentDisposable);

        myRootPanel.setBorder(JBUI.Borders.empty());
        myList.setBorder(JBUI.Borders.empty(4));

        installGroupKeys();

        if (welcomeScreen) {
            myRootPanel.setBackground(UIUtil.getPanelBackground());

            myScrollPane.setOpaque(false);
            myScrollPane.getViewport().setOpaque(false);
            myList.setOpaque(false);

            myScrollLayout.setSize(RecentProjectItemRender.LIST_WIDTH, RecentProjectItemRender.LIST_HEIGHT);
            myScrollLayout.setMinWidth(RecentProjectItemRender.LIST_WIDTH);
            myScrollLayout.setMinHeight(RecentProjectItemRender.LIST_HEIGHT);
        }
    }

    @Override
    protected boolean isUseGroups() {
        return true;
    }

    private void installGroupKeys() {
        myList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                ProjectGroup group = myListBox.getValue() instanceof PopupProjectGroupActionGroup selected
                    ? selected.getGroup()
                    : null;

                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_RIGHT) {
                    if (group == null) {
                        focusNext();
                    }
                    else if (!group.isExpanded()) {
                        group.setExpanded(true);

                        int index = myModel.indexOf(myListBox.getValue());
                        RecentProjectsWelcomeScreenActionBase.rebuildRecentProjectDataModel(myModel);
                        myListBox.setValueByIndex(group.getProjects().isEmpty() ? index : index + 1);
                    }
                }
                else if (keyCode == KeyEvent.VK_LEFT && group != null && group.isExpanded()) {
                    group.setExpanded(false);

                    int index = myModel.indexOf(myListBox.getValue());
                    RecentProjectsWelcomeScreenActionBase.rebuildRecentProjectDataModel(myModel);
                    myListBox.setValueByIndex(index);
                }
            }
        });
    }

    private void focusNext() {
        JFrame frame = UIUtil.getParentOfType(JFrame.class, myList);
        if (frame == null) {
            return;
        }

        FocusTraversalPolicy policy = frame.getFocusTraversalPolicy();
        if (policy == null) {
            return;
        }

        Component next = policy.getComponentAfter(frame, myList);
        if (next != null) {
            IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(next);
        }
    }

}

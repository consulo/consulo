/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.debugger;

import consulo.component.extension.Extensions;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ui.tabs.impl.JBEditorTabs;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.tab.JBTabs;
import consulo.ui.ex.awt.tab.TabInfo;
import consulo.ui.ex.awt.tab.UiDecorator;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class UiDebugger extends JPanel implements Disposable {

    private final DialogWrapper myDialog;
    private final JBTabs myTabs;
    private final UiDebuggerExtension[] myExtensions;

    public UiDebugger() {
        Disposer.register(Disposer.get("ui"), this);

        myTabs = new JBEditorTabs(null, ActionManager.getInstance(), null, this);
        myTabs.getPresentation().setPaintBorder(JBUI.scale(1), 0, 0, 0)
            .setActiveTabFillIn(JBColor.GRAY).setUiDecorator(new UiDecorator() {
                @Override
                @Nonnull
                public UiDecoration getDecoration() {
                    return new UiDecoration(null, JBUI.insets(4, 4, 4, 4));
                }
            });

        myExtensions = Extensions.getExtensions(UiDebuggerExtension.EP_NAME);
        addToUi(myExtensions);

        myDialog = new DialogWrapper((Project)null, true) {
            {
                init();
            }

            @Override
            protected JComponent createCenterPanel() {
                Disposer.register(getDisposable(), UiDebugger.this);
                return myTabs.getComponent();
            }

            @Override
            @RequiredUIAccess
            public JComponent getPreferredFocusedComponent() {
                return myTabs.getComponent();
            }

            @Override
            protected String getDimensionServiceKey() {
                return "UiDebugger";
            }

            @Override
            @RequiredUIAccess
            protected JComponent createSouthPanel() {
                JPanel result = new JPanel(new BorderLayout());
                result.add(super.createSouthPanel(), BorderLayout.EAST);
                JSlider slider = new JSlider(0, 100);
                slider.setValue(100);
                slider.addChangeListener(e -> {
                    int value = slider.getValue();
                    float alpha = value / 100f;

                    Window wnd = SwingUtilities.getWindowAncestor(slider);
                    if (wnd != null) {
                        WindowManagerEx mgr = WindowManagerEx.getInstanceEx();
                        if (value == 100) {
                            mgr.setAlphaModeEnabled(wnd, false);
                        }
                        else {
                            mgr.setAlphaModeEnabled(wnd, true);
                            mgr.setAlphaModeRatio(wnd, 1f - alpha);
                        }
                    }
                });
                result.add(slider, BorderLayout.WEST);
                return result;
            }

            @Nonnull
            @Override
            protected Action[] createActions() {
                return new Action[]{new AbstractAction("Close") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        doOKAction();
                    }
                }};
            }
        };
        myDialog.setModal(false);
        myDialog.setTitle("UI Debugger");
        myDialog.setResizable(true);

        myDialog.show();
    }

    @Override
    public void show() {
        myDialog.getPeer().getWindow().toFront();
    }

    private void addToUi(UiDebuggerExtension[] extensions) {
        for (UiDebuggerExtension each : extensions) {
            myTabs.addTab(new TabInfo(each.getComponent()).setText(each.getName()));
        }
    }

    @Override
    public void dispose() {
        for (UiDebuggerExtension each : myExtensions) {
            each.disposeUiResources();
        }
    }
}

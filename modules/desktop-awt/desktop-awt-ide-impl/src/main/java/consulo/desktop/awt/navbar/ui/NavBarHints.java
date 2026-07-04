/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 *
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.navbar.ui;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.language.editor.hint.HintManager;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Shows the floating navigation bar hint, either centered above the editor or at the best popup
 * location of the invocation context.
 */
public final class NavBarHints {
    private NavBarHints() {
    }

    public static LightweightHintImpl showHint(DataContext dataContext,
                                               Project project,
                                               NewNavBarPanel panel,
                                               @RequiredUIAccess Runnable onCancel) {
        JPanel wrappedPanel = wrapNavbarPanel(panel);
        LightweightHintImpl hint = createHint(wrappedPanel, onCancel);
        panel.setOnSizeChange(() -> hint.setSize(wrappedPanel.getPreferredSize()));

        Editor editor = dataContext.getData(Editor.KEY);
        if (editor != null) {
            showEditorHint(editor, project, hint);
        }
        else {
            showNonEditorHint(dataContext, project, hint);
        }
        return hint;
    }

    private static void showEditorHint(Editor editor, Project project, LightweightHintImpl hint) {
        JComponent hintContainer = editor.getContentComponent();
        Point center = AbstractPopup.getCenterOf(hintContainer, hint.getComponent());
        center.y -= hintContainer.getVisibleRect().height / 4;
        RelativePoint showPoint = guessEvenBetterPopupLocation(RelativePoint.fromScreen(center), project);
        Point absoluteShowPoint = showPoint.getPoint(hintContainer);
        HintHint hintInfo = new HintHint(editor.getContentComponent(), absoluteShowPoint);
        HintManagerImpl.getInstanceImpl()
            .showEditorHint(hint, editor, absoluteShowPoint, HintManager.HIDE_BY_ESCAPE, 0, true, hintInfo);
    }

    private static void showNonEditorHint(DataContext dataContext, Project project, LightweightHintImpl hint) {
        Component contextComponent = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (contextComponent == null) {
            return;
        }
        RelativePoint showPoint = guessEvenBetterPopupLocation(
            JBPopupFactory.getInstance().guessBestPopupLocation(dataContext),
            project
        );

        Component component = showPoint.getComponent();
        if (component instanceof JComponent jComponent && jComponent.isShowing()) {
            HintHint hintInfo = new HintHint(component, showPoint.getPoint());
            hint.show(
                jComponent,
                showPoint.getPoint().x,
                showPoint.getPoint().y,
                contextComponent instanceof JComponent contextJComponent ? contextJComponent : null,
                hintInfo
            );
        }
    }

    private static JPanel wrapNavbarPanel(NewNavBarPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel);
        wrapper.setOpaque(true);
        wrapper.setBorder(JBUI.Borders.empty(4));
        wrapper.setBackground(NavBarUi.floatingBackground());
        return wrapper;
    }

    private static LightweightHintImpl createHint(JPanel contents, Runnable onCancel) {
        LightweightHintImpl hint = new LightweightHintImpl(contents) {
            @Override
            protected void onPopupCancel() {
                onCancel.run();
            }
        };
        hint.setForceShowAsPopup(true);
        hint.setFocusRequestor(contents);
        return hint;
    }

    private static RelativePoint guessEvenBetterPopupLocation(RelativePoint point, Project project) {
        if (point.getComponent() instanceof JComponent component && component.isShowing()) {
            return point;
        }

        //Yes. It happens sometimes.
        // 1. Empty frame. call nav bar, select some package and open it in Project View
        // 2. Call nav bar, then Esc
        // 3. Hide all tool windows (Ctrl+Shift+F12), so we've got empty frame again
        // 4. Call nav bar. NPE. ta da
        Component ideFrame = TargetAWT.to(WindowManager.getInstance().getIdeFrame(project).getWindow());
        JRootPane rootPane = UIUtil.getRootPane(ideFrame);
        return JBPopupFactory.getInstance().guessBestPopupLocation(rootPane);
    }
}

/*
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
package consulo.it.internal;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import jakarta.inject.Singleton;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
@ServiceImpl
@Singleton
@Deprecated
@DeprecationInfo("AWT dep - must removed")
public class HeadlessCommonActionsManager extends CommonActionsManager {
    private static AnAction stub() {
        return new AnAction() {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {

            }
        };
    }
    
    @Override
    public AnAction createPrevOccurenceAction(OccurenceNavigator navigator) {
        return stub();
    }

    @Override
    public AnAction createNextOccurenceAction(OccurenceNavigator navigator) {
        return stub();
    }

    @Override
    public AnAction createExpandAllAction(TreeExpander expander) {
        return stub();
    }

    @Override
    public AnAction createExpandAllAction(TreeExpander expander, JComponent component) {
        return stub();
    }

    @Override
    public AnAction createExpandAllHeaderAction(JTree tree) {
        return stub();
    }

    @Override
    public AnAction createCollapseAllAction(TreeExpander expander) {
        return stub();
    }

    @Override
    public AnAction createCollapseAllAction(TreeExpander expander, JComponent component) {
        return stub();
    }

    @Override
    public AnAction createCollapseAllHeaderAction(JTree tree) {
        return stub();
    }

    @Override
    public AnAction createHelpAction(String helpId) {
        return stub();
    }

    @Override
    public AnAction installAutoscrollToSourceHandler(ComponentManager project, JTree tree, AutoScrollToSourceOptionProvider optionProvider) {
        return stub();
    }

    @Override
    public AnAction createExportToTextFileAction(ExporterToTextFile exporter) {
        return stub();
    }
}

/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.ToolWindowLayout;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.concurrent.ActionCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "TogglePresentationMode")
public class TogglePresentationModeAction extends AnAction implements DumbAware {
    private static final Map<Object, Object> ourSavedValues = new LinkedHashMap<>();
    private static float ourSavedScaleFactor = JBUI.scale(1f);
    private static int ourSavedConsoleFontSize;

    public TogglePresentationModeAction() {
        super(ActionLocalize.actionTogglepresentationmodeText(), ActionLocalize.actionTogglepresentationmodeDescription());
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        boolean selected = UISettings.getInstance().PRESENTATION_MODE;
        e.getPresentation().setTextValue(
            selected
                ? ActionLocalize.actionTogglepresentationmodeTextExit()
                : ActionLocalize.actionTogglepresentationmodeTextEnter()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        UISettings settings = UISettings.getInstance();
        Project project = e.getData(Project.KEY);

        setPresentationMode(project, !settings.PRESENTATION_MODE);
    }

    @RequiredUIAccess
    public static void setPresentationMode(Project project, boolean inPresentation) {
        UISettings settings = UISettings.getInstance();
        settings.PRESENTATION_MODE = inPresentation;

        boolean layoutStored = storeToolWindows(project);

        tweakUIDefaults(settings, inPresentation);

        ActionCallback callback = project == null ? ActionCallback.DONE : tweakFrameFullScreen(project, inPresentation);
        callback.doWhenProcessed(() -> {
            tweakEditorAndFireUpdateUI(settings, inPresentation);

            //noinspection RequiredXAction
            restoreToolWindows(project, layoutStored, inPresentation);
        });
    }

    private static ActionCallback tweakFrameFullScreen(Project project, boolean inPresentation) {
        IdeFrameEx frame = (IdeFrameEx) IdeFrameUtil.findActiveRootIdeFrame();
        if (frame != null) {
            PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
            if (inPresentation) {
                propertiesComponent.setValue("full.screen.before.presentation.mode", String.valueOf(frame.isInFullScreen()));
                return frame.toggleFullScreen(true);
            }
            else {
                if (frame.isInFullScreen()) {
                    String value = propertiesComponent.getValue("full.screen.before.presentation.mode");
                    return frame.toggleFullScreen("true".equalsIgnoreCase(value));
                }
            }
        }
        return ActionCallback.DONE;
    }

    private static void tweakEditorAndFireUpdateUI(UISettings settings, boolean inPresentation) {
        EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
        int fontSize = inPresentation ? settings.PRESENTATION_MODE_FONT_SIZE : globalScheme.getEditorFontSize();
        if (inPresentation) {
            ourSavedConsoleFontSize = globalScheme.getConsoleFontSize();
            globalScheme.setConsoleFontSize(fontSize);
        }
        else {
            globalScheme.setConsoleFontSize(ourSavedConsoleFontSize);
        }
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (editor instanceof EditorEx editorEx) {
                editorEx.setFontSize(fontSize);
            }
        }
        UISettings.getInstance().fireUISettingsChanged();
        LafManager.getInstance().updateUI();
        EditorUtil.reinitSettings();
    }

    private static void tweakUIDefaults(UISettings settings, boolean inPresentation) {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        if (inPresentation) {
            while (keys.hasMoreElements()) {
                if (keys.nextElement() instanceof String name) {
                    if (name.endsWith(".font")) {
                        Font font = defaults.getFont(name);
                        ourSavedValues.put(name, font);
                    }
                    else if (name.endsWith(".rowHeight")) {
                        ourSavedValues.put(name, defaults.getInt(name));
                    }
                }
            }
            float scaleFactor = settings.PRESENTATION_MODE_FONT_SIZE / UIUtil.DEF_SYSTEM_FONT_SIZE;
            ourSavedScaleFactor = JBUI.scale(1f);
            JBUI.setUserScaleFactor(scaleFactor);
            for (Object key : ourSavedValues.keySet()) {
                Object v = ourSavedValues.get(key);
                if (v instanceof Font font) {
                    defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), JBUI.scale(font.getSize())));
                }
                else if (v instanceof Integer i) {
                    defaults.put(key, JBUI.scale(i));
                }
            }
        }
        else {
            for (Object key : ourSavedValues.keySet()) {
                defaults.put(key, ourSavedValues.get(key));
            }
            JBUI.setUserScaleFactor(ourSavedScaleFactor);
            ourSavedValues.clear();
        }
    }

    @RequiredUIAccess
    private static boolean hideAllToolWindows(ToolWindowManagerEx manager) {
        // to clear windows stack
        manager.clearSideStack();

        String[] ids = manager.getToolWindowIds();
        boolean hasVisible = false;
        for (String id : ids) {
            ToolWindow toolWindow = manager.getToolWindow(id);
            if (toolWindow.isVisible()) {
                toolWindow.hide(null);
                hasVisible = true;
            }
        }
        return hasVisible;
    }

    @RequiredUIAccess
    static boolean storeToolWindows(@Nullable Project project) {
        if (project == null) {
            return false;
        }
        ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);

        ToolWindowLayout layout = new ToolWindowLayout();
        layout.copyFrom(manager.getLayout());
        boolean hasVisible = hideAllToolWindows(manager);

        if (hasVisible) {
            manager.setLayoutToRestoreLater(layout);
            manager.activateEditorComponent();
        }
        return hasVisible;
    }

    @RequiredUIAccess
    static void restoreToolWindows(Project project, boolean needsRestore, boolean inPresentation) {
        if (project == null || !needsRestore) {
            return;
        }
        ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);
        ToolWindowLayout restoreLayout = manager.getLayoutToRestoreLater();
        if (!inPresentation && restoreLayout != null) {
            manager.setLayout(restoreLayout);
        }
    }
}

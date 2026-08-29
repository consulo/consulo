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
package consulo.desktop.awt.ui.impl.style;

import consulo.application.ui.UIFontManager;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.internal.EditorColorsManagerInternal;
import consulo.desktop.awt.ui.impl.plaf.LafWithColorScheme;
import consulo.desktop.awt.ui.impl.plaf.LookAndFeelInfoWithClassLoader;
import consulo.desktop.awt.ui.impl.plaf2.*;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.AntialiasingType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.UIModificationTracker;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.impl.style.PersistentStyleManagerImpl;
import consulo.ui.style.Style;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.virtualFileSystem.status.FileStatusManager;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-29
 */
public class DesktopAWTStyleManagerImpl extends PersistentStyleManagerImpl<DesktopStyleImpl> {
    public static final DesktopAWTStyleManagerImpl INSTANCE = new DesktopAWTStyleManagerImpl();

    private static final Logger LOG = Logger.getInstance(DesktopAWTStyleManagerImpl.class);

    @Override
    protected void fill(Consumer<DesktopStyleImpl> consumer) {
        consumer.accept(new DesktopStyleImpl(new IdeLookAndFeelInfo(Style.LIGHT_ID, "Light", ConsuloLightLaf.class.getName(), false)));
        consumer.accept(new DesktopStyleImpl(new IdeLookAndFeelInfo(Style.SEMI_DARK, "Dark Grey", ConsuloDarkGreyLaf.class.getName(), true)));
        consumer.accept(new DesktopStyleImpl(new IdeLookAndFeelInfo(Style.DARK_ID, "Dark", ConsuloDarkLaf.class.getName(), true)));
    }

    @Override
    @RequiredUIAccess
    public void setCurrentStyle(DesktopStyleImpl style, boolean wantChangeScheme, boolean fire, String iconLibraryId) {
        if (myStyles.get(style.getId()) == null) {
            LOG.error("unknown LookAndFeel : " + style);
            return;
        }

        UIManager.LookAndFeelInfo lookAndFeelInfo = style.getLookAndFeelInfo();

        try {
            UIModificationTracker.getInstance().incModificationCount();

            Thread thread = Thread.currentThread();
            ClassLoader old = thread.getContextClassLoader();

            ClassLoader targetClassLoader;
            try {
                targetClassLoader = null;
                if (lookAndFeelInfo instanceof LookAndFeelInfoWithClassLoader) {
                    targetClassLoader = ((LookAndFeelInfoWithClassLoader) lookAndFeelInfo).getClassLoader();

                    thread.setContextClassLoader(targetClassLoader);

                    UIManager.setLookAndFeel(newInstance((LookAndFeelInfoWithClassLoader) lookAndFeelInfo));
                }
                else {
                    UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                }
            }
            finally {
                thread.setContextClassLoader(old);
            }

            myCurrentStyle = style;

            if (targetClassLoader != null) {
                UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();

                uiDefaults.put("ClassLoader", targetClassLoader);
            }

            if (iconLibraryId != null) {
                myIconLibraryManager.setActiveLibrary(iconLibraryId);
            }
            else {
                myIconLibraryManager.setActiveLibraryFromActiveStyle();
            }

            if (wantChangeScheme) {
                if (lookAndFeelInfo instanceof LafWithColorScheme) {
                    EditorColorsManagerInternal editorColorsManager = (EditorColorsManagerInternal) EditorColorsManager.getInstance();
                    EditorColorsScheme editorColorsScheme = editorColorsManager.getScheme(((LafWithColorScheme) lookAndFeelInfo).getColorSchemeName());
                    if (editorColorsScheme != null) {
                        editorColorsManager.setGlobalSchemeNoRefreshUI(editorColorsScheme);
                    }
                }
            }

            if (fire) {
                fireUpdate();
            }
        }
        catch (Exception e) {
            LOG.error(e);
            SwingUtilities.invokeLater(() -> Messages.showMessageDialog(
                IdeLocalize.errorCannotSetLookAndFeel(lookAndFeelInfo.getName(), e.getMessage()).get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            ));
        }
    }

    @RequiredUIAccess
    private void fireUpdate() {
        forceReinitAll();

        UISettings.getInstance().fireUISettingsChanged();

        EditorFactory factory = EditorFactory.getInstance();

        for (Editor editor : factory.getAllEditors()) {
            if (editor instanceof EditorEx editorEx) {
                editorEx.updateUI();
            }
        }

        factory.refreshAllEditors();

        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project openProject : openProjects) {
            FileStatusManager.getInstance(openProject).fileStatusesChanged();
            DaemonCodeAnalyzer.getInstance(openProject).restart();
        }

        for (IdeFrame frame : WindowManagerEx.getInstanceEx().getAllProjectFrames()) {
            if (frame instanceof IdeFrameEx) {
                ((IdeFrameEx) frame).updateView();
            }
        }

        ActionToolbarsHolder.updateAllToolbarsImmediately();
    }

    private LookAndFeel newInstance(LookAndFeelInfoWithClassLoader lookAndFeel) throws Exception {
        ClassLoader classLoader = lookAndFeel.getClassLoader();

        Class<?> clazz = Class.forName(lookAndFeel.getClassName(), true, classLoader);

        return (LookAndFeel) ReflectionUtil.newInstance(clazz);
    }

    @Override
    public void refreshAntialiasingType(AntialiasingType antialiasingType) {
        for (Window w : Window.getWindows()) {
            for (JComponent c : UIUtil.uiTraverser(w).filter(JComponent.class)) {
                GraphicsUtil.setAntialiasingType(c, DesktopAntialiasingTypeUtil.getAntialiasingTypeForSwingComponent());
            }
        }
    }

    @Override
    public void forceReinitAll() {
        patchLafFonts();

        updateToolWindows();

        for (Frame frame : Frame.getFrames()) {
            updateUI(frame);
        }

        for (Window window : Window.getWindows()) {
            updateUI(window);
        }

        SmoothScrollingListener.set(UISettings.getInstance());

        Style currentStyle = getCurrentStyle();

        fireStyleChanged(currentStyle, currentStyle);
    }

    private void patchLafFonts() {
        UIFontManager uiSettings = UIFontManager.getInstance();
        if (uiSettings.isOverrideFont()) {
            Font font = StyleContext.getDefaultStyleContext().getFont(uiSettings.getFontName(), Font.PLAIN, uiSettings.getFontSize());
            if (font instanceof UIResource) {
                font = font.deriveFont(Font.PLAIN);
            }

            UIManager.put("defaultFont", font);
        }
        else {
            UIManager.put("defaultFont", null);
        }
    }

    private static void updateUI(Window window) {
        if (!window.isDisplayable()) {
            return;
        }

        IJSwingUtilities.updateComponentTreeUI(window);

        Window[] children = window.getOwnedWindows();
        for (Window aChildren : children) {
            updateUI(aChildren);
        }
    }

    public static void updateToolWindows() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            for (String id : toolWindowManager.getToolWindowIds()) {
                ToolWindow toolWindow = toolWindowManager.getToolWindow(id);
                for (Content content : toolWindow.getContentManager().getContents()) {
                    JComponent component = content.getComponent();
                    if (component != null) {
                        IJSwingUtilities.updateComponentTreeUI(component);
                    }
                }
                JComponent c = toolWindow.getComponent();
                if (c != null) {
                    IJSwingUtilities.updateComponentTreeUI(c);
                }
            }
        }
    }

    @Override
    public void forceRepaintAll() {
        Frame[] frames = Frame.getFrames();
        for (Frame frame : frames) {
            repaintUI(frame);
        }
    }

    private static void repaintUI(Window window) {
        if (!window.isDisplayable()) {
            return;
        }

        window.repaint();

        Window[] children = window.getOwnedWindows();
        for (Window aChildren : children) {
            repaintUI(aChildren);
        }
    }
}

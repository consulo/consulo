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
package consulo.desktop.awt.ui.plaf;

import consulo.annotation.component.ServiceImpl;
import consulo.application.CommonBundle;
import consulo.application.ui.UIFontManager;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.internal.EditorColorsManagerInternal;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.desktop.awt.ui.impl.style.DesktopStyleImpl;
import consulo.desktop.awt.ui.plaf2.*;
import consulo.disposer.Disposable;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.ide.impl.idea.ide.ui.LafManagerListener;
import consulo.ide.impl.idea.util.ReflectionUtil;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.localize.LocalizeManager;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.proxy.EventDispatcher;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.UIModificationTracker;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.style.Style;
import consulo.util.collection.impl.map.LinkedHashMap;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Belyaev
 * @author Vladimir Kondratyev
 */
@State(name = "LafManager", storages = @Storage(value = "laf.xml", roamingType = RoamingType.PER_OS))
@Singleton
@ServiceImpl
public final class LafManagerImpl implements LafManager, Disposable, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(LafManagerImpl.class);

    private static final String ELEMENT_LAF = "laf";
    private static final String ELEMENT_LOCALE = "locale";
    private static final String ELEMENT_ICON = "icon";
    private static final String ATTRIBUTE_CLASS_NAME = "class-name";
    private static final String ELEMENT_THEME = "theme";

    private final EventDispatcher<LafManagerListener> myListenerList = EventDispatcher.create(LafManagerListener.class);

    private final Map<String, DesktopStyleImpl> myStyles = new LinkedHashMap<>();
    private DesktopStyleImpl myCurrentStyle;

    private final LocalizeManager myLocalizeManager;
    private final IconLibraryManager myIconLibraryManager;

    private boolean myInitialLoadState = true;

    private static Map<String, String> ourOldMapping = Map.of(
        "consulo.desktop.awt.ui.plaf.intellij.IntelliJLaf", Style.LIGHT_ID,
        "consulo.desktop.awt.ui.plaf.darcula.DarculaLaf", Style.SEMI_DARK
    );

    @Inject
    LafManagerImpl() {
        myLocalizeManager = LocalizeManager.get();
        myIconLibraryManager = IconLibraryManager.get();

        List<UIManager.LookAndFeelInfo> lafList = new ArrayList<>();

        lafList.add(new IdeLookAndFeelInfo(Style.LIGHT_ID, "Light", ConsuloLightLaf.class.getName(), false));
        lafList.add(new IdeLookAndFeelInfo(Style.SEMI_DARK, "Dark Grey", ConsuloDarkGreyLaf.class.getName(), true));
        lafList.add(new IdeLookAndFeelInfo(Style.DARK_ID, "Dark", ConsuloDarkLaf.class.getName(), true));

        for (UIManager.LookAndFeelInfo lookAndFeelInfo : lafList) {
            DesktopStyleImpl style = new DesktopStyleImpl(lookAndFeelInfo);

            myStyles.put(style.getId(), style);
        }

        myCurrentStyle = getDefaultStyle();
    }

    @Override
    public void addLafManagerListener(@Nonnull LafManagerListener l) {
        myListenerList.addListener(l);
    }

    @Override
    public void addLafManagerListener(LafManagerListener l, Disposable disposable) {
        myListenerList.addListener(l, disposable);
    }

    @Override
    public void removeLafManagerListener(@Nonnull LafManagerListener l) {
        myListenerList.removeListener(l);
    }

    private void fireLookAndFeelChanged() {
        myListenerList.getMulticaster().lookAndFeelChanged(this);
    }

    @Override
    public void afterLoadState() {
        myInitialLoadState = false;

        DesktopStyleImpl style = myCurrentStyle;
        
        if (style != null) {
            DesktopStyleImpl laf = myStyles.get(style.getId());
            if (laf != null) {
                BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager) IconLibraryManager.get();
                boolean fromStyle = iconLibraryManager.isFromStyle();
                String activeLibraryId = iconLibraryManager.getActiveLibraryId(style);
                setCurrentStyle(laf, false, false, fromStyle ? null : activeLibraryId);
            }
        }

        updateUI();

        // refresh UI on localize change
        LocalizeManager.get().addListener((oldLocale, newLocale) -> updateUI(), this);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void loadState(Element element) {
        String oldLafClassName = null;

        Element lafElement = element.getChild(ELEMENT_LAF);
        if (lafElement != null) {
            oldLafClassName = lafElement.getAttributeValue(ATTRIBUTE_CLASS_NAME);
        }

        String localeText = element.getChildText(ELEMENT_LOCALE);
        if (localeText != null) {
            myLocalizeManager.setLocale(myLocalizeManager.parseLocale(localeText), false);
        }

        String iconId = element.getChildText(ELEMENT_ICON);
        if (iconId != null) {
            if (myIconLibraryManager.getLibraries().containsKey(iconId)) {
                myIconLibraryManager.setActiveLibrary(iconId);
            }
            else {
                myIconLibraryManager.setActiveLibrary(null);
            }
        }

        String themeId = element.getChildTextTrim(ELEMENT_THEME);
        if (themeId == null && oldLafClassName != null) {
            themeId = ourOldMapping.get(oldLafClassName);
        }

        DesktopStyleImpl styleFromXml = themeId == null ? null : myStyles.get(themeId);

        if (styleFromXml == null) {
            styleFromXml = getDefaultStyle();
        }

        if (myCurrentStyle != null && !styleFromXml.equals(myCurrentStyle)) {
            boolean fire = !myInitialLoadState;
            setCurrentStyle(styleFromXml, false, fire, iconId);

            if (fire) {
                // will be called #afterLoadState()
                updateUI();
            }
        }

        myCurrentStyle = styleFromXml;
    }

    @Override
    public Element getState() {
        Element element = new Element("state");
        if (myCurrentStyle != null) {
            Element child = new Element(ELEMENT_THEME);
            child.setText(myCurrentStyle.getId());
            element.addContent(child);
        }

        if (!myLocalizeManager.isDefaultLocale()) {
            Element localeElement = new Element(ELEMENT_LOCALE);
            element.addContent(localeElement);

            localeElement.setText(myLocalizeManager.getLocale().toString());
        }

        if (!myIconLibraryManager.isFromStyle()) {
            Element iconElement = new Element(ELEMENT_ICON);
            element.addContent(iconElement);

            iconElement.setText(myIconLibraryManager.getActiveLibraryId());
        }
        return element;
    }

    @Nonnull
    @Override
    public List<Style> getStyles() {
        return new ArrayList<>(myStyles.values());
    }

    @Nonnull
    @Override
    public Style getCurrentStyle() {
        return myCurrentStyle;
    }

    @Nonnull
    private DesktopStyleImpl getDefaultStyle() {
        boolean darked = Platform.current().user().darkTheme();
        DesktopStyleImpl style = myStyles.get(darked ? Style.DARK_ID : Style.LIGHT_ID);
        if (style != null) {
            return style;
        }
        throw new IllegalStateException("No default look&feel found");
    }

    @Override
    public void setCurrentStyle(@Nonnull Style currentStyle) {
        DesktopStyleImpl style = (DesktopStyleImpl) currentStyle;
        setCurrentStyle(style, true, true, null);
    }

    @RequiredUIAccess
    private void setCurrentStyle(DesktopStyleImpl style, boolean wantChangeScheme, boolean fire, String iconLibraryId) {
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
            SwingUtilities.invokeLater(() -> {
                Messages.showMessageDialog(IdeBundle.message("error.cannot.set.look.and.feel", lookAndFeelInfo.getName(), e.getMessage()),
                    CommonBundle.getErrorTitle(),
                    Messages.getErrorIcon());
            });
        }
    }

    @Nonnull
    private LookAndFeel newInstance(LookAndFeelInfoWithClassLoader lookAndFeel) throws Exception {
        ClassLoader classLoader = lookAndFeel.getClassLoader();

        Class<?> clazz = Class.forName(lookAndFeel.getClassName(), true, classLoader);

        return (LookAndFeel) ReflectionUtil.newInstance(clazz);
    }

    @RequiredUIAccess
    private void fireUpdate() {
        updateUI();

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

    /**
     * Updates LAF of all windows. The method also updates font of components
     * as it's configured in <code>UISettings</code>.
     */
    @Override
    public void updateUI() {
        patchLafFonts();

        updateToolWindows();

        for (Frame frame : Frame.getFrames()) {
            updateUI(frame);
        }

        for (Window window : Window.getWindows()) {
            updateUI(window);
        }

        SmoothScrollingListener.set(UISettings.getInstance());

        fireLookAndFeelChanged();
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

    /**
     * Repaints all displayable window.
     */
    @Override
    public void repaintUI() {
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

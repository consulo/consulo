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

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import consulo.annotation.component.ServiceImpl;
import consulo.application.CommonBundle;
import consulo.application.ui.UIFontManager;
import consulo.application.ui.UISettings;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.desktop.awt.ui.impl.style.DesktopStyleImpl;
import consulo.desktop.awt.ui.plaf2.IdeLookAndFeelInfo;
import consulo.disposer.Disposable;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.ide.impl.idea.ide.ui.LafManagerListener;
import consulo.ide.impl.idea.util.EventDispatcher;
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
import consulo.ui.ex.UIModificationTracker;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.style.Style;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.util.List;
import java.util.*;

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

    private final EventDispatcher<LafManagerListener> myListenerList = EventDispatcher.create(LafManagerListener.class);

    private final List<DesktopStyleImpl> myStyles;
    private DesktopStyleImpl myCurrentStyle;

    private final Map<UIManager.LookAndFeelInfo, HashMap<String, Object>> myStoredDefaults = new HashMap<>();
    private final LocalizeManager myLocalizeManager;
    private final IconLibraryManager myIconLibraryManager;

    private boolean initialLoadState = true;

    @Inject
    LafManagerImpl() {
        myLocalizeManager = LocalizeManager.get();
        myIconLibraryManager = IconLibraryManager.get();

        List<UIManager.LookAndFeelInfo> lafList = new ArrayList<>();

        Couple<Class<? extends FlatLaf>> defaultLafs = getDefaultLafs();

        lafList.add(new IdeLookAndFeelInfo("Default Light", defaultLafs.getFirst().getName(), false));
        lafList.add(new IdeLookAndFeelInfo("Default Dark", defaultLafs.getSecond().getName(), true));

        lafList.add(new IdeLookAndFeelInfo("IntelliJ", FlatIntelliJLaf.class.getName(), false));
        lafList.add(new IdeLookAndFeelInfo("Darcula", FlatDarculaLaf.class.getName(), true));

        for (FlatAllIJThemes.FlatIJLookAndFeelInfo info : FlatAllIJThemes.INFOS) {
            lafList.add(new IdeLookAndFeelInfo(info.getName(), info.getClassName(), info.isDark()));
        }

        myStyles = new ArrayList<>(lafList.size());

        for (UIManager.LookAndFeelInfo lookAndFeelInfo : lafList) {
            myStyles.add(new DesktopStyleImpl(lookAndFeelInfo));
        }

        myCurrentStyle = getDefaultStyle();
    }

    public static Couple<Class<? extends FlatLaf>> getDefaultLafs() {
        Platform platform = Platform.current();

        Class<? extends FlatLaf> light = FlatLightLaf.class;
        Class<? extends FlatLaf> dark = FlatDarkLaf.class;
        if (platform.os().isMac()) {
            light = FlatMacLightLaf.class;
            dark = FlatMacDarkLaf.class;
        }

        return Couple.of(light, dark);
    }

    @Override
    public void addLafManagerListener(@Nonnull final LafManagerListener l) {
        myListenerList.addListener(l);
    }

    @Override
    public void addLafManagerListener(LafManagerListener l, Disposable disposable) {
        myListenerList.addListener(l, disposable);
    }

    @Override
    public void removeLafManagerListener(@Nonnull final LafManagerListener l) {
        myListenerList.removeListener(l);
    }

    private void fireLookAndFeelChanged() {
        myListenerList.getMulticaster().lookAndFeelChanged(this);
    }

    @Override
    public void afterLoadState() {
        initialLoadState = false;

        if (myCurrentStyle != null) {
            final DesktopStyleImpl laf = findStyleByClassName(myCurrentStyle.getLookAndFeelInfo().getClassName());
            if (laf != null) {
                BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager) IconLibraryManager.get();
                boolean fromStyle = iconLibraryManager.isFromStyle();
                String activeLibraryId = iconLibraryManager.getActiveLibraryId(myCurrentStyle);
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
    public void loadState(final Element element) {
        String className = null;

        Element lafElement = element.getChild(ELEMENT_LAF);
        if (lafElement != null) {
            className = lafElement.getAttributeValue(ATTRIBUTE_CLASS_NAME);
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

        DesktopStyleImpl styleFromXml = className == null ? null : findStyleByClassName(className);
        // If LAF is undefined (wrong class name or something else) we have set default LAF anyway.
        if (styleFromXml == null) {
            styleFromXml = getDefaultStyle();
        }

        if (myCurrentStyle != null && !styleFromXml.equals(myCurrentStyle)) {
            boolean fire = !initialLoadState;
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
            String className = myCurrentStyle.getClassName();

            Element child = new Element(ELEMENT_LAF);
            child.setAttribute(ATTRIBUTE_CLASS_NAME, className);
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
        return Collections.unmodifiableList(myStyles);
    }

    @Nonnull
    @Override
    public Style getCurrentStyle() {
        return myCurrentStyle;
    }

    @Nonnull
    @Override
    public UIManager.LookAndFeelInfo[] getInstalledLookAndFeels() {
        return myStyles.stream().map(DesktopStyleImpl::getLookAndFeelInfo).toArray(UIManager.LookAndFeelInfo[]::new);
    }

    @Override
    public UIManager.LookAndFeelInfo getCurrentLookAndFeel() {
        return myCurrentStyle.getLookAndFeelInfo();
    }

    @Nonnull
    private DesktopStyleImpl getDefaultStyle() {
        Couple<Class<? extends FlatLaf>> defaultLafs = getDefaultLafs();
        boolean darked = Platform.current().user().darkTheme();
        Class<? extends FlatLaf> themeClass = darked ? defaultLafs.getSecond() : defaultLafs.getFirst();

        DesktopStyleImpl ideaLaf = findStyleByClassName(themeClass.getName());
        if (ideaLaf != null) {
            return ideaLaf;
        }
        throw new IllegalStateException("No default look&feel found");
    }

    /**
     * Finds LAF by its class name.
     * will be returned.
     */
    @Nullable
    private DesktopStyleImpl findStyleByClassName(@Nonnull String className) {
        for (DesktopStyleImpl style : myStyles) {
            UIManager.LookAndFeelInfo lookAndFeelInfo = style.getLookAndFeelInfo();

            if (Comparing.equal(lookAndFeelInfo.getClassName(), className)) {
                return style;
            }
        }
        return null;
    }

    @Override
    public void setCurrentLookAndFeel(@Nonnull UIManager.LookAndFeelInfo lookAndFeelInfo) {
        DesktopStyleImpl style = findStyleByClassName(lookAndFeelInfo.getClassName());
        LOG.assertTrue(style != null, "Class not found : " + lookAndFeelInfo.getClassName());
        setCurrentStyle(style, true, true, null);
    }

    @Override
    public void setCurrentStyle(@Nonnull Style currentStyle) {
        DesktopStyleImpl style = (DesktopStyleImpl) currentStyle;
        setCurrentStyle(style, true, true, null);
    }

    private void setCurrentStyle(DesktopStyleImpl style, boolean wantChangeScheme, boolean fire, String iconLibraryId) {
        if (findStyleByClassName(style.getClassName()) == null) {
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
                final UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();

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
                    EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
                    EditorColorsScheme editorColorsScheme = editorColorsManager.getScheme(((LafWithColorScheme) lookAndFeelInfo).getColorSchemeName());
                    if (editorColorsScheme != null) {
                        editorColorsManager.setGlobalScheme(editorColorsScheme);
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

    private static void fireUpdate() {
        UISettings.getInstance().fireUISettingsChanged();
        EditorFactory.getInstance().refreshAllEditors();

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
        final UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();

        patchLafFonts(uiDefaults);

        patchHiDPI(uiDefaults);

        updateToolWindows();
        for (Frame frame : Frame.getFrames()) {
            updateUI(frame);
        }
        fireLookAndFeelChanged();
    }

    public static void updateToolWindows() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            for (String id : toolWindowManager.getToolWindowIds()) {
                final ToolWindow toolWindow = toolWindowManager.getToolWindow(id);
                for (Content content : toolWindow.getContentManager().getContents()) {
                    final JComponent component = content.getComponent();
                    if (component != null) {
                        IJSwingUtilities.updateComponentTreeUI(component);
                    }
                }
                final JComponent c = toolWindow.getComponent();
                if (c != null) {
                    IJSwingUtilities.updateComponentTreeUI(c);
                }
            }
        }
    }

    private static void patchHiDPI(UIDefaults defaults) {
        Object prevScaleVal = defaults.get("hidpi.scaleFactor");
        // used to normalize previously patched values
        float prevScale = prevScaleVal != null ? (Float) prevScaleVal : 1f;

        if (prevScale == JBUI.scale(1f) && prevScaleVal != null) {
            return;
        }

        List<String> myIntKeys = Arrays.asList("Tree.leftChildIndent", "Tree.rightChildIndent", "Tree.rowHeight");

        List<String> myDimensionKeys = Arrays.asList("Slider.horizontalSize", "Slider.verticalSize", "Slider.minimumHorizontalSize", "Slider.minimumVerticalSize");

        for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey().toString();
            if (value instanceof Dimension) {
                if (value instanceof UIResource || myDimensionKeys.contains(key)) {
                    entry.setValue(JBUI.size((Dimension) value).asUIResource());
                }
            }
            else if (value instanceof Insets) {
                if (value instanceof UIResource) {
                    entry.setValue(JBUI.insets(((Insets) value)).asUIResource());
                }
            }
            else if (value instanceof Integer) {
                // FIXME [VISTALL] we already fix maxGutterIconWidth with UI classes
                if (/*key.endsWith(".maxGutterIconWidth") || */myIntKeys.contains(key)) {
                    int normValue = (int) ((Integer) value / prevScale);
                    entry.setValue(Integer.valueOf(JBUI.scale(normValue)));
                }
            }
        }
        defaults.put("hidpi.scaleFactor", JBUI.scale(1f));
    }

    private void patchLafFonts(UIDefaults uiDefaults) {
        UIFontManager uiSettings = UIFontManager.getInstance();
        if (uiSettings.isOverrideFont()) {
            storeOriginalFontDefaults(uiDefaults);
            JBUI.setUserScaleFactor(uiSettings.getFontSize() / UIUtil.DEF_SYSTEM_FONT_SIZE);
            initFontDefaults(uiDefaults, UIUtil.getFontWithFallback(uiSettings.getFontName(), Font.PLAIN, uiSettings.getFontSize()));
        }
        else {
            restoreOriginalFontDefaults(uiDefaults);
        }
    }

    private void restoreOriginalFontDefaults(UIDefaults defaults) {
        UIManager.LookAndFeelInfo lf = getCurrentLookAndFeel();
        HashMap<String, Object> lfDefaults = myStoredDefaults.get(lf);
        if (lfDefaults != null) {
            for (String resource : LafManagerImplUtil.ourPatchableFontResources) {
                defaults.put(resource, lfDefaults.get(resource));
            }
        }
    }

    private void storeOriginalFontDefaults(UIDefaults defaults) {
        UIManager.LookAndFeelInfo lf = getCurrentLookAndFeel();
        HashMap<String, Object> lfDefaults = myStoredDefaults.get(lf);
        if (lfDefaults == null) {
            lfDefaults = new HashMap<>();
            for (String resource : LafManagerImplUtil.ourPatchableFontResources) {
                lfDefaults.put(resource, defaults.get(resource));
            }
            myStoredDefaults.put(lf, lfDefaults);
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

    public static void initFontDefaults(@Nonnull UIDefaults defaults, @Nonnull FontUIResource uiFont) {
        LafManagerImplUtil.initFontDefaults(defaults, uiFont);
    }
}

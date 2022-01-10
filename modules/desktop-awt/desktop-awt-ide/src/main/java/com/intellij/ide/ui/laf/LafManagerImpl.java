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
package com.intellij.ide.ui.laf;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.ide.ui.laf.intellij.IntelliJLaf;
import com.intellij.ide.ui.laf.intellij.IntelliJLookAndFeelInfo;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.plaf.beg.BegMenuItemUI;
import com.intellij.ui.plaf.beg.IdeaMenuUI;
import com.intellij.ui.tree.ui.DefaultTreeUI;
import com.intellij.util.EventDispatcher;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.impl.ui.LookAndFeelProvider;
import consulo.desktop.ui.laf.LookAndFeelInfoWithClassLoader;
import consulo.desktop.util.awt.UIModificationTracker;
import consulo.desktop.util.awt.laf.GTKPlusUIUtil;
import consulo.disposer.Disposable;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.ide.ui.laf.GTKPlusEAPDescriptor;
import consulo.ide.ui.laf.LafWithColorScheme;
import consulo.ide.ui.laf.intellij.IntelliJEditorTabsUI;
import consulo.ide.ui.laf.mac.MacButtonlessScrollbarUI;
import consulo.localize.LocalizeManager;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.desktop.internal.style.DesktopStyleImpl;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageEffects;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.style.Style;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.JdkConstants;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthStyle;
import javax.swing.plaf.synth.SynthStyleFactory;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

/**
 * @author Eugene Belyaev
 * @author Vladimir Kondratyev
 */
@State(name = "LafManager", storages = @Storage(value = "laf.xml", roamingType = RoamingType.PER_PLATFORM))
@Singleton
public final class LafManagerImpl extends LafManager implements Disposable, PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(LafManagerImpl.class);

  private static final String ELEMENT_LAF = "laf";
  private static final String ELEMENT_LOCALE = "locale";
  private static final String ELEMENT_ICON = "icon";
  private static final String ATTRIBUTE_CLASS_NAME = "class-name";

  private static final String GNOME_THEME_PROPERTY_NAME = "gnome.Net/ThemeName";

  private static final String[] ourFileChooserTextKeys =
          {"FileChooser.viewMenuLabelText", "FileChooser.newFolderActionLabelText", "FileChooser.listViewActionLabelText", "FileChooser.detailsViewActionLabelText",
                  "FileChooser.refreshActionLabelText"};

  private static final String[] ourOptionPaneIconKeys = {"OptionPane.errorIcon", "OptionPane.informationIcon", "OptionPane.warningIcon", "OptionPane.questionIcon"};

  // A constant from Mac OS X implementation. See CPlatformWindow.WINDOW_ALPHA
  private static final String WINDOW_ALPHA = "Window.alpha";

  private final EventDispatcher<LafManagerListener> myListenerList = EventDispatcher.create(LafManagerListener.class);

  private final List<DesktopStyleImpl> myStyles;
  private DesktopStyleImpl myCurrentStyle;

  private PropertyChangeListener myThemeChangeListener = null;

  private final Map<UIManager.LookAndFeelInfo, HashMap<String, Object>> myStoredDefaults = new HashMap<>();
  private final LocalizeManager myLocalizeManager;
  private final IconLibraryManager myIconLibraryManager;

  @Inject
  LafManagerImpl() {
    myLocalizeManager = LocalizeManager.get();
    myIconLibraryManager = IconLibraryManager.get();

    List<UIManager.LookAndFeelInfo> lafList = new ArrayList<>();

    lafList.add(new IntelliJLookAndFeelInfo());
    lafList.add(new DarculaLookAndFeelInfo());

    if (SystemInfo.isLinux && EarlyAccessProgramManager.is(GTKPlusEAPDescriptor.class)) {
      lafList.add(new UIManager.LookAndFeelInfo("GTK+", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"));
    }

    for (LookAndFeelProvider provider : LookAndFeelProvider.EP_NAME.getExtensionList()) {
      provider.register(lafList::add);
    }

    myStyles = new ArrayList<>(lafList.size());

    for (UIManager.LookAndFeelInfo lookAndFeelInfo : lafList) {
      myStyles.add(new DesktopStyleImpl(lookAndFeelInfo));
    }

    myCurrentStyle = getDefaultStyle();
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
    if (myCurrentStyle != null) {
      final DesktopStyleImpl laf = findStyleByClassName(myCurrentStyle.getLookAndFeelInfo().getClassName());
      if (laf != null) {
        BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();
        boolean fromStyle = iconLibraryManager.isFromStyle();
        String activeLibraryId = iconLibraryManager.getActiveLibraryId(myCurrentStyle);
        setCurrentStyle(laf, false, false, fromStyle ? null : activeLibraryId);
      }
    }

    updateUI();

    if (SystemInfo.isXWindow) {
      myThemeChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
          //noinspection SSBasedInspection
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              fixGtkPopupStyle();
              patchOptionPaneIcons(UIManager.getLookAndFeelDefaults());
            }
          });
        }
      };
      Toolkit.getDefaultToolkit().addPropertyChangeListener(GNOME_THEME_PROPERTY_NAME, myThemeChangeListener);
    }

    // refresh UI on localize change
    LocalizeManager.get().addListener((oldLocale, newLocale) -> updateUI(), this);
  }

  @Override
  public void dispose() {
    if (myThemeChangeListener != null) {
      Toolkit.getDefaultToolkit().removePropertyChangeListener(GNOME_THEME_PROPERTY_NAME, myThemeChangeListener);
      myThemeChangeListener = null;
    }
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
    if(iconId != null) {
      if(myIconLibraryManager.getLibraries().containsKey(iconId)) {
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
      setCurrentStyle(styleFromXml, false, true, iconId);
      updateUI();
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

    if(!myIconLibraryManager.isFromStyle()){
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
    // Default
    DesktopStyleImpl ideaLaf = findStyleByClassName(IntelliJLaf.class.getName());
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
    DesktopStyleImpl style = (DesktopStyleImpl)currentStyle;
    setCurrentStyle(style, true, true, null);
  }

  private void setCurrentStyle(DesktopStyleImpl style, boolean wantChangeScheme, boolean fire, String iconLibraryId) {
    if (findStyleByClassName(style.getClassName()) == null) {
      LOG.error("unknown LookAndFeel : " + style);
      return;
    }

    UIManager.LookAndFeelInfo lookAndFeelInfo = style.getLookAndFeelInfo();

    try {
      JBColor.resetDark();

      UIModificationTracker.getInstance().incModificationCount();

      ClassLoader targetClassLoader = null;
      if (lookAndFeelInfo instanceof LookAndFeelInfoWithClassLoader) {
        targetClassLoader = ((LookAndFeelInfoWithClassLoader)lookAndFeelInfo).getClassLoader();
        UIManager.setLookAndFeel(newInstance((LookAndFeelInfoWithClassLoader)lookAndFeelInfo));
      }
      else {
        UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
      }

      myCurrentStyle = style;

      if (targetClassLoader != null) {
        final UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();

        uiDefaults.put("ClassLoader", targetClassLoader);
      }

      if (SystemInfo.isMacOSYosemite) {
        installMacOSXFonts(UIManager.getLookAndFeelDefaults());
      }

      if(iconLibraryId != null) {
        myIconLibraryManager.setActiveLibrary(iconLibraryId);
      }
      else {
        myIconLibraryManager.setActiveLibraryFromActiveStyle();
      }

      if (wantChangeScheme) {
        if (lookAndFeelInfo instanceof LafWithColorScheme) {
          EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
          EditorColorsScheme editorColorsScheme = editorColorsManager.getScheme(((LafWithColorScheme)lookAndFeelInfo).getColorSchemeName());
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
      Messages.showMessageDialog(IdeBundle.message("error.cannot.set.look.and.feel", lookAndFeelInfo.getName(), e.getMessage()), CommonBundle.getErrorTitle(), Messages.getErrorIcon());
    }
  }

  public static void installMacOSXFonts(UIDefaults defaults) {
    final String face = "Helvetica Neue";
    final FontUIResource uiFont = getFont(face, 13, Font.PLAIN);
    initFontDefaults(defaults, uiFont);
    for (Object key : new HashSet<>(defaults.keySet())) {
      if (!(key instanceof String)) continue;
      if (!StringUtil.endsWithIgnoreCase(((String)key), "font")) continue;
      Object value = defaults.get(key);
      if (value instanceof FontUIResource) {
        FontUIResource font = (FontUIResource)value;
        if (font.getFamily().equals("Lucida Grande") || font.getFamily().equals("Serif")) {
          if (!key.toString().contains("Menu")) {
            defaults.put(key, getFont(face, font.getSize(), font.getStyle()));
          }
        }
      }
    }

    FontUIResource uiFont11 = getFont(face, 11, Font.PLAIN);
    defaults.put("TableHeader.font", uiFont11);

    FontUIResource buttonFont = getFont("Helvetica Neue", 13, Font.PLAIN);
    defaults.put("Button.font", buttonFont);
    Font menuFont = getFont("Lucida Grande", 14, Font.PLAIN);
    defaults.put("Menu.font", menuFont);
    defaults.put("MenuItem.font", menuFont);
    defaults.put("MenuItem.acceleratorFont", menuFont);
    defaults.put("PasswordField.font", defaults.getFont("TextField.font"));
  }

  @Nonnull
  private static FontUIResource getFont(String yosemite, int size, @JdkConstants.FontStyle int style) {
    if (SystemInfo.isMacOSElCapitan) {
      // Text family should be used for relatively small sizes (<20pt), don't change to Display
      // see more about SF https://medium.com/@mach/the-secret-of-san-francisco-fonts-4b5295d9a745#.2ndr50z2v
      Font font = new Font(SystemInfo.isMacOSCatalina ? ".AppleSystemUIFont" : ".SF NS Text", style, size);
      if (!UIUtil.isDialogFont(font)) {
        return new FontUIResource(font);
      }
    }
    return new FontUIResource(yosemite, style, size);
  }

  @Nonnull
  private LookAndFeel newInstance(LookAndFeelInfoWithClassLoader lookAndFeel) throws Exception {
    ClassLoader classLoader = lookAndFeel.getClassLoader();

    Class<?> clazz = Class.forName(lookAndFeel.getClassName(), true, classLoader);

    return (LookAndFeel)ReflectionUtil.newInstance(clazz);
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
        ((IdeFrameEx)frame).updateView();
      }
    }
    ActionToolbarImpl.updateAllToolbarsImmediately();
  }

  /**
   * Updates LAF of all windows. The method also updates font of components
   * as it's configured in <code>UISettings</code>.
   */
  @Override
  public void updateUI() {
    final UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();

    GTKPlusUIUtil.updateUI();

    fixPopupWeight();

    fixGtkPopupStyle();

    if (UIUtil.isUnderAquaLookAndFeel()) {
      uiDefaults.put("Panel.opaque", Boolean.TRUE);
      uiDefaults.put("ScrollBarUI", MacButtonlessScrollbarUI.class.getName());
      uiDefaults.put("JBEditorTabsUI", IntelliJEditorTabsUI.class.getName());
      uiDefaults.put("MenuItemUI", BegMenuItemUI.class.getName());
      uiDefaults.put("CheckBoxMenuItemUI", BegMenuItemUI.class.getName());
      uiDefaults.put("MenuUI", IdeaMenuUI.class.getName());
    }
    else if (UIUtil.isWinLafOnVista()) {
      uiDefaults.put("ComboBox.border", null);
    }

    initInputMapDefaults(uiDefaults);

    uiDefaults.put("Button.defaultButtonFollowsFocus", Boolean.FALSE);

    patchFileChooserStrings(uiDefaults);

    patchListUI(uiDefaults);
    
    patchTreeUI(uiDefaults);

    patchLafFonts(uiDefaults);

    patchHiDPI(uiDefaults);

    patchOptionPaneIcons(uiDefaults);

    fixSeparatorColor(uiDefaults);
                                                      
    fixMenuIssues(uiDefaults);

    LafManagerImplUtil.insertCustomComponentUI(uiDefaults);

    updateToolWindows();
    for (Frame frame : Frame.getFrames()) {
      updateUI(frame);
    }
    fireLookAndFeelChanged();
  }

  private static void patchTreeUI(UIDefaults defaults) {
    patchBorder(defaults, "Tree.border");
    defaults.put("TreeUI", DefaultTreeUI.class.getName());
    defaults.put("Tree.repaintWholeRow", true);
    //if (isUnsupported(defaults.getIcon("Tree.collapsedIcon"))) {
    //  defaults.put("Tree.collapsedIcon", LafIconLookup.getIcon("treeCollapsed"));
    //  defaults.put("Tree.collapsedSelectedIcon", LafIconLookup.getSelectedIcon("treeCollapsed"));
    //}
    //if (isUnsupported(defaults.getIcon("Tree.expandedIcon"))) {
    //  defaults.put("Tree.expandedIcon", LafIconLookup.getIcon("treeExpanded"));
    //  defaults.put("Tree.expandedSelectedIcon", LafIconLookup.getSelectedIcon("treeExpanded"));
    //}
  }

  private static void patchListUI(UIDefaults defaults) {
    patchBorder(defaults, "List.border");
  }

  private static void patchBorder(UIDefaults defaults, String key) {
    if (defaults.getBorder(key) == null) {
      defaults.put(key, JBUI.Borders.empty(0, 0).asUIResource());
    }
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

  private static void fixSeparatorColor(UIDefaults uiDefaults) {
    if (UIUtil.isUnderAquaLookAndFeel()) {
      uiDefaults.put("Separator.background", UIUtil.AQUA_SEPARATOR_BACKGROUND_COLOR);
      uiDefaults.put("Separator.foreground", UIUtil.AQUA_SEPARATOR_FOREGROUND_COLOR);
    }
  }

  private static void fixMenuIssues(@Nonnull UIDefaults uiDefaults) {
    uiDefaults.put("Menu.arrowIcon", new DefaultMenuArrowIcon());
    uiDefaults.put("MenuItem.background", UIManager.getColor("Menu.background"));
  }

  private static final class DefaultMenuArrowIcon extends MenuArrowIcon {
    private DefaultMenuArrowIcon() {
      super(() -> PlatformIconGroup.ideMenuArrow(), () -> PlatformIconGroup.ideMenuArrowSelected(), () -> ImageEffects.grayed(PlatformIconGroup.ideMenuArrow()));
    }
  }

  /**
   * The following code is a trick! By default Swing uses lightweight and "medium" weight
   * popups to show JPopupMenu. The code below force the creation of real heavyweight menus -
   * this increases speed of popups and allows to get rid of some drawing artifacts.
   */
  private static void fixPopupWeight() {
    int popupWeight = OurPopupFactory.WEIGHT_MEDIUM;
    String property = System.getProperty("idea.popup.weight");
    if (property != null) property = property.toLowerCase().trim();
    if (SystemInfo.isMacOSLeopard) {
      // force heavy weight popups under Leopard, otherwise they don't have shadow or any kind of border.
      popupWeight = OurPopupFactory.WEIGHT_HEAVY;
    }
    else if (property == null) {
      // use defaults if popup weight isn't specified
      if (SystemInfo.isWindows) {
        popupWeight = OurPopupFactory.WEIGHT_HEAVY;
      }
    }
    else {
      if ("light".equals(property)) {
        popupWeight = OurPopupFactory.WEIGHT_LIGHT;
      }
      else if ("medium".equals(property)) {
        popupWeight = OurPopupFactory.WEIGHT_MEDIUM;
      }
      else if ("heavy".equals(property)) {
        popupWeight = OurPopupFactory.WEIGHT_HEAVY;
      }
      else {
        LOG.error("Illegal value of property \"idea.popup.weight\": " + property);
      }
    }

    PopupFactory factory = PopupFactory.getSharedInstance();
    if (!(factory instanceof OurPopupFactory)) {
      factory = new OurPopupFactory(factory);
      PopupFactory.setSharedInstance(factory);
    }
    PopupUtil.setPopupType(factory, popupWeight);
  }

  private static void fixGtkPopupStyle() {
    if (!UIUtil.isUnderGTKLookAndFeel()) return;

    // it must be instance of com.sun.java.swing.plaf.gtk.GTKStyleFactory, but class package-local
    if (SystemInfo.isJavaVersionAtLeast(10, 0, 0)) {
      return;
    }

    final SynthStyleFactory original = SynthLookAndFeel.getStyleFactory();

    SynthLookAndFeel.setStyleFactory(new SynthStyleFactory() {
      @Override
      public SynthStyle getStyle(final JComponent c, final Region id) {
        final SynthStyle style = original.getStyle(c, id);
        if (id == Region.POPUP_MENU) {
          final Integer x = ReflectionUtil.getField(style.getClass(), style, int.class, "xThickness");
          if (x != null && x == 0) {
            // workaround for Sun bug #6636964
            ReflectionUtil.setField(style.getClass(), style, int.class, "xThickness", 1);
            ReflectionUtil.setField(style.getClass(), style, int.class, "yThickness", 3);
          }
        }
        return style;
      }
    });

    new JBPopupMenu();  // invokes updateUI() -> updateStyle()

    SynthLookAndFeel.setStyleFactory(original);
  }

  private static void patchHiDPI(UIDefaults defaults) {
    Object prevScaleVal = defaults.get("hidpi.scaleFactor");
    // used to normalize previously patched values
    float prevScale = prevScaleVal != null ? (Float)prevScaleVal : 1f;

    if (prevScale == JBUI.scale(1f) && prevScaleVal != null) return;

    List<String> myIntKeys = Arrays.asList("Tree.leftChildIndent", "Tree.rightChildIndent", "Tree.rowHeight");

    List<String> myDimensionKeys = Arrays.asList("Slider.horizontalSize", "Slider.verticalSize", "Slider.minimumHorizontalSize", "Slider.minimumVerticalSize");

    for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
      Object value = entry.getValue();
      String key = entry.getKey().toString();
      if (value instanceof Dimension) {
        if (value instanceof UIResource || myDimensionKeys.contains(key)) {
          entry.setValue(JBUI.size((Dimension)value).asUIResource());
        }
      }
      else if (value instanceof Insets) {
        if (value instanceof UIResource) {
          entry.setValue(JBUI.insets(((Insets)value)).asUIResource());
        }
      }
      else if (value instanceof Integer) {
        // FIXME [VISTALL] we already fix maxGutterIconWidth with UI classes
        if (/*key.endsWith(".maxGutterIconWidth") || */myIntKeys.contains(key)) {
          int normValue = (int)((Integer)value / prevScale);
          entry.setValue(Integer.valueOf(JBUI.scale(normValue)));
        }
      }
    }
    defaults.put("hidpi.scaleFactor", JBUI.scale(1f));
  }

  private static void patchFileChooserStrings(final UIDefaults defaults) {
    if (!defaults.containsKey(ourFileChooserTextKeys[0])) {
      // Alloy L&F does not define strings for names of context menu actions, so we have to patch them in here
      for (String key : ourFileChooserTextKeys) {
        defaults.put(key, IdeBundle.message(key));
      }
    }
  }

  private static void patchOptionPaneIcons(final UIDefaults defaults) {
    if (UIUtil.isUnderGTKLookAndFeel() && defaults.get(ourOptionPaneIconKeys[0]) == null) {
      // GTK+ L&F keeps icons hidden in style
      final SynthStyle style = SynthLookAndFeel.getStyle(new JOptionPane(""), Region.DESKTOP_ICON);
      if (style != null) {
        for (final String key : ourOptionPaneIconKeys) {
          final Object icon = style.get(null, key);
          if (icon != null) defaults.put(key, icon);
        }
      }
    }
  }

  private void patchLafFonts(UIDefaults uiDefaults) {
    //if (JBUI.isHiDPI()) {
    //  HashMap<Object, Font> newFonts = new HashMap<Object, Font>();
    //  for (Object key : uiDefaults.keySet().toArray()) {
    //    Object val = uiDefaults.get(key);
    //    if (val instanceof Font) {
    //      newFonts.put(key, JBFont.create((Font)val));
    //    }
    //  }
    //  for (Map.Entry<Object, Font> entry : newFonts.entrySet()) {
    //    uiDefaults.put(entry.getKey(), entry.getValue());
    //  }
    //} else
    UISettings uiSettings = UISettings.getInstance();
    if (uiSettings.OVERRIDE_NONIDEA_LAF_FONTS) {
      storeOriginalFontDefaults(uiDefaults);
      JBUI.setUserScaleFactor(uiSettings.FONT_SIZE / UIUtil.DEF_SYSTEM_FONT_SIZE);
      initFontDefaults(uiDefaults, UIUtil.getFontWithFallback(uiSettings.FONT_FACE, Font.PLAIN, uiSettings.getFontSize()));
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

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static void initInputMapDefaults(UIDefaults defaults) {
    LafManagerImplUtil.initInputMapDefaults(defaults);
  }

  public static void initFontDefaults(@Nonnull UIDefaults defaults, @Nonnull FontUIResource uiFont) {
    LafManagerImplUtil.initFontDefaults(defaults, uiFont);
  }

  private static class OurPopupFactory extends PopupFactory {
    public static final int WEIGHT_LIGHT = 0;
    public static final int WEIGHT_MEDIUM = 1;
    public static final int WEIGHT_HEAVY = 2;

    private final PopupFactory myDelegate;

    public OurPopupFactory(final PopupFactory delegate) {
      myDelegate = delegate;
    }

    @Override
    public Popup getPopup(final Component owner, final Component contents, final int x, final int y) throws IllegalArgumentException {
      final Point point = fixPopupLocation(contents, x, y);

      final int popupType = PopupUtil.getPopupType(this);
      if (popupType >= 0) {
        PopupUtil.setPopupType(myDelegate, popupType);
      }

      Popup popup = myDelegate.getPopup(owner, contents, point.x, point.y);
      Window window = ComponentUtil.getWindow(contents);
      String cleanupKey = "LafManagerImpl.rootPaneCleanup";
      boolean isHeavyWeightPopup = window instanceof RootPaneContainer && window != ComponentUtil.getWindow(owner);
      if (isHeavyWeightPopup) {
        UIUtil.markAsTypeAheadAware(window);
        window.setMinimumSize(null); // clear min-size from prev invocations on JBR11
      }
      if (isHeavyWeightPopup && ((RootPaneContainer)window).getRootPane().getClientProperty(cleanupKey) == null) {
        final JRootPane rootPane = ((RootPaneContainer)window).getRootPane();
        rootPane.setGlassPane(new IdeGlassPaneImpl(rootPane, false));
        rootPane.putClientProperty(WINDOW_ALPHA, 1.0f);
        rootPane.putClientProperty(cleanupKey, cleanupKey);
        window.addWindowListener(new WindowAdapter() {
          @Override
          public void windowOpened(WindowEvent e) {
            // cleanup will be handled by AbstractPopup wrapper
            if (PopupUtil.getPopupContainerFor(rootPane) != null) {
              window.removeWindowListener(this);
              rootPane.putClientProperty(cleanupKey, null);
            }
          }

          @Override
          public void windowClosed(WindowEvent e) {
            window.removeWindowListener(this);
            rootPane.putClientProperty(cleanupKey, null);
            DialogWrapper.cleanupRootPane(rootPane);
            DialogWrapper.cleanupWindowListeners(window);
          }
        });
        
        //if (IdeaPopupMenuUI.isUnderPopup(contents)/* && IdeaPopupMenuUI.isRoundBorder()*/) {
        //  window.setBackground(Gray.TRANSPARENT);
        //  window.setOpacity(1);
        //}
      }
      return popup;
    }

    private static Point fixPopupLocation(final Component contents, final int x, final int y) {
      if (!(contents instanceof JToolTip)) return new Point(x, y);

      final PointerInfo info;
      try {
        info = MouseInfo.getPointerInfo();
      }
      catch (InternalError e) {
        // http://www.jetbrains.net/jira/browse/IDEADEV-21390
        // may happen under Mac OSX 10.5
        return new Point(x, y);
      }
      int deltaY = 0;

      if (info != null) {
        final Point mouse = info.getLocation();
        deltaY = mouse.y - y;
      }

      final Dimension size = contents.getPreferredSize();
      final Rectangle rec = new Rectangle(new Point(x, y), size);
      ScreenUtil.moveRectangleToFitTheScreen(rec);

      if (rec.y < y) {
        rec.y += deltaY;
      }

      return rec.getLocation();
    }
  }
}

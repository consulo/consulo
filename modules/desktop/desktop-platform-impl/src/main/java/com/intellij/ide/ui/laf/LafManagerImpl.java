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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.plaf.beg.BegMenuItemUI;
import com.intellij.ui.plaf.beg.IdeaMenuUI;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.ui.laf.LookAndFeelInfoWithClassLoader;
import consulo.desktop.util.awt.UIModificationTracker;
import consulo.desktop.util.awt.laf.GTKPlusUIUtil;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.ide.ui.laf.GTKPlusEAPDescriptor;
import consulo.ide.ui.laf.MacDefaultLookAndFeelInfo;
import consulo.ide.ui.laf.mac.MacButtonlessScrollbarUI;
import consulo.ide.ui.laf.mac.MacEditorTabsUI;
import consulo.ide.ui.laf.modernDark.ModernDarkLookAndFeelInfo;
import consulo.ide.ui.laf.modernWhite.ModernWhiteLookAndFeelInfo;
import consulo.ide.ui.laf.modernWhite.NativeModernWhiteLookAndFeelInfo;
import consulo.logging.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthStyle;
import javax.swing.plaf.synth.SynthStyleFactory;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Belyaev
 * @author Vladimir Kondratyev
 */
@State(name = "LafManager", storages = @Storage(value = "laf.xml", roamingType = RoamingType.PER_PLATFORM))
@Singleton
public final class LafManagerImpl extends LafManager implements Disposable, PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(LafManagerImpl.class);

  @NonNls
  private static final String ELEMENT_LAF = "laf";
  @NonNls
  private static final String ATTRIBUTE_CLASS_NAME = "class-name";
  @NonNls
  private static final String GNOME_THEME_PROPERTY_NAME = "gnome.Net/ThemeName";

  @NonNls
  private static final String[] ourFileChooserTextKeys =
          {"FileChooser.viewMenuLabelText", "FileChooser.newFolderActionLabelText", "FileChooser.listViewActionLabelText", "FileChooser.detailsViewActionLabelText",
                  "FileChooser.refreshActionLabelText"};

  @NonNls
  private static final String[] ourOptionPaneIconKeys = {"OptionPane.errorIcon", "OptionPane.informationIcon", "OptionPane.warningIcon", "OptionPane.questionIcon"};

  private final EventListenerList myListenerList;
  private final UIManager.LookAndFeelInfo[] myLaFs;
  private UIManager.LookAndFeelInfo myCurrentLaf;
  private final HashMap<UIManager.LookAndFeelInfo, HashMap<String, Object>> myStoredDefaults = new HashMap<>();
  private PropertyChangeListener myThemeChangeListener = null;

  @Inject
  LafManagerImpl() {
    myListenerList = new EventListenerList();

    List<UIManager.LookAndFeelInfo> lafList = ContainerUtil.newArrayList();

    if (SystemInfo.isMac) {
      lafList.add(new MacDefaultLookAndFeelInfo("Default", UIManager.getSystemLookAndFeelClassName()));
    }

    lafList.add(new IntelliJLookAndFeelInfo());

    if (!SystemInfo.isMac) {
      if (SystemInfo.isWin8OrNewer) {
        lafList.add(new NativeModernWhiteLookAndFeelInfo());
      }
      lafList.add(new ModernWhiteLookAndFeelInfo());
      lafList.add(new ModernDarkLookAndFeelInfo());
    }
    lafList.add(new DarculaLookAndFeelInfo());

    if (SystemInfo.isLinux && EarlyAccessProgramManager.is(GTKPlusEAPDescriptor.class)) {
      lafList.add(new UIManager.LookAndFeelInfo("GTK+", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"));
    }

    myLaFs = lafList.toArray(new UIManager.LookAndFeelInfo[lafList.size()]);

    myCurrentLaf = getDefaultLaf();
  }

  /**
   * Adds specified listener
   */
  @Override
  public void addLafManagerListener(@Nonnull final LafManagerListener l) {
    myListenerList.add(LafManagerListener.class, l);
  }

  /**
   * Removes specified listener
   */
  @Override
  public void removeLafManagerListener(@Nonnull final LafManagerListener l) {
    myListenerList.remove(LafManagerListener.class, l);
  }

  private void fireLookAndFeelChanged() {
    LafManagerListener[] listeners = myListenerList.getListeners(LafManagerListener.class);
    for (LafManagerListener listener : listeners) {
      listener.lookAndFeelChanged(this);
    }
  }

  @Override
  public void afterLoadState() {
    if (myCurrentLaf != null) {
      final UIManager.LookAndFeelInfo laf = findLaf(myCurrentLaf.getClassName());
      if (laf != null) {
        setCurrentLookAndFeel(laf, false);
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
    for (final Object o : element.getChildren()) {
      Element child = (Element)o;
      if (ELEMENT_LAF.equals(child.getName())) {
        className = child.getAttributeValue(ATTRIBUTE_CLASS_NAME);
        break;
      }
    }

    UIManager.LookAndFeelInfo laf = findLaf(className);
    // If LAF is undefined (wrong class name or something else) we have set default LAF anyway.
    if (laf == null) {
      laf = getDefaultLaf();
    }

    if (myCurrentLaf != null && !laf.getClassName().equals(myCurrentLaf.getClassName())) {
      setCurrentLookAndFeel(laf);
      updateUI();
    }

    myCurrentLaf = laf;
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    if (myCurrentLaf != null) {
      String className = myCurrentLaf.getClassName();
      if (className != null) {
        Element child = new Element(ELEMENT_LAF);
        child.setAttribute(ATTRIBUTE_CLASS_NAME, className);
        element.addContent(child);
      }
    }
    return element;
  }

  @Override
  public UIManager.LookAndFeelInfo[] getInstalledLookAndFeels() {
    return myLaFs.clone();
  }

  @Override
  public UIManager.LookAndFeelInfo getCurrentLookAndFeel() {
    return myCurrentLaf;
  }

  /**
   * @return default LookAndFeelInfo for the running OS. For Win32 and
   * Linux the method returns Alloy LAF or IDEA LAF if first not found, for Mac OS X it returns Aqua
   * RubyMine uses Native L&F for linux as well
   */
  private UIManager.LookAndFeelInfo getDefaultLaf() {
    final String systemLafClassName = UIManager.getSystemLookAndFeelClassName();
    if (SystemInfo.isMac) {
      UIManager.LookAndFeelInfo laf = findLaf(systemLafClassName);
      LOG.assertTrue(laf != null);
      return laf;
    }
    // Default
    UIManager.LookAndFeelInfo ideaLaf = findLaf(IntelliJLaf.class.getName());
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
  private UIManager.LookAndFeelInfo findLaf(@Nullable String className) {
    if (className == null) {
      return null;
    }
    for (UIManager.LookAndFeelInfo laf : myLaFs) {
      if (Comparing.equal(laf.getClassName(), className)) {
        return laf;
      }
    }
    return null;
  }

  @Override
  public void setCurrentLookAndFeel(UIManager.LookAndFeelInfo lookAndFeelInfo) {
    setCurrentLookAndFeel(lookAndFeelInfo, true);
  }

  private void setCurrentLookAndFeel(UIManager.LookAndFeelInfo lookAndFeelInfo, boolean fire) {
    if (findLaf(lookAndFeelInfo.getClassName()) == null) {
      LOG.error("unknown LookAndFeel : " + lookAndFeelInfo);
      return;
    }
    try {
      JBColor.resetDark();

      IconLoader.resetDark();

      UIModificationTracker.getInstance().incModificationCount();

      ClassLoader targetClassLoader = null;
      if (lookAndFeelInfo instanceof LookAndFeelInfoWithClassLoader) {
        targetClassLoader = ((LookAndFeelInfoWithClassLoader)lookAndFeelInfo).getClassLoader();
        UIManager.setLookAndFeel(newInstance((LookAndFeelInfoWithClassLoader)lookAndFeelInfo));
      }
      else {
        UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
      }

      if (targetClassLoader != null) {
        final UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();

        uiDefaults.put("ClassLoader", targetClassLoader);
      }

      if (fire) {
        fireUpdate();
      }
    }
    catch (Exception e) {
      LOG.error(e);
      Messages.showMessageDialog(IdeBundle.message("error.cannot.set.look.and.feel", lookAndFeelInfo.getName(), e.getMessage()), CommonBundle.getErrorTitle(), Messages.getErrorIcon());
      return;
    }
    myCurrentLaf = lookAndFeelInfo;
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
      uiDefaults.put("JBEditorTabsUI", MacEditorTabsUI.class.getName());
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

    patchLafFonts(uiDefaults);

    patchHiDPI(uiDefaults);

    patchOptionPaneIcons(uiDefaults);

    fixSeparatorColor(uiDefaults);

    LafManagerImplUtil.insertCustomComponentUI(uiDefaults);

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

  private static void fixSeparatorColor(UIDefaults uiDefaults) {
    if (UIUtil.isUnderAquaLookAndFeel()) {
      uiDefaults.put("Separator.background", UIUtil.AQUA_SEPARATOR_BACKGROUND_COLOR);
      uiDefaults.put("Separator.foreground", UIUtil.AQUA_SEPARATOR_FOREGROUND_COLOR);
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
      initFontDefaults(uiDefaults, uiSettings.FONT_SIZE, new FontUIResource(uiSettings.FONT_FACE, Font.PLAIN, uiSettings.FONT_SIZE));
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

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static void initFontDefaults(UIDefaults defaults, int fontSize, FontUIResource uiFont) {
    LafManagerImplUtil.initFontDefaults(defaults, fontSize, uiFont);
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

      final int popupType = UIUtil.isUnderGTKLookAndFeel() ? WEIGHT_HEAVY : PopupUtil.getPopupType(this);
      if (popupType >= 0) {
        PopupUtil.setPopupType(myDelegate, popupType);
      }

      final Popup popup = myDelegate.getPopup(owner, contents, point.x, point.y);
      fixPopupSize(popup, contents);
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

    private static void fixPopupSize(final Popup popup, final Component contents) {
      if (!UIUtil.isUnderGTKLookAndFeel() || !(contents instanceof JPopupMenu)) return;

      for (Class<?> aClass = popup.getClass(); aClass != null && Popup.class.isAssignableFrom(aClass); aClass = aClass.getSuperclass()) {
        try {
          final Method getComponent = aClass.getDeclaredMethod("getComponent");
          getComponent.setAccessible(true);
          final Object component = getComponent.invoke(popup);
          if (component instanceof JWindow) {
            ((JWindow)component).setSize(new Dimension(0, 0));
          }
          break;
        }
        catch (Exception ignored) {
        }
      }
    }
  }
}

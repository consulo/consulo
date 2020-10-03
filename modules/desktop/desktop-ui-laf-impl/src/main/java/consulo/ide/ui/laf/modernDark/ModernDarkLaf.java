/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.ui.laf.modernDark;

import com.intellij.ide.ui.laf.DarculaMetalTheme;
import com.intellij.ide.ui.laf.LafManagerImplUtil;
import com.intellij.ide.ui.laf.darcula.DarculaLaf;
import com.intellij.ide.ui.laf.ideaOld.IntelliJMetalLookAndFeel;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.util.awt.laf.BuildInLookAndFeel;
import consulo.ide.ui.laf.BaseLookAndFeel;
import consulo.ui.desktop.laf.extend.textBox.SupportTextBoxWithExpandActionExtender;
import consulo.ui.desktop.laf.extend.textBox.SupportTextBoxWithExtensionsExtender;
import sun.awt.AppContext;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author VISTALL
 * @since 02.03.14
 */
public class ModernDarkLaf extends BaseLookAndFeel implements BuildInLookAndFeel {
  BasicLookAndFeel base;

  public ModernDarkLaf() {
    try {
      if (SystemInfo.isWindows || SystemInfo.isLinux) {
        base = new IntelliJMetalLookAndFeel();
      }
      else {
        final String name = UIManager.getSystemLookAndFeelClassName();
        base = (BasicLookAndFeel)Class.forName(name).newInstance();
      }
    }
    catch (Exception e) {
      log(e);
    }
  }

  private void callInit(String method, UIDefaults defaults) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod(method, UIDefaults.class);
      superMethod.setAccessible(true);
      superMethod.invoke(base, defaults);
    }
    catch (Exception e) {
      log(e);
    }
  }

  @SuppressWarnings("UnusedParameters")
  private static void log(Exception e) {
//    everything is gonna be alright
    e.printStackTrace();
  }

  @Nonnull
  @Override
  public UIDefaults getDefaultsImpl(UIDefaults superDefaults) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("getDefaults");
      superMethod.setAccessible(true);
      final UIDefaults metalDefaults = (UIDefaults)superMethod.invoke(new MetalLookAndFeel());
      final UIDefaults defaults = (UIDefaults)superMethod.invoke(base);
      if (SystemInfo.isLinux && !Registry.is("darcula.use.native.fonts.on.linux")) {
        Font font = findFont("DejaVu Sans");
        if (font != null) {
          for (Object key : defaults.keySet()) {
            if (key instanceof String && ((String)key).endsWith(".font")) {
              defaults.put(key, new FontUIResource(font.deriveFont(13f)));
            }
          }
        }
      }

      LafManagerImplUtil.initInputMapDefaults(defaults);
      defaults.put(SupportTextBoxWithExpandActionExtender.class, SupportTextBoxWithExpandActionExtender.INSTANCE);
      defaults.put(SupportTextBoxWithExtensionsExtender.class, SupportTextBoxWithExtensionsExtender.INSTANCE);

      initIdeaDefaults(defaults);
      patchStyledEditorKit(defaults);
      patchComboBox(metalDefaults, defaults);
      defaults.remove("Spinner.arrowButtonBorder");
      defaults.put("Spinner.arrowButtonSize", new Dimension(16, 5));

      MetalLookAndFeel.setCurrentTheme(createMetalTheme());

      defaults.put("EditorPane.font", defaults.getFont("TextField.font"));
      return defaults;
    }
    catch (Exception e) {
      log(e);
    }
    return super.getDefaultsImpl(superDefaults);
  }

  protected DefaultMetalTheme createMetalTheme() {
    return new DarculaMetalTheme();
  }

  private static Font findFont(String name) {
    for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
      if (font.getName().equals(name)) {
        return font;
      }
    }
    return null;
  }

  private static void patchComboBox(UIDefaults metalDefaults, UIDefaults defaults) {
    defaults.remove("ComboBox.ancestorInputMap");
    defaults.remove("ComboBox.actionMap");
    defaults.put("ComboBox.ancestorInputMap", metalDefaults.get("ComboBox.ancestorInputMap"));
    defaults.put("ComboBox.actionMap", metalDefaults.get("ComboBox.actionMap"));
  }

  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  private void patchStyledEditorKit(UIDefaults defaults) {
    URL url = getClass().getResource(getPrefix() + (JBUI.isHiDPI() ? "@2x.css" : ".css"));
    StyleSheet styleSheet = UIUtil.loadStyleSheet(url);
    defaults.put("StyledEditorKit.JBDefaultStyle", styleSheet);
    try {
      Field keyField = HTMLEditorKit.class.getDeclaredField("DEFAULT_STYLES_KEY");
      keyField.setAccessible(true);
      AppContext.getAppContext().put(keyField.get(null), UIUtil.loadStyleSheet(url));
    }
    catch (Exception e) {
      log(e);
    }
  }

  protected String getPrefix() {
    return "modernDark";
  }

  private void call(String method) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod(method);
      superMethod.setAccessible(true);
      superMethod.invoke(base);
    }
    catch (Exception ignore) {
      log(ignore);
    }
  }

  public void initComponentDefaults(UIDefaults defaults) {
    callInit("initComponentDefaults", defaults);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  protected void initIdeaDefaults(UIDefaults defaults) {
    loadDefaults(defaults);
    defaults.put("Table.ancestorInputMap", new UIDefaults.LazyInputMap(
            new Object[]{"ctrl C", "copy", "ctrl V", "paste", "ctrl X", "cut", "COPY", "copy", "PASTE", "paste", "CUT", "cut", "control INSERT", "copy", "shift INSERT", "paste", "shift DELETE", "cut",
                    "RIGHT", "selectNextColumn", "KP_RIGHT", "selectNextColumn", "LEFT", "selectPreviousColumn", "KP_LEFT", "selectPreviousColumn", "DOWN", "selectNextRow", "KP_DOWN", "selectNextRow",
                    "UP", "selectPreviousRow", "KP_UP", "selectPreviousRow", "shift RIGHT", "selectNextColumnExtendSelection", "shift KP_RIGHT", "selectNextColumnExtendSelection", "shift LEFT",
                    "selectPreviousColumnExtendSelection", "shift KP_LEFT", "selectPreviousColumnExtendSelection", "shift DOWN", "selectNextRowExtendSelection", "shift KP_DOWN",
                    "selectNextRowExtendSelection", "shift UP", "selectPreviousRowExtendSelection", "shift KP_UP", "selectPreviousRowExtendSelection", "PAGE_UP", "scrollUpChangeSelection",
                    "PAGE_DOWN", "scrollDownChangeSelection", "HOME", "selectFirstColumn", "END", "selectLastColumn", "shift PAGE_UP", "scrollUpExtendSelection", "shift PAGE_DOWN",
                    "scrollDownExtendSelection", "shift HOME", "selectFirstColumnExtendSelection", "shift END", "selectLastColumnExtendSelection", "ctrl PAGE_UP", "scrollLeftChangeSelection",
                    "ctrl PAGE_DOWN", "scrollRightChangeSelection", "ctrl HOME", "selectFirstRow", "ctrl END", "selectLastRow", "ctrl shift PAGE_UP", "scrollRightExtendSelection",
                    "ctrl shift PAGE_DOWN", "scrollLeftExtendSelection", "ctrl shift HOME", "selectFirstRowExtendSelection", "ctrl shift END", "selectLastRowExtendSelection", "TAB",
                    "selectNextColumnCell", "shift TAB", "selectPreviousColumnCell",
                    //"ENTER", "selectNextRowCell",
                    "shift ENTER", "selectPreviousRowCell", "ctrl A", "selectAll",
                    //"ESCAPE", "cancel",
                    "F2", "startEditing"}));
  }

  @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
  protected void loadDefaults(UIDefaults defaults) {
    final Properties properties = new Properties();
    final String osSuffix = SystemInfo.isMac ? "mac" : SystemInfo.isWindows ? "windows" : "linux";
    try {
      InputStream stream = getClass().getResourceAsStream(getPrefix() + ".properties");
      properties.load(stream);
      stream.close();

      stream = getClass().getResourceAsStream(getPrefix() + "_" + osSuffix + ".properties");
      properties.load(stream);
      stream.close();

      HashMap<String, Object> darculaGlobalSettings = new HashMap<>();
      final String prefix = getPrefix() + ".";
      for (String key : properties.stringPropertyNames()) {
        if (key.startsWith(prefix)) {
          darculaGlobalSettings.put(key.substring(prefix.length()), DarculaLaf.parseValue(key, properties.getProperty(key)));
        }
      }

      for (Object key : defaults.keySet()) {
        if (key instanceof String && ((String)key).contains(".")) {
          final String s = (String)key;
          final String darculaKey = s.substring(s.lastIndexOf('.') + 1);
          if (darculaGlobalSettings.containsKey(darculaKey)) {
            defaults.put(key, darculaGlobalSettings.get(darculaKey));
          }
        }
      }

      for (String key : properties.stringPropertyNames()) {
        final String value = properties.getProperty(key);
        defaults.put(key, DarculaLaf.parseValue(key, value));
      }
    }
    catch (IOException e) {
      log(e);
    }
  }

  @Override
  public String getName() {
    return "Modern Dark";
  }

  @Override
  public String getID() {
    return "modern-dark";
  }

  @Override
  public String getDescription() {
    return "Modern Dark Look and Feel";
  }

  @Override
  public boolean isNativeLookAndFeel() {
    return true;
  }

  @Override
  public boolean isSupportedLookAndFeel() {
    return true;
  }

  @Override
  protected void initSystemColorDefaults(UIDefaults defaults) {
    callInit("initSystemColorDefaults", defaults);
  }

  @Override
  protected void initClassDefaults(UIDefaults defaults) {
    callInit("initClassDefaults", defaults);
  }

  @Override
  public void initialize() {
    call("initialize");
  }

  @Override
  public void uninitialize() {
    call("uninitialize");
  }

  @Override
  protected void loadSystemColors(UIDefaults defaults, String[] systemColors, boolean useNative) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("loadSystemColors", UIDefaults.class, String[].class, boolean.class);
      superMethod.setAccessible(true);
      superMethod.invoke(base, defaults, systemColors, useNative);
    }
    catch (Exception ignore) {
      log(ignore);
    }
  }

  @Override
  public boolean getSupportsWindowDecorations() {
    return true;
  }

  @Override
  public boolean isDark() {
    return true;
  }
}

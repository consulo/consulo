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
package consulo.desktop.awt.ui.plaf.darcula;

import com.formdev.flatlaf.ui.FlatNativeWindowBorder;
import com.formdev.flatlaf.util.SystemInfo;
import consulo.application.util.registry.Registry;
import consulo.awt.hacking.AppContextHacking;
import consulo.awt.hacking.BasicLookAndFeelHacking;
import consulo.awt.hacking.HTMLEditorKitHacking;
import consulo.desktop.awt.ui.plaf.BaseLookAndFeel;
import consulo.desktop.awt.ui.plaf.DarculaMetalTheme;
import consulo.desktop.awt.ui.plaf.LafManagerImplUtil;
import consulo.desktop.awt.ui.plaf.extend.textBox.SupportTextBoxWithExpandActionExtender;
import consulo.desktop.awt.ui.plaf.extend.textBox.SupportTextBoxWithExtensionsExtender;
import consulo.desktop.awt.ui.plaf.ideaOld.IntelliJMetalLookAndFeel;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.List;
import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaLaf extends BaseLookAndFeel {
    public static final String NAME = "Darcula";
    BasicLookAndFeel base;

    public DarculaLaf() {
        try {
            if (Platform.current().os().isMac()) {
                final String name = UIManager.getSystemLookAndFeelClassName();
                base = BasicLookAndFeelHacking.newInstance(name);
            }

            if (base == null) {
                base = new IntelliJMetalLookAndFeel();
            }
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
            final UIDefaults metalDefaults = new MetalLookAndFeel().getDefaults();
            final UIDefaults defaults = base.getDefaults();
            if (Platform.current().os().isLinux() && !Registry.is("darcula.use.native.fonts.on.linux")) {
                Font font = findFont("DejaVu Sans");
                if (font != null) {
                    for (Object key : defaults.keySet()) {
                        if (key instanceof String && ((String) key).endsWith(".font")) {
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
            defaults.put("Spinner.arrowButtonSize", JBUI.size(16, 5).asUIResource());
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
            Object defaultStylesKey = HTMLEditorKitHacking.DEFAULT_STYLES_KEY();
            if (defaultStylesKey != null) {
                AppContextHacking.put(defaultStylesKey, UIUtil.loadStyleSheet(url));
            }
        }
        catch (Exception e) {
            log(e);
        }
    }

    protected String getPrefix() {
        return "darcula";
    }

    @Override
    public void initComponentDefaults(UIDefaults defaults) {
        BasicLookAndFeelHacking.initComponentDefaults(base, defaults);
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
        final String osSuffix;

        PlatformOperatingSystem os = Platform.current().os();
        if (os.isMac()) {
            osSuffix = "mac";
        }
        else {
            if (os.isWindows()) {
                if (os.asWindows().isWindows11OrNewer()) {
                    osSuffix = "windows11";
                }
                else {
                    osSuffix = "windows";
                }
            }
            else {
                osSuffix = "linux";
            }
        }

        try {
            InputStream stream = getClass().getResourceAsStream(getPrefix() + ".properties");
            properties.load(stream);
            stream.close();

            stream = getClass().getResourceAsStream(getPrefix() + "_" + osSuffix + ".properties");
            if (stream != null) {
                properties.load(stream);
                stream.close();
            }

            Map<String, Object> darculaGlobalSettings = new HashMap<>();
            final String prefix = getPrefix() + ".";
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith(prefix)) {
                    darculaGlobalSettings.put(key.substring(prefix.length()), parseValue(key, properties.getProperty(key)));
                }
            }

            for (Object key : defaults.keySet()) {
                if (key instanceof String && ((String) key).contains(".")) {
                    final String s = (String) key;
                    final String darculaKey = s.substring(s.lastIndexOf('.') + 1);
                    if (darculaGlobalSettings.containsKey(darculaKey)) {
                        defaults.put(key, darculaGlobalSettings.get(darculaKey));
                    }
                }
            }

            for (String key : properties.stringPropertyNames()) {
                final String value = properties.getProperty(key);
                defaults.put(key, parseValue(key, value));
            }
        }
        catch (IOException e) {
            log(e);
        }
    }

    public static Object parseValue(String key, @Nonnull String value) {
        if (key.endsWith("Size")) {
            final List<String> numbers = StringUtil.split(value, ",");
            return new Dimension(Integer.parseInt(numbers.get(0)), Integer.parseInt(numbers.get(1)));
        }
        else if (key.endsWith("Insets") || key.endsWith("padding") || key.endsWith("Margins")) {
            final List<String> numbers = StringUtil.split(value, ",");
            return new InsetsUIResource(Integer.parseInt(numbers.get(0)), Integer.parseInt(numbers.get(1)), Integer.parseInt(numbers.get(2)), Integer.parseInt(numbers.get(3)));
        }
        else if (key.endsWith("Border") || key.endsWith("border")) {
            try {
                List<String> ints = StringUtil.split(value, ",");
                if (ints.size() == 4) {
                    return new BorderUIResource.EmptyBorderUIResource(parseInsets(value));
                }
                else if (ints.size() == 5) {
                    return asUIResource(
                        JBUI.Borders.customLine(ColorUtil.fromHex(ints.get(4)), Integer.parseInt(ints.get(0)), Integer.parseInt(ints.get(1)), Integer.parseInt(ints.get(2)), Integer.parseInt(ints.get(3))));
                }
                Class<?> aClass = Class.forName(value, true, DarculaLaf.class.getClassLoader());
                Constructor<?> constructor = aClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            }
            catch (Exception e) {
                log(e);
            }
        }
        else {
            if (key.endsWith("Icon") && value.startsWith("com.formdev.flatlaf.icons.")) {
                return (UIDefaults.LazyValue) (uiDefaults) -> {
                    try {
                        Class<?> iconClass = Class.forName(value);
                        return iconClass.newInstance();
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }

            final Color color = parseColor(value);
            final Integer invVal = getInteger(value);
            final Boolean boolVal = "true".equals(value) ? Boolean.TRUE : "false".equals(value) ? Boolean.FALSE : null;

            Image image = null;
            if (value.contains("@")) {
                String[] ids = value.split("@");

                String groupId = ids[0];
                if (groupId.endsWith("IconGroup")) {
                    image = parseImageKey(groupId, ids[1]);
                }
            }

            if (color != null) {
                return new ColorUIResource(color);
            }
            else if (invVal != null) {
                return invVal;
            }
            else if (image != null) {
                return new IconUIResource(TargetAWT.to(image));
            }
            else if (boolVal != null) {
                return boolVal;
            }
        }
        return value;
    }

    public static Border asUIResource(@Nonnull Border border) {
        if (border instanceof UIResource) {
            return border;
        }
        return new BorderUIResource(border);
    }

    private static Insets parseInsets(String value) {
        List<String> numbers = StringUtil.split(value, ",");
        return new JBInsets(Integer.parseInt(numbers.get(0)), Integer.parseInt(numbers.get(1)), Integer.parseInt(numbers.get(2)), Integer.parseInt(numbers.get(3))).asUIResource();
    }

    private static ImageKey parseImageKey(@Nonnull String groupId, @Nonnull String imageIdWithSize) {
        if (imageIdWithSize.contains(",")) {
            String[] idSize = imageIdWithSize.split(",");
            String imageId = idSize[0];
            String[] size = idSize[1].split(":");
            return ImageKey.of(groupId, imageId.toLowerCase(Locale.ROOT), Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
        return ImageKey.of(groupId, imageIdWithSize.toLowerCase(Locale.ROOT), Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
    }

    @SuppressWarnings("UseJBColor")
    private static Color parseColor(String value) {
        if (value != null && value.length() == 8) {
            final Color color = ColorUtil.fromHex(value.substring(0, 6));
            if (color != null) {
                try {
                    int alpha = Integer.parseInt(value.substring(6, 8), 16);
                    return new ColorUIResource(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                }
                catch (Exception ignore) {
                }
            }
            return null;
        }
        return ColorUtil.fromHex(value, null);
    }

    private static Integer getInteger(String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getID() {
        return "darcula";
    }

    @Override
    public String getDescription() {
        return "Darcula";
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
        BasicLookAndFeelHacking.initSystemColorDefaults(base, defaults);
    }

    @Override
    protected void initClassDefaults(UIDefaults defaults) {
        BasicLookAndFeelHacking.initClassDefaults(base, defaults);
    }

    @Override
    public void initialize() {
        BasicLookAndFeelHacking.initialize(base);
    }

    @Override
    public void uninitialize() {
        BasicLookAndFeelHacking.uninitialize(base);
    }

    @Override
    protected void loadSystemColors(UIDefaults defaults, String[] systemColors, boolean useNative) {
        BasicLookAndFeelHacking.loadSystemColors(base, defaults, systemColors, useNative);
    }

    @Override
    public boolean getSupportsWindowDecorations() {
        if (SystemInfo.isWindows_10_orLater &&  FlatNativeWindowBorder.isSupported()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isDark() {
        return true;
    }
}

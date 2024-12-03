/*
 * @(#)LookAndFeelFactory.java 5/28/2005
 *
 * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.plaf;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * JIDE Software created many new components that need their own ComponentUI classes and additional UIDefaults in
 * UIDefaults table. LookAndFeelFactory can take the UIDefaults from any existing look and feel and add the extra
 * UIDefaults JIDE components need.
 * <p/>
 * Before using any JIDE components, please make you call one of the two LookAndFeelFactory.installJideExtension(...)
 * methods. Basically, you set L&F using UIManager first just like before, then call installJideExtension. See code
 * below for an example.
 * <code><pre>
 * UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName()); // you need to catch the exceptions on this call.
 * LookAndFeelFactory.installJideExtension();
 * </pre></code>
 * LookAndFeelFactory.installJideExtension() method will check what kind of L&F you set and what operating system you
 * are on and decide which style of JIDE extension it will install. Here is the rule. <ul> <li> OS: Windows Vista or
 * Windows 7, L&F: Windows L&F => OFFICE2007_STYLE </li><li> OS: Windows XP with XP theme on, L&F: Windows L&F =>
 * OFFICE2003_STYLE <li> OS: any Windows, L&F: Windows L&F => VSNET_STYLE <li> OS: Linux, L&F: any L&F based on Metal
 * L&F => VSNET_STYLE <li> OS: Mac OS X, L&F: Aqua L&F => AQUA_STYLE <li> OS: any OS, L&F: Quaqua L&F => AQUA_STYLE <li>
 * Otherwise => VSNET_STYLE </ul> There is also another installJideExtension which takes an int style parameter. You can
 * pass in {@link #EXTENSION_STYLE_VSNET}, {@link #EXTENSION_STYLE_ECLIPSE}, {@link #EXTENSION_STYLE_ECLIPSE3X}, {@link
 * #EXTENSION_STYLE_OFFICE2003}, {@link #EXTENSION_STYLE_OFFICE2007}, or {@link #EXTENSION_STYLE_XERTO}, {@link
 * #EXTENSION_STYLE_OFFICE2003_WITHOUT_MENU}, {@link #EXTENSION_STYLE_OFFICE2007_WITHOUT_MENU}, {@link
 * #EXTENSION_STYLE_ECLIPSE_WITHOUT_MENU}, {@link #EXTENSION_STYLE_ECLIPSE3X_WITHOUT_MENU}. In the other word, you will
 * make the choice of style instead of letting LookAndFeelFactory to decide one for you. Please note, there is no
 * constant defined for AQUA_STYLE. The only way to use it is when you are using Aqua L&F or Quaqua L&F and you call
 * installJideExtension() method, the one without parameter.
 * <p/>
 * Another way is to call {@link LookAndFeelFactory#installDefaultLookAndFeelAndExtension()} which will set the default
 * L&Fs based on the OS and install JIDE extension.
 * <p/>
 * LookAndFeelFactory supports a number of known L&Fs. Some are L&Fs in the JDK such as Metal, Windows, Aqua (on Apple
 * JDK only), GTK. We also support some 3rd party L&Fs such as Plastic XP or Plastic 3D, Tonic, A03, Synthetica etc.
 * <p/>
 * If you are using a 3rd party L&F we are not officially supporting, you might need to customize it. Here are two
 * classes you can use - {@link LookAndFeelFactory.UIDefaultsInitializer} and {@link
 * LookAndFeelFactory.UIDefaultsCustomizer}.
 * <p/>
 * Let's start with UIDefaultsCustomizer. No matter what unknown L&F you are trying to use, LookAndFeelFactory's
 * installJideExtension() will try to install the UIDefaults that JIDE components required. Hopefully JIDE will run on
 * your L&F without any exception. But most likely, it won't look very good. That's why you need {@link
 * UIDefaultsCustomizer} to customize the UIDefaults.
 * <p/>
 * Most likely, you will not need to use {@link UIDefaultsInitializer}. The only exception is Synth L&F and any L&Fs
 * based on it. The reason is we calculate all colors we will use in JIDE components from existing well-known
 * UIDefaults. For example, we will use UIManagerLookup.getColor("activeCaption") to calculate a color that we can use
 * in dockable frame's title pane. We will use UIManagerLookup.getColor("control") to calculate a color that we can use
 * as background of JIDE component. Most L&Fs will fill those UIDefaults. However in Synth L&F, those UIDefaults may or
 * may not have a valid value. You will end up with NPE later in the code when you call installJideExtension. In this
 * case, you can add those extra UIDefaults in UIDefaultsInitializer. We will call it before installJideExtension is
 * called so that those UIDefaults are there ready for us to use. This is how added support to GTK L&F and Synthetica
 * L&F.
 * <p/>
 * After you create your own UIDefaultsCustomizer or Initializer, you can call {@link
 * #addUIDefaultsCustomizer(com.jidesoft.plaf.LookAndFeelFactory.UIDefaultsCustomizer)} or {@link
 * #addUIDefaultsInitializer(com.jidesoft.plaf.LookAndFeelFactory.UIDefaultsInitializer)} which will make the customizer
 * or the initializer triggered all the times. If you only want it to be used for a certain L&F, you should use {@link
 * #registerDefaultCustomizer(String, String)} or {@link #registerDefaultInitializer(String, String)}.
 * <p/>
 * By default, we also use UIDefaultsCustomizer and UIDefaultsInitializer internally to provide support for non-standard
 * L&Fs. However we look into the classes under "com.jidesoft.plaf" package for default customizers and initializers.
 * For example, for PlasticXPLookAndFeel, the corresponding customizer is "oom.jidesoft.plaf.plasticxp.PlasticXPCustomizer".
 * We basically take the L&F name "PlasticXP", append it after "com.jidesoft.plaf" using lower case to get package name,
 * take the L&F name, append with "Customizer" to get the class name. We will look at PlasticXPLookAndFeel's super class
 * which is PlasticLookAndFeel. The customizer corresponding to PlasticLookAndFeel is
 * "com.jidesoft.plaf.plastic.PlasticCustomizer". This searching process continues till we find all super classes of a
 * L&F. Then we start from its top-most super class and call the customizers one by one, if it is there. The
 * src-plaf.jar or src-plaf-jdk7.jar contain some of these customizers. You could use this naming pattern to create the
 * customizers so that you don't need to register them explicitly.
 * <p/>
 * {@link #installJideExtension()} method will only add the additional UIDefaults to current ClassLoader. If you have
 * several class loaders in your system, you probably should tell the UIManager to use the class loader that called
 * <code>installJideExtension</code>. Otherwise, you might some unexpected errors. Here is how to specify the class
 * loaders.
 * <code><pre>
 * UIManager.put("ClassLoader", currentClass.getClassLoader()); // currentClass is the class where the code is.
 * LookAndFeelFactory.installDefaultLookAndFeelAndExtension(); // or installJideExtension()
 * </pre></code>
 */
public class LookAndFeelFactory {
    /**
     * If installJideExtension is called, it will put an entry on UIDefaults table.
     * UIManagerLookup.getBoolean(JIDE_EXTENSION_INSTALLLED) will return true. You can also use {@link
     * #isJideExtensionInstalled()} to check the value instead of using UIManagerLookup.getBoolean(JIDE_EXTENSION_INSTALLLED).
     */
    public static final String JIDE_EXTENSION_INSTALLED = "jidesoft.extensionInstalled";

    /**
     * An interface to make the customization of UIDefaults easier. This customizer will be called after
     * installJideExtension() is called. So if you want to further customize UIDefault, you can use this customizer to
     * do it.
     */
    public static interface UIDefaultsCustomizer {
        void customize(UIDefaults defaults);
    }

    /**
     * An interface to make the initialization of UIDefaults easier. This initializer will be called before
     * installJideExtension() is called. So if you want to initialize UIDefault before installJideExtension is called,
     * you can use this initializer to do it.
     */
    public static interface UIDefaultsInitializer {
        void initialize(UIDefaults defaults);
    }

    protected LookAndFeelFactory() {
    }

    /**
     * Adds additional UIDefaults JIDE needed to UIDefault table. You must call this method every time switching look
     * and feel. And call updateComponentTreeUI() in corresponding DockingManager or DockableBarManager after this
     * call.
     * <pre><code>
     *  try {
     *      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
     *  }
     *  catch (ClassNotFoundException e) {
     *     e.printStackTrace();
     *  }
     *  catch (InstantiationException e) {
     *     e.printStackTrace();
     *  }
     *  catch (IllegalAccessException e) {
     *      e.printStackTrace();
     *  }
     *  catch (UnsupportedLookAndFeelException e) {
     *      e.printStackTrace();
     *  }
     * <p/>
     *  // to additional UIDefault for JIDE components
     *  LookAndFeelFactory.installJideExtension(); // use default style VSNET_STYLE. You can change
     * to a different style
     * using setDefaultStyle(int style) and then call this method. Or simply call
     * installJideExtension(style).
     * <p/>
     *  // call updateComponentTreeUI
     *  frame.getDockableBarManager().updateComponentTreeUI();
     *  frame.getDockingManager().updateComponentTreeUI();
     * </code></pre>
     */
    @SuppressWarnings("unchecked")
    public static void installJideExtension() {
        if (isJideExtensionInstalled()) {
            return;
        }

        UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();

        uiDefaults.put(JIDE_EXTENSION_INSTALLED, Boolean.TRUE);

        try {
            if (_defaultCustomizers != null) {
                for (String clazz : _defaultCustomizers.values()) {
                    Class<? extends UIDefaultsCustomizer> c = (Class<? extends UIDefaultsCustomizer>) Class.forName(clazz, true, LookAndFeelFactory.class.getClassLoader());

                    UIDefaultsCustomizer customizer = c.newInstance();

                    customizer.customize(uiDefaults);
                }
            }

            if (_defaultInitializers != null) {
                for (String clazz : _defaultInitializers.values()) {
                    Class<? extends UIDefaultsInitializer> c = (Class<? extends UIDefaultsInitializer>) Class.forName(clazz, true, LookAndFeelFactory.class.getClassLoader());

                    UIDefaultsInitializer initializer = c.newInstance();

                    initializer.initialize(uiDefaults);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if JIDE extension is installed. Please note, UIManager.setLookAndFeel() method will overwrite the whole
     * UIDefaults table. So even you called {@link #installJideExtension()} method before, UIManager.setLookAndFeel()
     * method make isJideExtensionInstalled returning false.
     *
     * @return true if installed.
     */
    public static boolean isJideExtensionInstalled() {
        return UIDefaultsLookup.getBoolean(JIDE_EXTENSION_INSTALLED);
    }

    private static Map<String, String> _defaultInitializers;
    private static Map<String, String> _defaultCustomizers;

    /**
     * Registers a UIDefaultsInitializer with a L&F. Note that you can only register one initializer for a L&F.
     *
     * @param lnfClassName         full class name of the L&F
     * @param initializerClassName full class name of the UIDefaultInitializer
     */
    public static void registerDefaultInitializer(String lnfClassName, String initializerClassName) {
        if (_defaultInitializers == null) {
            _defaultInitializers = new HashMap<String, String>();
        }
        _defaultInitializers.put(lnfClassName, initializerClassName);
    }

    /**
     * Unregisters a UIDefaultsInitializer for L&F.
     *
     * @param lnfClassName full class name of the L&F
     */
    public static void unregisterDefaultInitializer(String lnfClassName) {
        if (_defaultInitializers != null) {
            _defaultInitializers.remove(lnfClassName);
        }
    }

    /**
     * Clears all registered initializers.
     */
    public static void clearDefaultInitializers() {
        if (_defaultInitializers != null) {
            _defaultInitializers.clear();
        }
    }

    /**
     * Registers a UIDefaultsCustomizer with a L&F. Note that you can only register one customizer for a L&F.
     *
     * @param lnfClassName        full class name of the L&F
     * @param customizerClassName full class name of the UIDefaultsCustomizer
     */
    public static void registerDefaultCustomizer(String lnfClassName, String customizerClassName) {
        if (_defaultCustomizers == null) {
            _defaultCustomizers = new HashMap<String, String>();
        }
        _defaultCustomizers.put(lnfClassName, customizerClassName);
    }

    /**
     * Unregisters a UIDefaultCustomizer for L&F.
     *
     * @param lnfClassName full class name of the L&F
     */
    public static void unregisterDefaultCustomizer(String lnfClassName) {
        if (_defaultCustomizers != null) {
            _defaultCustomizers.remove(lnfClassName);
        }
    }

    public static boolean isMnemonicHidden() {
        return !UIManager.getBoolean("Button.showMnemonics");
    }
}
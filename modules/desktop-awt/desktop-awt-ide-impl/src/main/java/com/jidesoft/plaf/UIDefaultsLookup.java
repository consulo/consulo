/*
 * @(#)UIManagerLookup.java 4/5/2007
 *
 * Copyright 2002 - 2007 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.plaf;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class simply uses UIManager's get method to lookup the UIDefaults. We used this everywhere in our code so that
 * we have one central place to find out which UIDefaults we are using. Another good thing is you can use {@link
 * #setTrace(boolean)} and {@link #setDebug(boolean)} to turn on the trace so that it will print out which UIDefaults we
 * are trying to get.
 */
public class UIDefaultsLookup {
    private static Logger LOGGER = Logger.getLogger(UIDefaultsLookup.class.getName());

    private static boolean _debug = false;
    private static boolean _trace = false;

    /**
     * Sets the debug mode. If debug mode is on, we will print out any UIDefaults that the value is null.
     *
     * @param debug true or false.
     */
    public static void setDebug(boolean debug) {
        _debug = debug;
    }

    /**
     * Sets the trace mode. If trace mode is on, we will print out any UIDefaults we are trying to get and its current
     * value.
     *
     * @param trace true or false.
     */
    public static void setTrace(boolean trace) {
        _trace = trace;
    }

    public static void put(UIDefaults table, String key, Object value) {
        Object v = table.get(key);
        if (v == null || !(v instanceof Map)) {
            v = new HashMap<ClassLoader, Object>();
            table.put(key, v);
        }
        Object cl = UIManager.get("ClassLoader");
        if (!(cl instanceof ClassLoader)) {
            cl = value.getClass().getClassLoader();
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Put " + key + " " + value + " using ClassLoader: " + cl);
        }
        ((Map) v).put(cl, value);
    }

/*  This is the old method used before JDk8 b87. The getCallerClass(int) is removed in b87 so we can't use it any longer.
    // Returns the invoker's class loader, or null if none.
    // NOTE: This must always be invoked when there is exactly one intervening
    // frame from the core libraries on the stack between this method's
    // invocation and the desired invoker.
    static ClassLoader getCallerClassLoader() {
        Object cl = UIManager.get("ClassLoader");
        if (cl instanceof ClassLoader) {
            return (ClassLoader) cl;
        }

        // NOTE use of more generic Reflection.getCallerClass()
        Class caller = Reflection.getCallerClass(3);
        // This can be null if the VM is requesting it
        if (caller == null) {
            return null;
        }
        // Circumvent security check since this is package-private
        return caller.getClassLoader();
    }
*/


    static ClassLoader getCallerClassLoader() {
        Object cl = UIManager.get("ClassLoader");
        if (cl instanceof ClassLoader) {
            return (ClassLoader) cl;
        }

        String className = Thread.currentThread().getStackTrace()[3].getClassName();
        try {
            return Class.forName(className).getClassLoader();
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Object get(Object key) {
        Object value = UIManager.get(key);
        log(value, key, null);
        if (value instanceof Map && "Theme.painter".equals(key)) {
            Map map = (Map) value;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Getting " + key + " from a map");
                for (Object o : map.keySet()) {
                    LOGGER.fine("\t" + o + " => " + map.get(o));
                }
            }
            try {
                ClassLoader classLoader = getCallerClassLoader();
                Object o = map.get(classLoader);
                if (o != null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("\tGetting " + o + " using CallerClassLoader" + classLoader);
                    }
                }
                while (o == null && classLoader.getParent() != null) {
                    classLoader = classLoader.getParent();
                    o = map.get(classLoader);
                    if (o != null) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("\tGetting " + o + " using one of the parent ClassLoader " + classLoader);
                        }
                        break;
                    }
                }
                if (o != null) return o;
            }
            catch (Exception e) {
                // ignore
            }
            if (map.size() == 1) {
                Object o = map.values().iterator().next();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Failed...getting the only one " + o);
                }
                return o;
            }
        }
        return value;
    }

    public static Object get(Object key, Locale l) {
        Object value = UIManager.get(key, l);
        log(value, key, l);
        return value;
    }

    private static void log(Object value, Object key, Locale l) {
        if (_debug && value == null) {
            System.out.println("\"" + key + (l == null ? "" : l.toString()) + " \" ==> null ------------------------");
        }
        else if (_trace) {
            System.out.println("\"" + key + (l == null ? "" : l.toString()) + " \" ==> null ------------------------");
        }
    }

    /**
     * If the value of <code>key</code> is a <code>Font</code> return it, otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is a <code>Font</code>, return the <code>Font</code> object; otherwise
     * return <code>null</code>
     */
    public static Font getFont(Object key) {
        Object value = get(key);
        return (value instanceof Font) ? (Font) value : null;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is a <code>Font</code> return it, otherwise
     * return <code>null</code>.
     *
     * @param key the desired key
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is a <code>Font</code>, return the
     * <code>Font</code> object; otherwise return <code>null</code>
     * @since 1.9.5.04
     */
    public static Font getFont(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Font) ? (Font) value : null;
    }

    /**
     * If the value of <code>key</code> is a <code>Color</code> return it, otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is a <code>Color</code>, return the <code>Color</code> object;
     * otherwise return <code>null</code>
     */
    public static Color getColor(Object key) {
        Object value = get(key);
        return (value instanceof Color) ? (Color) value : null;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is a <code>Color</code> return it, otherwise
     * return <code>null</code>.
     *
     * @param key the desired key
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is a <code>Color</code>, return the
     * <code>Color</code> object; otherwise return <code>null</code>
     * @since 1.9.5.04
     */
    public static Color getColor(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Color) ? (Color) value : null;
    }


    /**
     * If the value of <code>key</code> is an <code>Icon</code> return it, otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is an <code>Icon</code>, return the <code>Icon</code> object; otherwise
     * return <code>null</code>
     */
    public static Icon getIcon(Object key) {
        Object value = get(key);
        return (value instanceof Icon) ? (Icon) value : null;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is an <code>Icon</code> return it, otherwise
     * return <code>null</code>.
     *
     * @param key the desired key
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is an <code>Icon</code>, return the
     * <code>Icon</code> object; otherwise return <code>null</code>
     * @since 1.9.5.04
     */
    public static Icon getIcon(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Icon) ? (Icon) value : null;
    }


    /**
     * If the value of <code>key</code> is a <code>Border</code> return it, otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is a <code>Border</code>, return the <code>Border</code> object;
     * otherwise return <code>null</code>
     */
    public static Border getBorder(Object key) {
        Object value = get(key);
        return (value instanceof Border) ? (Border) value : null;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is a <code>Border</code> return it, otherwise
     * return <code>null</code>.
     *
     * @param key the desired key
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is a <code>Border</code>, return the
     * <code>Border</code> object; otherwise return <code>null</code>
     * @since 1.9.5.04
     */
    public static Border getBorder(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Border) ? (Border) value : null;
    }


    /**
     * If the value of <code>key</code> is a <code>String</code> return it, otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is a <code>String</code>, return the <code>String</code> object;
     * otherwise return <code>null</code>
     */
    public static String getString(Object key) {
        Object value = get(key);
        return (value instanceof String) ? (String) value : null;
    }

    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is a <code>String</code> return it, otherwise
     * return <code>null</code>.
     *
     * @param key the desired key
     * @param l   the desired <code>Locale</code>
     * @return if the value for <code>key</code> for the given <code>Locale</code> is a <code>String</code>, return the
     * <code>String</code> object; otherwise return <code>null</code>
     * @since 1.9.5.04
     */
    public static String getString(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof String) ? (String) value : null;
    }

    /**
     * If the value of <code>key</code> is an <code>Integer</code> return its integer value, otherwise return 0.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is an <code>Integer</code>, return its value, otherwise return 0
     */
    public static int getInt(Object key) {
        Object value = get(key);
        return (value instanceof Integer) ? (Integer) value : 0;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is an <code>Integer</code> return its integer
     * value, otherwise return 0.
     *
     * @param key the desired key
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is an <code>Integer</code>, return its value,
     * otherwise return 0
     * @since 1.9.5.04
     */
    public static int getInt(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Integer) ? (Integer) value : 0;
    }


    /**
     * If the value of <code>key</code> is boolean, return the boolean value, otherwise return false.
     *
     * @param key an <code>Object</code> specifying the key for the desired boolean value
     * @return if the value of <code>key</code> is boolean, return the boolean value, otherwise return false.
     * @since 1.9.5.04
     */
    public static boolean getBoolean(Object key) {
        Object value = get(key);
        return (value instanceof Boolean) ? (Boolean) value : false;
    }

    /**
     * If the value of <code>key</code> is boolean, return the boolean value, otherwise return false.
     *
     * @param key          an <code>Object</code> specifying the key for the desired boolean value
     * @param defaultValue the default value if the key is missing
     * @return if the value of <code>key</code> is boolean, return the boolean value, otherwise return false.
     */
    public static boolean getBoolean(Object key, boolean defaultValue) {
        Object value = get(key);
        return (value instanceof Boolean) ? (Boolean) value : defaultValue;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is boolean, return the boolean value,
     * otherwise return false.
     *
     * @param key an <code>Object</code> specifying the key for the desired boolean value
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is boolean, return the boolean value, otherwise
     * return false.
     * @since 1.9.5.04
     */
    public static boolean getBoolean(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Boolean) ? (Boolean) value : false;
    }


    /**
     * If the value of <code>key</code> is an <code>Insets</code> return it, otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is an <code>Insets</code>, return the <code>Insets</code> object;
     * otherwise return <code>null</code>
     */
    public static Insets getInsets(Object key) {
        Object value = get(key);
        return (value instanceof Insets) ? (Insets) value : null;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is an <code>Insets</code> return it, otherwise
     * return <code>null</code>.
     *
     * @param key the desired key
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is an <code>Insets</code>, return the
     * <code>Insets</code> object; otherwise return <code>null</code>
     * @since 1.9.5.04
     */
    public static Insets getInsets(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Insets) ? (Insets) value : null;
    }


    /**
     * If the value of <code>key</code> is a <code>Dimension</code> return it, otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @return if the value for <code>key</code> is a <code>Dimension</code>, return the <code>Dimension</code> object;
     * otherwise return <code>null</code>
     */
    public static Dimension getDimension(Object key) {
        Object value = get(key);
        return (value instanceof Dimension) ? (Dimension) value : null;
    }


    /**
     * If the value of <code>key</code> for the given <code>Locale</code> is a <code>Dimension</code> return it,
     * otherwise return <code>null</code>.
     *
     * @param key the desired key
     * @param l   the desired locale
     * @return if the value for <code>key</code> and <code>Locale</code> is a <code>Dimension</code>, return the
     * <code>Dimension</code> object; otherwise return <code>null</code>
     * @since 1.9.5.04
     */
    public static Dimension getDimension(Object key, Locale l) {
        Object value = get(key, l);
        return (value instanceof Dimension) ? (Dimension) value : null;
    }
}
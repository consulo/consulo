/*
 * @(#)JideSwingUtilities.java
 *
 * Copyright 2002 JIDE Software. All rights reserved.
 */
package com.jidesoft.swing;

import com.formdev.flatlaf.util.UIScale;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.utils.SystemInfo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;

/**
 * A utilities class for Swing.
 */
public class JideSwingUtilities implements SwingConstants {

    private static final Logger LOGGER_FOCUS = Logger.getLogger(JideSwingUtilities.class.getName() + ".focus");

    /**
     * Gets the top level Frame of the component.
     *
     * @param component the component
     * @return the top level Frame. Null if we didn't find an ancestor which is instance of Frame.
     */
    public static Frame getFrame(Component component) {
        if (component == null) {
            return null;
        }

        if (component instanceof Frame) {
            return (Frame) component;
        }

        // Find frame
        Container p = component.getParent();
        while (p != null) {
            if (p instanceof Frame) {
                return (Frame) p;
            }
            p = p.getParent();
        }
        return null;
    }


    /**
     * Toggles between RTL and LTR.
     *
     * @param topContainer the component
     */
    public static void toggleRTLnLTR(Component topContainer) {
        ComponentOrientation co = topContainer.getComponentOrientation();
        if (co == ComponentOrientation.RIGHT_TO_LEFT) {
            co = ComponentOrientation.LEFT_TO_RIGHT;
        }
        else {
            co = ComponentOrientation.RIGHT_TO_LEFT;
        }
        topContainer.applyComponentOrientation(co);
    }

    public static int getButtonState(AbstractButton b) {
        ButtonModel model = b.getModel();
        if (!model.isEnabled()) {
            if (model.isSelected()) {
                return ThemePainter.STATE_DISABLE_SELECTED;
            }
            else {
                return ThemePainter.STATE_DISABLE;
            }
        }
        else if (model.isPressed() && model.isArmed()) {
            if (model.isRollover()) {
                return ThemePainter.STATE_PRESSED;
            }
            else if (model.isSelected()) {
                return ThemePainter.STATE_SELECTED;
            }
        }
        else if (b.isRolloverEnabled() && model.isRollover()) {
            if (model.isSelected()) {
                return ThemePainter.STATE_PRESSED; // should be rollover selected
            }
            else {
                return ThemePainter.STATE_ROLLOVER;
            }
        }
        else if (model.isSelected()) {
            return ThemePainter.STATE_SELECTED;
        }
        else if (b.hasFocus() && b.isFocusPainted()) {
            if (model.isSelected()) {
                return ThemePainter.STATE_PRESSED;
            }
            else {
                return ThemePainter.STATE_ROLLOVER;
            }
        }
        return ThemePainter.STATE_DEFAULT;
    }

    public static int[] getButtonState(JideSplitButton b) {
        int[] states = new int[2];
        SplitButtonModel model = (SplitButtonModel) b.getModel();
        if (!model.isEnabled()) {
            if (model.isButtonSelected()) {
                states[0] = ThemePainter.STATE_DISABLE_SELECTED;
            }
            else {
                states[0] = ThemePainter.STATE_DISABLE;
            }
        }
        else if (b.hasFocus() && b.isFocusPainted()) {
            if (model.isButtonSelected()) {
                states[0] = ThemePainter.STATE_SELECTED;
                states[1] = ThemePainter.STATE_INACTIVE_ROLLOVER;
            }
            else if (model.isSelected()) {
                states[0] = ThemePainter.STATE_INACTIVE_ROLLOVER;
                states[1] = ThemePainter.STATE_SELECTED;
            }
            else {
                states[0] = ThemePainter.STATE_ROLLOVER;
                states[1] = ThemePainter.STATE_INACTIVE_ROLLOVER;
            }
        }
        else if (model.isPressed() && model.isArmed()) {
            if (model.isButtonRollover()) {
                states[0] = ThemePainter.STATE_PRESSED;
                states[1] = ThemePainter.STATE_INACTIVE_ROLLOVER;
            }
            else if (model.isRollover()) {
                states[0] = ThemePainter.STATE_INACTIVE_ROLLOVER;
                states[1] = ThemePainter.STATE_ROLLOVER;
            }
        }
        else if (b.isRolloverEnabled() && model.isButtonRollover()) {
            if (model.isButtonSelected()) {
                states[0] = ThemePainter.STATE_PRESSED;
                states[1] = ThemePainter.STATE_INACTIVE_ROLLOVER;
            }
            else if (model.isSelected()) {
                states[0] = ThemePainter.STATE_ROLLOVER;
                states[1] = ThemePainter.STATE_PRESSED;
            }
            else {
                states[0] = ThemePainter.STATE_ROLLOVER;
                states[1] = ThemePainter.STATE_INACTIVE_ROLLOVER;
            }
        }
        else if (b.isRolloverEnabled() && model.isRollover()) {
            if (model.isButtonSelected()) {
                states[0] = ThemePainter.STATE_PRESSED;
                states[1] = ThemePainter.STATE_ROLLOVER;
            }
            else if (model.isSelected()) {
                states[0] = ThemePainter.STATE_INACTIVE_ROLLOVER;
                states[1] = ThemePainter.STATE_PRESSED;
            }
            else {
                states[0] = ThemePainter.STATE_INACTIVE_ROLLOVER;
                states[1] = ThemePainter.STATE_ROLLOVER;
            }
        }
        else if (model.isButtonSelected()) {
            states[0] = ThemePainter.STATE_SELECTED;
            states[1] = ThemePainter.STATE_INACTIVE_ROLLOVER;
        }
        else if (model.isSelected()) {
            states[0] = ThemePainter.STATE_INACTIVE_ROLLOVER;
            states[1] = ThemePainter.STATE_SELECTED;
        }
        else {
            states[0] = ThemePainter.STATE_DEFAULT;
            states[1] = ThemePainter.STATE_DEFAULT;
        }
        return states;
    }

    /**
     * Checks if the two objects equal. If both are null, they are equal. If o1 and o2 both are Comparable, we will use
     * compareTo method to see if it equals 0. At last, we will use <code>o1.equals(o2)</code> to compare. If none of
     * the above conditions match, we return false.
     *
     * @param o1 the first object to compare
     * @param o2 the second object to compare
     * @return true if the two objects are equal. Otherwise false.
     */
    public static boolean equals(Object o1, Object o2) {
        return equals(o1, o2, false);
    }

    /**
     * Checks if the two objects equal. If both are the same instance, they are equal. If both are null, they are equal.
     * If o1 and o2 both are Comparable, we will use compareTo method to see if it equals 0. If considerArrayOrList is
     * true and o1 and o2 are both array, we will compare each element in the array. At last, we will use
     * <code>o1.equals(o2)</code> to compare. If none of the above conditions match, we return false.
     *
     * @param o1                  the first object to compare
     * @param o2                  the second object to compare
     * @param considerArrayOrList If true, and if o1 and o2 are both array, we will compare each element in the array
     *                            instead of just compare the two array objects.
     * @return true if the two objects are equal. Otherwise false.
     */
    public static boolean equals(Object o1, Object o2, boolean considerArrayOrList) {
        return equals(o1, o2, considerArrayOrList, true);
    }

    /**
     * Checks if the two objects equal. If both are the same instance, they are equal. If both are null, they are equal.
     * If o1 and o2 both are Comparable, we will use compareTo method to see if it equals 0. If considerArrayOrList is
     * true and o1 and o2 are both array, we will compare each element in the array. At last, we will use
     * <code>o1.equals(o2)</code> to compare. If none of the above conditions match, we return false.
     *
     * @param o1                  the first object to compare
     * @param o2                  the second object to compare
     * @param considerArrayOrList If true, and if o1 and o2 are both array, we will compare each element in the array
     *                            instead of just compare the two array objects.
     * @param caseSensitive       if the o1 and o2 are CharSequence, we will use this parameter to do a case sensitive
     *                            or insensitive comparison
     * @return true if the two objects are equal. Otherwise false.
     */
    public static boolean equals(Object o1, Object o2, boolean considerArrayOrList, boolean caseSensitive) {
        if (o1 == o2) {
            return true;
        }
        else if (o1 != null && o2 == null) {
            return false;
        }
        else if (o1 == null) {
            return false;
        }
        else if (o1 instanceof CharSequence && o2 instanceof CharSequence) {
            return equals((CharSequence) o1, (CharSequence) o2, caseSensitive);
        }
        else if (o1 instanceof Comparable && o2 instanceof Comparable && o1.getClass().isAssignableFrom(o2.getClass())) {
            return ((Comparable) o1).compareTo(o2) == 0;
        }
        else if (o1 instanceof Comparable && o2 instanceof Comparable && o2.getClass().isAssignableFrom(o1.getClass())) {
            return ((Comparable) o2).compareTo(o1) == 0;
        }
        else if (considerArrayOrList && o1 instanceof List && o2 instanceof List) {
            int length1 = ((List) o1).size();
            int length2 = ((List) o2).size();
            if (length1 != length2) {
                return false;
            }
            for (int i = 0; i < length1; i++) {
                if (!equals(((List) o1).get(i), ((List) o2).get(i), true)) {
                    return false;
                }
            }
            return true;
        }
        else if (considerArrayOrList && o1.getClass().isArray() && o2.getClass().isArray()) {
            int length1 = Array.getLength(o1);
            int length2 = Array.getLength(o2);
            if (length1 != length2) {
                return false;
            }
            for (int i = 0; i < length1; i++) {
                if (!equals(Array.get(o1, i), Array.get(o2, i), true)) {
                    return false;
                }
            }
            return true;
        }
        else {
            return o1.equals(o2);
        }
    }

    public static boolean equals(CharSequence s1, CharSequence s2, boolean caseSensitive) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }

        // Algorithm from String.regionMatches()

        if (s1.length() != s2.length()) {
            return false;
        }
        int to = 0;
        int po = 0;
        int len = s1.length();

        while (len-- > 0) {
            char c1 = s1.charAt(to++);
            char c2 = s2.charAt(po++);
            if (c1 == c2) {
                continue;
            }
            if (!caseSensitive && charsEqualIgnoreCase(c1, c2)) {
                continue;
            }
            return false;
        }

        return true;
    }

    public static boolean charsEqualIgnoreCase(char a, char b) {
        return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
    }

    public static char toUpperCase(char a) {
        if (a < 'a') {
            return a;
        }
        if (a >= 'a' && a <= 'z') {
            return (char) (a + ('A' - 'a'));
        }
        return Character.toUpperCase(a);
    }

    public static char toLowerCase(final char a) {
        if (a < 'A' || a >= 'a' && a <= 'z') {
            return a;
        }

        if (a >= 'A' && a <= 'Z') {
            return (char) (a + ('a' - 'A'));
        }

        return Character.toLowerCase(a);
    }

    /**
     * Convenience method that returns a scaled instance of the provided BufferedImage.
     *
     * @param img                 the original image to be scaled
     * @param targetWidth         the desired width of the scaled instance, in pixels
     * @param targetHeight        the desired height of the scaled instance, in pixels
     * @param hint                one of the rendering hints that corresponds to RenderingHints.KEY_INTERPOLATION (e.g.
     *                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, RenderingHints.VALUE_INTERPOLATION_BILINEAR,
     *                            RenderingHints.VALUE_INTERPOLATION_BICUBIC)
     * @param progressiveBilinear if true, this method will use a multi-step scaling technique that provides higher
     *                            quality than the usual one-step technique (only useful in down-scaling cases, where
     *                            targetWidth or targetHeight is smaller than the original dimensions)
     * @return a scaled version of the original BufferedImage
     */
    public static BufferedImage getFasterScaledInstance(BufferedImage img,
                                                        int targetWidth, int targetHeight, Object hint,
                                                        boolean progressiveBilinear) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        BufferedImage scratchImage = null;
        Graphics2D g2 = null;
        int w, h;
        int prevW = ret.getWidth();
        int prevH = ret.getHeight();
        boolean isTranslucent = img.getTransparency() != Transparency.OPAQUE;

        if (progressiveBilinear) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        }
        else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (progressiveBilinear && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (progressiveBilinear && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (scratchImage == null || isTranslucent) {
                // Use a single scratch buffer for all iterations
                // and then copy to the final, correctly-sized image
                // before returning
                scratchImage = new BufferedImage(w, h, type);
                g2 = scratchImage.createGraphics();
            }

            if (g2 != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
                g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
            }

            prevW = w;
            prevH = h;

            ret = scratchImage;
        }
        while (w != targetWidth || h != targetHeight);

        if (g2 != null) {
            g2.dispose();
        }

        // If we used a scratch buffer that is larger than our target size,
        // create an image of the right size and copy the results into it
        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g2 = scratchImage.createGraphics();
            g2.drawImage(ret, 0, 0, null);
            g2.dispose();
            ret = scratchImage;
        }

        return ret;
    }

    public static int getFocusAcceleratorKeyMask() {
//        Toolkit tk = Toolkit.getDefaultToolkit();
//        if (tk instanceof SunToolkit) {
//            return ((SunToolkit)tk).getFocusAcceleratorKeyMask();
//        }
        return ActionEvent.ALT_MASK;
    }

    private static class GetPropertyAction
        implements java.security.PrivilegedAction {
        private String theProp;
        private String defaultVal;

        /**
         * Constructor that takes the name of the system property whose string value needs to be determined.
         *
         * @param theProp the name of the system property.
         */
        public GetPropertyAction(String theProp) {
            this.theProp = theProp;
        }

        /**
         * Constructor that takes the name of the system property and the default value of that property.
         *
         * @param theProp    the name of the system property.
         * @param defaultVal the default value.
         */
        public GetPropertyAction(String theProp, String defaultVal) {
            this.theProp = theProp;
            this.defaultVal = defaultVal;
        }

        /**
         * Determines the string value of the system property whose name was specified in the constructor.
         *
         * @return the string value of the system property, or the default value if there is no property with that key.
         */
        public Object run() {
            String value = System.getProperty(theProp);
            return (value == null) ? defaultVal : value;
        }
    }

    public static void printUIDefaults() {
        Enumeration e = UIManager.getDefaults().keys();
        java.util.List<String> list = new ArrayList<String>();

        System.out.println("Non-string keys ---");
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            if (key instanceof String) {
                list.add((String) key);
            }
            else {
                System.out.println(key + " => " + UIDefaultsLookup.get(key));
            }
        }

        System.out.println();

        Collections.sort(list);
        System.out.println("String keys ---");
        for (Object key : list) {
            System.out.println(key + " => " + UIDefaultsLookup.get(key));
        }
    }

    /**
     * A simple handler used by setRecursively.
     * <pre>
     *  if ( condition() ) {
     *      action();
     *  }
     *  postAction();
     * </pre>.
     */
    public interface Handler {
        /**
         * If true, it will call {@link #action(java.awt.Component)} on this component.
         *
         * @param c the component
         * @return true or false.
         */
        boolean condition(Component c);

        /**
         * The action you want to perform on this component. This method will only be called if {@link
         * #condition(java.awt.Component)} returns true.
         *
         * @param c the component
         */
        void action(Component c);

        /**
         * The action you want to perform to any components. If action(c) is called, this action is after it.
         *
         * @param c the component.
         */
        void postAction(Component c);

    }

    /**
     * A simple handler used by setRecursively.
     * <pre>
     *  if ( condition() ) {
     *      action();
     *  }
     *  postAction();
     * </pre>.
     */
    public interface ConditionHandler extends Handler {
        /**
         * If this method returns true, the recursive call will stop at the component and will not call to its
         * children.
         *
         * @param c the component
         * @return true or false.
         */
        boolean stopCondition(Component c);
    }

    /**
     * A simple handler used by getRecursively.
     * <code><pre>
     *  if ( condition() ) {
     *      return action();
     *  }
     * </pre></code>.
     * Here is an example to get the first child of the specified type.
     * <code><pre>
     * public static Component getFirstChildOf(final Class clazz, Component c) {
     *     return getRecursively(c, new GetHandler() {
     *         public boolean condition(Component c) {
     *             return clazz.isAssignableFrom(c.getClass());
     *         }
     *         public Component action(Component c) {
     *             return c;
     *         }
     *     });
     * }
     * </pre></code>
     */
    public interface GetHandler {
        /**
         * If true, it will call {@link #action(java.awt.Component)} on this component.
         *
         * @param c the component
         * @return true or false.
         */
        boolean condition(Component c);

        /**
         * The action you want to perform on this component. This method will only be called if {@link
         * #condition(java.awt.Component)} returns true.
         *
         * @param c the component
         * @return the component that will be returned from {@link com.jidesoft.swing.JideSwingUtilities#getRecursively(java.awt.Component,
         * com.jidesoft.swing.JideSwingUtilities.GetHandler)}.
         */
        Component action(Component c);
    }

    /**
     * Calls the handler recursively on a component.
     *
     * @param c       component
     * @param handler handler to be called
     */
    public static void setRecursively(final Component c, final Handler handler) {
        setRecursively0(c, handler);
        handler.postAction(c);
    }

    private static void setRecursively0(final Component c, final Handler handler) {
        if (handler.condition(c)) {
            handler.action(c);
        }

        if (handler instanceof ConditionHandler && ((ConditionHandler) handler).stopCondition(c)) {
            return;
        }

        Component[] children = null;

        if (c instanceof JMenu) {
            children = ((JMenu) c).getMenuComponents();
        }
        else if (c instanceof JTabbedPane) {
            JTabbedPane tabbedPane = (JTabbedPane) c;
            children = new Component[tabbedPane.getTabCount()];
            for (int i = 0; i < children.length; i++) {
                children[i] = tabbedPane.getComponentAt(i);
            }
        }
        else if (c instanceof Container) {
            children = ((Container) c).getComponents();
        }
        if (children != null) {
            for (Component child : children) {
                setRecursively0(child, handler);
            }
        }
    }

    /**
     * Gets the first component inside the specified container that has the specified name.
     *
     * @param c    the container
     * @param name the name of the component
     * @return the component. Null if not found.
     */
    public static Component findFirstComponentByName(final Container c, final String name) {
        if (name != null && name.trim().length() != 0) {
            return getRecursively(c, new GetHandler() {
                @Override
                public boolean condition(Component c) {
                    return name.equals(c.getName());
                }

                @Override
                public Component action(Component c) {
                    return c;
                }
            });
        }
        else {
            return null;
        }
    }

    /**
     * Gets the first component inside the specified container that has the specified class.
     *
     * @param c     the container
     * @param clazz the class of the component
     * @return the component. Null if not found.
     */
    public static Component findFirstComponentByClass(final Container c, final Class<?> clazz) {
        if (clazz != null) {
            return getRecursively(c, new GetHandler() {
                @Override
                public boolean condition(Component c) {
                    return c.getClass().isAssignableFrom(clazz);
                }

                @Override
                public Component action(Component c) {
                    return c;
                }
            });
        }
        else {
            return null;
        }
    }

    /**
     * Gets to a child of a component recursively based on certain condition.
     *
     * @param c       component
     * @param handler handler to be called
     * @return the component that matches the condition specified in GetHandler.
     */
    public static Component getRecursively(final Component c, final GetHandler handler) {
        return getRecursively0(c, handler);
    }

    private static Component getRecursively0(final Component c, final GetHandler handler) {
        if (handler.condition(c)) {
            return handler.action(c);
        }

        Component[] children = null;

        if (c instanceof JMenu) {
            children = ((JMenu) c).getMenuComponents();
        }
        else if (c instanceof Container) {
            children = ((Container) c).getComponents();
        }

        if (children != null) {
            for (Component child : children) {
                Component result = getRecursively0(child, handler);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Calls setEnabled method recursively on component. <code>Component</code> c is usually a <code>Container</code>
     *
     * @param c       component
     * @param enabled true if enable; false otherwise
     */
    public static void setEnabledRecursively(final Component c, final boolean enabled) {
        setRecursively(c, new Handler() {
            public boolean condition(Component c) {
                return true;
            }

            public void action(Component c) {
                c.setEnabled(enabled);
            }

            public void postAction(Component c) {
            }
        });
    }

    /**
     * Calls putClientProperty method recursively on component and its child components as long as it is JComponent.
     *
     * @param c              component
     * @param clientProperty the client property name
     * @param value          the value for the client property
     */
    public static void putClientPropertyRecursively(final Component c, final String clientProperty, final Object value) {
        setRecursively(c, new Handler() {
            public boolean condition(Component c) {
                return c instanceof JComponent;
            }

            public void action(Component c) {
                ((JComponent) c).putClientProperty(clientProperty, value);
            }

            public void postAction(Component c) {
            }
        });
    }

    /**
     * Calls setRequestFocusEnabled method recursively on component. <code>Component</code> c is usually a
     * <code>Container</code>
     *
     * @param c       component
     * @param enabled true if setRequestFocusEnabled to true; false otherwise
     */
    public static void setRequestFocusEnabledRecursively(final Component c, final boolean enabled) {
        setRecursively(c, new Handler() {
            public boolean condition(Component c) {
                return true;
            }

            public void action(Component c) {
                if (c instanceof JComponent) {
                    ((JComponent) c).setRequestFocusEnabled(enabled);
                }
            }

            public void postAction(Component c) {
            }
        });
    }

    private static PropertyChangeListener _setOpaqueTrueListener;
    private static PropertyChangeListener _setOpaqueFalseListener;

    private static final String OPAQUE_LISTENER = "setOpaqueRecursively.opaqueListener";

    /**
     * setOpaqueRecursively method will make all child components opaque true or false. But if you call
     * jcomponent.putClientProperty(SET_OPAQUE_RECURSIVELY_EXCLUDED, Boolean.TRUE), we will not touch this particular
     * component when setOpaqueRecursively.
     */
    public static final String SET_OPAQUE_RECURSIVELY_EXCLUDED = "setOpaqueRecursively.excluded";

    /**
     * Calls setOpaque method recursively on each component except for JButton, JComboBox and JTextComponent.
     * <code>Component</code> c is usually a <code>Container</code>. If you would like certain child component not
     * affected by this call, you can call jcomponent.putClientProperty(SET_OPAQUE_RECURSIVELY_EXCLUDED, Boolean.TRUE)
     * before calling this method.
     *
     * @param c      component
     * @param opaque true if setOpaque to true; false otherwise
     */
    public static void setOpaqueRecursively(final Component c, final boolean opaque) {
        setRecursively(c, new Handler() {
            public boolean condition(Component c) {
                return !(c instanceof JComboBox || c instanceof JButton || c instanceof JTextComponent ||
                    c instanceof ListCellRenderer || c instanceof TreeCellRenderer || c instanceof TableCellRenderer || c instanceof CellEditor);
            }

            public void action(Component c) {
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    if (Boolean.TRUE.equals(jc.getClientProperty(SET_OPAQUE_RECURSIVELY_EXCLUDED))) {
                        return;
                    }

                    Object clientProperty = jc.getClientProperty(OPAQUE_LISTENER);
                    if (clientProperty != null) {
                        jc.removePropertyChangeListener("opaque", (PropertyChangeListener) clientProperty);
                        jc.putClientProperty(OPAQUE_LISTENER, null);
                    }
                    jc.setOpaque(opaque);
                    if (jc.getClientProperty(OPAQUE_LISTENER) == null) {
                        if (opaque) {
                            if (_setOpaqueTrueListener == null) {
                                _setOpaqueTrueListener = new PropertyChangeListener() {
                                    public void propertyChange(PropertyChangeEvent evt) {
                                        if (evt.getSource() instanceof JComponent) {
                                            Component component = ((Component) evt.getSource());
                                            component.removePropertyChangeListener("opaque", this);
                                            if (component instanceof JComponent) {
                                                ((JComponent) component).setOpaque(true);
                                            }
                                            component.addPropertyChangeListener("opaque", this);
                                        }
                                    }
                                };
                            }
                            jc.addPropertyChangeListener("opaque", _setOpaqueTrueListener);
                            jc.putClientProperty(OPAQUE_LISTENER, _setOpaqueTrueListener);
                        }
                        else {
                            if (_setOpaqueFalseListener == null) {
                                _setOpaqueFalseListener = new PropertyChangeListener() {
                                    public void propertyChange(PropertyChangeEvent evt) {
                                        if (evt.getSource() instanceof JComponent) {
                                            if (evt.getSource() instanceof JComponent) {
                                                Component component = ((Component) evt.getSource());
                                                component.removePropertyChangeListener("opaque", this);
                                                if (component instanceof JComponent) {
                                                    ((JComponent) component).setOpaque(false);
                                                }
                                                component.addPropertyChangeListener("opaque", this);
                                            }
                                        }
                                    }
                                };
                            }
                            jc.addPropertyChangeListener("opaque", _setOpaqueFalseListener);
                            jc.putClientProperty(OPAQUE_LISTENER, _setOpaqueFalseListener);
                        }
                    }
                }
            }

            public void postAction(Component c) {
            }
        });
    }

    public static Dimension getPreferredButtonSize(AbstractButton b, int textIconGap, boolean isHorizontal) {
        if (b.getComponentCount() > 0) {
            return null;
        }

        Icon icon = b.getIcon();
        String text = b.getText();

        Font font = b.getFont();
        FontMetrics fm = b.getFontMetrics(font);

        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        Rectangle viewR = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);

        layoutCompoundLabel(b, fm, text, icon, isHorizontal,
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewR, iconR, textR, (text == null ? 0 : textIconGap));

        /* The preferred size of the button is the size of
         * the text and icon rectangles plus the buttons insets.
         */

        Rectangle r = iconR.union(textR);

        Insets insets = b.getInsets();
        r.width += insets.left + insets.right;
        r.height += insets.top + insets.bottom;

        return r.getSize();
    }

    /**
     * Compute and return the location of the icons origin, the location of origin of the text baseline, and a possibly
     * clipped version of the compound labels string.  Locations are computed relative to the viewR rectangle. The
     * JComponents orientation (LEADING/TRAILING) will also be taken into account and translated into LEFT/RIGHT values
     * accordingly.
     *
     * @param c                      the component
     * @param fm                     the font metrics
     * @param text                   the text
     * @param icon                   the icon
     * @param isHorizontal           the flag indicating horizontal or vertical
     * @param verticalAlignment      vertical alignment model
     * @param horizontalAlignment    horizontal alignment model
     * @param verticalTextPosition   vertical text position
     * @param horizontalTextPosition horizontal text position
     * @param viewR                  view rectangle
     * @param iconR                  icon rectangle
     * @param textR                  text rectangle
     * @param textIconGap            the gap between the text and the gap
     * @return the string after layout.
     */
    public static String layoutCompoundLabel(JComponent c,
                                             FontMetrics fm,
                                             String text,
                                             Icon icon,
                                             boolean isHorizontal,
                                             int verticalAlignment,
                                             int horizontalAlignment,
                                             int verticalTextPosition,
                                             int horizontalTextPosition,
                                             Rectangle viewR,
                                             Rectangle iconR,
                                             Rectangle textR,
                                             int textIconGap) {
        boolean orientationIsLeftToRight = true;
        int hAlign = horizontalAlignment;
        int hTextPos = horizontalTextPosition;

        if (c != null) {
            if (!(c.getComponentOrientation().isLeftToRight())) {
                orientationIsLeftToRight = false;
            }
        }

        // Translate LEADING/TRAILING values in horizontalAlignment
        // to LEFT/RIGHT values depending on the components orientation
        switch (horizontalAlignment) {
            case LEADING:
                hAlign = (orientationIsLeftToRight) ? LEFT : RIGHT;
                break;
            case TRAILING:
                hAlign = (orientationIsLeftToRight) ? RIGHT : LEFT;
                break;
        }

        // Translate LEADING/TRAILING values in horizontalTextPosition
        // to LEFT/RIGHT values depending on the components orientation
        switch (horizontalTextPosition) {
            case LEADING:
                hTextPos = (orientationIsLeftToRight) ? LEFT : RIGHT;
                break;
            case TRAILING:
                hTextPos = (orientationIsLeftToRight) ? RIGHT : LEFT;
                break;
        }

        return layoutCompoundLabelImpl(c,
            fm,
            text,
            icon,
            isHorizontal,
            verticalAlignment,
            hAlign,
            verticalTextPosition,
            hTextPos,
            viewR,
            iconR,
            textR,
            textIconGap);
    }

    /**
     * Compute and return the location of the icons origin, the location of origin of the text baseline, and a possibly
     * clipped version of the compound labels string.  Locations are computed relative to the viewR rectangle. This
     * layoutCompoundLabel() does not know how to handle LEADING/TRAILING values in horizontalTextPosition (they will
     * default to RIGHT) and in horizontalAlignment (they will default to CENTER). Use the other version of
     * layoutCompoundLabel() instead.
     *
     * @param fm                     the font metrics
     * @param text                   the text to layout
     * @param icon                   the icon to layout
     * @param isHorizontal           if the layout is horizontal
     * @param verticalAlignment      the vertical alignment
     * @param horizontalAlignment    the horizontal alignment
     * @param verticalTextPosition   the vertical text position
     * @param horizontalTextPosition the horizontal text position
     * @param viewR                  the view rectangle
     * @param iconR                  the icon rectangle
     * @param textR                  the text rectangle
     * @param textIconGap            the gap between the text and the icon
     * @return the string after layout.
     */
    public static String layoutCompoundLabel(FontMetrics fm,
                                             String text,
                                             Icon icon,
                                             boolean isHorizontal,
                                             int verticalAlignment,
                                             int horizontalAlignment,
                                             int verticalTextPosition,
                                             int horizontalTextPosition,
                                             Rectangle viewR,
                                             Rectangle iconR,
                                             Rectangle textR,
                                             int textIconGap) {
        return layoutCompoundLabelImpl(null, fm, text, icon,
            isHorizontal,
            verticalAlignment,
            horizontalAlignment,
            verticalTextPosition,
            horizontalTextPosition,
            viewR, iconR, textR, textIconGap);
    }

    /*
     * Compute and return the location of the icons origin, the location of origin of the text baseline, and a possibly
     * clipped version of the compound labels string.  Locations are computed relative to the viewR rectangle. This
     * layoutCompoundLabel() does not know how to handle LEADING/TRAILING values in horizontalTextPosition (they will
     * default to RIGHT) and in horizontalAlignment (they will default to CENTER). Use the other version of
     * layoutCompoundLabel() instead.
     */

    private static String layoutCompoundLabelImpl(JComponent c,
                                                  FontMetrics fm,
                                                  String text,
                                                  Icon icon,
                                                  boolean isHorizontal,
                                                  int verticalAlignment,
                                                  int horizontalAlignment,
                                                  int verticalTextPosition,
                                                  int horizontalTextPosition,
                                                  Rectangle viewR,
                                                  Rectangle iconR,
                                                  Rectangle textR,
                                                  int textIconGap) {
        /* Initialize the icon bounds rectangle iconR.
         */
        if (isHorizontal) {
            return layoutCompoundLabelImplHorizontal(c,
                fm,
                text,
                icon,
                verticalAlignment,
                horizontalAlignment,
                verticalTextPosition,
                horizontalTextPosition,
                viewR,
                iconR,
                textR,
                textIconGap);
        }
        else {
            return layoutCompoundLabelImplVertical(c,
                fm,
                text,
                icon,
                verticalAlignment,
                horizontalAlignment,
                verticalTextPosition,
                horizontalTextPosition,
                viewR,
                iconR,
                textR,
                textIconGap);
        }

    }


    private static String getMaxLengthWord(String text) {
        if (text.indexOf(' ') == -1) {
            return text;
        }
        else {
            int minDiff = text.length();
            int minPos = -1;
            int mid = text.length() / 2;

            int pos = -1;
            while (true) {
                pos = text.indexOf(' ', pos + 1);
                if (pos == -1) {
                    break;
                }
                int diff = Math.abs(pos - mid);
                if (diff < minDiff) {
                    minDiff = diff;
                    minPos = pos;
                }
            }
            return minPos >= mid ? text.substring(0, minPos) : text.substring(minPos + 1);
        }
    }

    private static String layoutCompoundLabelImplHorizontal(JComponent c,
                                                            FontMetrics fm,
                                                            String text,
                                                            Icon icon,
                                                            int verticalAlignment,
                                                            int horizontalAlignment,
                                                            int verticalTextPosition,
                                                            int horizontalTextPosition,
                                                            Rectangle viewR,
                                                            Rectangle iconR,
                                                            Rectangle textR,
                                                            int textIconGap) {
        /* Initialize the icon bounds rectangle iconR.
         */

        if (icon != null) {
            iconR.width = icon.getIconWidth();
            iconR.height = icon.getIconHeight();
        }
        else {
            iconR.width = iconR.height = 0;
        }

        /* Initialize the text bounds rectangle textR.  If a null
         * or and empty String was specified we substitute "" here
         * and use 0,0,0,0 for textR.
         */

        boolean textIsEmpty = (text == null) || text.equals("");

        View v = null;
        if (textIsEmpty) {
            textR.width = textR.height = 0;
            text = "";
        }
        else {
            v = (c != null) ? (View) c.getClientProperty("html") : null;
            if (v != null) {
                textR.width = (int) v.getPreferredSpan(View.X_AXIS);
                textR.height = (int) v.getPreferredSpan(View.Y_AXIS);
            }
            else {
                if (false) { // TODO: debug switch
                    boolean wrapText = false;
                    if (verticalTextPosition == BOTTOM && horizontalTextPosition == CENTER) { // in this case, we will wrap the text into two lines
                        wrapText = true;
                    }

                    if (wrapText) {
                        textR.width = SwingUtilities.computeStringWidth(fm, getMaxLengthWord(text));
                        textR.height = fm.getHeight() + fm.getAscent() + 2; // gap between the two lines is 2.
                    }
                    else {
                        textR.width = SwingUtilities.computeStringWidth(fm, text) + 1; // add an extra pixel at the end of the text
                        textR.height = fm.getHeight();
                    }
                }
                else {
                    textR.width = SwingUtilities.computeStringWidth(fm, text); // add an extra pixel at the end of the text
                    textR.height = fm.getHeight();
                }
            }
        }

        /* Unless both text and icon are non-null, we effectively ignore
         * the value of textIconGap.  The code that follows uses the
         * value of gap instead of textIconGap.
         */

        int gap = (textIsEmpty || (icon == null)) ? 0 : textIconGap;

        if (!textIsEmpty) {

            /* If the label text string is too wide to fit within the available
             * space "..." and as many characters as will fit will be
             * displayed instead.
             */

            int availTextWidth;

            if (horizontalTextPosition == CENTER) {
                availTextWidth = viewR.width;
            }
            else {
                availTextWidth = viewR.width - (iconR.width + gap);
            }


            if (textR.width > availTextWidth) {
                if (v != null) {
                    textR.width = availTextWidth;
                }
                else {
                    String clipString = "...";
                    int totalWidth = SwingUtilities.computeStringWidth(fm, clipString);
                    int nChars;
                    for (nChars = 0; nChars < text.length(); nChars++) {
                        totalWidth += fm.charWidth(text.charAt(nChars));
                        if (totalWidth > availTextWidth) {
                            break;
                        }
                    }
                    text = text.substring(0, nChars) + clipString;
                    textR.width = SwingUtilities.computeStringWidth(fm, text);
                }
            }
        }

        /* Compute textR.x,y given the verticalTextPosition and
         * horizontalTextPosition properties
         */

        if (verticalTextPosition == TOP) {
            if (horizontalTextPosition != CENTER) {
                textR.y = 0;
            }
            else {
                textR.y = -(textR.height + gap);
            }
        }
        else if (verticalTextPosition == CENTER) {
            textR.y = (iconR.height >> 1) - (textR.height >> 1);
        }
        else { // (verticalTextPosition == BOTTOM)
            if (horizontalTextPosition != CENTER) {
                textR.y = iconR.height - textR.height;
            }
            else {
                textR.y = (iconR.height + gap);
            }
        }

        if (horizontalTextPosition == LEFT) {
            textR.x = -(textR.width + gap);
        }
        else if (horizontalTextPosition == CENTER) {
            textR.x = (iconR.width >> 1) - (textR.width >> 1);
        }
        else { // (horizontalTextPosition == RIGHT)
            textR.x = (iconR.width + gap);
        }

        /* labelR is the rectangle that contains iconR and textR.
         * Move it to its proper position given the labelAlignment
         * properties.
         *
         * To avoid actually allocating a Rectangle, Rectangle.union
         * has been inlined below.
         */
        int labelR_x = Math.min(iconR.x, textR.x);
        int labelR_width = Math.max(iconR.x + iconR.width,
            textR.x + textR.width) - labelR_x;
        int labelR_y = Math.min(iconR.y, textR.y);
        int labelR_height = Math.max(iconR.y + iconR.height,
            textR.y + textR.height) - labelR_y;

        int dx, dy;

        if (verticalAlignment == TOP) {
            dy = viewR.y - labelR_y;
        }
        else if (verticalAlignment == CENTER) {
            dy = (viewR.y + (viewR.height >> 1)) - (labelR_y + (labelR_height >> 1));
        }
        else { // (verticalAlignment == BOTTOM)
            dy = (viewR.y + viewR.height) - (labelR_y + labelR_height);
        }

        if (horizontalAlignment == LEFT) {
            dx = viewR.x - labelR_x;
        }
        else if (horizontalAlignment == RIGHT) {
            dx = (viewR.x + viewR.width) - (labelR_x + labelR_width);
        }
        else { // (horizontalAlignment == CENTER)
            dx = (viewR.x + (viewR.width >> 1)) -
                (labelR_x + (labelR_width >> 1));
        }

        /* Translate textR and glypyR by dx,dy.
         */

        textR.x += dx;
        textR.y += dy;

        iconR.x += dx;
        iconR.y += dy;

        return text;
    }

    private static String layoutCompoundLabelImplVertical(JComponent c,
                                                          FontMetrics fm,
                                                          String text,
                                                          Icon icon,
                                                          int verticalAlignment,
                                                          int horizontalAlignment,
                                                          int verticalTextPosition,
                                                          int horizontalTextPosition,
                                                          Rectangle viewR,
                                                          Rectangle iconR,
                                                          Rectangle textR,
                                                          int textIconGap) {
        /* Initialize the icon bounds rectangle iconR.
         */

        if (icon != null) {
            iconR.width = icon.getIconWidth();
            iconR.height = icon.getIconHeight();
        }
        else {
            iconR.width = iconR.height = 0;
        }

        /* Initialize the text bounds rectangle textR.  If a null
         * or and empty String was specified we substitute "" here
         * and use 0,0,0,0 for textR.
         */

        boolean textIsEmpty = (text == null) || text.equals("");

        View v = null;
        if (textIsEmpty) {
            textR.width = textR.height = 0;
            text = "";
        }
        else {
            v = (c != null) ? (View) c.getClientProperty("html") : null;
            if (v != null) {
                textR.height = (int) v.getPreferredSpan(View.X_AXIS);
                textR.width = (int) v.getPreferredSpan(View.Y_AXIS);
            }
            else {
                textR.height = SwingUtilities.computeStringWidth(fm, text);
                textR.width = fm.getHeight();
            }
        }

        /* Unless both text and icon are non-null, we effectively ignore
         * the value of textIconGap.  The code that follows uses the
         * value of gap instead of textIconGap.
         */

        int gap = (textIsEmpty || (icon == null)) ? 0 : textIconGap;

        if (!textIsEmpty) {

            /* If the label text string is too wide to fit within the available
             * space "..." and as many characters as will fit will be
             * displayed instead.
             */

            int availTextHeight;

            if (horizontalTextPosition == CENTER) {
                availTextHeight = viewR.height;
            }
            else {
                availTextHeight = viewR.height - (iconR.height + gap);
            }


            if (textR.height > availTextHeight) {
                if (v != null) {
                    textR.height = availTextHeight;
                }
                else {
                    String clipString = "...";
                    int totalHeight = SwingUtilities.computeStringWidth(fm, clipString);
                    int nChars;
                    for (nChars = 0; nChars < text.length(); nChars++) {
                        totalHeight += fm.charWidth(text.charAt(nChars));
                        if (totalHeight > availTextHeight) {
                            break;
                        }
                    }
                    text = text.substring(0, nChars) + clipString;
                    textR.height = SwingUtilities.computeStringWidth(fm, text);
                }
            }
        }

        /* Compute textR.x,y given the verticalTextPosition and
         * horizontalTextPosition properties
         */

        if (verticalTextPosition == TOP) {
            if (horizontalTextPosition != CENTER) {
                textR.x = 0;
            }
            else {
                textR.x = -(textR.width + gap);
            }
        }
        else if (verticalTextPosition == CENTER) {
            textR.y = (iconR.width >> 1) - (textR.width >> 1);
        }
        else { // (verticalTextPosition == BOTTOM)
            if (horizontalTextPosition != CENTER) {
                textR.x = iconR.width - textR.width;
            }
            else {
                textR.x = (iconR.width + gap);
            }
        }

        if (horizontalTextPosition == LEFT) {
            textR.y = -(textR.height + gap);
        }
        else if (horizontalTextPosition == CENTER) {
            textR.y = (iconR.height >> 1) - (textR.height >> 1);
        }
        else { // (horizontalTextPosition == RIGHT)
            textR.y = (iconR.height + gap);
        }

        /* labelR is the rectangle that contains iconR and textR.
         * Move it to its proper position given the labelAlignment
         * properties.
         *
         * To avoid actually allocating a Rectangle, Rectangle.union
         * has been inlined below.
         */
        int labelR_x = Math.min(iconR.y, textR.y);
        int labelR_width = Math.max(iconR.y + iconR.height,
            textR.y + textR.height) - labelR_x;
        int labelR_y = Math.min(iconR.x, textR.x);
        int labelR_height = Math.max(iconR.x + iconR.width,
            textR.x + textR.width) - labelR_y;

        int dx, dy;
        int dIcony; // because we will rotate icon, so the position will
        // be different from text. However after transform, they will be same

        if (verticalAlignment == TOP) {
            dy = viewR.x - labelR_y;
            dIcony = (viewR.x + viewR.width) - (labelR_y + labelR_height);
        }
        else if (verticalAlignment == CENTER) {
            dy = (viewR.x + (viewR.width >> 1)) - (labelR_y + (labelR_height >> 1));
            dIcony = dy;
        }
        else { // (verticalAlignment == BOTTOM)
            dy = (viewR.x + viewR.width) - (labelR_y + labelR_height);
            dIcony = viewR.x - labelR_y;
        }

        if (horizontalAlignment == LEFT) {
            dx = viewR.y - labelR_x;
        }
        else if (horizontalAlignment == RIGHT) {
            dx = (viewR.y + viewR.height) - (labelR_x + labelR_width);
        }
        else { // (horizontalAlignment == CENTER)
            dx = (viewR.y + (viewR.height >> 1)) -
                (labelR_x + (labelR_width >> 1));
        }

        /* Translate textR and iconR by dx,dy.
         */

        textR.y += dx;
        textR.x += dy;

        iconR.y += dx;
        iconR.x += dIcony;

        return text;
    }

    public static int getOrientationOf(Component component) {
        if (component instanceof Alignable) {
            return ((Alignable) component).getOrientation();
        }
        else if (component instanceof JComponent) {
            Integer value = (Integer) ((JComponent) component).getClientProperty(Alignable.PROPERTY_ORIENTATION);
            if (value != null) {
                return value;
            }
        }
        return HORIZONTAL;
    }

    public static void setOrientationOf(Component component, int orientation) {
        int old = getOrientationOf(component);
        if (orientation != old) {
            if (component instanceof Alignable) {
                ((Alignable) component).setOrientation(orientation);
            }
            else if (component instanceof JComponent) {
                ((JComponent) component).putClientProperty(Alignable.PROPERTY_ORIENTATION, orientation);
            }
        }
    }

    public static void setChildrenOrientationOf(Container c, int orientation) {
        Component[] components = c.getComponents();
        for (Component component : components) {
            setOrientationOf(component, orientation);
        }
    }

    /**
     * Disables the double buffered flag of the component and its children. The return map contains the components that
     * were double buffered. After this call, you can then restore the double buffered flag using {@link
     * #restoreDoubleBuffered(java.awt.Component, java.util.Map)} using the map that is returned from this method.
     *
     * @param c the parent container.
     * @return the map that contains all components that were double buffered.
     */
    public static Map<Component, Boolean> disableDoubleBuffered(final Component c) {
        final Map<Component, Boolean> map = new HashMap<Component, Boolean>();
        if (c instanceof JComponent) {
            JideSwingUtilities.setRecursively(c, new JideSwingUtilities.Handler() {
                public boolean condition(Component c) {
                    return c instanceof JComponent && c.isDoubleBuffered();
                }

                public void action(Component c) {
                    map.put(c, Boolean.TRUE);
                    ((JComponent) c).setDoubleBuffered(false);
                }

                public void postAction(Component c) {

                }
            });
        }
        return map;
    }

    /**
     * Enables the double buffered flag of the component and its children. The return map contains the components that
     * weren't double buffered. After this call, you can then restore the double buffered flag using {@link
     * #restoreDoubleBuffered(java.awt.Component, java.util.Map)} using the map that is returned from this method.
     *
     * @param c the parent container.
     * @return the map that contains all components that weren't double buffered.
     */
    public static Map<Component, Boolean> enableDoubleBuffered(final Component c) {
        final Map<Component, Boolean> map = new HashMap<Component, Boolean>();
        if (c instanceof JComponent) {
            JideSwingUtilities.setRecursively(c, new JideSwingUtilities.Handler() {
                public boolean condition(Component c) {
                    return c instanceof JComponent && !c.isDoubleBuffered();
                }

                public void action(Component c) {
                    map.put(c, Boolean.FALSE);
                    ((JComponent) c).setDoubleBuffered(true);
                }

                public void postAction(Component c) {

                }
            });
        }
        return map;
    }

    /**
     * Restores the double buffered flag of the component and its children. Only components that are in the map will be
     * changed.
     *
     * @param c   the parent container.
     * @param map a map maps from component to a boolean. If the boolean is true, it means the component was double
     *            buffered bore. Otherwise, not double buffered.
     */
    public static void restoreDoubleBuffered(final Component c, final Map<Component, Boolean> map) {
        JideSwingUtilities.setRecursively(c, new JideSwingUtilities.Handler() {
            public boolean condition(Component c) {
                return c instanceof JComponent;
            }

            public void action(Component c) {
                Boolean value = map.get(c);
                if (value != null) {
                    ((JComponent) c).setDoubleBuffered(Boolean.TRUE.equals(value));
                }
            }

            public void postAction(Component c) {
            }
        });
    }

    public static void paintBackground(Graphics g, Rectangle rect, Color border, Color bk) {
        Color old = g.getColor();
        g.setColor(bk);
        g.fillRect(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2);
        g.setColor(border);
        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
        g.setColor(old);
    }

    public static void paintBackground(Graphics2D g2d, Rectangle rect, Color border, Paint paint) {
        Color old = g2d.getColor();
        g2d.setPaint(paint);
        g2d.fillRect(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2);
        g2d.setColor(border);
        g2d.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
        g2d.setColor(old);
    }

    /**
     * Get rendering hints from a Graphics instance. "hintsToSave" is a Map of RenderingHint key-values. For each hint
     * key present in that map, the value of that hint is obtained from the Graphics and stored as the value for the key
     * in savedHints.
     */
    private static Map getRenderingHints(Graphics2D g2d,
                                         Map hintsToSave,
                                         Map savedHints) {
        if (savedHints == null) {
            savedHints = new RenderingHints(null);
        }
        else {
            savedHints.clear();
        }
        if (hintsToSave == null || hintsToSave.size() == 0) {
            return savedHints;
        }
        /* RenderingHints.keySet() returns Set*/
        Set objects = hintsToSave.keySet();
        for (Object o : objects) {
            RenderingHints.Key key = (RenderingHints.Key) o;
            Object value = g2d.getRenderingHint(key);
            if (value != null) {
                savedHints.put(key, value);
            }
        }

        return savedHints;
    }

    /**
     * Restores the old setting for text anti-alias.
     *
     * @param c
     * @param g
     * @param oldHints the value returned from {@link #setupAntialiasing(java.awt.Component, java.awt.Graphics)}.
     */
    public static void restoreAntialiasing(Component c, Graphics g, Object oldHints) {
        Graphics2D g2d = (Graphics2D) g;
        if (SystemInfo.isJdk6Above()) {
            if (oldHints instanceof RenderingHints) {
                g2d.addRenderingHints((RenderingHints) oldHints);
            }
        }
        else {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldHints);
        }
    }

    /**
     * Setups the graphics to draw shape using anti-alias.
     *
     * @param g
     * @return the old hints. You will need this value as the third parameter in {@link
     * #restoreShapeAntialiasing(java.awt.Graphics, Object)}.
     */
    public static Object setupShapeAntialiasing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Object oldHints = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return oldHints;
    }

    /**
     * Restores the old setting for shape anti-alias.
     *
     * @param g
     * @param oldHints the value returned from {@link #setupShapeAntialiasing(java.awt.Graphics)}.
     */
    public static void restoreShapeAntialiasing(Graphics g, Object oldHints) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHints);
    }

    public static void drawGrip(Graphics g, Rectangle rectangle, int maxLength, int maxThickness) {
        drawGrip(g, rectangle, maxLength, maxThickness, true);
    }

    public static void drawGrip(Graphics g, Rectangle rectangle, int maxLength, int maxThickness, boolean isSelected) {
        if (rectangle.width > rectangle.height) {
            int count = maxLength;
            if (maxLength * 3 > rectangle.width) {
                count = rectangle.width / 3;
            }
            int startX = rectangle.x + ((rectangle.width - (count * 3)) >> 1);
            int startY = rectangle.y + ((rectangle.height - (maxThickness * 3)) >> 1);
            for (int i = 0; i < maxThickness; i++) {
                for (int j = 0; j < count; j++) {
                    if (isSelected) {
                        g.setColor(UIDefaultsLookup.getColor("controlLtHighlight"));
                        g.drawLine(startX + j * 3, startY + i * 3, startX + j * 3, startY + i * 3);
                    }
                    g.setColor(UIDefaultsLookup.getColor("controlShadow"));
                    g.drawLine(startX + j * 3 + 1, startY + i * 3 + 1, startX + j * 3 + 1, startY + i * 3 + 1);
                }
            }
        }
        else {
            int count = maxLength;
            if (maxLength * 3 > rectangle.height) {
                count = rectangle.height / 3;
            }
            int startX = rectangle.x + ((rectangle.width - (maxThickness * 3)) >> 1);
            int startY = rectangle.y + ((rectangle.height - (count * 3)) >> 1);
            for (int i = 0; i < maxThickness; i++) {
                for (int j = 0; j < count; j++) {
                    if (isSelected) {
                        g.setColor(UIDefaultsLookup.getColor("controlLtHighlight"));
                        g.drawLine(startX + i * 3, startY + j * 3, startX + i * 3, startY + j * 3);
                    }
                    g.setColor(UIDefaultsLookup.getColor("controlShadow"));
                    g.drawLine(startX + i * 3 + 1, startY + j * 3 + 1, startX + i * 3 + 1, startY + j * 3 + 1);
                }
            }
        }
    }

    /**
     * containerContainsFocus, does the specified container contain the current focusOwner?
     *
     * @param cont the specified container
     * @return Is the current focusOwner a descendant of the specified container, or the container itself?
     */

    public static boolean containerContainsFocus(Container cont) {
        Component focusOwner =
            KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        Component permFocusOwner =
            KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        boolean focusOwned;
        focusOwned = ((focusOwner != null) && SwingUtilities.isDescendingFrom(focusOwner, cont));
        if (!focusOwned) {
            focusOwned = ((permFocusOwner != null) &&
                SwingUtilities.isDescendingFrom(permFocusOwner, cont));
        }
        return focusOwned;
    }

//<syd_0002>

    public static boolean componentIsPermanentFocusOwner(Component comp) {
        return ((comp != null) && (KeyboardFocusManager.getCurrentKeyboardFocusManager().
            getPermanentFocusOwner() == comp));
    }

//</syd_0002>

    public static void installColorsAndFont(Component c,
                                            Color background,
                                            Color foreground,
                                            Font font) {
        installFont(c, font);
        installColors(c, background, foreground);
    }

    public static void installFont(Component c, Font font) {
        Font f = c.getFont();
        if (f == null || f instanceof UIResource) {
            c.setFont(font);
        }
    }

    public static void installColors(Component c,
                                     Color background, Color foreground) {
        Color bg = c.getBackground();
        if (background != null && (bg == null || bg instanceof UIResource)) {
            c.setBackground(background);
        }

        Color fg = c.getForeground();
        if (foreground != null && (fg == null || fg instanceof UIResource)) {
            c.setForeground(foreground);
        }
    }

    public static void installBorder(JComponent c, Border defaultBorder) {
        Border border = c.getBorder();
        if (border == null || border instanceof UIResource) {
            c.setBorder(defaultBorder);
        }
    }

    public static void fillNormalGradient(Graphics2D g2d, Shape s, Color startColor, Color endColor, boolean isVertical) {
        Rectangle rect = s.getBounds();
        GradientPaint paint;
        if (isVertical) {
            paint = new GradientPaint(rect.x, rect.y, startColor, rect.x, rect.height + rect.y, endColor, true); // turn cyclic to true will be faster
        }
        else {
            paint = new GradientPaint(rect.x, rect.y, startColor, rect.width + rect.x, rect.y, endColor, true);  // turn cyclic to true will be faster
        }
        Paint old = g2d.getPaint();
        g2d.setPaint(paint);
        g2d.fill(s);
        g2d.setPaint(old);
    }


    /**
     * Gets the top modal dialog of current window.
     *
     * @param w
     * @return the top modal dialog of current window.
     */
    public static Window getTopModalDialog(Window w) {
        Window[] ws = w.getOwnedWindows();
        for (Window w1 : ws) {
            if (w1.isVisible() && w1 instanceof Dialog && ((Dialog) w1).isModal()) {
                return (getTopModalDialog(w1));
            }
        }
        return w;
    }

    protected static boolean tracingFocus = false;

    /**
     * For internal usage only.
     */
    public static void traceFocus() {
        traceFocus(false);
    }

    /**
     * For internal usage only.
     */
    public static void traceFocus(final boolean useBorders) {
        if (tracingFocus) {
            return;
        }
        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (useBorders) {
                    Component oldValue = (Component) evt.getOldValue();
                    if (oldValue instanceof JComponent) {
                        Border oldBorder = ((JComponent) oldValue).getBorder();
                        if (oldBorder instanceof TraceDebugBorder) {
                            ((JComponent) oldValue).setBorder(((TraceDebugBorder) oldBorder).getInsideBorder());
                        }
                    }

                    Component newValue = (Component) evt.getNewValue();
                    if (newValue instanceof JComponent) {
                        Border oldBorder = ((JComponent) newValue).getBorder();
                        if (oldBorder == null) {
                            oldBorder = new EmptyBorder(0, 0, 0, 0);
                        }
                        if (!(oldBorder instanceof TraceDebugBorder)) {
                            ((JComponent) newValue).setBorder(new TraceDebugBorder(oldBorder));
                        }
                    }
                }

                String oldName = evt.getOldValue() == null ? "null" : evt.getOldValue().getClass().getName();
                if (evt.getOldValue() instanceof Component && ((Component) evt.getOldValue()).getName() != null) {
                    oldName = oldName + "'" + ((Component) evt.getOldValue()).getName() + "'";
                }
                String newName = evt.getNewValue() == null ? "null" : evt.getNewValue().getClass().getName();
                if (evt.getNewValue() instanceof Component && ((Component) evt.getNewValue()).getName() != null) {
                    newName = newName + "'" + ((Component) evt.getNewValue()).getName() + "'";
                }

                System.out.println(evt.getPropertyName() + ": " + oldName + " ==> " + newName);
            }
        };
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", listener);
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", listener);
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow", listener);
        tracingFocus = true;
    }

    public static class TraceDebugBorder extends CompoundBorder {
        private static final long serialVersionUID = -1396250213346461982L;

        public TraceDebugBorder(Border insideBorder) {
            super(BorderFactory.createLineBorder(Color.RED, 1), insideBorder);
        }

        public Insets getBorderInsets(Component c) {
            return getInsideBorder().getBorderInsets(c);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            return getInsideBorder().getBorderInsets(c);
        }
    }

    /**
     * Find some subcomponent of the specified container that will accept focus.
     * <p/>
     * Note that this doesn't do something smart like trying to walk the hierarchy horizontally at each level so that
     * the focused subcomponent is as high as possible. Rather, it drills vertically. It's just a safety valve so that
     * focus can be requested somewhere rather than being lost.
     *
     * @param container
     * @return a focusable subcomponent
     */
    public static Component findSomethingFocusable(Container container) {
        if (passesFocusabilityTest(container)) {
            container.requestFocusInWindow();
            return container;
        }
        Component[] comps;
        Component comp;
        comps = container.getComponents();
        for (Component comp1 : comps) {
            if (passesFocusabilityTest(comp1)) {
                container.requestFocusInWindow();
                return container;
            }
            else if (comp1 instanceof Container) {
                comp = findSomethingFocusable((Container) (comp1));
                if (comp != null) {
                    return comp;
                }
            }
        }
        return null;
    }

    /**
     * There are four standard tests which determine if Swing will be able to request focus for a component. Test them.
     *
     * @param comp
     * @return does the specified component pass the four focusability tests
     */
    public static boolean passesFocusabilityTest(Component comp) {
        return ((comp != null) &&
            comp.isEnabled() && comp.isDisplayable() &&
            comp.isVisible() && comp.isFocusable() && comp.isShowing());
    }

    /**
     * Ignore the exception. This method does nothing. However it's a good practice to use this method so that we can
     * easily find out the place that ignoring exception. In development phase, we can log a message in this method so
     * that we can verify if it makes sense to ignore.
     *
     * @param e
     */
    public static void ignoreException(Exception e) {
    }

    /**
     * Prints out the message of the exception.
     *
     * @param e
     */
    public static void printException(Exception e) {
        System.err.println(e.getLocalizedMessage());
    }

    /**
     * Throws the exception. If the exception is RuntimeException, just throw it. Otherwise, wrap it in RuntimeException
     * and throw it.
     *
     * @param e
     */
    public static void throwException(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        else {
            throw new RuntimeException(e);
        }
    }

    /**
     * Throws the InvocationTargetException. Usually InvocationTargetException has a nested exception as target
     * exception. If the target exception is a RuntimeException or Error, we will throw it. Otherwise, we will wrap it
     * inside RuntimeException and throw it.
     *
     * @param e
     */
    public static void throwInvocationTargetException(InvocationTargetException e) {
        // in most cases, target exception will be RuntimeException
        // but to be on safer side (it may be Error) we explicitly check it
        if (e.getTargetException() instanceof RuntimeException) {
            throw (RuntimeException) e.getTargetException();
        }
        else if (e.getTargetException() instanceof Error) {
            throw (Error) e.getTargetException();
        }
        else {
            throw new RuntimeException(e.getTargetException());
        }
    }

    public static int findDisplayedMnemonicIndex(String text, int mnemonic) {
        if (text == null || mnemonic == '\0') {
            return -1;
        }

        char uc = Character.toUpperCase((char) mnemonic);
        char lc = Character.toLowerCase((char) mnemonic);

        int uci = text.indexOf(uc);
        int lci = text.indexOf(lc);

        if (uci == -1) {
            return lci;
        }
        else if (lci == -1) {
            return uci;
        }
        else {
            return (lci < uci) ? lci : uci;
        }
    }

    /**
     * Gets the first occurrence of the component with specified type in the container. It used deep-first searching to
     * find it.
     *
     * @param c
     * @param container
     * @return the first occurrence of the component with specified type in the container. Null if nothing is found.
     */
    public static Component getDescendantOfClass(Class c, Container container) {
        if (container == null || c == null) {
            return null;
        }

        Component[] components = container.getComponents();

        for (Component component : components) {
            if (c.isInstance(component)) {
                return component;
            }
            if (component instanceof Container) {
                Component found = getDescendantOfClass(c, (Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Draws a border based on an image. The image can be divided into nine different areas. Each area size is
     * determined by the insets.
     */
    public static void drawImageBorder(Graphics g, ImageIcon img, Rectangle rect, Insets ins, boolean drawCenter) {
        int left = ins.left;
        int right = ins.right;
        int top = ins.top;
        int bottom = ins.bottom;
        int x = rect.x;
        int y = rect.y;
        int w = rect.width;
        int h = rect.height;

// top
        g.drawImage(img.getImage(), x, y, x + left, y + top,
            0, 0, left, top, null);
        g.drawImage(img.getImage(), x + left, y, x + w - right, y + top,
            left, 0, img.getIconWidth() - right, top, null);
        g.drawImage(img.getImage(), x + w - right, y, x + w, y + top,
            img.getIconWidth() - right, 0, img.getIconWidth(), top, null);

// middle
        g.drawImage(img.getImage(), x, y + top, x + left, y + h - bottom,
            0, top, left, img.getIconHeight() - bottom, null);
        g.drawImage(img.getImage(), x + left, y + top, x + w - right, y + h - bottom,
            left, top, img.getIconWidth() - right, img.getIconHeight() - bottom, null);
        g.drawImage(img.getImage(), x + w - right, y + top, x + w, y + h - bottom,
            img.getIconWidth() - right, top, img.getIconWidth(), img.getIconHeight() - bottom, null);

// bottom
        g.drawImage(img.getImage(), x, y + h - bottom, x + left, y + h,
            0, img.getIconHeight() - bottom, left, img.getIconHeight(), null);
        g.drawImage(img.getImage(), x + left, y + h - bottom, x + w - right, y + h,
            left, img.getIconHeight() - bottom, img.getIconWidth() - right, img.getIconHeight(), null);
        g.drawImage(img.getImage(), x + w - right, y + h - bottom, x + w, y + h,
            img.getIconWidth() - right, img.getIconHeight() - bottom, img.getIconWidth(), img.getIconHeight(), null);

        if (drawCenter) {
            g.drawImage(img.getImage(), x + left, y + top, x + w - right, y + h - bottom,
                left, top, img.getIconWidth() - right, img.getIconHeight() - bottom, null);
        }
    }

    /**
     * Copied from BasicLookAndFeel as the method is package local.
     *
     * @param component
     * @return if request focus is success or not.
     */
    public static boolean compositeRequestFocus(Component component) {
        LOGGER_FOCUS.fine("compositeRequestFocus " + component);
        if (component instanceof Container) {
            LOGGER_FOCUS.fine("compositeRequestFocus " + "is container.");
            Container container = (Container) component;
            if (container.isFocusCycleRoot()) {
                LOGGER_FOCUS.fine("compositeRequestFocus " + "is focuscycleroot.");
                FocusTraversalPolicy policy = container.getFocusTraversalPolicy();
                Component comp = policy.getDefaultComponent(container);
                LOGGER_FOCUS.fine("compositeRequestFocus " + "default component = " + comp);

                if ((comp != null) && comp.isShowing() && container.getComponentCount() > 0) {
                    LOGGER_FOCUS.fine("compositeRequestFocus " + "default component passesFocusabilityTest =" + passesFocusabilityTest(comp));
                    LOGGER_FOCUS.fine("compositeRequestFocus " + "requestFocus for " + comp);
                    return comp.requestFocusInWindow();
                }
            }
            Container rootAncestor = container.getFocusCycleRootAncestor();
            if (rootAncestor != null) {
                LOGGER_FOCUS.fine("compositeRequestFocus " + "using rootAncestor =" + rootAncestor);
                FocusTraversalPolicy policy = rootAncestor.getFocusTraversalPolicy();
                Component comp = null;
                try {
                    comp = policy.getComponentAfter(rootAncestor, container);
                }
                catch (Exception e) {
                    // ClassCastException when docking frames on Solaris
                    // http://jidesoft.com/forum/viewtopic.php?p=32569
                }

                LOGGER_FOCUS.fine("compositeRequestFocus " + "getComponentAfter =" + comp);
                if (comp != null && SwingUtilities.isDescendingFrom(comp, container)) {
                    LOGGER_FOCUS.fine("compositeRequestFocus " + "getComponentAfter passesFocusabilityTest =" + passesFocusabilityTest(comp));
                    LOGGER_FOCUS.fine("compositeRequestFocus " + "requestFocus for " + comp);
                    return comp.requestFocusInWindow();
                }
            }
        }
        if (!passesFocusabilityTest(component)) {
            LOGGER_FOCUS.fine("compositeRequestFocus " + "returingfalse because !passesFocusabilityTest" + component);
            return false;
        }

        LOGGER_FOCUS.fine("compositeRequestFocus " + "component=" + component);
        return component.requestFocusInWindow();
    }

    public static boolean isAncestorOfFocusOwner(Component component) {
        boolean hasFocus = false;
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (component == focusOwner || (component instanceof Container && ((Container) component).isAncestorOf(focusOwner))) {
            hasFocus = true;
        }
        return hasFocus;
    }

    /**
     * Gets the top level Dialog or Frame of the component.
     *
     * @param parentComponent
     * @return the top level Frame or Dialog. Null if we didn't find an ancestor which is instance of Frame.
     */
    public static Window getWindowForComponent(Component parentComponent)
        throws HeadlessException {
        if (parentComponent == null) {
            return JOptionPane.getRootFrame();
        }
        if (parentComponent instanceof Frame || parentComponent instanceof Dialog) {
            return (Window) parentComponent;
        }
        return getWindowForComponent(parentComponent.getParent());
    }

    /**
     * Checks if the key listener is already registered on the component.
     *
     * @param component the component
     * @param l         the listener
     * @return true if already registered. Otherwise false.
     */
    public static boolean isKeyListenerRegistered(Component component, KeyListener l) {
        KeyListener[] listeners = component.getKeyListeners();
        for (KeyListener listener : listeners) {
            if (listener == l) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts the key listener at the particular index in the listeners' chain.
     *
     * @param component
     * @param l
     * @param index
     */
    public static void insertKeyListener(Component component, KeyListener l, int index) {
        KeyListener[] listeners = component.getKeyListeners();
        for (KeyListener listener : listeners) {
            component.removeKeyListener(listener);
        }
        for (int i = 0; i < listeners.length; i++) {
            KeyListener listener = listeners[i];
            if (index == i) {
                component.addKeyListener(l);
            }
            component.addKeyListener(listener);
        }
        // index is too large, add to the end.
        if (index > listeners.length - 1) {
            component.addKeyListener(l);
        }
    }

    /**
     * Inserts the table model listener at the particular index in the listeners' chain. The listeners are fired in
     * reverse order. So the listener at index 0 will be fired at last.
     *
     * @param model the AbstractTableModel
     * @param l     the TableModelListener to be inserted
     * @param index the index.
     */
    public static void insertTableModelListener(TableModel model, TableModelListener l, int index) {
        if (!(model instanceof AbstractTableModel)) {
            model.addTableModelListener(l);
            return;
        }
        TableModelListener[] listeners = ((AbstractTableModel) model).getTableModelListeners();
        for (TableModelListener listener : listeners) {
            model.removeTableModelListener(listener);
        }
        for (int i = 0; i < listeners.length; i++) {
            TableModelListener listener = listeners[i];
            if (index == i) {
                model.addTableModelListener(l);
            }
            model.addTableModelListener(listener);
        }
        // index is too large, add to the end.
        if (index < 0 || index > listeners.length - 1) {
            model.addTableModelListener(l);
        }
    }

    /**
     * Inserts the property change listener at the particular index in the listeners' chain.
     *
     * @param component    the component where the listener will be inserted.
     * @param l            the listener to be inserted
     * @param propertyName the name of the property. Could be null.
     * @param index        the index to be inserted
     */
    public static void insertPropertyChangeListener(Component component, PropertyChangeListener l, String propertyName, int index) {
        PropertyChangeListener[] listeners = propertyName == null ? component.getPropertyChangeListeners() : component.getPropertyChangeListeners(propertyName);
        for (PropertyChangeListener listener : listeners) {
            if (propertyName == null) {
                component.removePropertyChangeListener(listener);
            }
            else {
                component.removePropertyChangeListener(propertyName, listener);
            }
        }
        for (int i = 0; i < listeners.length; i++) {
            PropertyChangeListener listener = listeners[i];
            if (index == i) {
                if (propertyName == null) {
                    component.addPropertyChangeListener(l);
                }
                else {
                    component.addPropertyChangeListener(propertyName, l);
                }
            }
            if (propertyName == null) {
                component.addPropertyChangeListener(listener);
            }
            else {
                component.addPropertyChangeListener(propertyName, listener);
            }
        }
        // index is too large, add to the end.
        if (index > listeners.length - 1) {
            if (propertyName == null) {
                component.addPropertyChangeListener(l);
            }
            else {
                component.addPropertyChangeListener(propertyName, l);
            }
        }
    }

    /**
     * Inserts the property change listener at the particular index in the listeners' chain.
     *
     * @param manager      the KeyboardFocusManager where the listener will be inserted.
     * @param l            the listener to be inserted
     * @param propertyName the name of the property. Could be null.
     * @param index        the index to be inserted
     */
    public static void insertPropertyChangeListener(KeyboardFocusManager manager, PropertyChangeListener l, String propertyName, int index) {
        PropertyChangeListener[] listeners = propertyName == null ? manager.getPropertyChangeListeners() : manager.getPropertyChangeListeners(propertyName);
        for (PropertyChangeListener listener : listeners) {
            if (propertyName == null) {
                manager.removePropertyChangeListener(listener);
            }
            else {
                manager.removePropertyChangeListener(propertyName, listener);
            }
        }
        for (int i = 0; i < listeners.length; i++) {
            PropertyChangeListener listener = listeners[i];
            if (index == i) {
                if (propertyName == null) {
                    manager.addPropertyChangeListener(l);
                }
                else {
                    manager.addPropertyChangeListener(propertyName, l);
                }
            }
            if (propertyName == null) {
                manager.addPropertyChangeListener(listener);
            }
            else {
                manager.addPropertyChangeListener(propertyName, listener);
            }
        }
        // index is too large, add to the end.
        if (index > listeners.length - 1) {
            if (propertyName == null) {
                manager.addPropertyChangeListener(l);
            }
            else {
                manager.addPropertyChangeListener(propertyName, l);
            }
        }
    }

    /**
     * Checks if the property change listener is already registered on the component.
     *
     * @param component the component
     * @param l         the listener
     * @return true if already registered. Otherwise false.
     */
    public static boolean isPropertyChangeListenerRegistered(Component component, PropertyChangeListener l) {
        PropertyChangeListener[] listeners = component.getPropertyChangeListeners();
        for (PropertyChangeListener listener : listeners) {
            if (listener == l) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the property change listener is already registered on the component.
     *
     * @param component    the component
     * @param propertyName the property name
     * @param l            the listener
     * @return true if already registered. Otherwise false.
     */
    public static boolean isPropertyChangeListenerRegistered(Component component, String propertyName, PropertyChangeListener l) {
        if (propertyName == null) {
            return isPropertyChangeListenerRegistered(component, l);
        }
        PropertyChangeListener[] listeners = component.getPropertyChangeListeners(propertyName);
        for (PropertyChangeListener listener : listeners) {
            if (listener == l) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the mouse listener is already registered on the component.
     *
     * @param component the component
     * @param l         the listener
     * @return true if already registered. Otherwise false.
     */
    public static boolean isMouseListenerRegistered(Component component, MouseListener l) {
        MouseListener[] listeners = component.getMouseListeners();
        for (MouseListener listener : listeners) {
            if (listener == l) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts the mouse listener at the particular index in the listeners' chain.
     *
     * @param component
     * @param l
     * @param index
     */
    public static void insertMouseListener(Component component, MouseListener l, int index) {
        MouseListener[] listeners = component.getMouseListeners();
        for (MouseListener listener : listeners) {
            component.removeMouseListener(listener);
        }
        for (int i = 0; i < listeners.length; i++) {
            MouseListener listener = listeners[i];
            if (index == i) {
                component.addMouseListener(l);
            }
            component.addMouseListener(listener);
        }
        // index is too large, add to the end.
        if (index < 0 || index > listeners.length - 1) {
            component.addMouseListener(l);
        }
    }

    /**
     * Checks if the mouse motion listener is already registered on the component.
     *
     * @param component the component
     * @param l         the listener
     * @return true if already registered. Otherwise false.
     */
    public static boolean isMouseMotionListenerRegistered(Component component, MouseMotionListener l) {
        MouseMotionListener[] listeners = component.getMouseMotionListeners();
        for (MouseMotionListener listener : listeners) {
            if (listener == l) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts the mouse motion listener at the particular index in the listeners' chain.
     *
     * @param component
     * @param l
     * @param index
     */
    public static void insertMouseMotionListener(Component component, MouseMotionListener l, int index) {
        MouseMotionListener[] listeners = component.getMouseMotionListeners();
        for (MouseMotionListener listener : listeners) {
            component.removeMouseMotionListener(listener);
        }
        for (int i = 0; i < listeners.length; i++) {
            MouseMotionListener listener = listeners[i];
            if (index == i) {
                component.addMouseMotionListener(l);
            }
            component.addMouseMotionListener(listener);
        }
        // index is too large, add to the end.
        if (index < 0 || index > listeners.length - 1) {
            component.addMouseMotionListener(l);
        }
    }

    /**
     * Gets the scroll pane around the component.
     *
     * @param innerComponent
     * @return the scroll pane. Null if the component is not in any JScrollPane.
     */
    public static Component getScrollPane(Component innerComponent) {
        Component component = innerComponent;
        if (innerComponent instanceof JScrollPane) {
            return innerComponent;
        }
        if (component.getParent() != null && component.getParent().getParent() != null && component.getParent().getParent() instanceof JScrollPane) {
            component = component.getParent().getParent();
            return component;
        }
        else {
            return null;
        }
    }

    /**
     * Checks if the listener is always registered to the EventListenerList to avoid duplicated registration of the same
     * listener
     *
     * @param list the EventListenerList to register the listener.
     * @param t    the type of the EventListener.
     * @param l    the listener.
     * @return true if already registered. Otherwise false.
     */
    public static boolean isListenerRegistered(EventListenerList list, Class t, EventListener l) {
        Object[] objects = list.getListenerList();
        return isListenerRegistered(objects, t, l);
    }

    /**
     * Checks if the listener is always registered to the Component to avoid duplicated registration of the same
     * listener
     *
     * @param component the component that you want to register the listener.
     * @param t         the type of the EventListener.
     * @param l         the listener.
     * @return true if already registered. Otherwise false.
     */
    public static boolean isListenerRegistered(Component component, Class t, EventListener l) {
        Object[] objects = component.getListeners(t);
        return isListenerRegistered(objects, t, l);
    }

    private static boolean isListenerRegistered(Object[] objects, Class t, EventListener l) {
        for (int i = objects.length - 2; i >= 0; i -= 2) {
            if ((objects[i] == t) && (objects[i + 1].equals(l))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the first child of the component that is the specified type.
     *
     * @param clazz the type of the component to look for
     * @param c     the component
     * @return the first child of the component that is the specified type.
     */
    public static Component getFirstChildOf(final Class<?> clazz, Component c) {
        return getRecursively(c, new GetHandler() {
            public boolean condition(Component c) {
                return clazz.isAssignableFrom(c.getClass());
            }

            public Component action(Component c) {
                return c;
            }
        });
    }

    /**
     * Get the index of the component in the container. It will return -1 if c's parent is not container.
     *
     * @param container the container
     * @param c         the component
     * @return the index
     */
    public static int getComponentIndex(Container container, Component c) {
        if (c.getParent() != container) {
            return -1;
        }
        Component[] children = container.getComponents();
        for (int i = 0; i < children.length; i++) {
            if (children[i] == c) {
                return i;
            }
        }
        return -1;
    }


    public static Vector convertDefaultComboBoxModelToVector(DefaultComboBoxModel model) {
        Vector v = new Vector();
        for (int i = 0; i < model.getSize(); i++) {
            v.add(model.getElementAt(i));
        }
        return v;

    }

    /**
     * To make sure the row is visible. If the table's horizontal scroll bar is visible, the method will not change the
     * horizontal scroll bar's position.
     *
     * @param table
     * @param row
     */
    public static void ensureRowVisible(JTable table, int row) {
        Rectangle r = table.getVisibleRect();
// Hack! make above and below visible if necessary
// TODO: how to center it or make it the first?
        Rectangle rMid = table.getCellRect(row, 0, true);
        Rectangle rBefore = null, rAfter = null;
        if (row < table.getModel().getRowCount() - 1) {
            rAfter = table.getCellRect(row + 1, 0, true);
        }
        if (row > 0) {
            rBefore = table.getCellRect(row - 1, 0, true);
        }

        int yLow = (int) rMid.getMinY();
        int yHi = (int) rMid.getMaxY();
        int xLow = r.x;
        int xHi = r.x + r.width;

        if (rBefore != null) {
            yLow = (int) rBefore.getMinY();
        }

        if (rAfter != null) {
            yHi = (int) rAfter.getMaxY();
        }

        Rectangle rScrollTo = new Rectangle(xLow, yLow, xHi - xLow, yHi - yLow);
        if (!r.contains(rScrollTo) && rScrollTo.height != 0) {
            table.scrollRectToVisible(rScrollTo);
        }
    }

    public static void retargetMouseEvent(int id, MouseEvent e, Component target) {
        if (target == null || (target == e.getSource() && id == e.getID())) {
            return;
        }
        if (e.isConsumed()) {
            return;
        }

        // fix for bug #4202966 -- hania
        // When re-targeting a mouse event, we need to translate
        // the event's coordinates relative to the target.

        Point p = SwingUtilities.convertPoint((Component) e.getSource(),
            e.getX(), e.getY(),
            target);
        MouseEvent retargeted = new MouseEvent(target,
            id,
            e.getWhen(),
            e.getModifiersEx() | e.getModifiers(),
            p.x,
            p.y,
            e.getClickCount(),
            e.isPopupTrigger(),
            e.getButton());
        target.dispatchEvent(retargeted);
    }

    /**
     * If c is a JRootPane descendant return its outermost JRootPane ancestor. If c is a RootPaneContainer then return
     * its JRootPane.
     *
     * @param c the component.
     * @return the outermost JRootPane for Component c or {@code null}.
     */
    public static JRootPane getOutermostRootPane(Component c) {
        if (c instanceof RootPaneContainer && c.getParent() == null) {
            return ((RootPaneContainer) c).getRootPane();
        }
        JRootPane lastRootPane;
        for (; c != null; c = SwingUtilities.getRootPane(c)) {
            if (c instanceof JRootPane) {
                lastRootPane = (JRootPane) c;
                if (c.getParent().getParent() == null) {
                    return lastRootPane;
                }
                if (c.getParent() instanceof JDialog || c.getParent() instanceof JWindow
                    || c.getParent() instanceof JFrame || c.getParent() instanceof JApplet) {
                    return lastRootPane;
                }
                c = c.getParent().getParent();
            }
        }
        return null;
    }

    /**
     * Checks if the font specified by the font name is fixed width font. Fixed width font means all chars have the
     * exact same width.
     *
     * @param fontName  the font name
     * @param component the component where the font will be displayed.
     * @return true if the font is fixed width. Otherwise false.
     */
    public static boolean isFixedWidthFont(String fontName, Component component) {
        if (fontName.endsWith(" Bold") || fontName.endsWith(" ITC") || fontName.endsWith(" MT") || fontName.endsWith(" LET")
            || fontName.endsWith(".bold") || fontName.endsWith(".italic")) {
            return false;
        }
        try {
            Font font = new Font(fontName, 0, 12);
            if (!font.canDisplay('W')) {
                return false;
            }
            Font boldFont = font.deriveFont(Font.BOLD);
            FontMetrics fm = component.getFontMetrics(font);
            FontMetrics fmBold = component.getFontMetrics(boldFont);
            int l1 = fm.charWidth('l');
            int l2 = fmBold.charWidth('l');
            if (l1 == l2) {
                int w1 = fm.charWidth('W');
                int w2 = fmBold.charWidth('W');
                if (w1 == w2 && l1 == w1) {
                    int s1 = fm.charWidth(' ');
                    int s2 = fmBold.charWidth(' ');
                    if (s1 == s2) {
                        return true;
                    }
                }
            }
        }
        catch (Throwable throwable) {
            // ignore it and return false
        }
        return false;
    }

    /**
     * Sets the locale recursively on the component and all its child components if any.
     *
     * @param c      the component
     * @param locale the new locales.
     */
    public static void setLocaleRecursively(final Component c, final Locale locale) {
        JideSwingUtilities.setRecursively(c, new JideSwingUtilities.Handler() {
            public boolean condition(Component c) {
                return true;
            }

            public void action(Component c) {
                c.setLocale(locale);
            }

            public void postAction(Component c) {

            }
        });
    }

    /**
     * Sets the bounds. If the container orientation is from right to left, this method will adjust the x to the
     * opposite.
     *
     * @param container the container. It is usually the parent of the component.
     * @param component the component to set bounds
     * @param bounds    the bounds.
     */
    public static void setBounds(Container container, Component component, Rectangle bounds) {
        if (container.getComponentOrientation().isLeftToRight()) {
            component.setBounds(bounds);
        }
        else {
            Rectangle r = new Rectangle(bounds);
            int w = container.getWidth();
            r.x = w - (bounds.x + bounds.width);
            component.setBounds(r);
        }
    }

    /**
     * Sets the bounds. If the container orientation is from right to left, this method will adjust the x to the
     * opposite.
     *
     * @param container the container. It is usually the parent of the component.
     * @param component the component to set bounds
     * @param x         the x of the bounds
     * @param y         the y of the bounds
     * @param width     the the height of the bounds. of the bounds.
     * @param height    the height of the bounds.
     */
    public static void setBounds(Container container, Component component, int x, int y, int width, int height) {
        if (container.getComponentOrientation().isLeftToRight()) {
            component.setBounds(x, y, width, height);
        }
        else {
            int w = container.getWidth();
            component.setBounds(w - x - width, y, width, height);
        }
    }

    /**
     * Invalidate and doLayout on the component and all its child components if any.
     *
     * @param c the component
     */
    public static void invalidateRecursively(final Component c) {
        if (c instanceof JComponent) {
            JideSwingUtilities.setRecursively(c, new JideSwingUtilities.Handler() {
                public boolean condition(Component c) {
                    return true;
                }

                public void action(Component c) {
                    if (c instanceof JComponent) {
                        ((JComponent) c).revalidate();
                    }
                    c.invalidate();
                }

                public void postAction(Component c) {
                }
            });
        }
        c.doLayout();
        c.repaint();
    }

    /**
     * Registers all actions registered on the source component and registered them on the target component at the
     * specified condition.
     *
     * @param sourceComponent the source component.
     * @param targetComponent the target component.
     * @param keyStrokes      the keystrokes
     * @param condition       the condition which will be used in {@link javax.swing.JComponent#registerKeyboardAction(java.awt.event.ActionListener,
     *                        javax.swing.KeyStroke, int)} as the last parameter.
     */
    public static void synchronizeKeyboardActions(JComponent sourceComponent, JComponent targetComponent, KeyStroke[] keyStrokes, int condition) {
        for (KeyStroke keyStroke : keyStrokes) {
            ActionListener actionListener = sourceComponent.getActionForKeyStroke(keyStroke);
            if (actionListener != null) {
                targetComponent.registerKeyboardAction(actionListener, keyStroke, condition);
            }
        }
    }

    /**
     * Gets the first JComponent from the RootPaneContainer.
     *
     * @param rootPaneContainer a rootPaneContainer
     * @return the first JComponent from the rootPaneContainer's content pane.
     */
    public static JComponent getFirstJComponent(RootPaneContainer rootPaneContainer) {
        return (JComponent) getRecursively(rootPaneContainer.getContentPane(), new GetHandler() {
            public boolean condition(Component c) {
                return c instanceof JComponent;
            }

            public Component action(Component c) {
                return c;
            }
        });
    }


    /**
     * This method can be used to fix two JDK bugs. One is to fix the row height is wrong when the first element in the
     * model is null or empty string. The second bug is only on JDK1.4.2 where the vertical scroll bar is shown even all
     * rows are visible. To use it, you just need to override JList#getPreferredScrollableViewportSize and call this
     * method.
     * <pre><code>
     * public Dimension getPreferredScrollableViewportSize() {
     *    return JideSwingUtilities.adjustPreferredScrollableViewportSize(this, super.getPreferredScrollableViewportSize());
     * }
     * <p/>
     * </code></pre>
     *
     * @param list                the JList
     * @param defaultViewportSize the default viewport size from JList#getPreferredScrollableViewportSize().
     * @return the adjusted size.
     */
    public static Dimension adjustPreferredScrollableViewportSize(JList list, Dimension defaultViewportSize) {
        // workaround the bug that the list is tiny when the first element is empty
        Rectangle cellBonds = list.getCellBounds(0, 0);
        if (cellBonds != null && cellBonds.height < 3) {
            ListCellRenderer renderer = list.getCellRenderer();
            if (renderer != null) {
                Component c = renderer.getListCellRendererComponent(list, "DUMMY STRING", 0, false, false);
                if (c != null) {
                    Dimension preferredSize = c.getPreferredSize();
                    if (preferredSize != null) {
                        int height = preferredSize.height;
                        if (height < 3) {
                            try {
                                height = list.getCellBounds(1, 1).height;
                            }
                            catch (Exception e) {
                                height = 16;
                            }
                        }
                        list.setFixedCellHeight(height);
                    }
                }
            }
        }
        if (SystemInfo.isJdk15Above()) {
            return defaultViewportSize;
        }
        else {
            // in JDK1.4.2, the vertical scroll bar is shown because of the wrong size is calculated.
            defaultViewportSize.height++;
            return defaultViewportSize;
        }
    }

    /**
     * The semantics in AWT of hiding a component, removing a component, and reparenting a component are inconsistent
     * with respect to focus. By calling this function before any of the operations above focus is guaranteed a
     * consistent degregation.
     *
     * @param component
     */
    public static void removeFromParentWithFocusTransfer(Component component) {
        boolean wasVisible = component.isVisible();
        component.setVisible(false);
        if (component.getParent() != null) {
            component.getParent().remove(component);
        }
        component.setVisible(wasVisible);
    }

    /**
     * Gets the line height for the font for the component
     *
     * @param c             the component
     * @param defaultHeight the default height if the font on the specified component is null
     * @return the line height for the font for the component (or the passed in the default value if the font on the
     * specified component is null)
     */
    public static int getLineHeight(Component c, int defaultHeight) {
        Font f = c == null ? null : c.getFont();
        if (f == null) {
            return defaultHeight;
        }
        FontMetrics fm = c.getFontMetrics(f);
        float h = fm.getHeight();

        h += fm.getDescent();

        return (int) h;
    }

    /**
     * Adds a separator to the popup menu if there are menu items on it already.
     *
     * @param popup the popup menu.
     */
    public static void addSeparatorIfNecessary(JPopupMenu popup) {
        int count = popup.getComponentCount();
        if (count > 0 && !(popup.getComponent(count - 1) instanceof JSeparator)) {
            popup.addSeparator();
        }
    }

    /**
     * Removes extra separators, if any. This can be used when you remove some menu items and leave extra separators on
     * the UI.
     *
     * @param popup the popup menu.
     */
    public static void removeExtraSeparators(JPopupMenu popup) {
        Component[] components = popup.getComponents();
        if (components.length <= 1) {
            return;
        }
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            if (component instanceof JSeparator) {
                if (i == 0 || i == components.length - 1) { // if the separator is the first one or the last one, remove it because the separator is not necessary here
                    popup.remove(component);
                }
                else if (components[i - 1] instanceof JSeparator) {
                    popup.remove(component);
                }
            }
        }
    }

    /**
     * Sets the text component transparent. It will call setOpaque(false) and also set client property for certain L&Fs
     * in case the L&F doesn't respect the opaque flag.
     *
     * @param component the text component to be set to transparent.
     * @deprecated replaced by {@link #setComponentTransparent(javax.swing.JComponent)}.
     */
    @Deprecated
    public static void setTextComponentTransparent(JComponent component) {
        setComponentTransparent(component);
    }

    /**
     * Sets the text component transparent. It will call setOpaque(false) and also set client property for certain L&Fs
     * in case the L&F doesn't respect the opaque flag.
     *
     * @param component the text component to be set to transparent.
     */
    public static void setComponentTransparent(JComponent component) {
        component.setOpaque(false);

// add this for the Synthetica
        component.putClientProperty("Synthetica.opaque", false);
// add this for Nimbus to disable all the painting of a component in Nimbus
        component.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        component.putClientProperty("Nimbus.Overrides", new UIDefaults());

    }

    /**
     * Perform a binary search over a sorted list for the given key.
     *
     * @param a   the array to search
     * @param key the key to search for
     * @return the index of the given key if it exists in the list, otherwise -1 times the index value at the insertion
     * point that would be used if the key were added to the list.
     */
    public static <T> int binarySearch(List<T> a, T key) {
        int x1 = 0;
        int x2 = a.size();
        int i = x2 / 2, c;
        while (x1 < x2) {
            if (!(a.get(i) instanceof Comparable)) {
                return i;
            }
            c = ((Comparable) a.get(i)).compareTo(key);
            if (c == 0) {
                return i;
            }
            else if (c < 0) {
                x1 = i + 1;
            }
            else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1 * i;
    }

    /**
     * Perform a binary search over a sorted array for the given key.
     *
     * @param a   the array to search
     * @param key the key to search for
     * @return the index of the given key if it exists in the array, otherwise -1 times the index value at the insertion
     * point that would be used if the key were added to the array.
     */
    public static <T> int binarySearch(T[] a, T key) {
        int x1 = 0;
        int x2 = a.length;
        int i = x2 / 2, c;
        while (x1 < x2) {
            if (!(a[i] instanceof Comparable)) {
                return i;
            }
            c = ((Comparable) a[i]).compareTo(key);
            if (c == 0) {
                return i;
            }
            else if (c < 0) {
                x1 = i + 1;
            }
            else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1 * i;
    }

    /**
     * Perform a binary search over a sorted array for the given key.
     *
     * @param a   the array to search
     * @param key the key to search for
     * @return the index of the given key if it exists in the array, otherwise -1 times the index value at the insertion
     * point that would be used if the key were added to the array.
     */
    public static int binarySearch(int[] a, int key) {
        return binarySearch(a, key, 0, a.length);
    }

    /**
     * Perform a binary search over a sorted array for the given key.
     *
     * @param a     the array to search
     * @param key   the key to search for
     * @param start the start index to search inclusive
     * @param end   the end index to search exclusive
     * @return the index of the given key if it exists in the array, otherwise -1 times the index value at the insertion
     * point that would be used if the key were added to the array.
     */
    public static int binarySearch(int[] a, int key, int start, int end) {
        int x1 = start;
        int x2 = end;
        int i = x2 / 2;
        while (x1 < x2) {
            if (a[i] == key) {
                return i;
            }
            else if (a[i] < key) {
                x1 = i + 1;
            }
            else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1 * i;
    }

    /**
     * Checks if the ctrl key is pressed. On Mac oS X, it will be command key.
     *
     * @param event the InputEvent.
     * @return true or false.
     */
    public static boolean isMenuShortcutKeyDown(InputEvent event) {
        return (event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
    }

    /**
     * Checks if the ctrl key is pressed. On Mac oS X, it will be command key.
     *
     * @param event the InputEvent.
     * @return true or false.
     */
    public static boolean isMenuShortcutKeyDown(ActionEvent event) {
        return (event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
    }

    /**
     * Copied from JDK's SwingUtilities2.java
     * <p/>
     * Returns the FontMetrics for the current Font of the passed in Graphics.  This method is used when a Graphics is
     * available, typically when painting.  If a Graphics is not available the JComponent method of the same name should
     * be used.
     * <p/>
     * Callers should pass in a non-null JComponent, the exception to this is if a JComponent is not readily available
     * at the time of painting.
     * <p/>
     * This does not necessarily return the FontMetrics from the Graphics.
     *
     * @param c JComponent requesting FontMetrics, may be null
     * @param g Graphics Graphics
     */
    public static FontMetrics getFontMetrics(JComponent c, Graphics g) {
        return getFontMetrics(c, g, g.getFont());
    }


    /**
     * Copied from JDK's SwingUtilities2.java
     * <p/>
     * Returns the FontMetrics for the specified Font. This method is used when a Graphics is available, typically when
     * painting.  If a Graphics is not available the JComponent method of the same name should be used.
     * <p/>
     * Callers should pass in a non-null JComponent, the exception to this is if a JComponent is not readily available
     * at the time of painting.
     * <p/>
     * This does not necessarily return the FontMetrics from the Graphics.
     *
     * @param c    JComponent requesting FontMetrics, may be null
     * @param c    Graphics Graphics
     * @param font Font to get FontMetrics for
     */
    public static FontMetrics getFontMetrics(JComponent c, Graphics g,
                                             Font font) {
        if (c != null) {
            // Note: We assume that we're using the FontMetrics
            // from the widget to layout out text, otherwise we can get
            // mismatches when printing.
            return c.getFontMetrics(font);
        }
        return Toolkit.getDefaultToolkit().getFontMetrics(font);
    }

    /**
     * Shows the popup menu with the consideration of the invoker's orientation.
     *
     * @param popup   the popup menu
     * @param invoker the invoker for the popup menu
     * @param x       the x, usually the x of the mouse clicked position
     * @param y       the y, usually the y of the mouse clicked position
     */
    public static void showPopupMenu(JPopupMenu popup, Component invoker, int x, int y) {
        popup.applyComponentOrientation(invoker.getComponentOrientation());
        if (popup.getComponentOrientation().isLeftToRight()) {
            popup.show(invoker, x, y);
        }
        else {
            popup.show(invoker, x - popup.getPreferredSize().width, y);
        }

    }

    public static final boolean JETBRAINS_JRE = System.getProperty("java.vendor").toLowerCase().contains("jetbrains");

    public static Double cachedScaleFactor = null;

    public static boolean isIntegerScaleFactor(Graphics2D g) {
        double scaleFactor = getScaleFactor(g);
        return Math.floor(scaleFactor) == scaleFactor;
    }

    public static double getScaleFactor(Graphics2D g) {
        if (g == null) {
            throw new NullPointerException("graphics is null");
        }
        GraphicsConfiguration deviceConfiguration = g.getDeviceConfiguration();
        if (deviceConfiguration == null) {
            throw new NullPointerException("deviceConfiguration is null");
        }
        double scale = deviceConfiguration.getDefaultTransform().getScaleX();
        if (SystemInfo.isMacOSX() && scale == 1f && !JETBRAINS_JRE) {
            if (cachedScaleFactor == null) {
                initCachedScaleFactor();
            }
            return cachedScaleFactor;
        }

        return scale;
    }

    private static void initCachedScaleFactor() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = graphicsEnvironment.getDefaultScreenDevice();
        cachedScaleFactor = 1d;
        try {
            Field field = device.getClass().getDeclaredField("scale");
            if (field != null) {
                field.setAccessible(true);
                Object scaleValue = field.get(device);
                if (scaleValue instanceof Integer && (Integer) scaleValue == 2) {
                    cachedScaleFactor = 2d;
                }
            }
        }
        catch (Exception ignore) {
        }
    }

    public static void withFractionalAntiAliasing(Graphics g, Runnable r) {
        withFractionalAntiAliasing(g, RenderingHints.VALUE_ANTIALIAS_ON, r);
    }

    public static void withoutFractionalAntiAliasing(Graphics g, Runnable r) {
        withFractionalAntiAliasing(g, RenderingHints.VALUE_ANTIALIAS_OFF, r);
    }

    public static void withFractionalAntiAliasing(Graphics g, Object value, Runnable r) {
        Graphics2D g2 = (Graphics2D) g;
        boolean fractionalScale = !isIntegerScaleFactor(g2);
        Object oldAntiAliasingHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (fractionalScale) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, value);
        }
        try {
            r.run();
        }
        finally {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntiAliasingHint);
        }
    }

    /**
     * Paints an arrow shape.
     *
     * @param g           the graphics instance
     * @param color       color
     * @param startX      start X
     * @param startY      start Y
     * @param width       width
     * @param orientation horizontal or vertical
     */
    public static void paintArrow(Graphics g, Color color, int startX, int startY, int width, int orientation) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Get the scaling factor
            double scaleFactor = UIScale.getSystemScaleFactor(g2d);
            g2d.scale(1 / scaleFactor, 1 / scaleFactor);
            g2d.setColor(color);
            width = width / 2 * 2 + 1; // make sure it's odd
            if (orientation == HORIZONTAL) {
                for (int i = 0; i < (width + 1) / 2; i++) {
                    g.drawLine(startX + i, startY + i, startX + width - i - 1, startY + i);
                }
            }
            else if (orientation == VERTICAL) {
                for (int i = 0; i < (width + 1) / 2; i++) {
                    g.drawLine(startX + i, startY + i, startX + i, startY + width - i - 1);
                }
            }
        }
        finally {
            g2d.dispose();
        }

    }
}
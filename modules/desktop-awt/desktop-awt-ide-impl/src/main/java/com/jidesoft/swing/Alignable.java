/*
 * @(#)Alignable.java
 *
 * Copyright 2002 - 2004 JIDE Software. All rights reserved.
 */
package com.jidesoft.swing;

/**
 * <code>Alignable</code> is an interface that can be implemented by any components to provide information such as how
 * to set orientation and check whether a component supports vertical orientation or horizontal orientation.
 * <p/>
 * Some components support both vertical orientation and horizontal orientation. For example, an icon-only JideButton.
 * It can be put on either a vertical toolbar or normal horizontal toolbar. However most components don't support both.
 * For example, a combo box. It's hard to imagine a combobox putting on a vertical toolbar.
 * <p/>
 * By implementing this interface, a component can choose if it wants to support vertical orientation or horizontal
 * orientation. However if a component which doesn't implement this interface is added to toolbar, by default, it will
 * be treated as supportHorizontalOrientation() returning true and supportVerticalOrientation() returning false.
 */
public interface Alignable {
    /**
     * Property name to indicate the orientation is changed.
     */
    public static final String PROPERTY_ORIENTATION = "orientation";

    /**
     * Checks if the component support vertical orientation. doesn't consider the component orientation, it should
     * return false.
     *
     * @return true if it supports vertical orientation
     */
    boolean supportVerticalOrientation();

    /**
     * Checks if the component support horizontal orientation.
     *
     * @return true if it supports horizontal orientation
     */
    boolean supportHorizontalOrientation();

    /**
     * Changes the orientation. If the component is a Swing component, the default implementation is this.
     * <br><code>JideSwingUtilities.setOrientationOf(this, orientation);<code>
     *
     * @param orientation the new orientation
     */
    void setOrientation(int orientation);

    /**
     * Gets the orientation. If the component is a Swing component, the default implementation is this. <br><code>return
     * JideSwingUtilities.getOrientationOf(this);<code>
     *
     * @return orientation
     */
    int getOrientation();
}
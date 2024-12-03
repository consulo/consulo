/*
 * @(#)UIAction.java 8/20/2006
 *
 * Copyright 2002 - 2006 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.plaf.basic;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * UIAction is the basis of all of basic's action classes that are used in an ActionMap. Subclasses
 * need to override <code>actionPerformed</code>.
 * <p/>
 * A typical subclass will look like:
 * <pre>
 *    private static class Actions extends UIAction {
 *        Actions(String name) {
 *            super(name);
 *        }
 * <p/>
 *        public void actionPerformed(ActionEvent ae) {
 *            if (getName() == "selectAll") {
 *                selectAll();
 *            }
 *            else if (getName() == "cancelEditing") {
 *                cancelEditing();
 *            }
 *        }
 *    }
 * </pre>
 * <p/>
 * Subclasses that wish to conditionalize the enabled state should override
 * <code>isEnabled(Component)</code>, and be aware that the passed in <code>Component</code> may be
 * null.
 *
 * @author Scott Violet
 * @version 1.4 11/17/05
 * @see javax.swing.Action
 */
public abstract class UIAction implements Action {
    private String name;

    public UIAction(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    @Override
    public Object getValue(String key) {
        if (NAME.equals(key)) {
            return name;
        }
        return null;
    }

    // UIAction is not mutable, this does nothing.
    @Override
    public void putValue(String key, Object value) {
    }

    // UIAction is not mutable, this does nothing.
    @Override
    public void setEnabled(boolean b) {
    }

    /**
     * Cover method for <code>isEnabled(null)</code>.
     */
    @Override
    public final boolean isEnabled() {
        return isEnabled(null);
    }

    /**
     * Subclasses that need to conditionalize the enabled state should override this. Be aware that
     * <code>sender</code> may be null.
     *
     * @param sender Widget enabled state is being asked for, may be null.
     * @return true.
     */
    public boolean isEnabled(Object sender) {
        return true;
    }

    // UIAction is not mutable, this does nothing.
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    // UIAction is not mutable, this does nothing.
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
}
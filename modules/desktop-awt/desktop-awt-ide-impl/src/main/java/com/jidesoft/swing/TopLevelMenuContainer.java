/*
 * @(#)TopLevelMenuContainer.java
 *
 * Copyright 2002 - 2004 JIDE Software Inc. All rights reserved.
 */
package com.jidesoft.swing;

/**
 * A markup interface to indicate this is a top level menu or command bar. The original Swing code used JMenuBar to
 * determine if it is TopLeveMenu. However since we introduced CommandBar, this criteria is not correct anymore. The new
 * condition is if a container implements TopLevelMenuContainer, the children in that container is top level menu. If
 * isMenuBar returns true, it means the container is really a menu bar, just like JMenuBar.
 */
public interface TopLevelMenuContainer {
    /**
     * Checks if the TopLevelMenuContainer is used as JMenuBar.
     *
     * @return true if the TopLevelMenuContainer is used as JMenuBar.
     */
    boolean isMenuBar();
}
package com.intellij.ide.ui.laf.darcula.ui;

import com.intellij.util.ui.JBUI;

import javax.swing.plaf.BorderUIResource;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaMenuItemBorder extends BorderUIResource.EmptyBorderUIResource {
  public DarculaMenuItemBorder() {
    super(JBUI.insets(2));
  }
}

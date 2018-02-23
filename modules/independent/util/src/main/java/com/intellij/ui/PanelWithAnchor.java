package com.intellij.ui;

import javax.annotation.Nullable;

import javax.swing.*;

/**
 * @author evgeny.zakrevsky
 */

public interface PanelWithAnchor {
  JComponent getAnchor();
  void setAnchor(@Nullable JComponent anchor);
}

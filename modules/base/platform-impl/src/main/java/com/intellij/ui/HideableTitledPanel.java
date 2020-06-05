package com.intellij.ui;

import consulo.annotation.DeprecationInfo;

import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author evgeny zakrevsky
 */
@Deprecated
@DeprecationInfo("Use consulo.ui.layout.FoldoutLayout")
public class HideableTitledPanel extends JPanel {

  private final HideableDecorator myDecorator;

  public HideableTitledPanel(String title) {
    this(title, true);
  }

  public HideableTitledPanel(String title, boolean adjustWindow) {
    super(new BorderLayout());
    myDecorator = new HideableDecorator(this, title, adjustWindow);
  }

  public HideableTitledPanel(String title, JComponent content, boolean on) {
    this(title);
    setContentComponent(content);
    setOn(on);
  }

  public void setContentComponent(@Nullable JComponent content) {
    myDecorator.setContentComponent(content);
  }

  public void setOn(boolean on) {
    myDecorator.setOn(on);
  }

  public boolean isExpanded() {
    return myDecorator.isExpanded();
  }

  public void setTitle(String title) {
    myDecorator.setTitle(title);
  }

  public String getTitle() {
    return myDecorator.getTitle();
  }

  @Override
  public void setEnabled(boolean enabled) {
    myDecorator.setEnabled(enabled);
  }
}

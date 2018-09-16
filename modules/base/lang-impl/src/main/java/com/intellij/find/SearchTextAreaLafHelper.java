package com.intellij.find;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public abstract class SearchTextAreaLafHelper {
  public abstract Border getBorder();

  public abstract String getLayoutConstraints();

  public abstract String getHistoryButtonConstraints();

  public abstract String getIconsPanelConstraints();

  public abstract Border getIconsPanelBorder(int rows);

  public abstract Icon getShowHistoryIcon();

  public abstract Icon getClearIcon();

  public abstract void paint(Graphics2D g);
}

package com.intellij.codeInsight.generation;

import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.actionSystem.DataContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

/**
* @author Dmitry Avdeev
*/
public abstract class PatternDescriptor {

  public static final String ROOT = "root";

  @Nullable
  public String getId() {
    return null;
  }

  @Nonnull
  public abstract String getParentId();

  @Nonnull
  public abstract String getName();

  @Nullable
  public abstract Icon getIcon();

  @Nullable
  public abstract Template getTemplate();

  public abstract void actionPerformed(DataContext context);
}

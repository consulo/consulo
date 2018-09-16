package com.intellij.ide.browsers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.UUID;

public abstract class WebBrowser {
  @Nonnull
  public abstract String getName();

  @Nonnull
  public abstract UUID getId();

  @Nonnull
  public abstract BrowserFamily getFamily();

  @Nonnull
  public abstract Icon getIcon();

  @Nullable
  public abstract String getPath();

  @Nonnull
  public abstract String getBrowserNotFoundMessage();

  @Nullable
  public abstract BrowserSpecificSettings getSpecificSettings();
}
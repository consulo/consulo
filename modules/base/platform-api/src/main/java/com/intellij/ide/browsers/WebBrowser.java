package com.intellij.ide.browsers;

import consulo.ui.image.Image;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public abstract class WebBrowser {
  @Nonnull
  public abstract String getName();

  @Nonnull
  public abstract UUID getId();

  @Nonnull
  public abstract BrowserFamily getFamily();

  @Nonnull
  public abstract Image getIcon();

  @Nullable
  public abstract String getPath();

  @Nonnull
  public abstract String getBrowserNotFoundMessage();

  @Nullable
  public abstract BrowserSpecificSettings getSpecificSettings();

  public void addOpenUrlParameter(@Nonnull List<? super String> command, @Nonnull String url) {
    command.add(url);
  }
}
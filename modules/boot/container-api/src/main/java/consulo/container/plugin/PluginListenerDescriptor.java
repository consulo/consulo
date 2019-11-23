// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.container.plugin;

import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;

//@ApiStatus.Internal
public final class PluginListenerDescriptor {
  public final String listenerClassName;
  public final String topicClassName;

  public final boolean activeInTestMode;
  public final boolean activeInHeadlessMode;

  public transient PluginDescriptor pluginDescriptor;

  public PluginListenerDescriptor(@Nonnull String listenerClassName, @Nonnull String topicClassName, boolean activeInTestMode, boolean activeInHeadlessMode) {
    this.listenerClassName = listenerClassName;
    this.topicClassName = topicClassName;
    this.activeInTestMode = activeInTestMode;
    this.activeInHeadlessMode = activeInHeadlessMode;
  }
}

package consulo.ide.updateSettings.impl;

import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-07-08
 */
public class PlatformOrPluginNode {
  @Nonnull
  private final PluginId myPluginId;
  @Nullable
  private final PluginDescriptor myCurrentDescriptor;
  @Nullable
  private final PluginDescriptor myFutureDescriptor;

  public PlatformOrPluginNode(@Nonnull PluginId pluginId, @Nullable PluginDescriptor currentDescriptor, @Nullable PluginDescriptor futureDescriptor) {
    myPluginId = pluginId;
    myCurrentDescriptor = currentDescriptor;
    myFutureDescriptor = futureDescriptor;

    if(myCurrentDescriptor == null && myFutureDescriptor == null) {
      throw new IllegalArgumentException("Current or future descriptor must be set");
    }
  }

  @Nonnull
  public PluginId getPluginId() {
    return myPluginId;
  }

  @Nullable
  public PluginDescriptor getCurrentDescriptor() {
    return myCurrentDescriptor;
  }

  @Nullable
  public PluginDescriptor getFutureDescriptor() {
    return myFutureDescriptor;
  }
}

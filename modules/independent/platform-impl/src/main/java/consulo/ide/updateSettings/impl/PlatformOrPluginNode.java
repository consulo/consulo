package consulo.ide.updateSettings.impl;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;

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
  private final IdeaPluginDescriptor myCurrentDescriptor;
  @Nullable
  private final IdeaPluginDescriptor myFutureDescriptor;

  public PlatformOrPluginNode(@Nonnull PluginId pluginId, @Nullable IdeaPluginDescriptor currentDescriptor, @Nullable IdeaPluginDescriptor futureDescriptor) {
    myPluginId = pluginId;
    myCurrentDescriptor = currentDescriptor;
    myFutureDescriptor = futureDescriptor;

    if(myCurrentDescriptor == null || myFutureDescriptor == null) {
      throw new IllegalArgumentException();
    }
  }

  @Nonnull
  public PluginId getPluginId() {
    return myPluginId;
  }

  @Nullable
  public IdeaPluginDescriptor getCurrentDescriptor() {
    return myCurrentDescriptor;
  }

  @Nullable
  public IdeaPluginDescriptor getFutureDescriptor() {
    return myFutureDescriptor;
  }
}

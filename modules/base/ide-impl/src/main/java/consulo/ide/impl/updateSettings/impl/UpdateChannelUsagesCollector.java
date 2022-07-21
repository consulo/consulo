package consulo.ide.impl.updateSettings.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.UsagesCollector;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.project.Project;
import consulo.ide.impl.updateSettings.UpdateChannel;
import consulo.ide.impl.updateSettings.UpdateSettings;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-05-31
 */
@ExtensionImpl
public class UpdateChannelUsagesCollector extends UsagesCollector {
  private final UpdateSettings myUpdateSettings;

  @Inject
  public UpdateChannelUsagesCollector(UpdateSettings updateSettings) {
    myUpdateSettings = updateSettings;
  }

  @Nonnull
  @Override
  public Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException {
    UpdateChannel channel = myUpdateSettings.getChannel();
    return Collections.singleton(new UsageDescriptor(channel.name(), 1));
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.base:update.channel";
  }
}

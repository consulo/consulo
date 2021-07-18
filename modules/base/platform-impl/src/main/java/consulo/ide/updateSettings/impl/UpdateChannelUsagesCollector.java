package consulo.ide.updateSettings.impl;

import com.intellij.internal.statistic.CollectUsagesException;
import com.intellij.internal.statistic.UsagesCollector;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.project.Project;
import consulo.ide.updateSettings.UpdateChannel;
import consulo.ide.updateSettings.UpdateSettings;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-05-31
 */
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

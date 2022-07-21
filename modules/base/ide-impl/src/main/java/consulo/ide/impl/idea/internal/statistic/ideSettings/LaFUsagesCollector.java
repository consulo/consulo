package consulo.ide.impl.idea.internal.statistic.ideSettings;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ide.ui.LafManager;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.UsagesCollector;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.project.Project;
import consulo.application.util.SystemInfo;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.Set;

@ExtensionImpl
public class LaFUsagesCollector extends UsagesCollector {
  @Nonnull
  @Override
  public Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException {
    UIManager.LookAndFeelInfo laf = LafManager.getInstance().getCurrentLookAndFeel();
    String key = SystemInfo.OS_NAME + " - ";
    if (!StringUtil.isEmptyOrSpaces(SystemInfo.SUN_DESKTOP)) {
      key += SystemInfo.SUN_DESKTOP + " - ";
    }
    return laf != null ? Collections.singleton(new UsageDescriptor(key + laf.getName(), 1)) : Collections.<UsageDescriptor>emptySet();
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.desktop:look.and.feel";
  }
}

package com.intellij.internal.statistic.ideSettings;

import com.intellij.ide.ui.LafManager;
import com.intellij.internal.statistic.CollectUsagesException;
import com.intellij.internal.statistic.UsagesCollector;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.Set;

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

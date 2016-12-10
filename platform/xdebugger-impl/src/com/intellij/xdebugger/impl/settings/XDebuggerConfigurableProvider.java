package com.intellij.xdebugger.impl.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SimpleConfigurable;
import com.intellij.openapi.util.Getter;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.settings.DebuggerSettingsCategory;
import com.intellij.xdebugger.settings.XDebuggerSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class XDebuggerConfigurableProvider {
  @NotNull
  public static Collection<? extends Configurable> getConfigurables(@NotNull DebuggerSettingsCategory category) {
    List<Configurable> list;
    if (category == DebuggerSettingsCategory.GENERAL) {
      list = new SmartList<>(SimpleConfigurable.create("debugger.general", "", GeneralConfigurableUi.class, new Getter<XDebuggerGeneralSettings>() {
        @Override
        public XDebuggerGeneralSettings get() {
          return XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings();
        }
      }));
    }
    else {
      list = null;
    }

    for (XDebuggerSettings<?> settings : XDebuggerSettingManagerImpl.getInstanceImpl().getSettingsList()) {
      Collection<? extends Configurable> configurables = settings.createConfigurables(category);
      if (!configurables.isEmpty()) {
        if (list == null) {
          list = new SmartList<>();
        }
        list.addAll(configurables);
      }
    }
    return ContainerUtil.notNullize(list);
  }

  public static void generalApplied(@NotNull DebuggerSettingsCategory category) {
    for (XDebuggerSettings<?> settings : XDebuggerSettingManagerImpl.getInstanceImpl().getSettingsList()) {
      settings.generalApplied(category);
    }
  }
}
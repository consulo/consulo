package consulo.execution.debug.impl.internal.setting;

import consulo.configurable.Configurable;
import consulo.execution.debug.setting.DebuggerSettingsCategory;
import consulo.execution.debug.setting.XDebuggerSettings;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;

import java.util.Collection;
import java.util.List;

public class XDebuggerConfigurableProvider {
  
  public static Collection<Configurable> getConfigurables(DebuggerSettingsCategory category) {
    List<Configurable> list;
    if (category == DebuggerSettingsCategory.GENERAL) {
      list = new SmartList<>(new XDebuggerGeneralConfigurable());
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
    return Lists.notNullize(list);
  }

  public static void generalApplied(DebuggerSettingsCategory category) {
    for (XDebuggerSettings<?> settings : XDebuggerSettingManagerImpl.getInstanceImpl().getSettingsList()) {
      settings.generalApplied(category);
    }
  }
}
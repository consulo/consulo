package com.intellij.xdebugger.impl.settings;

import com.intellij.xdebugger.settings.XDebuggerSettings;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.DeprecationInfo;

/**
 * @author VISTALL
 * @since 21.03.2016
 */
@Deprecated
@DeprecationInfo(value = "Use com.intellij.xdebugger.impl.settings.XDebuggerSettingManagerImpl", until = "1.0")
public class XDebuggerSettingsManager extends com.intellij.xdebugger.settings.XDebuggerSettingsManager {
  public static XDebuggerSettingsManager INSTANCE = new XDebuggerSettingsManager();

  public static XDebuggerSettingsManager getInstanceImpl() {
    return INSTANCE;
  }

  public <T extends XDebuggerSettings<?>> T getSettings(final Class<T> aClass) {
    return XDebuggerSettingManagerImpl.getInstanceImpl().getSettings(aClass);
  }

  @NotNull
  @Override
  public DataViewSettings getDataViewSettings() {
    return XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings();
  }
}

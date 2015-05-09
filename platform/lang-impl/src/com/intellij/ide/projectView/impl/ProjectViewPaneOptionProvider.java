package com.intellij.ide.projectView.impl;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.KeyWithDefaultValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 09.05.2015
 */
public interface ProjectViewPaneOptionProvider<T> {
  ExtensionPointName<ProjectViewPaneOptionProvider> EX_NAME = ExtensionPointName.create("com.intellij.projectViewOptionProvider");

  abstract class BoolValue implements ProjectViewPaneOptionProvider<Boolean> {
    @NotNull
    @Override
    public Boolean parseValue(@NotNull String value) {
      return Boolean.parseBoolean(value);
    }

    @Nullable
    @Override
    public String toString(@Nullable Boolean value) {
      if(value == null || getKey().getDefaultValue() == value) {
        return null;
      }
      return value.toString();
    }
  }

  @NotNull
  KeyWithDefaultValue<T> getKey();

  void addToolbarActions(@NotNull AbstractProjectViewPane pane, @NotNull DefaultActionGroup actionGroup);

  @NotNull
  T parseValue(@NotNull String value);

  @Nullable
  String toString(@Nullable T value);
}

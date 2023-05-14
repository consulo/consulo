/*
 * Copyright 2013-2022 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.setting.module;

import consulo.ide.setting.ShowSettingsUtil;
import consulo.module.content.layer.orderEntry.CustomOrderEntry;
import consulo.module.content.layer.orderEntry.CustomOrderEntryModel;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredTextContainer;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 28-May-22
 */
public interface CustomOrderEntryTypeEditor<M extends CustomOrderEntryModel> extends OrderEntryTypeEditor<CustomOrderEntry<M>> {
  @Override
  @Nonnull
  default Consumer<ColoredTextContainer> getRender(@Nonnull CustomOrderEntry<M> orderEntry) {
    return getRender(orderEntry, orderEntry.getModel());
  }

  @Nonnull
  @Override
  default ClasspathTableItem<CustomOrderEntry<M>> createTableItem(@Nonnull CustomOrderEntry<M> orderEntry,
                                                                  @Nonnull Project project,
                                                                  @Nonnull ModulesConfigurator modulesConfigurator,
                                                                  @Nonnull LibrariesConfigurator librariesConfigurator) {
    return createTableItem(orderEntry, orderEntry.getModel(), project, modulesConfigurator, librariesConfigurator);
  }

  @RequiredUIAccess
  @Override
  default void navigate(@Nonnull final CustomOrderEntry<M> orderEntry) {
    navigate(orderEntry, orderEntry.getModel());
  }

  @Nonnull
  default Consumer<ColoredTextContainer> getRender(@Nonnull CustomOrderEntry<M> orderEntry, @Nonnull M model) {
    return it -> it.append(orderEntry.getPresentableName());
  }

  @RequiredUIAccess
  default void navigate(@Nonnull final CustomOrderEntry<M> orderEntry, @Nonnull M model) {
    Project project = orderEntry.getOwnerModule().getProject();
    ShowSettingsUtil.getInstance().showProjectStructureDialog(project, config -> config.selectOrderEntry(orderEntry.getOwnerModule(), orderEntry));
  }

  @Nonnull
  default ClasspathTableItem<CustomOrderEntry<M>> createTableItem(@Nonnull CustomOrderEntry<M> orderEntry,
                                                                  @Nonnull M model,
                                                                  @Nonnull Project project,
                                                                  @Nonnull ModulesConfigurator modulesConfigurator,
                                                                  @Nonnull LibrariesConfigurator librariesConfigurator) {
    return new ClasspathTableItem<>(orderEntry);
  }
}

/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.xdebugger.attach;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @deprecated use {@link XAttachPresentationGroup} instead
 */
@Deprecated
public class XDefaultLocalAttachGroup implements XLocalAttachGroup {
  public static final XDefaultLocalAttachGroup INSTANCE = new XDefaultLocalAttachGroup();

  @Override
  public int getOrder() {
    return 0;
  }

  @Nonnull
  @Override
  public String getGroupName() {
    return "";
  }

  @Nonnull
  @Override
  public Image getItemIcon(@Nonnull Project project, @Nonnull ProcessInfo info, @Nonnull UserDataHolder dataHolder) {
    return getProcessIcon(project, info, dataHolder);
  }

  @Nonnull
  @Override
  public String getItemDisplayText(@Nonnull Project project, @Nonnull ProcessInfo info, @Nonnull UserDataHolder dataHolder) {
    return getProcessDisplayText(project, info, dataHolder);
  }

  @Nullable
  @Override
  public String getItemDescription(@Nonnull Project project, @Nonnull ProcessInfo info, @Nonnull UserDataHolder dataHolder) {
    return null;
  }

  @Override
  @Nonnull
  public Image getProcessIcon(@Nonnull Project project, @Nonnull ProcessInfo info, @Nonnull UserDataHolder dataHolder) {
    return AllIcons.RunConfigurations.Application;
  }

  @Override
  @Nonnull
  public String getProcessDisplayText(@Nonnull Project project, @Nonnull ProcessInfo info, @Nonnull UserDataHolder dataHolder) {
    return info.getExecutableDisplayName();
  }
}

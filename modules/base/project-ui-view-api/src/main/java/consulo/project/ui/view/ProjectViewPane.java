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
package consulo.project.ui.view;

import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.module.Module;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
public interface ProjectViewPane extends UserDataHolder, DataProvider, Disposable {
  @Nonnull
  String getId();

  @Nullable
  String getSubId();

  @Nonnull
  SelectInTarget createSelectInTarget();

  @Nonnull
  ActionCallback updateFromRoot(boolean restoreExpandedPaths);

  void select(Object element, VirtualFile file, boolean requestFocus);

  void selectModule(@Nonnull Module module, final boolean requestFocus);

  void selectModuleGroup(@Nonnull ModuleGroup moduleGroup, boolean requestFocus);

  @Nonnull
  default AsyncResult<Void> selectCB(Object element, VirtualFile file, boolean requestFocus) {
    select(element, file, requestFocus);
    return AsyncResult.resolved();
  }

  @Nullable
  NodeDescriptor getSelectedDescriptor();

  @Nonnull
  String[] getSubIds();

  @Nonnull
  String getPresentableSubIdName(@Nonnull final String subId);

  @Nonnull
  Image getPresentableSubIdIcon(@Nonnull String subId);

  default void queueUpdate() {
  }
}

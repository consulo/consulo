/*
 * Copyright 2013-2021 consulo.io
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
package consulo.sandboxPlugin.tasks;

import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.impl.BaseRepositoryType;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 15/12/2021
 */
public class SandRepositoryType extends BaseRepositoryType<SandTaskRepository> {
  @Nonnull
  @Override
  public String getName() {
    return "Sand";
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.toolbarUnknown();
  }

  @Nonnull
  @Override
  public TaskRepository createRepository() {
    SandTaskRepository repository = new SandTaskRepository();
    repository.setRepositoryType(this);
    return repository;
  }

  @Override
  public Class<SandTaskRepository> getRepositoryClass() {
    return SandTaskRepository.class;
  }
}

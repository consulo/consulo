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
package consulo.application.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 15/10/2022
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AsyncFileService {
  @Nonnull
  default Future<Void> asyncDelete(@Nonnull File file) {
    return asyncDelete(List.of(file));
  }

  @Nonnull
  Future<Void> asyncDelete(@Nonnull Collection<File> files);
}

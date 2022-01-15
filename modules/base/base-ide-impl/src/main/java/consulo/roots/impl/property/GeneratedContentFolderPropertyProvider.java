/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.impl.property;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Comparing;
import consulo.util.dataholder.Key;
import consulo.roots.ContentFolderPropertyProvider;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 22:01/25.11.13
 */
public class GeneratedContentFolderPropertyProvider extends ContentFolderPropertyProvider<Boolean> {
  public static final Key<Boolean> IS_GENERATED = Key.create("is-generated-root");

  @Nonnull
  @Override
  public Key<Boolean> getKey() {
    return IS_GENERATED;
  }

  @Nullable
  @Override
  public Image getLayerIcon(@Nonnull Boolean value) {
    return Comparing.equal(value, Boolean.TRUE) ? AllIcons.Modules.GeneratedMark : null;
  }

  @Override
  public Boolean fromString(@Nonnull String value) {
    return Boolean.valueOf(value);
  }

  @Override
  public String toString(@Nonnull Boolean value) {
    return value.toString();
  }

  @Nonnull
  @Override
  public Boolean[] getValues() {
    return new Boolean[]{Boolean.TRUE, Boolean.FALSE};
  }
}

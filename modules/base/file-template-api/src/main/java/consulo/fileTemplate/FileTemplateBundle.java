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
package consulo.fileTemplate;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import consulo.fileTemplate.localize.FileTemplateLocalize;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 27-Mar-22
 */
@Deprecated
@DeprecationInfo("Use FileTemplateLocalize")
@MigratedExtensionsTo(FileTemplateLocalize.class)
public class FileTemplateBundle extends AbstractBundle {
  public static final FileTemplateBundle INSTANCE = new FileTemplateBundle();

  private FileTemplateBundle() {
    super("consulo.fileTemplate.FileTemplateBundle");
  }

  @Nonnull
  public static String message(@PropertyKey(resourceBundle = "consulo.fileTemplate.FileTemplateBundle") String key) {
    return INSTANCE.getMessage(key);
  }

  @Nonnull
  public static String message(@PropertyKey(resourceBundle = "consulo.fileTemplate.FileTemplateBundle") String key, Object... args) {
    return INSTANCE.getMessage(key, args);
  }
}

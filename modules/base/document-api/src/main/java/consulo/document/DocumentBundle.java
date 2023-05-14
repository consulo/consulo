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
package consulo.document;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19/01/2022
 */
public class DocumentBundle extends AbstractBundle {
  public static final DocumentBundle INSTANCE = new DocumentBundle();

  private DocumentBundle() {
    super("consulo.document.DocumentBundle");
  }

  @Nonnull
  public static String message(@PropertyKey(resourceBundle = "consulo.document.DocumentBundle") String key, Object... args) {
    return INSTANCE.getMessage(key, args);
  }
}

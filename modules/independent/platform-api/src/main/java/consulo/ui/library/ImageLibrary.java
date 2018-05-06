/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.library;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-05-06
 */
public class ImageLibrary {
  private static final Map<String, ImageProvider> cache = new ConcurrentHashMap<>();

  @Nonnull
  public static Supplier<Image> define(String id, Class<?> targetClazz) {
    return cache.computeIfAbsent(id, __ -> new ImageProvider(id, targetClazz));
  }
}

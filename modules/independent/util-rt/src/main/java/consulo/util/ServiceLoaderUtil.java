/*
 * Copyright 2013-2017 consulo.io
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
package consulo.util;

import javax.annotation.Nonnull;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 19-Oct-17
 */
public class ServiceLoaderUtil {
  @Nonnull
  public static <T> T loadSingleOrError(@Nonnull Class<T> clazz) {
    ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz, clazz.getClassLoader());
    Iterator<T> iterator = serviceLoader.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    throw new Error("Unable to find '" + clazz.getName() + "' implementation");
  }
}

/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.service;

import jakarta.annotation.Nonnull;

import java.net.URL;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 4/17/13 11:32 AM
 */
public interface ParametersEnhancer {
  /**
   * Allows to define custom classpath to be used at the in-process mode.
   * <p/>
   * <b>Note:</b> implement this method as no-op whenever possible. General design considerations are:
   * <pre>
   * <ul>
   *   <li>
   *     a class which implements this interface is located at an ide plugin. This class is loaded by corresponding 
   *     plugin class loader, i.e. the plugin' classpath is implicitly available during processing methods of object
   *     of the current class. This is the preferred approach (define all dependencies at the plugin level);
   *   </li>
   *   <li>
   *     it's possible that objects of the current class should be executed at context of a custom classpath (customized
   *     via the current method). Corresponding class loader with that custom classpath is created then, this class is loaded
   *     by it and new object of that new class is instantiated. That means that it's possible to have more than one instance
   *     of the same class which implements current interface at the single program. Those objects are loaded by different class loaders;
   *   </li>
   * </ul>
   * </pre>
   * 
   * @param urls
   */
  void enhanceLocalProcessing(@Nonnull List<URL> urls);
}
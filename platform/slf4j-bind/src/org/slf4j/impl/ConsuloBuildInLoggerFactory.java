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
package org.slf4j.impl;

import com.intellij.util.containers.ConcurrentFactoryMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * @author VISTALL
 */
public class ConsuloBuildInLoggerFactory implements ILoggerFactory {

  private ConcurrentFactoryMap<String, Logger> myLoggerCache = new ConcurrentFactoryMap<String, Logger>() {
    @Nullable
    @Override
    protected Logger create(String key) {
      com.intellij.openapi.diagnostic.Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(key);
      return new ConsuloBuildInLoggerAdapter(logger);
    }
  };

  @Override
  public Logger getLogger(String name) {
    return myLoggerCache.get(name);
  }
}

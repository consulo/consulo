/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.components;

import com.intellij.openapi.diagnostic.ControlFlowException;

/**
 * The base interface class for all components.
 *
 * @see ProjectComponent
 */
@Deprecated
public interface BaseComponent extends NamedComponent {
  class DefaultImplException extends RuntimeException implements ControlFlowException {
    public static final DefaultImplException INSTANCE = new DefaultImplException();

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  }

  /**
   * Component should perform initialization and communication with other components in this method.
   * This is called after {@link PersistentStateComponent#loadState(Object)}.
   */
  default void initComponent() {
    throw DefaultImplException.INSTANCE;
  }
}

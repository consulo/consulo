/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.execution.testframework.sm.runner.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;

/**
 * @author Roman Chernyatchik
 *
 * Should be used when one component wan't to propagate (transmit) selection to
 * other component(with/without capturing focus)
 */
public interface PropagateSelectionHandler {
  void handlePropagateSelectionRequest(@Nullable SMTestProxy selectedTestProxy,
                                       @Nonnull Object sender,
                                       boolean requestFocus);
}

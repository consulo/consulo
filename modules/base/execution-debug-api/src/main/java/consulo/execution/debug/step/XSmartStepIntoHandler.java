/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.step;

import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.frame.XSuspendContext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Implement this class and return its instance from {@link XDebugProcess#getSmartStepIntoHandler()} to support
 * Smart Step Into action
 *
 * @author nik
 */
public abstract class XSmartStepIntoHandler<Variant extends XSmartStepIntoVariant> {

  /**
   * @param position current position
   * @return list of function/method calls containing in the current line
   */
  @Nonnull
  public abstract List<Variant> computeSmartStepVariants(@Nonnull XSourcePosition position);

  /**
   * Resume execution and call {@link XDebugSession#positionReached(XSuspendContext)}
   * when {@code variant} function/method is reached
   * @param variant selected variant
   */
  public void startStepInto(@Nonnull Variant variant) {
    throw new AbstractMethodError();
  }

  public void startStepInto(@Nonnull Variant variant, @Nullable XSuspendContext context) {
    startStepInto(variant);
  }

  /**
   * @return title for popup which will be shown to select method/function
   * @param position current position
   */
  public abstract String getPopupTitle(@Nonnull XSourcePosition position);
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.step;

public interface ForceSmartStepIntoSource {
  /** Whether smart step into should be used even for one-variant code */
  default boolean needForceSmartStepInto() {
    return false;
  }
}

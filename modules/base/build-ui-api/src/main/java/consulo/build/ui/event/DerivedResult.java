// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.event;


/**
 * result status of task is derived by its children.
 */
public interface DerivedResult extends EventResult {
  
  FailureResult createFailureResult();

  
  EventResult createDefaultResult();
}


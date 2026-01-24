// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.ui;

import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface LinkedValuesMapping {
  @Nullable
  List<ValueWithPosition> getLinkedValues(ValueWithPosition value);
}

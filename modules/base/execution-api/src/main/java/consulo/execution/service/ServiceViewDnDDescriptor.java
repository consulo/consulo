// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.service;

import consulo.ui.ex.awt.dnd.DnDEvent;

public interface ServiceViewDnDDescriptor {
  boolean canDrop(DnDEvent event, Position position);

  void drop(DnDEvent event, Position position);

  enum Position {
    ABOVE,
    INTO,
    BELOW
  }
}

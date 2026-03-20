// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal.perfomance;

public interface Activity {
  void end();

  void setDescription(String description);

  /**
   * Convenient method to end token and start a new sibling one.
   * So, start of new is always equals to this item end and yet another System.nanoTime() call is avoided.
   */
  Activity endAndStart(String name);

  
  Activity startChild(String name);
}

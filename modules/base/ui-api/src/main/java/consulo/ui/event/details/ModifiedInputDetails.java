/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.event.details;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 17/08/2021
 *
 * Always abstract
 */
public abstract class ModifiedInputDetails extends InputDetails {
  public enum Modifier {
    ALT,
    CTRL,
    SHIFT,
    META,
    // WINDOWS ? windows eat all combination for it
  }

  private final EnumSet<Modifier> myModifiers;

  public ModifiedInputDetails(int x, int y, @Nonnull EnumSet<Modifier> modifiers) {
    super(x, y);
    myModifiers = modifiers;
  }

  @Nonnull
  public Set<Modifier> getModifiers() {
    return myModifiers;
  }

  public boolean withAlt() {
    return myModifiers.contains(Modifier.ALT);
  }

  public boolean withCtrl() {
    return myModifiers.contains(Modifier.CTRL);
  }

  public boolean withMeta() {
    return myModifiers.contains(Modifier.META);
  }

  public boolean withShift() {
    return myModifiers.contains(Modifier.SHIFT);
  }
}

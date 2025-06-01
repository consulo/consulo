/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor.inspection.scheme;

import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.component.util.pointer.Named;
import jakarta.annotation.Nonnull;

/**
 * User: anna
 * Date: 20-Nov-2005
 *
 * TODO [VISTALL] move all methods to InspectionProfile and drop
 */
@Deprecated
public interface Profile extends JDOMExternalizable, Comparable<Profile>, Named {
  void copyFrom(@Nonnull Profile profile);

  void setLocal(boolean isLocal);

  @Deprecated
  /**
   * @deprecated Use !{@link #isProjectLevel()}
   */
  boolean isLocal();

  boolean isProjectLevel();

  void setProjectLevel(boolean isProjectLevel);

  void setName(@Nonnull String name);

  @Override
  @Nonnull
  String getName();

  void setProfileManager(@Nonnull ProfileManager profileManager);

  @Nonnull
  ProfileManager getProfileManager();
}

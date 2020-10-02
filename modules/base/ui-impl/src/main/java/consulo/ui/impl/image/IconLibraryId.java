/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.impl.image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-10-02
 */
public class IconLibraryId {
  private final String myId;
  private final String myBaseId;

  public IconLibraryId(@Nonnull String id, @Nullable String baseId) {
    myId = id;
    myBaseId = baseId;
  }

  public String getId() {
    return myId;
  }

  public String getBaseId() {
    return myBaseId;
  }

  @Override
  public String toString() {
    return "IconLibName{" + "myId='" + myId + '\'' + ", myBaseId='" + myBaseId + '\'' + '}';
  }
}

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
package consulo.desktop.awt.fileChooser.impl;

import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

public interface FileLookup {

  interface Finder {
    @Nullable
    LookupFile find(@Nonnull String path);

    String normalize(@Nonnull final String path);

    String getSeparator();
  }

  interface LookupFile {

    String getName();
    String getAbsolutePath();
    boolean isDirectory();

    void setMacro(String macro);
    String getMacro();

    List<LookupFile> getChildren(LookupFilter filter);

    @Nullable
    LookupFile getParent();

    boolean exists();

    @Nullable
    Image getIcon();
  }

  interface LookupFilter {
    boolean isAccepted(LookupFile file);
  }
}

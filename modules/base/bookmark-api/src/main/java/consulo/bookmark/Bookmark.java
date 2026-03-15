/*
 * Copyright 2013-2022 consulo.io
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
package consulo.bookmark;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.navigation.Navigatable;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
public interface Bookmark extends Navigatable {
  @Nullable
  Document getDocument();

  
  VirtualFile getFile();

  
  @Deprecated(forRemoval = true)
  default Image getIcon() {
    return getIcon(true);
  }

  String getDescription();

  char getMnemonic();

  
  Image getIcon(boolean gutter);

  boolean isValid();

  int getLine();

  @RequiredReadAction
  
  String getQualifiedName();
}

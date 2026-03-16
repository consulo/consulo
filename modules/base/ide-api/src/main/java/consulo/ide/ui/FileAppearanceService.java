/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.ui.ex.ColoredTextContainer;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;
import java.util.function.Consumer;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class FileAppearanceService {
  public static FileAppearanceService getInstance() {
    return Application.get().getInstance(FileAppearanceService.class);
  }

  
  public abstract CellAppearanceEx forVirtualFile(VirtualFile file);

  
  public abstract CellAppearanceEx forIoFile(File file);

  
  public abstract CellAppearanceEx forInvalidUrl(String url);

  
  public Consumer<ColoredTextContainer> getRenderForVirtualFile(VirtualFile file) {
    return it -> forVirtualFile(file).customize(it);
  }

  
  public Consumer<ColoredTextContainer> getRenderForIoFile(File file) {
    return it -> forIoFile(file).customize(it);
  }

  
  public Consumer<ColoredTextContainer> getRenderForInvalidUrl(String url) {
    return it -> forInvalidUrl(url).customize(it);
  }
}

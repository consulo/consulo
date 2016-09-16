/*
 * Copyright 2013-2016 consulo.io
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
package consulo.projectImport.model.library;

import consulo.projectImport.model.ModelContainer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author VISTALL
 * @since 17:36/19.06.13
 */
public class LibraryRootTableModel extends ModelContainer {
  private final OrderRootTypeModel myRootType;

  public LibraryRootTableModel(OrderRootTypeModel rootType) {
    myRootType = rootType;
  }

  @NotNull
  public List<String> getRoots() {
    return findChildren(String.class);
  }

  public void addUrl(@NotNull String url) {
    addChild(url);
  }

  public OrderRootTypeModel getRootType() {
    return myRootType;
  }
}

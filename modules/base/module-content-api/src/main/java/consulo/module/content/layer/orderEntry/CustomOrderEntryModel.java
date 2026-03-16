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
package consulo.module.content.layer.orderEntry;

import consulo.content.RootProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.Synthetic;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 21-May-22
 */
public interface CustomOrderEntryModel extends Synthetic {
  /**
   * Initialize module with target moduleRootLayer once, after adding to layer. Also called after {@link #clone()} call.
   *
   * This method will be called before all other method calls
   * 
   * @param moduleRootLayer target layer
   */
  void bind(ModuleRootLayer moduleRootLayer);

  
  String getPresentableName();

  boolean isValid();

  
  RootProvider getRootProvider();

  
  CustomOrderEntryModel clone();

  @Nullable
  default Object getEqualObject() {
    return null;
  }

  default boolean isEquivalentTo(CustomOrderEntryModel otherModel) {
    return false;
  }
}

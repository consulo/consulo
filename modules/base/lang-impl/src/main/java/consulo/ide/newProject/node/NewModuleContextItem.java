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
package consulo.ide.newProject.node;

import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-06-02
 */
public class NewModuleContextItem extends NewModuleContextNode implements Comparable<NewModuleContextItem> {
  private final NewModuleBuilderProcessor<?> myProcessor;

  private int myWeight;

  public NewModuleContextItem(@Nonnull LocalizeValue name, @Nullable Image image, int weight, NewModuleBuilderProcessor<?> processor) {
    super(name, image);
    myProcessor = processor;
    myWeight = weight;
  }

  public NewModuleBuilderProcessor<?> getProcessor() {
    return myProcessor;
  }

  @Override
  public int compareTo(@Nonnull NewModuleContextItem o) {
    if (myWeight == o.myWeight) {
      return getName().getValue().compareToIgnoreCase(o.getName().getValue());
    }
    return Integer.compare(myWeight, o.myWeight);
  }
}

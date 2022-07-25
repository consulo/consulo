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

import consulo.content.OrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.fileChooser.FileChooserDescriptor;

import javax.annotation.Nullable;
import java.util.List;

public class SdkPathEditor extends PathEditor {
  private final String myDisplayName;
  private final OrderRootType myOrderRootType;
  private final boolean myImmutable;

  public SdkPathEditor(String displayName, OrderRootType orderRootType, FileChooserDescriptor descriptor, Sdk sdk) {
    this(displayName, orderRootType, descriptor, sdk.isPredefined());
  }

  public SdkPathEditor(String displayName, OrderRootType orderRootType, FileChooserDescriptor descriptor, boolean immutable) {
    super(descriptor);
    myDisplayName = displayName;
    myOrderRootType = orderRootType;
    myImmutable = immutable;
  }

  @Override
  public boolean isImmutable() {
    return myImmutable;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  public OrderRootType getOrderRootType() {
    return myOrderRootType;
  }

  public void apply(SdkModificator sdkModificator) {
    sdkModificator.removeRoots(myOrderRootType);
    // add all items
    for (int i = 0; i < getRowCount(); i++) {
      sdkModificator.addRoot(getValueAt(i), myOrderRootType);
    }
    setModified(false);
  }

  public void reset(@Nullable SdkModificator modificator) {
    if (modificator != null) {
      resetPath(List.of(modificator.getRoots(myOrderRootType)));
    }
    else {
      setEnabled(false);
    }
  }

}

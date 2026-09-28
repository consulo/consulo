/*
 * Copyright 2013-2026 consulo.io
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
package consulo.module.impl.internal.layer.orderEntry;

import consulo.module.content.layer.orderEntry.*;
import consulo.module.impl.internal.layer.ModuleRootLayerImpl;

/// A custom order entry that can be exported, so that a module depending on its owner sees its roots.
///
/// Chosen over [CustomOrderEntryImpl] when the type answers [CustomOrderEntryTypeProvider#isExportable()].
/// A type that does not opt in gets plain entries, and its module files keep the shape they had
/// before this variant existed.
public class ExportableCustomOrderEntryImpl<M extends CustomOrderEntryModel> extends CustomOrderEntryImpl<M>
  implements ExportableOrderEntry {
  private boolean myExported;
  private DependencyScope myScope = DependencyScope.COMPILE;

  public ExportableCustomOrderEntryImpl(OrderEntryType<?> provider, ModuleRootLayerImpl rootLayer, M data, boolean init) {
    super(provider, rootLayer, data, init);
  }

  @Override
  public boolean isExported() {
    return myExported;
  }

  @Override
  public void setExported(boolean value) {
    getRootModel().assertWritable();
    myExported = value;
  }

  @Override
  public DependencyScope getScope() {
    return myScope;
  }

  @Override
  public void setScope(DependencyScope scope) {
    getRootModel().assertWritable();
    myScope = scope;
  }

  /// Applied while reading a module file, where the layer is not writable yet.
  void setExportedRaw(boolean value) {
    myExported = value;
  }

  /// Applied while reading a module file, where the layer is not writable yet.
  void setScopeRaw(DependencyScope scope) {
    myScope = scope;
  }

  @Override
  @SuppressWarnings("unchecked")
  public OrderEntry cloneEntry(ModuleRootLayerImpl layer) {
    assert !isDisposed();

    M cloneModel = (M)myModel.clone();
    cloneModel.bind(layer);

    ExportableCustomOrderEntryImpl<M> clone = new ExportableCustomOrderEntryImpl<>(getType(), layer, cloneModel, true);
    // assigned rather than set, since the layer being cloned into is not writable yet
    clone.myExported = myExported;
    clone.myScope = myScope;
    return clone;
  }
}

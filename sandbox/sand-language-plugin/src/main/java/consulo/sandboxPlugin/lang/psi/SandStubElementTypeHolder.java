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
package consulo.sandboxPlugin.lang.psi;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.ObjectStubSerializerProvider;
import consulo.language.psi.stub.StubElementTypeHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author VISTALL
 * @since 09-Jul-22
 */
@ExtensionImpl
public class SandStubElementTypeHolder extends StubElementTypeHolder<SandStubTokenType> {
  @Nullable
  @Override
  public String getExternalIdPrefix() {
    return "sand";
  }

  @Nonnull
  @Override
  public List<ObjectStubSerializerProvider> loadSerializers() {
    return allFromStaticFields(SandStubTokenType.class, Field::get);
  }
}

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
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * An extension that enumerates classes defining stub element types for all languages. Index infrastructure needs
 * their names to work correctly, so they're loaded via this extension when necessary.<p></p>
 * <p>
 * To speed up IDE loading, it's recommended that this extension is used for interfaces containing only
 * {@link IStubElementType} (or {@link ObjectStubSerializer}) constants,
 * and all other language's element types are kept in a separate interface that can be loaded later.
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class StubElementTypeHolder<T> {
  private static final Logger LOG = Logger.getInstance(StubElementTypeHolder.class);

  protected interface FieldValueGetter {
    Object get(Field field, Object obj) throws IllegalArgumentException, IllegalAccessException;
  }

  /**
   * Allows to avoid class initialization by declaring that the stub element type holder obeys the following contract:
   * <ul>
   * <li>It's an interface</li>
   * <li>All stub element types to load are declared as fields in the interface itself, not in super-interfaces</li>
   * <li>For all {@link IStubElementType} fields, their {@link IStubElementType#getExternalId()}:
   * <ul>
   * <li>doesn't depend on class fields, so that it can be called during IStubElementType construction</li>
   * <li>effectively returns {@code "somePrefix" + debugName}</li>
   * <li>{@code debugName} is equal to the field name</li>
   * <li>"somePrefix" is the value of "externalIdPrefix" attribute</li>
   * </ul>
   * </li>
   * <li>For all other fields, if any, the same {@code prefix+debugName} concatenation doesn't produce an external id used by any other stub element type</li>
   * </ul>
   */
  @Nullable
  public abstract String getExternalIdPrefix();

  /**
   * Load serializer. By default use {@link #allFromStaticFields(Class, FieldValueGetter)}
   */
  @Nonnull
  public abstract List<ObjectStubSerializerProvider> loadSerializers();

  @Nonnull
  protected final List<ObjectStubSerializerProvider> allFromStaticFields(@Nonnull Class<T> clazz, @Nonnull FieldValueGetter fieldGetter) {
    String externalIdPrefix = getExternalIdPrefix();
    boolean isLazy = externalIdPrefix != null;
    Field[] fields = clazz.getFields();
    List<ObjectStubSerializerProvider> serializers = new ArrayList<>(fields.length);
    for (Field field : fields) {
      LazyObjectStubSerializerProvider accessor = new LazyObjectStubSerializerProvider(externalIdPrefix, field, fieldGetter);
      if (!isLazy) {
        // initialize value due it's not lazy serializer types
        accessor.getObjectStubSerializer();
      }
      serializers.add(accessor);
    }

    return serializers;
  }
}

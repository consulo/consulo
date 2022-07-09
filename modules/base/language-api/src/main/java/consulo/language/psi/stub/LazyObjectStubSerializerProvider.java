// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Objects;

class LazyObjectStubSerializerProvider implements ObjectStubSerializerProvider {
  private static final Logger LOG = Logger.getInstance(LazyObjectStubSerializerProvider.class);

  private final Field myField;
  private final StubElementTypeHolder.FieldValueGetter myFieldValueGetter;

  final String myExternalIdPrefix;
  private volatile ObjectStubSerializer myFieldValue;

  LazyObjectStubSerializerProvider(@Nullable String externalIdPrefix, Field field, StubElementTypeHolder.FieldValueGetter fieldGetter) {
    myField = field;
    myExternalIdPrefix = externalIdPrefix;
    myFieldValueGetter = fieldGetter;
  }

  @Override
  public boolean isLazy() {
    return myExternalIdPrefix != null;
  }

  @Nonnull
  @Override
  public String getExternalId() {
    // not lazy
    if (myExternalIdPrefix == null) {
      ObjectStubSerializer serializer = getObjectStubSerializer();
      assert serializer != null;
      return serializer.getExternalId();
    }
    else {
      return myExternalIdPrefix + "." + myField.getName();
    }
  }

  @Override
  public ObjectStubSerializer getObjectStubSerializer() {
    ObjectStubSerializer delegate = myFieldValue;
    if (delegate == null) {
      try {
        myFieldValue = delegate = (ObjectStubSerializer)myFieldValueGetter.get(myField, null);

        if (!Objects.equals(getExternalId(), myFieldValue.getExternalId())) {
          LOG.error(myFieldValue.getClass().getName() +
                    "  return wrong 'externalId'. Expected: " +
                    getExternalId() +
                    ", actual: " +
                    myFieldValue.getExternalId() +
                    ". Please check " +
                    "StubElementTypeHolder#getExternalId() value");
        }
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      if (!delegate.getExternalId().equals(myExternalIdPrefix)) {
        throw new IllegalStateException(
                "External id mismatch in " + this + ". " + "Judging by extension declaration it should be " + myExternalIdPrefix + " but " + delegate.getExternalId() + " is returned.");
      }
    }
    return delegate;
  }

  @Override
  public String toString() {
    return myField.getName();
  }
}

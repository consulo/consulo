// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.language.ast.IElementType;

import java.util.List;

@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public abstract class SerializationManager {
  private volatile boolean mySerializersLoaded;

  public static SerializationManager getInstance() {
    return ApplicationManager.getApplication().getInstance(SerializationManager.class);
  }

  public void registerSerializer(ObjectStubSerializer serializer) {
    registerSerializer(serializer.getExternalId(), new NotLazyObjectStubSerializerProvider(serializer));
  }

  protected abstract void registerSerializer(String externalId, ObjectStubSerializerProvider provider);

  public void initSerializers() {
    if (mySerializersLoaded) return;
    //noinspection SynchronizeOnThis
    synchronized (this) {
      if (mySerializersLoaded) return;
      List<ObjectStubSerializerProvider> lazySerializers = IStubElementType.loadRegisteredStubElementTypes();
      final IElementType[] stubElementTypes = IElementType.enumerate(type -> type instanceof StubSerializer);

      for (IElementType type : stubElementTypes) {
        if (type instanceof StubFileElementType && StubFileElementType.DEFAULT_EXTERNAL_ID.equals(((StubFileElementType)type).getExternalId())) {
          continue;
        }

        registerSerializer((StubSerializer)type);
      }

      for (ObjectStubSerializerProvider serializerProvider : lazySerializers) {
        registerSerializer(serializerProvider.getExternalId(), serializerProvider);
      }
      mySerializersLoaded = true;
    }
  }

  public abstract String internString(String string);
}
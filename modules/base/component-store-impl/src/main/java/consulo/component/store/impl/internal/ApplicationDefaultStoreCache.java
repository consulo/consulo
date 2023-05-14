/*
 * Copyright 2013-2019 consulo.io
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
package consulo.component.store.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2019-04-10
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class ApplicationDefaultStoreCache implements Disposable {
  private final Map<Pair<ClassLoader, String>, Object> myUrlCache = new ConcurrentHashMap<>();

  @Nullable
  public Element findDefaultStoreElement(@Nonnull Class<?> clazz, @Nonnull String path) {
    Object result = myUrlCache.computeIfAbsent(Pair.create(clazz.getClassLoader(), path), pair -> {
      URL resource = pair.getFirst().getResource(pair.getSecond());

      if (resource != null) {
        try {
          Document document = JDOMUtil.loadDocument(resource);
          Element rootElement = document.getRootElement();
          rootElement.detach();
          return rootElement;
        }
        catch (JDOMException | IOException e) {
          throw new RuntimeException(e);
        }
      }

      return ObjectUtil.NULL;
    });

    return result == ObjectUtil.NULL ? null : (Element)result;
  }

  @Override
  public void dispose() {
    myUrlCache.clear();
  }
}

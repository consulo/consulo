/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.util;

import com.intellij.util.ImageLoader;

import java.awt.*;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 6/7/17
 */
public class URLDictionatyLoader extends Dictionary<URL, Image> {
  private Map<URL, Image> myImages = new HashMap<>();

  @Override
  public int size() {
    return myImages.size();
  }

  @Override
  public boolean isEmpty() {
    return myImages.isEmpty();
  }

  @Override
  public Enumeration<URL> keys() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Enumeration<Image> elements() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Image get(Object o) {
    return myImages.computeIfAbsent((URL) o, ImageLoader::loadFromUrl);
  }

  @Override
  public Image put(URL url, Image image) {
    return myImages.put(url, image);
  }

  @Override
  public Image remove(Object o) {
    return myImages.remove(o);
  }
}

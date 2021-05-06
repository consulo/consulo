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
package consulo.awt.hacking;

import consulo.awt.hacking.util.MethodInvocator;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class SunVolatileImageHacking {
  private static final Class sunVolatileImageClazz;

  static {
    try {
      sunVolatileImageClazz = Class.forName("sun.awt.image.SunVolatileImage");
    }
    catch (ClassNotFoundException e) {
      throw new Error(e);
    }
  }

  private static final MethodInvocator getGraphicsConfig = new MethodInvocator(sunVolatileImageClazz, "getGraphicsConfig");

  public static boolean isSunVolatileImage(Image image) {
    return sunVolatileImageClazz.isInstance(image);
  }

  public static GraphicsConfiguration getGraphicsConfig(Image image) {
    if (isSunVolatileImage(image) && getGraphicsConfig.isAvailable()) {
      return (GraphicsConfiguration)getGraphicsConfig.invoke(image);
    }
    return null;
  }
}

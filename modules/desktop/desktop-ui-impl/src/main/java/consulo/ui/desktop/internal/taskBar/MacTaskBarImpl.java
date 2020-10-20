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
package consulo.ui.desktop.internal.taskBar;

import com.intellij.util.ui.UIUtil;
import consulo.logging.Logger;
import consulo.ui.UIAccess;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
public class MacTaskBarImpl extends DefaultJava9TaskBarImpl {
  private static final Logger LOG = Logger.getInstance(MacTaskBarImpl.class);

  private BufferedImage myAppImage;

  private BufferedImage getAppImage() {
    assertIsDispatchThread();

    try {
      if (myAppImage != null) return myAppImage;

      Object app = getApp();
      Image appImage = (Image)getAppMethod("getDockIconImage").invoke(app);

      if (appImage == null) return null;

      int width = appImage.getWidth(null);
      int height = appImage.getHeight(null);
      BufferedImage img = UIUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = img.createGraphics();
      g2d.drawImage(appImage, null, null);
      myAppImage = img;
    }
    catch (NoSuchMethodException e) {
      return null;
    }
    catch (Exception e) {
      LOG.error(e);
    }

    return myAppImage;
  }



  //@Override
  //public void setOkBadge(@Nonnull Window window, boolean visible) {
  //  assertIsDispatchThread();
  //
  //  if (getAppImage() == null) return;
  //
  //  AppImage img = createAppImage();
  //
  //  if (visible) {
  //    consulo.ui.image.Image okIcon = AllIcons.Mac.AppIconOk512;
  //
  //    int x = img.myImg.getWidth() - okIcon.getWidth();
  //    int y = 0;
  //
  //    TargetAWT.to(okIcon).paintIcon(JOptionPane.getRootFrame(), img.myG2d, x, y);
  //  }
  //
  //  setDockIcon(img.myImg);
  //}


  private AppImage createAppImage() {
    BufferedImage appImage = getAppImage();
    assert appImage != null;
    BufferedImage current = UIUtil.createImage(appImage.getWidth(), appImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = current.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.drawImage(appImage, null, null);
    return new AppImage(current, g);
  }

  private static class AppImage {
    BufferedImage myImg;
    Graphics2D myG2d;

    AppImage(BufferedImage img, Graphics2D g2d) {
      myImg = img;
      myG2d = g2d;
    }
  }

  private static void setDockIcon(BufferedImage image) {
    try {
      getAppMethod("setDockIconImage", Image.class).invoke(getApp(), image);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private static Method getAppMethod(final String name, Class... args) throws NoSuchMethodException, ClassNotFoundException {
    return getAppClass().getMethod(name, args);
  }

  private static Object getApp() throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
    return getAppClass().getMethod("getApplication").invoke(null);
  }

  private static Class<?> getAppClass() throws ClassNotFoundException {
    return Class.forName("com.apple.eawt.Application");
  }

  private void assertIsDispatchThread() {
    UIAccess.assertIsUIThread();
  }
}

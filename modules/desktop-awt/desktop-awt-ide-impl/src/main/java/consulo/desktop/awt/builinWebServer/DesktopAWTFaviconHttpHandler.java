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
package consulo.desktop.awt.builinWebServer;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.http.HTTPMethod;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 16-Sep-22
 */
@ExtensionImpl
public class DesktopAWTFaviconHttpHandler extends HttpRequestHandler {
  @Override
  public boolean isSupported(HttpRequest request) {
    return request.method() == HTTPMethod.GET && request.path().equals("/favicon.ico");
  }

  @Nonnull
  @Override
  public HttpResponse process(@Nonnull HttpRequest request) throws IOException {
    Icon icon = TargetAWT.to(Application.get().getIcon());
    BufferedImage image = UIUtil.createImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    icon.paintIcon(null, image.getGraphics(), 0, 0);
    byte[] icoBytes;
    try {
      icoBytes = Imaging.writeImageToBytes(image, ImageFormats.ICO);
    }
    catch (ImageWriteException e) {
      throw new IOException(e);
    }
    return HttpResponse.ok("image/vnd.microsoft.icon", icoBytes);
  }
}

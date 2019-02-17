/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.web.servlet;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-05-10
 */
public class SchemeRequestHandler extends SynchronizedRequestHandler {

  private byte[] generatedCss;

  @Override
  protected boolean canHandleRequest(VaadinRequest request) {
    return hasPathPrefix(request, "GENERATED");
  }

  @Override
  public boolean synchronizedHandleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
    response.setContentType("text/css");

    byte[] text = generateCss();

    response.setCacheTime(-1);
    response.setStatus(HttpURLConnection.HTTP_OK);

    try (OutputStream stream = response.getOutputStream()) {
      response.setContentLength(text.length);
      stream.write(text);
    }

    return true;
  }

  @Nonnull
  private byte[] generateCss() {
    if (generatedCss != null) {
      return generatedCss;
    }

    Application application = ApplicationManager.getApplication();
    if (application == null) {
      return new byte[0];
    }

    EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();

    StringBuilder builder = new StringBuilder();
    Map<ColorKey, Color> colors = new LinkedHashMap<>();
    globalScheme.fillColors(colors);

    for (Map.Entry<ColorKey, Color> entry : colors.entrySet()) {
      ColorKey key = entry.getKey();
      Color value = entry.getValue();

      if (value == null) {
        continue;
      }

      builder.append(".").append(key.getExternalName()).append("_bg {\n");
      appendColor("background-color", value, builder);
      builder.append("}\n");

      builder.append(".").append(key.getExternalName()).append("_fg {\n");
      appendColor("color", value, builder);
      builder.append("}\n");

      builder.append(".").append(key.getExternalName()).append("_brc {\n");
      appendColor("border-right-color", value, builder);
      builder.append("}\n");
    }

    Map<TextAttributesKey, TextAttributes> attributes = new LinkedHashMap<>();
    globalScheme.fillAttributes(attributes);
    for (Map.Entry<TextAttributesKey, TextAttributes> entry : attributes.entrySet()) {
      TextAttributesKey key = entry.getKey();
      TextAttributes value = entry.getValue();
      if (value == null) {
        continue;
      }

      builder.append(".").append(key.getExternalName()).append("_attr {\n");
      Color backgroundColor = value.getBackgroundColor();
      if (backgroundColor != null) {
        appendColor("background-color", backgroundColor, builder);
      }

      Color foregroundColor = value.getForegroundColor();
      if (foregroundColor != null) {
        appendColor("color", foregroundColor, builder);
      }

      builder.append("}\n");
    }

    return generatedCss = builder.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void appendColor(String property, Color value, StringBuilder builder) {
    builder.append("    ").append(property).append(":").append("rgb(").append(value.getRed()).append(", ").append(value.getGreen()).append(", ").append(value.getBlue()).append(");\n");
  }

  private static boolean hasPathPrefix(VaadinRequest request, String prefix) {
    String pathInfo = request.getPathInfo();

    if (pathInfo == null) {
      return false;
    }

    if (!prefix.startsWith("/")) {
      prefix = '/' + prefix;
    }

    if (pathInfo.startsWith(prefix)) {
      return true;
    }

    return false;
  }
}

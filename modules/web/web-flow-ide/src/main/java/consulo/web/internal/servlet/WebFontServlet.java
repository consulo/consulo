/*
 * Copyright 2013-2026 consulo.io
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
package consulo.web.internal.servlet;

import consulo.ui.ex.font.BundledFont;
import consulo.web.ui.impl.internal.WebFontRegistry;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 2026-08-01
 */
@WebServlet(urlPatterns = WebFontRegistry.FONT_PATH + "*")
public class WebFontServlet extends HttpServlet {
    private static final String CONTENT_TYPE = "font/ttf";

    // the faces never change within a build, and a reload that refetches them would relayout every editor
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        BundledFont font = WebFontRegistry.findFont(pathInfo.substring(1));
        if (font == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (InputStream stream = font.url().openStream()) {
            resp.setContentType(CONTENT_TYPE);
            resp.setHeader("Cache-Control", CACHE_CONTROL);

            stream.transferTo(resp.getOutputStream());
        }
    }
}

/*
 * Copyright 2013-2016 consulo.io
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

import consulo.web.ui.impl.internal.image.WebImageRenderer;
import consulo.web.ui.impl.internal.image.WebImageSpec;
import consulo.web.ui.impl.internal.image.WebImageUrl;
import consulo.web.ui.impl.internal.image.WebRenderedImage;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
@WebServlet(urlPatterns = WebImageUrl.PATH)
public class UIIconServlet extends HttpServlet {
    private static final int MAX_CACHED_IMAGES = 4096;

    /**
     * Keyed on the whole composition and not on the icon it starts from - the same icon is served colorized,
     * faded and layered under different urls, and one of those results must never answer for another.
     */
    private final Map<String, WebRenderedImage> myCache = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebImageSpec spec = readSpec(req);
        if (spec == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String version = req.getParameter(WebImageUrl.VERSION);
        String key = WebImageSpec.encode(spec) + '@' + String.valueOf(version);

        WebRenderedImage image = myCache.get(key);
        if (image == null) {
            // the url names the library it was built for, and that is what it has to be answered from - the
            // active library is whatever the last style switch left behind, and a browser asks for an url long
            // after the page which built it was rendered
            image = WebImageRenderer.render(spec, WebImageUrl.toLibraryId(version));
            if (image == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (myCache.size() >= MAX_CACHED_IMAGES) {
                myCache.clear();
            }
            myCache.put(key, image);
        }

        byte[] bytes = image.data();

        resp.setContentType(image.contentType());
        resp.setContentLength(bytes.length);
        // the url carries the icon library version, so a hit never outlives the bytes it stands for
        resp.setHeader("Cache-Control", "private, max-age=86400");

        ServletOutputStream outputStream = resp.getOutputStream();
        outputStream.write(bytes);
        outputStream.close();
    }

    private static @Nullable WebImageSpec readSpec(HttpServletRequest req) {
        String spec = req.getParameter(WebImageUrl.SPEC);
        if (spec != null) {
            return WebImageSpec.decode(spec);
        }

        String groupId = req.getParameter(WebImageUrl.GROUP_ID);
        String imageId = req.getParameter(WebImageUrl.IMAGE_ID);
        if (groupId == null || imageId == null) {
            return null;
        }

        return new WebImageSpec.Key(groupId, imageId, 0, 0);
    }
}

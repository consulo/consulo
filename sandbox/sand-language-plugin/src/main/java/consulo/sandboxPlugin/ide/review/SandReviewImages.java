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
package consulo.sandboxPlugin.ide.review;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.ui.image.Image;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public final class SandReviewImages {
    public static final String DIAGRAM = "sand:diagram";

    public static final AsyncHtmlImageLoader HTML_IMAGES = (baseUrl, src) -> CompletableFuture.supplyAsync(
        () -> DIAGRAM.equals(src) ? diagram() : null,
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS, AppExecutorUtil.getAppExecutorService())
    );

    private static @Nullable Image diagram() {
        StringBuilder svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"900\" height=\"240\" viewBox=\"0 0 900 240\">");
        svg.append("<rect width=\"900\" height=\"240\" rx=\"12\" fill=\"#eef3fb\" stroke=\"#5dade2\" stroke-width=\"4\"/>");
        String[] colors = {"#f7a35c", "#5dade2", "#58d68d", "#bb8fce"};
        for (int i = 0; i < colors.length; i++) {
            int x = 40 + i * 215;
            svg.append("<rect x=\"").append(x).append("\" y=\"70\" width=\"170\" height=\"100\" rx=\"10\" fill=\"").append(colors[i]).append("\"/>");
            if (i < colors.length - 1) {
                svg.append("<path d=\"M").append(x + 172).append(" 120 L").append(x + 213).append(" 120\" stroke=\"#1f4e99\" stroke-width=\"6\"/>");
            }
        }
        svg.append("</svg>");
        try {
            return Image.fromBytes(Image.ImageType.SVG, svg.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            return null;
        }
    }

    private SandReviewImages() {
    }
}

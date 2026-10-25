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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.icon.AsyncImageIconsProvider;
import consulo.review.icon.AsyncImageLoader;
import consulo.review.icon.CachingIconsProvider;
import consulo.review.icon.IconsProvider;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public final class SandReviewAvatars {
    private static final int SIZE = 64;

    private static final String[][] COLORS = {
        {"#f7a35c", "#c0392b"},
        {"#5dade2", "#1f4e99"},
        {"#58d68d", "#117a65"},
        {"#bb8fce", "#6c3483"},
    };

    private static final IconsProvider<SandReviewUser> ourAvatars = new CachingIconsProvider<>(new AsyncImageIconsProvider<>(new AsyncImageLoader<>() {
        @Override
        public CompletableFuture<@Nullable Image> load(SandReviewUser key) {
            return CompletableFuture.supplyAsync(() -> render(key), CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS, AppExecutorUtil.getAppExecutorService()));
        }

        @Override
        public Image createBaseIcon(@Nullable SandReviewUser key, int iconSize) {
            return ImageEffects.resize(PlatformIconGroup.actionsLoginavatar(), iconSize, iconSize);
        }
    }));

    public static Image getAvatar(SandReviewUser user) {
        return ourAvatars.getIcon(user, SIZE);
    }

    private static @Nullable Image render(SandReviewUser user) {
        String[] colors = COLORS[Math.floorMod(user.username().hashCode(), COLORS.length)];
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + SIZE + "\" height=\"" + SIZE + "\" viewBox=\"0 0 64 64\">"
            + "<defs><linearGradient id=\"g\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
            + "<stop offset=\"0\" stop-color=\"" + colors[0] + "\"/>"
            + "<stop offset=\"1\" stop-color=\"" + colors[1] + "\"/>"
            + "</linearGradient></defs>"
            + "<rect width=\"64\" height=\"64\" fill=\"url(#g)\"/>"
            + "<circle cx=\"32\" cy=\"26\" r=\"12\" fill=\"#ffffff\" fill-opacity=\"0.85\"/>"
            + "<rect x=\"14\" y=\"42\" width=\"36\" height=\"22\" rx=\"11\" fill=\"#ffffff\" fill-opacity=\"0.85\"/>"
            + "</svg>";
        try {
            return Image.fromBytes(Image.ImageType.SVG, svg.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            return null;
        }
    }

    private SandReviewAvatars() {
    }
}

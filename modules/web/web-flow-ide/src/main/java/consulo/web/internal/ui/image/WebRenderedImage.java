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
package consulo.web.internal.ui.image;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author VISTALL
 * @since 2026-08-01
 */
public record WebRenderedImage(byte[] data, boolean svg) {
    public static WebRenderedImage svg(String text) {
        return new WebRenderedImage(text.getBytes(StandardCharsets.UTF_8), true);
    }

    public String contentType() {
        return svg ? "image/svg+xml" : "image/png";
    }

    public String toDataURI() {
        return "data:" + contentType() + ";base64," + Base64.getEncoder().encodeToString(data);
    }
}

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
package consulo.webBrowser;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-04-24
 */
public class WebBrowserBundle extends AbstractBundle {
    private static final String BUNDLE = "consulo.webBrowser.WebBrowserBundle";
    private static final WebBrowserBundle INSTANCE = new WebBrowserBundle();

    private WebBrowserBundle() {
        super(BUNDLE);
    }

    @Nonnull
    public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key) {
        return INSTANCE.getMessage(key);
    }

    @Nonnull
    public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}

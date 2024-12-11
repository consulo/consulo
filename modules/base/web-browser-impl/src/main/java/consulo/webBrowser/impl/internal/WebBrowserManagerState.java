/*
 * Copyright 2013-2024 consulo.io
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
package consulo.webBrowser.impl.internal;

import consulo.util.xml.serializer.annotation.Tag;
import consulo.webBrowser.BrowserFamily;
import consulo.webBrowser.DefaultBrowserPolicy;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author VISTALL
 * @since 2024-12-11
 */
public class WebBrowserManagerState {
    public static class Browser {
        public BrowserFamily family;

        public UUID id;

        @Tag("settings")
        public Element settings;

        public boolean active = true;

        public String path;

        public String name;
    }

    public DefaultBrowserPolicy defaultBrowserPolicy = DefaultBrowserPolicy.SYSTEM;

    public boolean showBrowserHover = true;

    public List<Browser> browsers = new ArrayList<>();

    public String browserPath;
    
    public boolean useDefaultBrowser = true;

    public boolean confirmExtractFiles = true;
}

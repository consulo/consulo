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
package consulo.ui.ex.impl.internal;

import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.util.DateFormatUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.platform.Platform;
import consulo.util.lang.StringUtil;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * What the about dialog of every frontend shows - the build, the plugins it loaded and the machine it runs on.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public class AboutInfoBuilder {
    private final StringBuilder myBuilder = new StringBuilder();

    public static String build() {
        AboutInfoBuilder builder = new AboutInfoBuilder();
        builder.collect();
        return StringUtil.trimTrailing(builder.myBuilder.toString());
    }

    private void group(String name) {
        myBuilder.append(" ").append(name).append(":\n");
    }

    private void item(String name, String value) {
        myBuilder.append("  ").append(name).append(" = ").append(value).append("\n");
    }

    private void collect() {
        ApplicationInfo info = ApplicationInfo.getInstance();

        group(Application.get().getName().get());
        item("version", info.getFullVersion());
        item("build number", String.valueOf(info.getBuild()));
        item("build date", DateFormatUtil.formatAboutDialogDate(info.getBuildDate().getTime()));

        group("Plugins");

        Map<String, String> plugins = new TreeMap<>();
        for (PluginDescriptor plugin : PluginManager.getPlugins()) {
            plugins.put(plugin.getPluginId().toString(), StringUtil.notNullize(plugin.getVersion(), info.getBuild().toString()));
        }

        for (Map.Entry<String, String> entry : plugins.entrySet()) {
            item(entry.getKey(), entry.getValue());
        }

        Platform platform = Platform.current();

        group("JVM");
        item("vendor", platform.jvm().vendor());
        item("version", platform.jvm().rawVersion());
        item("locale", Locale.getDefault().toString());

        group("JVM Env");
        for (Map.Entry<String, String> entry : new TreeMap<>(platform.jvm().getRuntimeProperties()).entrySet()) {
            item(entry.getKey(), StringUtil.escapeCharCharacters(entry.getValue()));
        }

        group("OS");
        item("name", platform.os().name());
        item("version", platform.os().version());
        item("arch", platform.os().arch());

        group("Env");
        for (Map.Entry<String, String> entry : new TreeMap<>(platform.os().environmentVariables()).entrySet()) {
            item(entry.getKey(), StringUtil.escapeCharCharacters(entry.getValue()));
        }
    }
}

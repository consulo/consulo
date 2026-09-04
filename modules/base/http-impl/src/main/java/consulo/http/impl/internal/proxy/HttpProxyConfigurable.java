/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.http.impl.internal.proxy;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.http.HttpProxyManager;
import consulo.http.localize.HttpLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class HttpProxyConfigurable extends SimpleConfigurable<HttpProxySettingsUi> implements ApplicationConfigurable {
    private final HttpProxyManagerImpl settings;

    public HttpProxyConfigurable() {
        this(HttpProxyManager.getInstance());
    }

    @Inject
    public HttpProxyConfigurable(HttpProxyManager settings) {
        this.settings = (HttpProxyManagerImpl) settings;
    }

    @Override
    public String getId() {
        return "http.proxy";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return HttpLocalize.httpProxyConfigurable();
    }

    @Override
    public @Nullable String getHelpTopic() {
        return "http.proxy";
    }

    @RequiredUIAccess
    @Override
    protected HttpProxySettingsUi createPanel(Disposable uiDisposable) {
        return new HttpProxySettingsUi(settings);
    }

    @RequiredUIAccess
    @Override
    protected boolean isModified(HttpProxySettingsUi component) {
        return component.isModified(settings);
    }

    @RequiredUIAccess
    @Override
    protected void apply(HttpProxySettingsUi component) {
        component.apply(settings);
    }

    @RequiredUIAccess
    @Override
    protected void reset(HttpProxySettingsUi component) {
        component.reset(settings);
    }
}

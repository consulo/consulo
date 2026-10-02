// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.http.request;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.http.request.NavigatorHttpRequest;
import consulo.endpoint.http.request.RequestNavigator;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.url.HttpMethodConstants;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.io.Url;
import consulo.util.io.Urls;
import consulo.util.lang.Pair;
import consulo.webBrowser.BrowserUtil;

import java.util.LinkedHashMap;
import java.util.Map;

@ExtensionImpl(order = "last")
public final class DefaultRequestNavigator implements RequestNavigator {
    @Override
    public String getId() {
        return "DefaultRequestNavigator";
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesPpweb();
    }

    @Override
    public boolean accept(NavigatorHttpRequest request) {
        return HttpMethodConstants.GET.equals(request.getRequestMethod());
    }

    @Override
    @RequiredUIAccess
    public void navigate(NavigatorHttpRequest request, String hint) {
        if (accept(request)) {
            Map<String, String> parametersMap = new LinkedHashMap<>();
            for (Pair<String, String> param : request.getParams()) {
                parametersMap.put(param.getFirst(), param.getSecond());
            }
            Url baseUrl = Urls.newFromEncoded(request.getUrl());
            if (baseUrl == null) {
                return;
            }
            Url url = baseUrl.addParameters(parametersMap);
            BrowserUtil.open(url.toExternalForm());
        }
    }

    @Override
    public LocalizeValue getDisplayText() {
        return EndpointLocalize.microservicesOpenInBrowserActionName();
    }
}

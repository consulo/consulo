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
package consulo.sandboxPlugin.ide.endpoint;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.endpoint.EndpointElementItem;
import consulo.endpoint.EndpointListItem;
import consulo.endpoint.EndpointSidePanel;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.Coroutine;

import java.util.ArrayList;
import java.util.List;

public final class SandInfoSidePanel implements EndpointSidePanel {
    private final Label myCountLabel;
    private final VerticalLayout myUrlsLayout;
    private final DockLayout myRoot;

    @RequiredUIAccess
    public SandInfoSidePanel() {
        myCountLabel = Label.create();
        myUrlsLayout = VerticalLayout.create();
        myRoot = DockLayout.create();
        myRoot.top(myCountLabel);
        myRoot.center(ScrollableLayout.create(myUrlsLayout));
    }

    @Override
    public LocalizeValue getTitle() {
        return LocalizeValue.localizeTODO("Sand Info");
    }

    @RequiredUIAccess
    @Override
    public Component getComponent() {
        return myRoot;
    }

    @Override
    public Coroutine<?, Boolean> isAvailable(List<? extends EndpointListItem> selectedItems) {
        return Coroutine.first(ReadLock.<Object, Boolean>apply(ignored -> hasValidItems(selectedItems)));
    }

    @Override
    public Coroutine<?, ?> update(List<? extends EndpointListItem> selectedItems) {
        return Coroutine
            .first(ReadLock.<Object, List<String>>apply(ignored -> collectUrls(selectedItems)))
            .then(UIAction.<List<String>, List<String>>apply(urls -> {
                show(urls);
                return urls;
            }));
    }

    @RequiredUIAccess
    private void show(List<String> urls) {
        myCountLabel.setText(LocalizeValue.localizeTODO("Selected endpoints: " + urls.size()));
        myUrlsLayout.removeAll();
        for (String url : urls) {
            myUrlsLayout.add(Label.create(LocalizeValue.of(url)));
        }
    }

    @RequiredReadAction
    private static boolean hasValidItems(List<? extends EndpointListItem> selectedItems) {
        for (EndpointListItem item : selectedItems) {
            if (item instanceof EndpointElementItem<?, ?> elementItem && elementItem.isValid()) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    private static List<String> collectUrls(List<? extends EndpointListItem> selectedItems) {
        List<String> result = new ArrayList<>();
        for (EndpointListItem item : selectedItems) {
            if (item instanceof EndpointElementItem<?, ?> elementItem && elementItem.isValid()) {
                collectUrls(elementItem, result);
            }
        }
        return result;
    }

    @RequiredReadAction
    private static <G, E> void collectUrls(EndpointElementItem<G, E> item, List<String> result) {
        Iterable<UrlTargetInfo> infos = item.getUrlTargetInfos();
        if (infos == null) {
            String text = item.getProvider().getEndpointPresentation(item.getGroup(), item.getEndpoint()).getPresentableText();
            if (text != null) {
                result.add(text);
            }
            return;
        }
        for (UrlTargetInfo info : infos) {
            result.add(format(info));
        }
    }

    private static String format(UrlTargetInfo info) {
        StringBuilder builder = new StringBuilder();
        if (!info.getMethods().isEmpty()) {
            builder.append(String.join("|", info.getMethods())).append(' ');
        }
        if (!info.getSchemes().isEmpty()) {
            builder.append(info.getSchemes().get(0));
        }
        for (Authority authority : info.getAuthorities()) {
            if (authority instanceof Authority.Exact exact) {
                builder.append(exact.getText());
                break;
            }
        }
        String path = info.getPath().getPresentation();
        if (!path.startsWith("/")) {
            builder.append('/');
        }
        builder.append(path);
        return builder.toString();
    }
}

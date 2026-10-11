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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.application.Application;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableAdapter;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.configurable.internal.ConfigurableWrapper;
import consulo.configurable.internal.FullContentConfigurable;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.AdvancedLabel;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.Space;
import consulo.ui.Spotlight;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class UnifiedOptionsPage implements Disposable {
    private final Configurable myConfigurable;
    private final DockLayout myComponent;
    private final @Nullable Component myContent;
    private final AdvancedLabel myErrorLabel;
    private final boolean myHasContent;
    private final boolean mySupported;
    private final @Nullable UnifiedOptionsBanner myBanner;

    private @Nullable ConfigurationException myShownError;
    private @Nullable Spotlight mySpotlight;

    @RequiredUIAccess
    public UnifiedOptionsPage(Configurable configurable) {
        myConfigurable = configurable;

        boolean group = isGroupWithoutContent(configurable);
        Component content = group ? null : ConfigurableUIMigrationUtil.createSupportedUIComponent(configurable, this);
        myContent = content;
        mySupported = content != null || group || !Application.get().isUnifiedApplication();
        myHasContent = content != null || !mySupported;

        FullContentConfigurable fullContent = content == null ? null : ConfigurableWrapper.cast(configurable, FullContentConfigurable.class);
        if (fullContent != null) {
            UnifiedOptionsBanner banner = new UnifiedOptionsBanner();
            fullContent.setBannerComponent(banner.getComponent());
            myBanner = banner;
        }
        else {
            myBanner = null;
        }

        myErrorLabel = AdvancedLabel.create();
        myErrorLabel.paddingBuilder().allSet(Space.SMALL).apply();
        myErrorLabel.setVisible(false);

        myComponent = DockLayout.create(Space.NONE);
        myComponent.top(myErrorLabel);
        if (content != null) {
            myComponent.center(wrap(configurable, content));
        }
        else if (!mySupported) {
            myComponent.center(wrap(configurable, ConfigurableUIMigrationUtil.createNotSupportedComponent()));
        }
        else {
            myComponent.center(createEmptyContent());
        }
    }

    public Configurable getConfigurable() {
        return myConfigurable;
    }

    public Component getComponent() {
        return myComponent;
    }

    public @Nullable Component getContent() {
        return myContent;
    }

    @RequiredUIAccess
    public Spotlight getSpotlight() {
        Spotlight spotlight = mySpotlight;
        if (spotlight == null) {
            spotlight = Spotlight.over(myComponent);
            Disposer.register(this, spotlight);
            mySpotlight = spotlight;
        }
        return spotlight;
    }

    @RequiredUIAccess
    public void clearSpotlight() {
        Spotlight spotlight = mySpotlight;
        if (spotlight != null) {
            spotlight.clear();
        }
    }

    public boolean hasContent() {
        return myHasContent;
    }

    public boolean isSupported() {
        return mySupported;
    }

    public boolean isFullContent() {
        return myBanner != null;
    }

    @RequiredUIAccess
    public void update(List<LocalizeValue> bannerText, @Nullable LocalizeValue projectTooltip, @Nullable ConfigurationException error) {
        UnifiedOptionsBanner banner = myBanner;
        if (banner != null) {
            banner.update(bannerText, projectTooltip);
        }

        if (myShownError == error) {
            return;
        }

        myShownError = error;

        if (error == null) {
            myErrorLabel.setVisible(false);
        }
        else {
            myErrorLabel.updatePresentation(presentation -> {
                presentation.withIcon(PlatformIconGroup.generalError());
                presentation.append(LocalizeValue.ofNullable(error.getMessage()), TextAttribute.ERROR);
            });
            myErrorLabel.setVisible(true);
        }
    }

    @Override
    public void dispose() {
    }

    @RequiredUIAccess
    private static Component wrap(Configurable configurable, Component content) {
        if (!ConfigurableWrapper.isNoMargin(configurable)) {
            content.paddingBuilder().verticalSet(Space.MEDIUM).horizontalSet(Space.LARGE).apply();
        }

        if (ConfigurableWrapper.isNoScroll(configurable)) {
            return content;
        }
        return ScrollableLayout.create(content);
    }

    @RequiredUIAccess
    private static Component createEmptyContent() {
        return Label.create(
            ConfigurableLocalize.configurableEmptyContentText(),
            LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build()
        );
    }

    private static boolean isGroupWithoutContent(Configurable configurable) {
        SearchableConfigurable.Parent parent = ConfigurableWrapper.cast(configurable, SearchableConfigurable.Parent.class);
        if (parent != null) {
            return !parent.hasOwnContent();
        }
        return ConfigurableWrapper.cast(configurable, ConfigurableAdapter.class) != null;
    }
}

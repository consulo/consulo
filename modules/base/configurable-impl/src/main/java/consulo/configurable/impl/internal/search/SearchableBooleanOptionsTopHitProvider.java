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
package consulo.configurable.impl.internal.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.component.ComponentManager;
import consulo.configurable.search.SearchableBooleanOption;
import consulo.configurable.search.SearchableBooleanOptions;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.localize.LocalizeValue;
import consulo.configurable.search.PublicMethodBasedOptionDescription;
import consulo.configurable.search.SearchTopHitProvider;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The check boxes of the settings pages, switched from Find Action - from the {@link SearchableBooleanOptions}
 * generated for the pages, as {@code Page: Option}. {@code #<page id> text} looks in the pages of that id only.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionImpl
public class SearchableBooleanOptionsTopHitProvider implements SearchTopHitProvider {
    private final Application myApplication;

    @Inject
    public SearchableBooleanOptionsTopHitProvider(Application application) {
        myApplication = application;
    }

    @Override
    public void consumeTopHits(String pattern, Consumer<Object> collector, @Nullable ComponentManager project) {
        String idPrefix = null;
        String text = pattern.trim();
        if (text.startsWith("#")) {
            int space = text.indexOf(' ');
            idPrefix = space < 0 ? text.substring(1) : text.substring(1, space);
            text = space < 0 ? "" : text.substring(space + 1).trim();
        }
        if (text.isEmpty()) {
            return;
        }

        MinusculeMatcher matcher = NameUtil.buildMatcher("*" + text.toLowerCase(), NameUtil.MatchingCaseSensitivity.NONE);
        String pageIdPrefix = idPrefix;
        myApplication.getExtensionPoint(SearchableBooleanOptions.class).forEach(options -> {
            if (pageIdPrefix != null && !options.getConfigurableId().startsWith(pageIdPrefix)) {
                return;
            }
            LocalizeValue pageName = options.getConfigurableName();
            for (SearchableBooleanOption option : options.getOptions()) {
                LocalizeValue label = pageName.isEmpty() ? option.text() : ConfigurableLocalize.labelOptionConfigurable(pageName, option.text());
                String labelText = StringUtil.stripHtml(label.get(), false);
                if (matcher.matches(labelText)) {
                    collector.accept(new PublicMethodBasedOptionDescription<>(
                        labelText,
                        options.getConfigurableId(),
                        () -> option,
                        it -> it.getter().getAsBoolean(),
                        (it, enabled) -> it.setter().accept(enabled)
                    ));
                }
            }
        });
    }
}

// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.param;

import consulo.language.Language;
import consulo.language.editor.impl.internal.inlay.HintInfoFilter;
import consulo.language.editor.inlay.HintInfo;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.internal.ParameterNameHintsSettings;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MethodInfoExcludeListFilter implements HintInfoFilter {
    private final List<Matcher> myMatchers;

    public MethodInfoExcludeListFilter(Set<String> list) {
        myMatchers = list.stream()
            .map(MatcherConstructor::createMatcher)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static @Nonnull MethodInfoExcludeListFilter forLanguage(@Nonnull Language language) {
        Set<String> list = fullExcludelist(language);
        return new MethodInfoExcludeListFilter(list);
    }

    @Override
    public boolean showHint(@Nonnull HintInfo info) {
        if (info instanceof HintInfo.MethodInfo methodInfo) {
            return !ContainerUtil.exists(myMatchers, (e) -> e.isMatching(methodInfo.getFullyQualifiedName(), methodInfo.getParamNames()));
        }
        return false;
    }

    private static @Nonnull Set<String> fullExcludelist(Language language) {
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        if (provider == null) {
            return Collections.emptySet();
        }

        Set<String> excludeList = excludeList(language);
        Language dependentLanguage = provider.getBlackListDependencyLanguage();
        if (dependentLanguage != null) {
            excludeList = ContainerUtil.union(excludeList, excludeList(dependentLanguage));
        }
        return excludeList;
    }

    private static @Nonnull Set<String> excludeList(@Nonnull Language language) {
        InlayParameterHintsProvider provider = InlayParameterHintsProvider.forLanguage(language);
        if (provider != null) {
            ParameterNameHintsSettings settings = ParameterNameHintsSettings.getInstance();
            ParameterNameHintsSettings.Diff diff = settings.getExcludeListDiff(HintUtils.getLanguageForSettingKey(language));
            return diff.applyOn(provider.getDefaultBlackList());
        }
        return Collections.emptySet();
    }

}
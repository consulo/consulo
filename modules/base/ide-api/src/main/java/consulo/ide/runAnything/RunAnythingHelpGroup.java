// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.runAnything;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 'Run Anything' popup help section is divided into groups by categories.
 * E.g. 'ruby' help group contains 'ruby' related run configuration commands, 'rvm use #sdk_version' commands etc.
 */

@ExtensionAPI(ComponentScope.APPLICATION)
public class RunAnythingHelpGroup<P extends RunAnythingProvider> extends RunAnythingGroupBase {
    
    private LocalizeValue myTitle = LocalizeValue.localizeTODO("undefined");
    
    private Collection<P> myProviders = Collections.emptyList();

    public RunAnythingHelpGroup(String title, Collection<P> providers) {
        myTitle = LocalizeValue.localizeTODO(title);
        myProviders = providers;
    }

    /**
     * @deprecated API compatibility
     */
    @Deprecated
    public RunAnythingHelpGroup() {
    }

    
    @Override
    public LocalizeValue getTitle() {
        return myTitle;
    }

    /**
     * Returns collections of providers each of them is expecting to provide not null {@link RunAnythingProvider#getHelpItem(DataContext)}
     * See also {@code RunAnythingProviderBase.getHelp*()} methods.
     *
     * @deprecated please use {@link RunAnythingProvider#getHelpGroupTitle()} instead
     */
    @Deprecated
    
    public Collection<P> getProviders() {
        return myProviders;
    }

    
    @Override
    public Collection<RunAnythingItem> getGroupItems(DataContext dataContext, String pattern) {
        return getProviders().stream()
            .map(provider -> provider.getHelpItem(dataContext))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    protected int getMaxInitialItems() {
        return 15;
    }
}
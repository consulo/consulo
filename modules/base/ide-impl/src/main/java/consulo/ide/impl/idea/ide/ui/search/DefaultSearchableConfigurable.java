/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.ui.search;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author anna
 * @since 2006-03-17
 */
public class DefaultSearchableConfigurable implements Configurable {
    private final SearchableConfigurable myDelegate;
    private JComponent myComponent;

    public DefaultSearchableConfigurable(SearchableConfigurable delegate) {
        myDelegate = delegate;
    }

    @Nonnull
    @Override
    public String getId() {
        return myDelegate.getId();
    }

    public void clearSearch() {
    }

    public void enableSearch(String option) {
        Runnable runnable = myDelegate.enableSearch(option);
        if (runnable != null) {
            runnable.run();
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return myDelegate.getDisplayName();
    }

    @Override
    @Nullable
    public String getHelpTopic() {
        return myDelegate.getHelpTopic();
    }

    @Override
    @RequiredUIAccess
    public JComponent createComponent() {
        myComponent = myDelegate.createComponent();
        return myComponent;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        return myDelegate.isModified();
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        myDelegate.apply();
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        myDelegate.reset();
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        myComponent = null;
        myDelegate.disposeUIResources();
    }

    public Configurable getDelegate() {
        return myDelegate;
    }
}

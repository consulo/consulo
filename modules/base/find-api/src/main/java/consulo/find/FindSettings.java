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

package consulo.find;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import jakarta.annotation.Nonnull;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class FindSettings {
    public static FindSettings getInstance() {
        return Application.get().getInstance(FindSettings.class);
    }

    public abstract boolean isSkipResultsWithOneUsage();

    public abstract void setSkipResultsWithOneUsage(boolean skip);

    public abstract String getDefaultScopeName();

    public abstract void setDefaultScopeName(String scope);

    public abstract boolean isSearchOverloadedMethods();

    public abstract void setSearchOverloadedMethods(boolean search);

    public abstract boolean isForward();

    public abstract void setForward(boolean findDirectionForward);

    public abstract boolean isFromCursor();

    public abstract void setFromCursor(boolean findFromCursor);

    public abstract boolean isGlobal();

    public abstract void setGlobal(boolean findGlobalScope);

    public abstract boolean isCaseSensitive();

    public abstract void setCaseSensitive(boolean caseSensitiveSearch);

    public abstract boolean isLocalCaseSensitive();

    public abstract void setLocalCaseSensitive(boolean caseSensitiveSearch);

    public abstract boolean isPreserveCase();

    public abstract void setPreserveCase(boolean preserveCase);

    public abstract boolean isWholeWordsOnly();

    public abstract void setWholeWordsOnly(boolean wholeWordsOnly);

    public abstract boolean isLocalWholeWordsOnly();

    public abstract void setLocalWholeWordsOnly(boolean wholeWordsOnly);

    public abstract boolean isRegularExpressions();

    public abstract void setRegularExpressions(boolean regularExpressions);

    public abstract boolean isLocalRegularExpressions();

    public abstract void setLocalRegularExpressions(boolean regularExpressions);

    /**
     * FindInProjectSettings.addDirectory
     */
    @Deprecated
    public abstract void addStringToFind(@Nonnull String s);

    /**
     * Use FindInProjectSettings.addDirectory
     */
    @Deprecated
    public abstract void addStringToReplace(@Nonnull String s);

    /**
     * Use FindInProjectSettings.addDirectory
     */
    @Deprecated
    public abstract void addDirectory(@Nonnull String s);

    /**
     * FindInProjectSettings.addDirectory
     */
    @Nonnull
    public abstract String[] getRecentFindStrings();

    /**
     * FindInProjectSettings.addDirectory
     */
    @Nonnull
    public abstract String[] getRecentReplaceStrings();

    /**
     * Returns the list of file masks used by the user in the "File name filter"
     * group box.
     *
     * @return the recent file masks list
     * @since 5.0.2
     */
    @Nonnull
    public abstract String[] getRecentFileMasks();

    /**
     * Use FindInProjectSettings.getRecentDirectories
     */
    @Deprecated
    @Nonnull
    public abstract List<String> getRecentDirectories();

    public abstract void setWithSubdirectories(boolean b);

    public abstract void initModelBySetings(@Nonnull FindModel model);

    public abstract String getFileMask();

    public abstract void setFileMask(String fileMask);

    public abstract void setCustomScope(String scopeName);

    public abstract String getCustomScope();

    public abstract FindSearchContext getSearchContext();

    public abstract void setSearchContext(FindSearchContext searchContext);

    public abstract boolean isShowResultsInSeparateView();

    public abstract void setShowResultsInSeparateView(boolean selected);
}

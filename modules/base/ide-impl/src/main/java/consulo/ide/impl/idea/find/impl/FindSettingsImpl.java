/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.*;
import consulo.find.FindBundle;
import consulo.find.FindModel;
import consulo.find.FindSettings;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
@State(name = "FindSettings", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/find.xml"), @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)})
@ServiceImpl
public class FindSettingsImpl extends FindSettings implements PersistentStateComponent<FindSettingsImpl> {
  @NonNls
  private static final String FIND_DIRECTION_FORWARD = "forward";
  @NonNls
  private static final String FIND_DIRECTION_BACKWARD = "backward";
  @NonNls
  private static final String FIND_ORIGIN_FROM_CURSOR = "from_cursor";
  @NonNls
  private static final String FIND_ORIGIN_ENTIRE_SCOPE = "entire_scope";
  @NonNls
  private static final String FIND_SCOPE_GLOBAL = "global";
  @NonNls
  private static final String FIND_SCOPE_SELECTED = "selected";

  public FindSettingsImpl() {
    recentFileMasks.add("*.properties");
    recentFileMasks.add("*.html");
    recentFileMasks.add("*.jsp");
    recentFileMasks.add("*.xml");
    recentFileMasks.add("*.java");
    recentFileMasks.add("*.js");
    recentFileMasks.add("*.as");
    recentFileMasks.add("*.css");
    recentFileMasks.add("*.mxml");
    recentFileMasks.add("*.py");
    recentFileMasks.add("*.rb");
    recentFileMasks.add("*.php");
  }

  @Override
  public boolean isSearchOverloadedMethods() {
    return SEARCH_OVERLOADED_METHODS;
  }

  @Override
  public void setSearchOverloadedMethods(boolean search) {
    SEARCH_OVERLOADED_METHODS = search;
  }

  @SuppressWarnings({"WeakerAccess"})
  public boolean SEARCH_OVERLOADED_METHODS = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean SEARCH_IN_LIBRARIES = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean SKIP_RESULTS_WHEN_ONE_USAGE = false;

  @SuppressWarnings({"WeakerAccess"})
  public String FIND_DIRECTION = FIND_DIRECTION_FORWARD;
  @SuppressWarnings({"WeakerAccess"})
  public String FIND_ORIGIN = FIND_ORIGIN_FROM_CURSOR;
  @SuppressWarnings({"WeakerAccess"})
  public String FIND_SCOPE = FIND_SCOPE_GLOBAL;
  @SuppressWarnings({"WeakerAccess"})
  public String FIND_CUSTOM_SCOPE = null;

  @SuppressWarnings({"WeakerAccess"})
  public boolean CASE_SENSITIVE_SEARCH = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean LOCAL_CASE_SENSITIVE_SEARCH = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean PRESERVE_CASE_REPLACE = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean WHOLE_WORDS_ONLY = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean COMMENTS_ONLY = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean STRING_LITERALS_ONLY = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean EXCEPT_COMMENTS = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean EXCEPT_COMMENTS_AND_STRING_LITERALS = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean EXCEPT_STRING_LITERALS = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean LOCAL_WHOLE_WORDS_ONLY = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean REGULAR_EXPRESSIONS = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean LOCAL_REGULAR_EXPRESSIONS = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean WITH_SUBDIRECTORIES = true;
  @SuppressWarnings({"WeakerAccess"})
  public boolean SHOW_RESULTS_IN_SEPARATE_VIEW = false;

  @SuppressWarnings({"WeakerAccess"})
  public String SEARCH_SCOPE = FindBundle.message("find.scope.all.project.classes");
  @SuppressWarnings({"WeakerAccess"})
  public String FILE_MASK;

  @Tag("recentFileMasks")
  @Property(surroundWithTag = false)
  @AbstractCollection(surroundWithTag = false, elementTag = "mask", elementValueAttribute = "")
  public List<String> recentFileMasks = new ArrayList<>();

  @Override
  public void loadState(FindSettingsImpl state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Override
  public FindSettingsImpl getState() {
    return this;
  }

  @Override
  public boolean isSkipResultsWithOneUsage() {
    return SKIP_RESULTS_WHEN_ONE_USAGE;
  }

  @Override
  public void setSkipResultsWithOneUsage(boolean skip) {
    SKIP_RESULTS_WHEN_ONE_USAGE = skip;
  }

  @Override
  public String getDefaultScopeName() {
    return SEARCH_SCOPE;
  }

  @Override
  public void setDefaultScopeName(String scope) {
    SEARCH_SCOPE = scope;
  }

  @Override
  public boolean isForward() {
    return FIND_DIRECTION_FORWARD.equals(FIND_DIRECTION);
  }

  @Override
  public void setForward(boolean findDirectionForward) {
    FIND_DIRECTION = findDirectionForward ? FIND_DIRECTION_FORWARD : FIND_DIRECTION_BACKWARD;
  }

  @Override
  public boolean isFromCursor() {
    return FIND_ORIGIN_FROM_CURSOR.equals(FIND_ORIGIN);
  }

  @Override
  public void setFromCursor(boolean findFromCursor) {
    FIND_ORIGIN = findFromCursor ? FIND_ORIGIN_FROM_CURSOR : FIND_ORIGIN_ENTIRE_SCOPE;
  }

  @Override
  public boolean isGlobal() {
    return FIND_SCOPE_GLOBAL.equals(FIND_SCOPE);
  }

  @Override
  public void setGlobal(boolean findGlobalScope) {
    FIND_SCOPE = findGlobalScope ? FIND_SCOPE_GLOBAL : FIND_SCOPE_SELECTED;
  }

  @Override
  public boolean isCaseSensitive() {
    return CASE_SENSITIVE_SEARCH;
  }

  @Override
  public void setCaseSensitive(boolean caseSensitiveSearch) {
    CASE_SENSITIVE_SEARCH = caseSensitiveSearch;
  }

  @Override
  public boolean isLocalCaseSensitive() {
    return LOCAL_CASE_SENSITIVE_SEARCH;
  }

  @Override
  public boolean isLocalWholeWordsOnly() {
    return LOCAL_WHOLE_WORDS_ONLY;
  }

  @Override
  public void setLocalCaseSensitive(final boolean caseSensitiveSearch) {
    LOCAL_CASE_SENSITIVE_SEARCH = caseSensitiveSearch;
  }

  @Override
  public void setLocalWholeWordsOnly(final boolean wholeWordsOnly) {
    LOCAL_WHOLE_WORDS_ONLY = wholeWordsOnly;
  }

  @Override
  public boolean isPreserveCase() {
    return PRESERVE_CASE_REPLACE;
  }

  @Override
  public void setPreserveCase(boolean preserveCase) {
    PRESERVE_CASE_REPLACE = preserveCase;
  }

  @Override
  public boolean isWholeWordsOnly() {
    return WHOLE_WORDS_ONLY;
  }

  @Override
  public void setWholeWordsOnly(boolean wholeWordsOnly) {
    WHOLE_WORDS_ONLY = wholeWordsOnly;
  }

  @Override
  public boolean isRegularExpressions() {
    return REGULAR_EXPRESSIONS;
  }

  @Override
  public void setRegularExpressions(boolean regularExpressions) {
    REGULAR_EXPRESSIONS = regularExpressions;
  }

  @Override
  public boolean isLocalRegularExpressions() {
    return LOCAL_REGULAR_EXPRESSIONS;
  }

  @Override
  public void setLocalRegularExpressions(boolean regularExpressions) {
    LOCAL_REGULAR_EXPRESSIONS = regularExpressions;
  }

  @Override
  public void setWithSubdirectories(boolean b) {
    WITH_SUBDIRECTORIES = b;
  }

  private boolean isWithSubdirectories() {
    return WITH_SUBDIRECTORIES;
  }

  @Override
  public void initModelBySetings(@Nonnull FindModel model) {
    model.setCaseSensitive(isCaseSensitive());
    model.setForward(isForward());
    model.setFromCursor(isFromCursor());
    model.setGlobal(isGlobal());
    model.setRegularExpressions(isRegularExpressions());
    model.setWholeWordsOnly(isWholeWordsOnly());
    FindModel.SearchContext searchContext = isInCommentsOnly()
                                            ? FindModel.SearchContext.IN_COMMENTS
                                            : isInStringLiteralsOnly()
                                              ? FindModel.SearchContext.IN_STRING_LITERALS
                                              : isExceptComments()
                                                ? FindModel.SearchContext.EXCEPT_COMMENTS
                                                : isExceptStringLiterals()
                                                  ? FindModel.SearchContext.EXCEPT_STRING_LITERALS
                                                  : isExceptCommentsAndLiterals() ? FindModel.SearchContext.EXCEPT_COMMENTS_AND_STRING_LITERALS : FindModel.SearchContext.ANY;
    model.setSearchContext(searchContext);
    model.setWithSubdirectories(isWithSubdirectories());
    model.setFileFilter(FILE_MASK);

    model.setCustomScopeName(FIND_SCOPE);
  }

  @Override
  public void addStringToFind(@Nonnull String s) {
    FindRecents.getInstance().addStringToFind(s);
  }

  @Override
  public void addStringToReplace(@Nonnull String s) {
    FindRecents.getInstance().addStringToReplace(s);
  }

  @Override
  public void addDirectory(@Nonnull String s) {
    FindRecents.getInstance().addDirectory(s);
  }

  @Nonnull
  @Override
  public String[] getRecentFindStrings() {
    return FindRecents.getInstance().getRecentFindStrings();
  }

  @Nonnull
  @Override
  public String[] getRecentReplaceStrings() {
    return FindRecents.getInstance().getRecentReplaceStrings();
  }

  @Nonnull
  @Override
  public String[] getRecentFileMasks() {
    return ArrayUtil.toStringArray(recentFileMasks);
  }

  @Nonnull
  @Override
  public List<String> getRecentDirectories() {
    return FindRecents.getInstance().getRecentDirectories();
  }

  @Override
  @Transient
  public String getFileMask() {
    return FILE_MASK;
  }

  @Override
  public void setFileMask(String _fileMask) {
    FILE_MASK = _fileMask;
    if (!StringUtil.isEmptyOrSpaces(_fileMask)) {
      FindInProjectSettingsBase.addRecentStringToList(_fileMask, recentFileMasks);
    }
  }

  @Override
  public String getCustomScope() {
    return SEARCH_SCOPE;
  }

  @Override
  public boolean isInStringLiteralsOnly() {
    return STRING_LITERALS_ONLY;
  }

  @Override
  public boolean isInCommentsOnly() {
    return COMMENTS_ONLY;
  }

  @Override
  public void setInCommentsOnly(boolean selected) {
    COMMENTS_ONLY = selected;
  }

  @Override
  public void setInStringLiteralsOnly(boolean selected) {
    STRING_LITERALS_ONLY = selected;
  }

  @Override
  public void setCustomScope(final String SEARCH_SCOPE) {
    this.SEARCH_SCOPE = SEARCH_SCOPE;
  }

  @Override
  public boolean isExceptComments() {
    return EXCEPT_COMMENTS;
  }

  @Override
  public void setExceptCommentsAndLiterals(boolean selected) {
    EXCEPT_COMMENTS_AND_STRING_LITERALS = selected;
  }

  @Override
  public boolean isShowResultsInSeparateView() {
    return SHOW_RESULTS_IN_SEPARATE_VIEW;
  }

  @Override
  public void setShowResultsInSeparateView(boolean optionValue) {
    SHOW_RESULTS_IN_SEPARATE_VIEW = optionValue;
  }

  @Override
  public boolean isExceptCommentsAndLiterals() {
    return EXCEPT_COMMENTS_AND_STRING_LITERALS;
  }

  @Override
  public void setExceptComments(boolean selected) {
    EXCEPT_COMMENTS = selected;
  }

  @Override
  public boolean isExceptStringLiterals() {
    return EXCEPT_STRING_LITERALS;
  }

  @Override
  public void setExceptStringLiterals(boolean selected) {
    EXCEPT_STRING_LITERALS = selected;
  }

}

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
package consulo.usage;

import consulo.util.lang.Comparing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * @author max
 */
public class UsageViewPresentation {
  private String myTabText;
  private String myScopeText = ""; // Default value. to be overwritten in most cases.
  private String myContextText = "";
  private String myUsagesString;
  private String myTargetsNodeText = UsageViewBundle.message("node.targets"); // Default value. to be overwritten in most cases.
  private String myNonCodeUsagesString = UsageViewBundle.message("node.non.code.usages");
  private String myCodeUsagesString = UsageViewBundle.message("node.found.usages");
  private String myUsagesInGeneratedCodeString = UsageViewBundle.message("node.usages.in.generated.code");
  private boolean myShowReadOnlyStatusAsRed = false;
  private boolean myShowCancelButton = false;
  private boolean myOpenInNewTab = true;
  private boolean myCodeUsages = true;
  private boolean myUsageTypeFilteringAvailable;
  private String myUsagesWord = UsageViewBundle.message("usage.name");

  private String myTabName;
  private String myToolwindowTitle;

  private boolean myDetachedMode; // no UI will be shown
  private String myDynamicCodeUsagesString;
  private boolean myMergeDupLinesAvailable = true;
  private boolean myExcludeAvailable = true;
  private Pattern mySearchPattern;
  private Pattern myReplacePattern;
  private boolean myReplaceMode;

  public String getTabText() {
    return myTabText;
  }

  public void setTabText(String tabText) {
    myTabText = tabText;
  }

  @Nonnull
  public String getScopeText() {
    return myScopeText;
  }

  public void setScopeText(@Nonnull String scopeText) {
    myScopeText = scopeText;
  }

  @Nonnull
  public String getContextText() {
    return myContextText;
  }

  public void setContextText(@Nonnull String contextText) {
    myContextText = contextText;
  }

  public boolean isShowReadOnlyStatusAsRed() {
    return myShowReadOnlyStatusAsRed;
  }

  public void setShowReadOnlyStatusAsRed(boolean showReadOnlyStatusAsRed) {
    myShowReadOnlyStatusAsRed = showReadOnlyStatusAsRed;
  }

  public String getUsagesString() {
    return myUsagesString;
  }

  public void setUsagesString(String usagesString) {
    myUsagesString = usagesString;
  }

  // null means the targets node must not be visible
  @Nullable()
  public String getTargetsNodeText() {
    return myTargetsNodeText;
  }

  public void setTargetsNodeText(String targetsNodeText) {
    myTargetsNodeText = targetsNodeText;
  }

  public boolean isShowCancelButton() {
    return myShowCancelButton;
  }

  public void setShowCancelButton(boolean showCancelButton) {
    myShowCancelButton = showCancelButton;
  }

  @Nonnull
  public String getNonCodeUsagesString() {
    return myNonCodeUsagesString;
  }

  public void setNonCodeUsagesString(@Nonnull String nonCodeUsagesString) {
    myNonCodeUsagesString = nonCodeUsagesString;
  }

  @Nonnull
  public String getCodeUsagesString() {
    return myCodeUsagesString;
  }

  public void setCodeUsagesString(@Nonnull String codeUsagesString) {
    myCodeUsagesString = codeUsagesString;
  }

  public boolean isOpenInNewTab() {
    return myOpenInNewTab;
  }

  public void setOpenInNewTab(boolean openInNewTab) {
    myOpenInNewTab = openInNewTab;
  }

  public boolean isCodeUsages() {
    return myCodeUsages;
  }

  public void setCodeUsages(final boolean codeUsages) {
    myCodeUsages = codeUsages;
  }

  @Nonnull
  public String getUsagesWord() {
    return myUsagesWord;
  }

  public void setUsagesWord(@Nonnull String usagesWord) {
    myUsagesWord = usagesWord;
  }

  public String getTabName() {
    return myTabName;
  }

  public void setTabName(final String tabName) {
    myTabName = tabName;
  }

  public String getToolwindowTitle() {
    return myToolwindowTitle;
  }

  public void setToolwindowTitle(final String toolwindowTitle) {
    myToolwindowTitle = toolwindowTitle;
  }

  public boolean isDetachedMode() {
    return myDetachedMode;
  }

  public void setDetachedMode(boolean detachedMode) {
    myDetachedMode = detachedMode;
  }

  public void setDynamicUsagesString(String dynamicCodeUsagesString) {
    myDynamicCodeUsagesString = dynamicCodeUsagesString;
  }

  public String getDynamicCodeUsagesString() {
    return myDynamicCodeUsagesString;
  }

  @Nonnull
  public String getUsagesInGeneratedCodeString() {
    return myUsagesInGeneratedCodeString;
  }

  public void setUsagesInGeneratedCodeString(@Nonnull String usagesInGeneratedCodeString) {
    myUsagesInGeneratedCodeString = usagesInGeneratedCodeString;
  }

  public boolean isMergeDupLinesAvailable() {
    return myMergeDupLinesAvailable;
  }

  public void setMergeDupLinesAvailable(boolean mergeDupLinesAvailable) {
    myMergeDupLinesAvailable = mergeDupLinesAvailable;
  }

  public boolean isUsageTypeFilteringAvailable() {
    return myCodeUsages || myUsageTypeFilteringAvailable;
  }

  public void setUsageTypeFilteringAvailable(boolean usageTypeFilteringAvailable) {
    myUsageTypeFilteringAvailable = usageTypeFilteringAvailable;
  }

  public boolean isExcludeAvailable() {
    return myExcludeAvailable;
  }

  public void setExcludeAvailable(boolean excludeAvailable) {
    myExcludeAvailable = excludeAvailable;
  }

  public void setSearchPattern(Pattern searchPattern) {
    mySearchPattern = searchPattern;
  }

  public Pattern getSearchPattern() {
    return mySearchPattern;
  }

  public void setReplacePattern(Pattern replacePattern) {
    myReplacePattern = replacePattern;
  }

  public Pattern getReplacePattern() {
    return myReplacePattern;
  }

  public boolean isReplaceMode() {
    return myReplaceMode;
  }

  public void setReplaceMode(boolean replaceMode) {
    myReplaceMode = replaceMode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UsageViewPresentation)) return false;

    UsageViewPresentation that = (UsageViewPresentation)o;

    if (myCodeUsages != that.myCodeUsages) return false;
    if (myDetachedMode != that.myDetachedMode) return false;
    if (myMergeDupLinesAvailable != that.myMergeDupLinesAvailable) return false;
    if (myOpenInNewTab != that.myOpenInNewTab) return false;
    if (myShowCancelButton != that.myShowCancelButton) return false;
    if (myShowReadOnlyStatusAsRed != that.myShowReadOnlyStatusAsRed) return false;
    if (myUsageTypeFilteringAvailable != that.myUsageTypeFilteringAvailable) return false;
    if (myExcludeAvailable != that.myExcludeAvailable) return false;
    if (myCodeUsagesString != null ? !myCodeUsagesString.equals(that.myCodeUsagesString) : that.myCodeUsagesString != null) return false;
    if (myDynamicCodeUsagesString != null ? !myDynamicCodeUsagesString.equals(that.myDynamicCodeUsagesString) : that.myDynamicCodeUsagesString != null) {
      return false;
    }
    if (myNonCodeUsagesString != null ? !myNonCodeUsagesString.equals(that.myNonCodeUsagesString) : that.myNonCodeUsagesString != null) {
      return false;
    }
    if (myScopeText != null ? !myScopeText.equals(that.myScopeText) : that.myScopeText != null) return false;
    if (myTabName != null ? !myTabName.equals(that.myTabName) : that.myTabName != null) return false;
    if (myTabText != null ? !myTabText.equals(that.myTabText) : that.myTabText != null) return false;
    if (myTargetsNodeText != null ? !myTargetsNodeText.equals(that.myTargetsNodeText) : that.myTargetsNodeText != null) return false;
    if (myToolwindowTitle != null ? !myToolwindowTitle.equals(that.myToolwindowTitle) : that.myToolwindowTitle != null) return false;
    if (myUsagesInGeneratedCodeString != null ? !myUsagesInGeneratedCodeString.equals(that.myUsagesInGeneratedCodeString) : that.myUsagesInGeneratedCodeString != null) {
      return false;
    }
    if (myUsagesString != null ? !myUsagesString.equals(that.myUsagesString) : that.myUsagesString != null) return false;
    if (myUsagesWord != null ? !myUsagesWord.equals(that.myUsagesWord) : that.myUsagesWord != null) return false;
    if (!arePatternsEqual(mySearchPattern, that.mySearchPattern)) return false;
    if (!arePatternsEqual(myReplacePattern, that.myReplacePattern)) return false;

    return true;
  }

  public static boolean arePatternsEqual(Pattern p1, Pattern p2) {
    if (p1 == null) return p2 == null;
    if (p2 == null) return false;
    return Comparing.equal(p1.pattern(), p2.pattern()) && p1.flags() == p2.flags();
  }

  public static int getHashCode(Pattern pattern) {
    if (pattern == null) return 0;
    String s = pattern.pattern();
    return (s != null ? s.hashCode() : 0) * 31 + pattern.flags();
  }


  @Override
  public int hashCode() {
    int result = myTabText != null ? myTabText.hashCode() : 0;
    result = 31 * result + (myScopeText != null ? myScopeText.hashCode() : 0);
    result = 31 * result + (myUsagesString != null ? myUsagesString.hashCode() : 0);
    result = 31 * result + (myTargetsNodeText != null ? myTargetsNodeText.hashCode() : 0);
    result = 31 * result + (myNonCodeUsagesString != null ? myNonCodeUsagesString.hashCode() : 0);
    result = 31 * result + (myCodeUsagesString != null ? myCodeUsagesString.hashCode() : 0);
    result = 31 * result + (myUsagesInGeneratedCodeString != null ? myUsagesInGeneratedCodeString.hashCode() : 0);
    result = 31 * result + (myShowReadOnlyStatusAsRed ? 1 : 0);
    result = 31 * result + (myShowCancelButton ? 1 : 0);
    result = 31 * result + (myOpenInNewTab ? 1 : 0);
    result = 31 * result + (myCodeUsages ? 1 : 0);
    result = 31 * result + (myUsageTypeFilteringAvailable ? 1 : 0);
    result = 31 * result + (myExcludeAvailable ? 1 : 0);
    result = 31 * result + (myUsagesWord != null ? myUsagesWord.hashCode() : 0);
    result = 31 * result + getHashCode(mySearchPattern);
    result = 31 * result + getHashCode(myReplacePattern);
    result = 31 * result + (myTabName != null ? myTabName.hashCode() : 0);
    result = 31 * result + (myToolwindowTitle != null ? myToolwindowTitle.hashCode() : 0);
    result = 31 * result + (myDetachedMode ? 1 : 0);
    result = 31 * result + (myDynamicCodeUsagesString != null ? myDynamicCodeUsagesString.hashCode() : 0);
    result = 31 * result + (myMergeDupLinesAvailable ? 1 : 0);
    return result;
  }

  public UsageViewPresentation copy() {
    UsageViewPresentation copyInstance = new UsageViewPresentation();
    copyInstance.myTabText = myTabText;
    copyInstance.myScopeText = myScopeText;
    copyInstance.myContextText = myContextText;
    copyInstance.myUsagesString = myUsagesString;
    copyInstance.myTargetsNodeText = myTargetsNodeText;
    copyInstance.myNonCodeUsagesString = myNonCodeUsagesString;
    copyInstance.myCodeUsagesString = myCodeUsagesString;
    copyInstance.myUsagesInGeneratedCodeString = myUsagesInGeneratedCodeString;
    copyInstance.myShowReadOnlyStatusAsRed = myShowReadOnlyStatusAsRed;
    copyInstance.myShowCancelButton = myShowCancelButton;
    copyInstance.myOpenInNewTab = myOpenInNewTab;
    copyInstance.myCodeUsages = myCodeUsages;
    copyInstance.myUsageTypeFilteringAvailable = myUsageTypeFilteringAvailable;
    copyInstance.myUsagesWord = myUsagesWord;
    copyInstance.myTabName = myTabName;
    copyInstance.myToolwindowTitle = myToolwindowTitle;
    copyInstance.myDetachedMode = myDetachedMode;
    copyInstance.myDynamicCodeUsagesString = myDynamicCodeUsagesString;
    copyInstance.myMergeDupLinesAvailable = myMergeDupLinesAvailable;
    copyInstance.myExcludeAvailable = myExcludeAvailable;
    copyInstance.mySearchPattern = mySearchPattern;
    copyInstance.myReplacePattern = myReplacePattern;
    copyInstance.myReplaceMode = myReplaceMode;
    return copyInstance;
  }
}


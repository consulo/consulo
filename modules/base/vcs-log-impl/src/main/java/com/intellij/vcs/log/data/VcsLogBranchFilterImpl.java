package com.intellij.vcs.log.data;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.VcsLogBranchFilter;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class VcsLogBranchFilterImpl implements VcsLogBranchFilter {
  private static final Logger LOG = Logger.getInstance(VcsLogBranchFilterImpl.class);

  @Nonnull
  private final List<String> myBranches;
  @Nonnull
  private final List<Pattern> myPatterns;

  @Nonnull
  private final List<String> myExcludedBranches;
  @Nonnull
  private final List<Pattern> myExcludedPatterns;

  private VcsLogBranchFilterImpl(@Nonnull List<String> branches,
                                 @Nonnull List<Pattern> patterns,
                                 @Nonnull List<String> excludedBranches,
                                 @Nonnull List<Pattern> excludedPatterns) {
    myBranches = branches;
    myPatterns = patterns;
    myExcludedBranches = excludedBranches;
    myExcludedPatterns = excludedPatterns;
  }

  @Deprecated
  public VcsLogBranchFilterImpl(@Nonnull Collection<String> branches, @Nonnull Collection<String> excludedBranches) {
    myBranches = new ArrayList<>(branches);
    myPatterns = new ArrayList<>();
    myExcludedBranches = new ArrayList<>(excludedBranches);
    myExcludedPatterns = new ArrayList<>();
  }

  @Nullable
  public static VcsLogBranchFilterImpl fromBranch(@Nonnull final String branchName) {
    return new VcsLogBranchFilterImpl(Collections.singletonList(branchName), Collections.<Pattern>emptyList(),
                                      Collections.<String>emptyList(), Collections.<Pattern>emptyList());
  }

  @Nonnull
  public static VcsLogBranchFilterImpl fromTextPresentation(@Nonnull Collection<String> strings, @Nonnull Set<String> existingBranches) {
    List<String> branchNames = new ArrayList<>();
    List<String> excludedBranches = new ArrayList<>();
    List<Pattern> patterns = new ArrayList<>();
    List<Pattern> excludedPatterns = new ArrayList<>();

    for (String string : strings) {
      boolean isExcluded = string.startsWith("-");
      string = isExcluded ? string.substring(1) : string;
      boolean isRegexp = !existingBranches.contains(string);

      if (isRegexp) {
        try {
          Pattern pattern = Pattern.compile(string);
          if (isExcluded) {
            excludedPatterns.add(pattern);
          }
          else {
            patterns.add(pattern);
          }
        }
        catch (PatternSyntaxException e) {
          LOG.warn("Pattern " + string + " is not a proper regular expression and no branch can be found with that name.", e);
          if (isExcluded) {
            excludedBranches.add(string);
          }
          else {
            branchNames.add(string);
          }
        }
      }
      else {
        if (isExcluded) {
          excludedBranches.add(string);
        }
        else {
          branchNames.add(string);
        }
      }
    }

    return new VcsLogBranchFilterImpl(branchNames, patterns, excludedBranches, excludedPatterns);
  }

  @Nonnull
  @Override
  public Collection<String> getTextPresentation() {
    List<String> result = new ArrayList<>();

    result.addAll(myBranches);
    result.addAll(ContainerUtil.map(myPatterns, pattern -> pattern.pattern()));

    result.addAll(ContainerUtil.map(myExcludedBranches, branchName -> "-" + branchName));
    result.addAll(ContainerUtil.map(myExcludedPatterns, pattern -> "-" + pattern.pattern()));

    return result;
  }

  @Override
  public String toString() {
    String result = "";
    if (!myPatterns.isEmpty()) {
      result += "on patterns: " + StringUtil.join(myPatterns, ", ");
    }
    if (!myBranches.isEmpty()) {
      if (!result.isEmpty()) result += "; ";
      result += "on branches: " + StringUtil.join(myBranches, ", ");
    }
    if (!myExcludedPatterns.isEmpty()) {
      if (result.isEmpty()) result += "; ";
      result += "not on patterns: " + StringUtil.join(myExcludedPatterns, ", ");
    }
    if (!myExcludedBranches.isEmpty()) {
      if (result.isEmpty()) result += "; ";
      result += "not on branches: " + StringUtil.join(myExcludedBranches, ", ");
    }
    return result;
  }

  @Override
  public boolean matches(@Nonnull String name) {
    return isIncluded(name) && !isExcluded(name);
  }

  private boolean isIncluded(@Nonnull String name) {
    if (myPatterns.isEmpty() && myBranches.isEmpty()) return true;
    if (myBranches.contains(name)) return true;
    for (Pattern regexp : myPatterns) {
      if (regexp.matcher(name).matches()) return true;
    }
    return false;
  }

  private boolean isExcluded(@Nonnull String name) {
    if (myExcludedBranches.contains(name)) return true;
    for (Pattern regexp : myExcludedPatterns) {
      if (regexp.matcher(name).matches()) return true;
    }
    return false;
  }
}

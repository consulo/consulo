/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.vcs;

import com.intellij.lifecycle.PeriodicalTasksCloser;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
@State(name = "IssueNavigationConfiguration", storages = {
        @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/vcs.xml")})
public class IssueNavigationConfiguration extends SimpleModificationTracker implements PersistentStateComponent<IssueNavigationConfiguration> {
  @NonNls
  private static final Pattern ourHtmlPattern =
          Pattern.compile("(http:|https:)\\/\\/([^\\s()](?!&(gt|lt|nbsp)+;))+[^\\p{Pe}\\p{Pc}\\p{Pd}\\p{Ps}\\p{Po}\\s]/?");

  public static IssueNavigationConfiguration getInstance(Project project) {
    return PeriodicalTasksCloser.getInstance().safeGetService(project, IssueNavigationConfiguration.class);
  }

  private List<IssueNavigationLink> myLinks = new ArrayList<>();

  public List<IssueNavigationLink> getLinks() {
    return myLinks;
  }

  public void setLinks(final List<IssueNavigationLink> links) {
    myLinks = new ArrayList<>(links);
    incModificationCount();
  }

  @Override
  public IssueNavigationConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(IssueNavigationConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public static class LinkMatch implements Comparable {
    private final TextRange myRange;
    private final String myTargetUrl;

    public LinkMatch(final TextRange range, final String targetUrl) {
      myRange = range;
      myTargetUrl = targetUrl;
    }

    public TextRange getRange() {
      return myRange;
    }

    public String getTargetUrl() {
      return myTargetUrl;
    }

    @Override
    public int compareTo(@NotNull Object o) {
      if (!(o instanceof LinkMatch)) {
        return 0;
      }
      LinkMatch rhs = (LinkMatch)o;
      return myRange.getStartOffset() - rhs.getRange().getStartOffset();
    }
  }

  public List<LinkMatch> findIssueLinks(CharSequence text) {
    final List<LinkMatch> result = new ArrayList<>();
    for (IssueNavigationLink link : myLinks) {
      Pattern issuePattern = link.getIssuePattern();
      Matcher m = issuePattern.matcher(text);
      while (m.find()) {
        String replacement = issuePattern.matcher(m.group(0)).replaceFirst(link.getLinkRegexp());
        addMatch(result, new TextRange(m.start(), m.end()), replacement);
      }
    }
    Matcher m = ourHtmlPattern.matcher(text);
    while (m.find()) {
      addMatch(result, new TextRange(m.start(), m.end()), m.group());
    }
    Collections.sort(result);
    return result;
  }

  private static void addMatch(final List<LinkMatch> result, final TextRange range, final String replacement) {
    for (Iterator<LinkMatch> iterator = result.iterator(); iterator.hasNext(); ) {
      LinkMatch oldMatch = iterator.next();
      if (range.contains(oldMatch.getRange())) {
        iterator.remove();
      }
      else if (oldMatch.getRange().contains(range)) {
        return;
      }
    }
    result.add(new LinkMatch(range, replacement));
  }
}

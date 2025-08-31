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

package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.util.SimpleModificationTracker;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
@Singleton
@State(name = "IssueNavigationConfiguration", storages = {@Storage(file = "vcs.xml")})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class IssueNavigationConfiguration extends SimpleModificationTracker implements PersistentStateComponent<IssueNavigationConfiguration> {
  private static final Pattern ourHtmlPattern =
    Pattern.compile("(http:|https:)\\/\\/([^\\s()](?!&(gt|lt|nbsp)+;))+[^\\p{Pe}\\p{Pc}\\p{Pd}\\p{Ps}\\p{Po}\\s]/?");

  public static IssueNavigationConfiguration getInstance(Project project) {
    return project.getInstance(IssueNavigationConfiguration.class);
  }

  private List<IssueNavigationLink> myLinks = new ArrayList<>();

  public List<IssueNavigationLink> getLinks() {
    return myLinks;
  }

  public void setLinks(List<IssueNavigationLink> links) {
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

  public static class LinkMatch implements Comparable<LinkMatch> {
    private final TextRange myRange;
    private final String myTargetUrl;

    public LinkMatch(TextRange range, String targetUrl) {
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
    public int compareTo(@Nonnull LinkMatch rhs) {
      return myRange.getStartOffset() - rhs.getRange().getStartOffset();
    }
  }

  public List<LinkMatch> findIssueLinks(CharSequence text) {
    List<LinkMatch> result = new ArrayList<>();
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

  private static void addMatch(List<LinkMatch> result, TextRange range, String replacement) {
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

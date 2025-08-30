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
package consulo.versionControlSystem.ui.awt;

import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.versionControlSystem.IssueNavigationConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class IssueLinkRenderer {
  private final SimpleColoredComponent myColoredComponent;
  private final IssueNavigationConfiguration myIssueNavigationConfiguration;

  public IssueLinkRenderer(Project project, SimpleColoredComponent coloredComponent) {
    myColoredComponent = coloredComponent;
    myIssueNavigationConfiguration = IssueNavigationConfiguration.getInstance(project);
  }

  public List<String> appendTextWithLinks(String text) {
    return appendTextWithLinks(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  public List<String> appendTextWithLinks(String text, SimpleTextAttributes baseStyle) {
    return appendTextWithLinks(text, baseStyle, s -> append(s, baseStyle));
  }

  public List<String> appendTextWithLinks(String text, SimpleTextAttributes baseStyle, Consumer<String> consumer) {
    List<String> pieces = new ArrayList<>();
    List<IssueNavigationConfiguration.LinkMatch> list = myIssueNavigationConfiguration.findIssueLinks(text);
    int pos = 0;
    SimpleTextAttributes linkAttributes = getLinkAttributes(baseStyle);
    for(IssueNavigationConfiguration.LinkMatch match: list) {
      TextRange textRange = match.getRange();
      if (textRange.getStartOffset() > pos) {
        String piece = text.substring(pos, textRange.getStartOffset());
        pieces.add(piece);
        consumer.accept(piece);
      }
      String piece = textRange.substring(text);
      pieces.add(piece);
      append(piece, linkAttributes, match);
      pos = textRange.getEndOffset();
    }
    if (pos < text.length()) {
      String piece = text.substring(pos);
      pieces.add(piece);
      consumer.accept(piece);
    }
    return pieces;
  }

  private void append(String piece, SimpleTextAttributes baseStyle) {
    myColoredComponent.append(piece, baseStyle);
  }

  private void append(String piece, SimpleTextAttributes baseStyle, IssueNavigationConfiguration.LinkMatch match) {
    myColoredComponent.append(piece, baseStyle, new SimpleColoredComponent.BrowserLauncherTag(match.getTargetUrl()));
  }

  private static SimpleTextAttributes getLinkAttributes(SimpleTextAttributes baseStyle) {
    return (baseStyle.getStyle() & SimpleTextAttributes.STYLE_BOLD) != 0 ?
           SimpleTextAttributes.LINK_BOLD_ATTRIBUTES : SimpleTextAttributes.LINK_ATTRIBUTES;
  }
}

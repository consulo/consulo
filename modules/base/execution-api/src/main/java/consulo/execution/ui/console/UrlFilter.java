// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.ui.console;

import consulo.application.dumb.DumbAware;
import consulo.execution.localize.ExecutionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.Messages;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlFilter implements Filter, DumbAware {
  private final Project myProject;

  public UrlFilter() {
    this(null);
  }

  public UrlFilter(Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public Result applyFilter(@Nonnull String line, int entireLength) {
    if (!URLUtil.canContainUrl(line)) return null;

    int textStartOffset = entireLength - line.length();
    Pattern pattern = line.contains(LocalFileSystem.PROTOCOL_PREFIX) ? URLUtil.FILE_URL_PATTERN : URLUtil.URL_PATTERN;
    Matcher m = pattern.matcher(line);
    ResultItem item = null;
    List<ResultItem> items = null;
    while (m.find()) {
      if (item == null) {
        item = new ResultItem(textStartOffset + m.start(), textStartOffset + m.end(), buildHyperlinkInfo(m.group()));
      }
      else {
        if (items == null) {
          items = new ArrayList<>(2);
          items.add(item);
        }
        items.add(new ResultItem(textStartOffset + m.start(), textStartOffset + m.end(), buildHyperlinkInfo(m.group())));
      }
    }
    return items != null ? new Result(items) : item != null ? new Result(item.getHighlightStartOffset(), item.getHighlightEndOffset(), item.getHyperlinkInfo()) : null;
  }

  @Nonnull
  protected HyperlinkInfo buildHyperlinkInfo(@Nonnull String url) {
    HyperlinkInfo fileHyperlinkInfo = buildFileHyperlinkInfo(url);
    return fileHyperlinkInfo != null ? fileHyperlinkInfo : new OpenUrlHyperlinkInfo(url);
  }

  @Nullable
  private HyperlinkInfo buildFileHyperlinkInfo(@Nonnull String url) {
    if (myProject != null && !url.endsWith(".html") && url.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
      int documentLine = 0, documentColumn = 0;
      int filePathEndIndex = url.length();
      final int lastColonInd = url.lastIndexOf(':');
      if (lastColonInd > LocalFileSystem.PROTOCOL_PREFIX.length() && lastColonInd < url.length() - 1) {
        int lastValue = StringUtil.parseInt(url.substring(lastColonInd + 1), Integer.MIN_VALUE);
        if (lastValue != Integer.MIN_VALUE) {
          documentLine = lastValue - 1;
          filePathEndIndex = lastColonInd;
          int preLastColonInd = url.lastIndexOf(':', lastColonInd - 1);
          if (preLastColonInd > LocalFileSystem.PROTOCOL_PREFIX.length()) {
            int preLastValue = StringUtil.parseInt(url.substring(preLastColonInd + 1, lastColonInd), Integer.MIN_VALUE);
            if (preLastValue != Integer.MIN_VALUE) {
              documentLine = preLastValue - 1;
              documentColumn = lastValue - 1;
              filePathEndIndex = preLastColonInd;
            }
          }
        }
      }
      String filePath = url.substring(LocalFileSystem.PROTOCOL_PREFIX.length(), filePathEndIndex);
      return new FileUrlHyperlinkInfo(filePath, documentLine, documentColumn, url);
    }
    return null;
  }

  private class FileUrlHyperlinkInfo extends LazyFileHyperlinkInfo implements HyperlinkWithPopupMenuInfo {
    private
    @Nonnull
    final String myUrl;

    FileUrlHyperlinkInfo(@Nonnull String filePath, int documentLine, int documentColumn, @Nonnull String url) {
      super(UrlFilter.this.myProject, filePath, documentLine, documentColumn);
      myUrl = url;
    }

    @RequiredUIAccess
    @Override
    public void navigate(@Nonnull Project project) {
      VirtualFile file = getVirtualFile();
      if (file == null || !file.isValid()) {
        Messages.showErrorDialog(
          project,
          ExecutionLocalize.messageCannotFindFile0(StringUtil.trimMiddle(myUrl, 150)).get(),
          ExecutionLocalize.titleCannotOpenFile().get()
        );
        return;
      }
      super.navigate(project);
    }

    @Override
    @Nullable
    public ActionGroup getPopupMenuGroup(@Nonnull MouseEvent event) {
      return new OpenUrlHyperlinkInfo(myUrl).getPopupMenuGroup(event);
    }
  }
}

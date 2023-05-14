/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.documentation;

import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.http.HttpFileSystem;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PlatformDocumentationUtil {

  private static final Logger LOG = Logger.getInstance(PlatformDocumentationUtil.class);

  private static final Pattern ourLtFixupPattern = Pattern.compile("<([^/^\\w^!])");
  private static final Pattern ourToQuote = Pattern.compile("[\\\\\\.\\^\\$\\?\\*\\+\\|\\)\\}\\]\\{\\(\\[]");
  private static final String LT_ENTITY = "&lt;";
  private static final Pattern ourAnchorSuffix = Pattern.compile("#(.*)$");

  public static String getDocURL(String url) {
    Matcher anchorMatcher = ourAnchorSuffix.matcher(url);
    return anchorMatcher.find() ? anchorMatcher.reset().replaceAll("") : url;
  }

  @Nullable
  public static List<String> getHttpRoots(final String[] roots, String relPath) {
    final ArrayList<String> result = new ArrayList<>();
    for (String root : roots) {
      final VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(root);
      if (virtualFile != null) {
        if (virtualFile.getFileSystem() instanceof HttpFileSystem) {
          String url = virtualFile.getUrl();
          if (!url.endsWith("/")) url += "/";
          result.add(url + relPath);
        }
        else {
          VirtualFile file = virtualFile.findFileByRelativePath(relPath);
          if (file != null) result.add(file.getUrl());
        }
      }
    }

    return result.isEmpty() ? null : result;
  }

  private static String quote(String x) {
    if (ourToQuote.matcher(x).find()) {
      return "\\" + x;
    }

    return x;
  }

  public static String fixupText(CharSequence docText) {
    Matcher fixupMatcher = ourLtFixupPattern.matcher(docText);
    LinkedList<String> secondSymbols = new LinkedList<>();

    while (fixupMatcher.find()) {
      String s = fixupMatcher.group(1);

      //[db] that's workaround to avoid internal bug
      if (!s.equals("\\") && !secondSymbols.contains(s)) {
        secondSymbols.addFirst(s);
      }
    }

    for (String s : secondSymbols) {
      String pattern = "<" + quote(s);

      try {
        docText = Pattern.compile(pattern).matcher(docText).replaceAll(LT_ENTITY + pattern);
      }
      catch (PatternSyntaxException e) {
        LOG.error("Pattern syntax exception on " + pattern);
      }
    }

    return docText.toString();
  }
}

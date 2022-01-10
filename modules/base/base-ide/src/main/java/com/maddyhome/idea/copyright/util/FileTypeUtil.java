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

package com.maddyhome.idea.copyright.util;

import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.copyright.CopyrightUpdaters;
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.copyright.config.CopyrightFileConfig;

public class FileTypeUtil {

  @Nonnull
  public static String buildComment(@Nonnull FileType type, @Nonnull String template, @Nonnull CopyrightFileConfig options) {
    Commenter commenter = getCommenter(type);
    UpdateCopyrightsProvider updateCopyrightsProvider = CopyrightUpdaters.INSTANCE.forFileType(type);
    return buildComment(commenter, updateCopyrightsProvider.isAllowSeparator(), template, options);
  }

  @Nonnull
  public static String buildComment(@Nullable Commenter commenter, boolean allowSeparator, @Nonnull String template, @Nonnull CopyrightFileConfig options) {
    if (commenter == null) {
      return "<No comments>";
    }

    String bs = commenter.getBlockCommentPrefix();
    String be = commenter.getBlockCommentSuffix();
    String ls = commenter.getLineCommentPrefix();

    if ((bs == null || be == null) && ls == null) {
      return "<No comments>";
    }

    boolean allowBlock = bs != null && be != null;
    boolean allowLine = ls != null;
    if (allowLine && !allowBlock) {
      bs = ls;
      be = ls;
    }

    String filler = options.getFiller();
    if (!allowSeparator) {
      if (options.getFiller() == CopyrightFileConfig.DEFAULT_FILLER) {
        filler = "~";
      }
    }

    boolean isBlock = options.isBlock();
    boolean isPrefix = options.isPrefixLines();
    if (isBlock && !allowBlock) {
      isPrefix = true;
    }
    boolean isBox = options.isBox() && options.isSeparateBefore() && options.isSeparateAfter() &&
                    options.getLenBefore() == options.getLenAfter();

    StringBuilder preview = new StringBuilder(80);
    String open = isBlock ? bs : allowLine ? ls : bs;
    String close = isBlock ? be : allowLine ? ls : be;
    StringBuilder pre = new StringBuilder(5);
    StringBuilder leader = new StringBuilder(5);
    StringBuilder post = new StringBuilder(5);
    if (filler == CopyrightFileConfig.DEFAULT_FILLER) {
      filler = open.substring(open.length() - 1);
    }
    int offset = 0;
    if (isBlock) {
      int pos = open.length() - 1;
      pre.append(allowBlock ? filler : open.charAt(pos));
      while (pos > 0 && open.charAt(pos) == open.charAt(open.length() - 1)) {
        pos--;
        offset++;
      }
      while (open.length() > 1 && pos >= 0) {
        leader.append(' ');
        pos--;
      }
      post.append(filler);
      if (!isPrefix) {
        pre = new StringBuilder(0);
      }
      if (!allowBlock) {
        close = filler;
      }
    }
    else {
      if (allowLine) {
        close = filler;
      }
      pre.append(open);
      post.append(close);
    }

    int diff = 0;
    if (options.isSeparateBefore()) {
      if (isBlock && isBox && allowBlock) {
        diff = close.length() - offset;
      }

      preview.append(open);
      for (int i = open.length() + 1; i <= options.getLenBefore() - diff - post.length(); i++) {
        preview.append(filler);
      }

      preview.append(post);

      preview.append('\n');
    }
    else if (isBlock) {
      preview.append(open).append('\n');
    }

    if (template.length() > 0) {
      String[] lines = template.split("\n", -1);
      for (String line : lines) {
        if (options.isTrim()) {
          line = line.trim();
        }
        line = StringUtil.trimStart(StringUtil.trimStart(line, pre.toString()), open);
        line = StringUtil.trimEnd(line, close);
        preview.append(leader).append(pre);
        int len = 0;
        if (pre.length() > 0 && line.length() > 0) {
          preview.append(' ');
          len++;
        }
        preview.append(line);
        len += line.length() + leader.length() + pre.length();
        if (isBox && len < options.getLenBefore() - diff) {
          for (; len < options.getLenBefore() - diff - post.length(); len++) {
            preview.append(' ');
          }
          if (isBlock || allowLine) {
            preview.append(post.substring(0, options.getLenBefore() - diff - len));
          }
        }

        if (!isBlock && !allowLine) {
          if (preview.charAt(preview.length() - 1) != ' ') {
            preview.append(' ');
          }
          preview.append(close);
        }

        preview.append('\n');
      }
    }

    preview.append(leader);
    if (options.isSeparateAfter()) {
      preview.append(pre);
      for (int i = leader.length() + pre.length(); i < options.getLenAfter() - close.length(); i++) {
        preview.append(filler);
      }
      preview.append(close);
      preview.append('\n');
    }
    else if (isBlock) {
      if (!allowBlock) {
        preview.append(pre).append('\n');
      }
      else {
        preview.append(close).append('\n');
      }
    }

    return preview.substring(0, preview.length() - 1);
  }

  public static boolean hasBlockComment(FileType fileType) {
    Commenter commenter = getCommenter(fileType);

    return commenter != null && commenter.getBlockCommentPrefix() != null;
  }

  public static boolean hasLineComment(FileType fileType) {
    Commenter commenter = getCommenter(fileType);

    return commenter != null && commenter.getLineCommentPrefix() != null;
  }

  public static Commenter getCommenter(FileType fileType) {
    if (fileType instanceof LanguageFileType) {
      return LanguageCommenters.INSTANCE.forLanguage(((LanguageFileType)fileType).getLanguage());
    }
    return null;
  }
}
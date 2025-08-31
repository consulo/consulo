/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.ide.impl.idea.ide.CommandLineProcessor;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.URLUtil;
import jakarta.annotation.Nonnull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class CustomProtocolHandler {
  public static final String LINE_NUMBER_ARG_NAME = "--line";

  private static final Logger LOG = Logger.getInstance(CustomProtocolHandler.class);

  public void openLink(@Nonnull URI uri) {
    LOG.info("CustomProtocolHandler.openLink");
    List<String> args = getOpenArgs(uri);
    CommandLineProcessor.processExternalCommandLine(CommandLineArgs.parse(ArrayUtil.toStringArray(args)), null);
  }

  @Nonnull
  public List<String> getOpenArgs(URI uri) {
    List<String> args = new ArrayList<String>();
    String query = uri.getQuery();
    String file = null;
    String line = null;
    if (query != null) {
      for (String param : query.split("&")) {
        String[] pair = param.split("=");
        String key = URLUtil.unescapePercentSequences(pair[0]);
        if (pair.length > 1) {
          if ("file".equals(key)) {
            file = URLUtil.unescapePercentSequences(pair[1]);
          } else if ("line".equals(key)) {
            line = URLUtil.unescapePercentSequences(pair[1]);
          }
        }
      }
    }

    if (file != null) {
      if (line != null) {
        args.add(LINE_NUMBER_ARG_NAME);
        args.add(line);
      }
      args.add(file);
    }
    return args;
  }
}

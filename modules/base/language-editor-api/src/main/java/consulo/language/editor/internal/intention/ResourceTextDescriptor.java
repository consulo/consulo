/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.internal.intention;

import consulo.util.io.ResourceUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author yole
 */
public class ResourceTextDescriptor implements TextDescriptor {
  private final URL myUrl;

  public ResourceTextDescriptor(@Nonnull URL url) {
    myUrl = URLUtil.internProtocol(url);
  }

  @Override
  public String getText() throws IOException {
    return ResourceUtil.loadText(myUrl);
  }

  @Override
  public String getFileName() {
    return StringUtil.trimEnd(new File(myUrl.getFile()).getName(), IntentionActionMetaData.EXAMPLE_USAGE_URL_SUFFIX);
  }
}

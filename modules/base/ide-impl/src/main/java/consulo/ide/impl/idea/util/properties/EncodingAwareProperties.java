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
package consulo.ide.impl.idea.util.properties;

import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.text.StringTokenizer;

import java.io.File;
import java.io.IOException;

/**
 * @author MYakovlev
 * @since 2002-10-29
 */
public class EncodingAwareProperties extends java.util.Properties{
  public void load(File file, String encoding) throws IOException{
    String propText = FileUtil.loadFile(file, encoding);
    propText = StringUtil.convertLineSeparators(propText);
    StringTokenizer stringTokenizer = new StringTokenizer(propText, "\n");
    while (stringTokenizer.hasMoreElements()){
      String line = stringTokenizer.nextElement();
      int i = line.indexOf('=');
      String propName = i == -1 ? line : line.substring(0,i);
      String propValue = i == -1 ? "" : line.substring(i+1);
      setProperty(propName, propValue);
    }
  }
}

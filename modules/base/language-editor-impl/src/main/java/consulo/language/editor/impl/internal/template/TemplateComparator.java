/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.impl.internal.template;

import consulo.language.editor.template.Template;

import java.util.Comparator;

/**
 * @author VISTALL
 * @since 11-Jun-24
 */
public class TemplateComparator implements Comparator<Template> {
  public static final TemplateComparator INSTANCE = new TemplateComparator();

  @Override
  public int compare(Template o1, Template o2) {
    return o1.getKey().compareToIgnoreCase(o2.getKey());
  }
}

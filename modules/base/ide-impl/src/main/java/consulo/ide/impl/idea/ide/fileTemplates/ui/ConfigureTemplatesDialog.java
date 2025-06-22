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

package consulo.ide.impl.idea.ide.fileTemplates.ui;

import consulo.application.ui.DimensionService;
import consulo.ide.impl.idea.ide.fileTemplates.impl.AllFileTemplatesConfigurable;
import consulo.ide.impl.idea.openapi.options.ex.SingleConfigurableEditor;
import consulo.project.Project;
import consulo.ui.Size2D;

/**
 * @author MYakovlev
 * @since 2002-10-18
 */
public class ConfigureTemplatesDialog extends SingleConfigurableEditor{
  private static final String DIMENSION_KEY = "#consulo.ide.impl.idea.ide.fileTemplates.ui.ConfigureTemplatesDialog";

  public ConfigureTemplatesDialog(Project project){
    this(project, new AllFileTemplatesConfigurable(project));
  }

  public ConfigureTemplatesDialog(Project project, AllFileTemplatesConfigurable fileTemplatesConfigurable){
    super(project, fileTemplatesConfigurable);
    initSize();
  }

  @Override
  protected String getDimensionServiceKey(){
    return DIMENSION_KEY;
  }

  private void initSize(){
    Size2D size = DimensionService.getInstance().getSize(DIMENSION_KEY, getProject());
    if (size == null){
      DimensionService.getInstance().setSize(DIMENSION_KEY, new Size2D(700, 500), getProject());
    }
  }
}

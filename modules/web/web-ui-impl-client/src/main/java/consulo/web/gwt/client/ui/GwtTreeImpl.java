/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.client.ui;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-Jun-16
 */
public class GwtTreeImpl extends CellTree implements InternalGwtComponent {
  private static class OurModel implements TreeViewModel {
    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
      return new DefaultNodeInfo<Object>(new ListDataProvider<Object>(Collections.emptyList()), new AbstractCell<Object>() {
        @Override
        public void render(Context context, Object value, SafeHtmlBuilder sb) {
          sb.appendEscapedLines("Hello world");
        }
      });
    }

    @Override
    public boolean isLeaf(Object value) {
      return false;
    }

  }

  public <T> GwtTreeImpl() {
    super(new OurModel(), 0, GWT.<CellTree.Resources>create(DefaultCellTreeResources.class));
  }

  @Override
  public void updateState(@NotNull Map<String, Serializable> map) {

  }
}

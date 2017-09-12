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

import java.util.Collections;

/**
 * @author VISTALL
 * @since 19-Jun-16
 */
public class GwtTreeImpl extends CellTree {
  private static class OurModel implements TreeViewModel {
    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
      /*if (value == null) {
        UIComponent.Child child = myChildren.get(null);
        return new DefaultNodeInfo<>(new ListDataProvider<>(Collections.emptyList()), new AbstractCell<Object>() {
          @Override
          public void render(Context context, Object value, SafeHtmlBuilder sb) {

            sb.appendEscapedLines("dummy");
          }

          @Override
          public void setValue(Context context, Element parent, Object value) {
            InternalGwtComponent internalGwtComponent = UIConverter.create(myProxy, child.getComponent());

            parent.appendChild(((Widget)internalGwtComponent).getElement());
          }
        });
      } */

      return new DefaultNodeInfo<>(new ListDataProvider<>(Collections.emptyList()), new AbstractCell<Object>() {
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

  public GwtTreeImpl() {
    super(new OurModel(), null, GWT.<CellTree.Resources>create(DefaultCellTreeResources.class));
  }

  public void clear() {
  }
}

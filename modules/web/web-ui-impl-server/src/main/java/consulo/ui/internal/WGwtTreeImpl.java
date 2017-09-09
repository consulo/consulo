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
package consulo.ui.internal;

import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.web.gwt.shared.UIComponent;
import consulo.web.gwt.shared.ui.InternalEventTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class WGwtTreeImpl<NODE> extends WGwtBaseComponent {
  private WGwtTreeModelImpl<NODE> myModel;

  private List<WGwtTreeNodeImpl<NODE>> myChildren = new ArrayList<>();

  public WGwtTreeImpl(WGwtTreeModelImpl<NODE> model) {
    myModel = model;

    enableNotify(InternalEventTypes.SHOW);
  }

  @RequiredUIAccess
  @Override
  public void invokeListeners(long type, Map<String, Object> variables) {
    UIAccess uiAccess = UIAccess.get();

    if (type == InternalEventTypes.SHOW) {
      AppExecutorUtil.getAppExecutorService().execute(() -> {
        NODE node = myModel.fetchRootNode();

        WGwtTreeNodeImpl<NODE> treeNode = new WGwtTreeNodeImpl<>(null, node);

        uiAccess.give(() -> {
          myModel.renderNode(node, treeNode.getPresentation());

          myChildren.add(treeNode);

          markAsChanged(CHILDREN_CHANGED);
        });
      });
    }
  }

  @Override
  protected void initChildren(List<UIComponent.Child> children) {
    for (WGwtTreeNodeImpl<NODE> child : myChildren) {
      UIComponent.Child e = new UIComponent.Child();
      e.setComponent(child.getPresentation().getLayout().convert());

      e.setVariables(Collections.singletonMap("parentId", child.getParentId()));

      children.add(e);
    }
  }
}

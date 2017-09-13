/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.gwt.shared.ui.state.tree;

import com.vaadin.shared.AbstractComponentState;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public class TreeState extends AbstractComponentState {
  private static final long serialVersionUID = 2585950367681585932L;

  public static class TreeNodeState extends ComboBoxState.Item {
    private static final long serialVersionUID = -364018744979706967L;
    public String myId;
    public String myParentId;

    public boolean myLeaf;
  }

  public static enum TreeChangeType {
    ADD,
    REMOVE,
    SET
  }

  public static class TreeChange implements Serializable {
    public String myId;

    public List<TreeNodeState> myNodes = new ArrayList<>();

    public TreeChangeType myType;
  }

  public List<TreeChange> myChanges = new ArrayList<>();
}

/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.*;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

public class ChangeListManagerSerialization {
  static final String ATT_ID = "id";
  static final String ATT_NAME = "name";
  static final String ATT_COMMENT = "comment";
  static final String ATT_DEFAULT = "default";
  static final String ATT_READONLY = "readonly";
  static final String ATT_VALUE_TRUE = "true";
  static final String ATT_CHANGE_TYPE = "type";
  static final String ATT_CHANGE_BEFORE_PATH = "beforePath";
  static final String ATT_CHANGE_AFTER_PATH = "afterPath";
  static final String NODE_LIST = "list";
  static final String NODE_CHANGE = "change";

  private final ChangeListWorker myWorker;

  public ChangeListManagerSerialization(ChangeListWorker worker) {
    myWorker = worker;
  }

  @SuppressWarnings({"unchecked"})
  public void readExternal(Element element) {
    List<Element> listNodes = element.getChildren(NODE_LIST);
    for (Element listNode : listNodes) {
      readChangeList(listNode);
    }
  }

  private void readChangeList(Element listNode) {
    // workaround for loading incorrect settings (with duplicate changelist names)
    String changeListName = listNode.getAttributeValue(ATT_NAME);
    LocalChangeList list = myWorker.getCopyByName(changeListName);
    if (list == null) {
      list = myWorker.addChangeList(listNode.getAttributeValue(ATT_ID), changeListName, listNode.getAttributeValue(ATT_COMMENT), false,
                                    null);
    }
    //noinspection unchecked
    List<Element> changeNodes = listNode.getChildren(NODE_CHANGE);
    for (Element changeNode : changeNodes) {
      myWorker.addChangeToList(changeListName, readChange(changeNode), null);
    }

    if (ATT_VALUE_TRUE.equals(listNode.getAttributeValue(ATT_DEFAULT))) {
      myWorker.setDefault(list.getName());
    }
    if (ATT_VALUE_TRUE.equals(listNode.getAttributeValue(ATT_READONLY))) {
      list.setReadOnly(true);
    }
  }

  public static void writeExternal(Element element, ChangeListWorker worker) {
    for (LocalChangeList list : worker.getListsCopy()) {
      Element listNode = new Element(NODE_LIST);
      element.addContent(listNode);
      if (list.isDefault()) {
        listNode.setAttribute(ATT_DEFAULT, ATT_VALUE_TRUE);
      }
      if (list.isReadOnly()) {
        listNode.setAttribute(ATT_READONLY, ATT_VALUE_TRUE);
      }

      listNode.setAttribute(ATT_ID, list.getId());
      listNode.setAttribute(ATT_NAME, list.getName());
      String comment = list.getComment();
      if (comment != null) {
        listNode.setAttribute(ATT_COMMENT, comment);
      }
      List<Change> changes = new ArrayList<>(list.getChanges());
      changes.sort((o1, o2) -> Comparing.compare(o1.toString(), o2.toString()));
      for (Change change : changes) {
        writeChange(listNode, change);
      }
    }
  }

  private static void writeChange(Element listNode, Change change) {
    Element changeNode = new Element(NODE_CHANGE);
    listNode.addContent(changeNode);
    changeNode.setAttribute(ATT_CHANGE_TYPE, change.getType().name());

    ContentRevision bRev = change.getBeforeRevision();
    ContentRevision aRev = change.getAfterRevision();

    changeNode.setAttribute(ATT_CHANGE_BEFORE_PATH, bRev != null ? bRev.getFile().getPath() : "");
    changeNode.setAttribute(ATT_CHANGE_AFTER_PATH, aRev != null ? aRev.getFile().getPath() : "");
  }

  private static Change readChange(Element changeNode) {
    String bRev = changeNode.getAttributeValue(ATT_CHANGE_BEFORE_PATH);
    String aRev = changeNode.getAttributeValue(ATT_CHANGE_AFTER_PATH);
    return new Change(StringUtil.isEmpty(bRev) ? null : new FakeRevision(bRev), StringUtil.isEmpty(aRev) ? null : new FakeRevision(aRev));
  }
}

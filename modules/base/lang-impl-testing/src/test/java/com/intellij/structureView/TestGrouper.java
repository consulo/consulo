package com.intellij.structureView;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.Group;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.util.containers.ContainerUtil;
import java.util.HashSet;
import javax.annotation.Nonnull;

import java.util.*;

public class TestGrouper implements Grouper {
  private final String[] mySubStrings;

  public TestGrouper(String[] subString) {
    mySubStrings = subString;
  }

  @Override
  @Nonnull
  public ActionPresentation getPresentation() {
    throw new RuntimeException();
  }

  @Override
  @Nonnull
  public String getName() {
    throw new RuntimeException();
  }

  private class StringGroup implements Group {
    private final String myString;
    private final Collection<TreeElement> myChildren;
    private final Collection<String> myChildrenUsedStrings;

    public StringGroup(String string, final Collection<TreeElement> children, Collection<String> childrenStrings) {
      myString = string;
      myChildrenUsedStrings = childrenStrings;
      myChildren = new ArrayList<TreeElement>(children);
    }

    @Override
    public Collection<TreeElement> getChildren() {
      Collection<TreeElement> result = new LinkedHashSet<TreeElement>();
      for (TreeElement object : myChildren) {
        if (object.toString().indexOf(myString) >= 0) {
          result.add(object);
        }
      }
      return result;
    }

    @Override
    public ItemPresentation getPresentation() {
      return null;
    }

    public String toString() {
      return "Group:" + myString;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Group)) return false;

      final StringGroup group = (StringGroup)o;

      if (myString != null ? !myString.equals(group.myString) : group.myString != null) return false;

      return true;
    }

    public int hashCode() {
      return (myString != null ? myString.hashCode() : 0);
    }
  }

  @Override
  @Nonnull
  public Collection<Group> group(final AbstractTreeNode parent, Collection<TreeElement> children) {
    List<Group> result = new ArrayList<Group>();
    Collection<String> parentGroupUsedStrings = parent.getValue() instanceof StringGroup ?
                                                ((StringGroup)parent.getValue()).myChildrenUsedStrings :
                                                Collections.<String>emptySet();
    Collection<TreeElement> elements = new LinkedHashSet<TreeElement>(children);
    for (String subString : mySubStrings) {
      if (parentGroupUsedStrings.contains(subString)) continue;
      Set<String> childrenStrings = new HashSet<String>(parentGroupUsedStrings);
      ContainerUtil.addAll(childrenStrings, mySubStrings);
      StringGroup group = new StringGroup(subString, elements, childrenStrings);
      Collection<TreeElement> groupChildren = group.getChildren();
      if (!groupChildren.isEmpty()) {
        elements.removeAll(groupChildren);
        result.add(group);
      }
    }
    return result;
  }
}

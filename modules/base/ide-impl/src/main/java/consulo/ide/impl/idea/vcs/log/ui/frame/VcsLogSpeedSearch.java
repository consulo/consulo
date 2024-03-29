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
package consulo.ide.impl.idea.vcs.log.ui.frame;

import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.AbstractList;
import java.util.ListIterator;

public class VcsLogSpeedSearch extends SpeedSearchBase<VcsLogGraphTable> {
  public VcsLogSpeedSearch(@Nonnull VcsLogGraphTable component) {
    super(component);
  }

  @Override
  protected int getElementCount() {
    return myComponent.getRowCount();
  }

  @Nonnull
  @Override
  protected ListIterator<Object> getElementIterator(int startingIndex) {
    return new MyRowsList().listIterator(startingIndex);
  }

  @Override
  protected int getSelectedIndex() {
    return myComponent.getSelectedRow();
  }

  @Nonnull
  @Override
  protected Object[] getAllElements() {
    throw new UnsupportedOperationException("Getting all elements in a Log in an array is unsupported.");
  }

  @Nullable
  @Override
  protected String getElementText(@Nonnull Object row) {
    return myComponent.getModel().getShortDetails((Integer)row).getSubject();
  }

  @Override
  protected void selectElement(@Nonnull Object row, @Nonnull String selectedText) {
    myComponent.jumpToRow((Integer)row);
  }

  private class MyRowsList extends AbstractList<Object> {
    @Override
    public int size() {
      return myComponent.getRowCount();
    }

    @Override
    public Object get(int index) {
      return index;
    }
  }
}

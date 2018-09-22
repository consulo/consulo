// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle;

import com.intellij.formatting.fileSet.FileSetDescriptor;
import com.intellij.formatting.fileSet.FileSetDescriptorFactory;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.OptionTag;
import org.jdom.Element;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

public class ExcludedFiles {
  private final List<FileSetDescriptor> myDescriptors = ContainerUtil.newArrayList();
  private final State myState = new State();

  public void serializeInto(@Nonnull Element element) {
    if (myDescriptors.size() > 0) {
      XmlSerializer.serializeInto(myState, element);
    }
  }

  public void deserializeFrom(@Nonnull Element element) {
    XmlSerializer.deserializeInto(myState, element);
  }

  public void addDescriptor(@Nonnull FileSetDescriptor descriptor) {
    myDescriptors.add(descriptor);
  }

  public List<FileSetDescriptor> getDescriptors() {
    return myDescriptors;
  }


  public void setDescriptors(@Nonnull List<FileSetDescriptor> descriptors) {
    myDescriptors.clear();
    myDescriptors.addAll(descriptors);
  }

  public boolean contains(@Nonnull PsiFile file) {
    if (file.isPhysical()) {
      for (FileSetDescriptor descriptor : myDescriptors) {
        if (descriptor.matches(file)) return true;
      }
    }
    return false;
  }

  public void clear() {
    myDescriptors.clear();
  }

  public boolean equals(@Nonnull Object o) {
    return o instanceof ExcludedFiles && myDescriptors.equals(((ExcludedFiles)o).myDescriptors);
  }

  public class State {
    @OptionTag("DO_NOT_FORMAT")
    public List<FileSetDescriptor.State> getDescriptors() {
      return myDescriptors.stream().map(descriptor -> descriptor.getState()).collect(Collectors.toList());
    }

    public void setDescriptors(@Nonnull List<FileSetDescriptor.State> states) {
      myDescriptors.clear();
      for (FileSetDescriptor.State state : states) {
        FileSetDescriptor descriptor = FileSetDescriptorFactory.createDescriptor(state);
        if (descriptor != null) {
          myDescriptors.add(descriptor);
        }
      }
    }
  }
}

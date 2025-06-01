// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.language.codeStyle.fileSet.FileSetDescriptor;
import consulo.language.codeStyle.fileSet.FileSetDescriptorFactory;
import consulo.language.psi.PsiFile;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.OptionTag;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExcludedFiles {
    private final List<FileSetDescriptor> myDescriptors = new ArrayList<>();
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
                if (descriptor.matches(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void clear() {
        myDescriptors.clear();
    }

    @Override
    @SuppressWarnings("EqualsHashCode")
    public boolean equals(@Nonnull Object o) {
        return o instanceof ExcludedFiles excludedFiles && myDescriptors.equals(excludedFiles.myDescriptors);
    }

    public class State {
        @OptionTag("DO_NOT_FORMAT")
        public List<FileSetDescriptor.State> getDescriptors() {
            return myDescriptors.stream().map(FileSetDescriptor::getState).collect(Collectors.toList());
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

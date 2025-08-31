/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.ide.impl.idea.codeInspection.ex.Descriptor;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Batkovich
 */
public class ToolDescriptors {

  @Nonnull
  private final Descriptor myDefaultDescriptor;
  @Nonnull
  private final List<Descriptor> myNonDefaultDescriptors;

  private ToolDescriptors(@Nonnull Descriptor defaultDescriptor,
                          @Nonnull List<Descriptor> nonDefaultDescriptors) {
    myDefaultDescriptor = defaultDescriptor;
    myNonDefaultDescriptors = nonDefaultDescriptors;
  }

  public static ToolDescriptors fromScopeToolState(ScopeToolState state,
                                                   InspectionProfileImpl profile,
                                                   Project project) {
    InspectionToolWrapper toolWrapper = state.getTool();
    List<ScopeToolState> nonDefaultTools = profile.getNonDefaultTools(toolWrapper.getShortName(), project);
    ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>(nonDefaultTools.size());
    for (ScopeToolState nonDefaultToolState : nonDefaultTools) {
      descriptors.add(new Descriptor(nonDefaultToolState, profile, project));
    }
    return new ToolDescriptors(new Descriptor(state, profile, project), descriptors);
  }

  @Nonnull
  public Descriptor getDefaultDescriptor() {
    return myDefaultDescriptor;
  }

  @Nonnull
  public List<Descriptor> getNonDefaultDescriptors() {
    return myNonDefaultDescriptors;
  }

  @Nonnull
  public ScopeToolState getDefaultScopeToolState() {
    return myDefaultDescriptor.getState();
  }
}

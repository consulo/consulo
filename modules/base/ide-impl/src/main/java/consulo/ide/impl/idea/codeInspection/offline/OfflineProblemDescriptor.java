/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.offline;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author anna
 * @since 2007-01-05
 */
public class OfflineProblemDescriptor {
    public String myType;
    public String myFQName;
    public String myDescription;
    public List<String> myHints;
    public int myProblemIndex;
    public int myLine;
    public String[] myParentType;
    public String[] myParentFQName;
    public String myModuleName;

    public String getType() {
        return myType;
    }

    public void setType(String type) {
        myType = type;
    }

    public String getFQName() {
        return myFQName;
    }

    public void setFQName(String FQName) {
        myFQName = FQName;
    }

    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        myDescription = description;
    }

    public List<String> getHints() {
        return myHints;
    }

    public void setHints(List<String> hints) {
        myHints = hints;
    }

    public int getProblemIndex() {
        return myProblemIndex;
    }

    public void setProblemIndex(int problemIndex) {
        myProblemIndex = problemIndex;
    }

    public int getLine() {
        return myLine;
    }

    public void setLine(int line) {
        myLine = line;
    }

    public String[] getParentType() {
        return myParentType;
    }

    public void setParentType(String[] parentType) {
        myParentType = parentType;
    }

    public String[] getParentFQName() {
        return myParentFQName;
    }

    public void setParentFQName(String[] parentFQName) {
        myParentFQName = parentFQName;
    }

    @Nullable
    @RequiredReadAction
    public RefEntity getRefElement(RefManager refManager) {
        RefEntity refEntity = refManager.getReference(myType, myFQName);
        if (refEntity instanceof RefElement refElement) {
            PsiElement element = refElement.getPsiElement();
            if (element != null && element.isValid()) {
                PsiDocumentManager.getInstance(element.getProject()).commitAllDocuments();
            }
        }
        return refEntity;
    }

    @Nullable
    public OfflineProblemDescriptor getOwner() {
        if (myParentType != null && myParentFQName != null) {
            OfflineProblemDescriptor descriptor = new OfflineProblemDescriptor();
            descriptor.setLine(myLine);
            descriptor.setFQName(myParentFQName[0]);
            descriptor.setType(myParentType[0]);
            if (myParentType.length > 1 && myParentFQName.length > 1) {
                descriptor.setParentType(ArrayUtil.remove(myParentType, 0));
                descriptor.setParentFQName(ArrayUtil.remove(myParentFQName, 0));
            }
            return descriptor;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OfflineProblemDescriptor that = (OfflineProblemDescriptor) o;

      return myLine == that.myLine
          && myProblemIndex == that.myProblemIndex
          && Objects.equals(myDescription, that.myDescription)
          && Objects.equals(myFQName, that.myFQName)
          && Objects.equals(myHints, that.myHints)
          && Objects.equals(myModuleName, that.myModuleName)
          && Arrays.equals(myParentFQName, that.myParentFQName)
          && Arrays.equals(myParentType, that.myParentType)
          && Objects.equals(myType, that.myType);
    }

    @Override
    public int hashCode() {
        int result;
        result = (myType != null ? myType.hashCode() : 0);
        result = 31 * result + (myFQName != null ? myFQName.hashCode() : 0);
        result = 31 * result + (myDescription != null ? myDescription.hashCode() : 0);
        result = 31 * result + (myHints != null ? myHints.hashCode() : 0);
        result = 31 * result + myProblemIndex;
        result = 31 * result + myLine;
        result = 31 * result + (myParentType != null ? Arrays.hashCode(myParentType) : 0);
        result = 31 * result + (myParentFQName != null ? Arrays.hashCode(myParentFQName) : 0);
        result = 31 * result + (myModuleName != null ? myModuleName.hashCode() : 0);
        return result;
    }

    public void setModule(String moduleName) {
        myModuleName = moduleName;
    }

    public String getModuleName() {
        return myModuleName;
    }
}

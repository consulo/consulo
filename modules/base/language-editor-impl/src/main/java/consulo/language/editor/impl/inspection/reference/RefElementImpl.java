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

package consulo.language.editor.impl.inspection.reference;

import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.component.util.Iconable;
import consulo.language.editor.inspection.SuppressionUtil;
import consulo.language.editor.inspection.reference.*;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class RefElementImpl extends RefEntityImpl implements RefElement, WritableRefElement {
  protected static final Logger LOG = Logger.getInstance(RefElementImpl.class);

  private static final int IS_ENTRY_MASK = 0x80;
  private static final int IS_PERMANENT_ENTRY_MASK = 0x100;


  private final SmartPsiElementPointer myID;

  private List<RefElement> myOutReferences; // guarded by this
  private List<RefElement> myInReferences; // guarded by this

  private String[] mySuppressions;

  private volatile boolean myIsDeleted;
  protected static final int IS_REACHABLE_MASK = 0x40;

  protected RefElementImpl(@Nonnull String name, @Nonnull RefElement owner) {
    super(name, owner.getRefManager());
    myID = null;
    myFlags = 0;
  }

  protected RefElementImpl(PsiFile file, RefManager manager) {
    this(file.getName(), file, manager);
  }

  protected RefElementImpl(@Nonnull String name, @Nonnull PsiElement element, @Nonnull RefManager manager) {
    super(name, manager);
    myID = SmartPointerManager.getInstance(manager.getProject()).createSmartPsiElementPointer(element);
    myFlags = 0;
  }

  protected boolean isDeleted() {
    return myIsDeleted;
  }

  @Override
  public boolean isValid() {
    if (myIsDeleted) return false;
    return ReadAction.compute(() -> {
      if (getRefManager().getProject().isDisposed()) return false;

      PsiFile file = myID.getContainingFile();
      //no need to check resolve in offline mode
      if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
        return file != null && file.isPhysical();
      }

      PsiElement element = getPsiElement();
      return element != null && element.isPhysical();
    });
  }

  @Override
  @Nullable
  public Image getIcon(boolean expanded) {
    PsiElement element = getPsiElement();
    if (element != null && element.isValid()) {
      return IconDescriptorUpdaters.getIcon(element, Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS);
    }
    return null;
  }

  @Override
  public RefModule getModule() {
    RefEntity owner = getOwner();
    return owner instanceof RefElement ? ((RefElement)owner).getModule() : null;
  }

  @Override
  public String getExternalName() {
    return getName();
  }

  @Override
  @Nullable
  public PsiElement getPsiElement() {
    return myID.getElement();
  }

  @Nullable
  public PsiFile getContainingFile() {
    return myID.getContainingFile();
  }

  public VirtualFile getVirtualFile() {
    return myID.getVirtualFile();
  }

  @Override
  public SmartPsiElementPointer getPointer() {
    return myID;
  }

  public void buildReferences() {
  }

  @Override
  public boolean isReachable() {
    return checkFlag(IS_REACHABLE_MASK);
  }

  public void setReachable(boolean reachable) {
    setFlag(reachable, IS_REACHABLE_MASK);
  }

  @Override
  public boolean isReferenced() {
    return !getInReferences().isEmpty();
  }

  public boolean hasSuspiciousCallers() {
    for (RefElement refCaller : getInReferences()) {
      if (((RefElementImpl)refCaller).isSuspicious()) return true;
    }

    return false;
  }

  @Override
  @Nonnull
  public synchronized Collection<RefElement> getOutReferences() {
    return ObjectUtil.notNull(myOutReferences, List.of());
  }

  @Override
  @Nonnull
  public synchronized Collection<RefElement> getInReferences() {
    return ObjectUtil.notNull(myInReferences, List.of());
  }

  @Override
  public synchronized void addInReference(RefElement refElement) {
    List<RefElement> inReferences = myInReferences;
    if (inReferences == null) {
      myInReferences = inReferences = new ArrayList<>(1);
    }
    if (!inReferences.contains(refElement)) {
      inReferences.add(refElement);
    }
  }

  @Override
  public synchronized void addOutReference(RefElement refElement) {
    List<RefElement> outReferences = myOutReferences;
    if (outReferences == null) {
      myOutReferences = outReferences = new ArrayList<>(1);
    }
    if (!outReferences.contains(refElement)) {
      outReferences.add(refElement);
    }
  }

  public void setEntry(boolean entry) {
    setFlag(entry, IS_ENTRY_MASK);
  }

  @Override
  public boolean isEntry() {
    return checkFlag(IS_ENTRY_MASK);
  }

  @Override
  public boolean isPermanentEntry() {
    return checkFlag(IS_PERMANENT_ENTRY_MASK);
  }


  @Override
  @Nonnull
  public RefElement getContainingEntry() {
    return this;
  }

  public void setPermanentEntry(boolean permanentEntry) {
    setFlag(permanentEntry, IS_PERMANENT_ENTRY_MASK);
  }

  public boolean isSuspicious() {
    return !isReachable();
  }

  public void referenceRemoved() {
    myIsDeleted = true;
    if (getOwner() != null) {
      getOwner().removeChild(this);
    }

    for (RefElement refCallee : getOutReferences()) {
      refCallee.getInReferences().remove(this);
    }

    for (RefElement refCaller : getInReferences()) {
      refCaller.getOutReferences().remove(this);
    }
  }

  @Override
  @Nullable
  public String getURL() {
    PsiElement element = getPsiElement();
    if (element == null || !element.isPhysical()) return null;
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) return null;
    VirtualFile virtualFile = containingFile.getVirtualFile();
    if (virtualFile == null) return null;
    return virtualFile.getUrl() + "#" + element.getTextOffset();
  }

  public abstract void initialize();

  @Override
  public void addSuppression(String text) {
    mySuppressions = text.split("[, ]");
  }

  @Override
  public boolean isSuppressed(@Nonnull String... toolId) {
    if (mySuppressions != null) {
      for (@NonNls String suppression : mySuppressions) {
        for (String id : toolId) {
          if (suppression.equals(id)) return true;
        }
        if (suppression.equalsIgnoreCase(SuppressionUtil.ALL)) {
          return true;
        }
      }
    }
    RefEntity entity = getOwner();
    return entity instanceof RefElementImpl && ((RefElementImpl)entity).isSuppressed(toolId);
  }
}

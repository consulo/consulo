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
package consulo.versionControlSystem.impl.internal.patch.apply;

import consulo.application.util.function.ThrowableComputable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.change.patch.BinaryFilePatch;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedBinaryFilePatch;
import consulo.versionControlSystem.impl.internal.util.RelativePathCalculator;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class PathsVerifier<BinaryType extends FilePatch> {
  // in
  private final Project myProject;
  private final VirtualFile myBaseDirectory;
  private final List<FilePatch> myPatches;
  // temp
  private final Map<VirtualFile, MovedFileData> myMovedFiles;
  private final List<FilePath> myBeforePaths;
  private final List<VirtualFile> myCreatedDirectories;
  // out
  private final List<Pair<VirtualFile, ApplyTextFilePatch>> myTextPatches;
  private final List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> myBinaryPatches;
  @Nonnull
  private final List<VirtualFile> myWritableFiles;
  private final BaseMapper myBaseMapper;
  private ProjectLevelVcsManager myVcsManager;
  private final List<FilePatch> mySkipped;
  private DelayedPrecheckContext myDelayedPrecheckContext;
  private List<FilePath> myAddedPaths;
  private List<FilePath> myDeletedPaths;
  private boolean myIgnoreContentRootsCheck;

  public PathsVerifier(Project project, VirtualFile baseDirectory, List<FilePatch> patches, BaseMapper baseMapper) {
    myProject = project;
    myBaseDirectory = baseDirectory;
    myPatches = patches;
    myBaseMapper = baseMapper;

    myMovedFiles = new HashMap<>();
    myBeforePaths = new ArrayList<>();
    myCreatedDirectories = new ArrayList<>();
    myTextPatches = new ArrayList<>();
    myBinaryPatches = new ArrayList<>();
    myWritableFiles = new ArrayList<>();
    myVcsManager = ProjectLevelVcsManager.getInstance(myProject);
    mySkipped = new ArrayList<>();

    myAddedPaths = new ArrayList<>();
    myDeletedPaths = new ArrayList<>();
  }

  // those to be moved to CL: target + created dirs
  public List<FilePath> getDirectlyAffected() {
    List<FilePath> affected = new ArrayList<>();
    addAllFilePath(myCreatedDirectories, affected);
    addAllFilePath(myWritableFiles, affected);
    affected.addAll(myBeforePaths);
    return affected;
  }

  // old parents of moved files
  public List<VirtualFile> getAllAffected() {
    List<VirtualFile> affected = new ArrayList<>();
    affected.addAll(myCreatedDirectories);
    affected.addAll(myWritableFiles);

    // after files' parent
    for (VirtualFile file : myMovedFiles.keySet()) {
      VirtualFile parent = file.getParent();
      if (parent != null) {
        affected.add(parent);
      }
    }
    // before..
    for (FilePath path : myBeforePaths) {
      FilePath parent = path.getParentPath();
      if (parent != null) {
        affected.add(parent.getVirtualFile());
      }
    }
    return affected;
  }

  private static void addAllFilePath(Collection<VirtualFile> files, Collection<FilePath> paths) {
    for (VirtualFile file : files) {
      paths.add(VcsUtil.getFilePath(file));
    }
  }

  @RequiredUIAccess
  public List<FilePatch> nonWriteActionPreCheck() {
    List<FilePatch> failedToApply = new ArrayList<>();
    myDelayedPrecheckContext = new DelayedPrecheckContext(myProject);
    for (FilePatch patch : myPatches) {
      CheckPath checker = getChecker(patch);
      if (!checker.canBeApplied(myDelayedPrecheckContext)) {
        revert(checker.getErrorMessage());
        failedToApply.add(patch);
      }
    }
    Collection<FilePatch> skipped = myDelayedPrecheckContext.doDelayed();
    mySkipped.addAll(skipped);
    myPatches.removeAll(skipped);
    myPatches.removeAll(failedToApply);
    return failedToApply;
  }

  public List<FilePatch> getSkipped() {
    return mySkipped;
  }

  public List<FilePatch> execute() {
    List<FilePatch> failedPatches = new ArrayList<>();
    try {
      List<CheckPath> checkers = new ArrayList<>(myPatches.size());
      for (FilePatch patch : myPatches) {
        CheckPath checker = getChecker(patch);
        checkers.add(checker);
      }
      for (CheckPath checker : checkers) {
        if (!checker.check()) {
          failedPatches.add(checker.getPatch());
          revert(checker.getErrorMessage());
        }
      }
    }
    catch (IOException e) {
      revert(e.getMessage());
    }
    myPatches.removeAll(failedPatches);
    return failedPatches;
  }

  private CheckPath getChecker(FilePatch patch) {
    String beforeFileName = patch.getBeforeName();
    String afterFileName = patch.getAfterName();

    if ((beforeFileName == null) || (patch.isNewFile())) {
      return new CheckAdded(patch);
    } else if ((afterFileName == null) || (patch.isDeletedFile())) {
      return new CheckDeleted(patch);
    } else {
      if (! beforeFileName.equals(afterFileName)) {
        return new CheckMoved(patch);
      } else {
        return new CheckModified(patch);
      }
    }
  }

  public Collection<FilePath> getToBeAdded() {
    return myAddedPaths;
  }

  public Collection<FilePath> getToBeDeleted() {
    return myDeletedPaths;
  }

  @Nonnull
  public Collection<FilePatch> filterBadFileTypePatches() {
    List<Pair<VirtualFile, ApplyTextFilePatch>> failedTextPatches =
      ContainerUtil.findAll(myTextPatches, textPatch -> {
        VirtualFile file = textPatch.getFirst();
        return !file.isDirectory() && !isFileTypeOk(file);
      });
    myTextPatches.removeAll(failedTextPatches);
    return ContainerUtil.map(
      failedTextPatches,
      (Function<Pair<VirtualFile, ApplyTextFilePatch>, FilePatch>)patchInfo -> patchInfo.getSecond().getPatch()
    );
  }

  private boolean isFileTypeOk(@Nonnull VirtualFile file) {
    FileType fileType = file.getFileType();
    if (fileType == UnknownFileType.INSTANCE) {
      fileType = FileTypeRegistry.getInstance().getKnownFileTypeOrAssociate(file.getName());
      if (fileType == null) {
        PatchApplier
                .showError(myProject, "Cannot apply content for " + file.getPresentableName() + " file from patch because its type not defined.",
                           true);
        return false;
      }
    }
    if (fileType.isBinary()) {
      PatchApplier.showError(myProject, "Cannot apply file " + file.getPresentableName() + " from patch because it is binary.", true);
      return false;
    }
    return true;
  }

  private class CheckModified extends CheckDeleted {
    private CheckModified(FilePatch path) {
      super(path);
    }
  }

  private class CheckDeleted extends CheckPath {
    protected CheckDeleted(FilePatch path) {
      super(path);
    }

    @Override
    protected boolean precheck(VirtualFile beforeFile, VirtualFile afterFile, DelayedPrecheckContext context) {
      if (beforeFile == null) {
        context.addSkip(myBaseMapper.getPath(myPatch, myBeforeName), myPatch);
      }
      return true;
    }

    @Override
    protected boolean check() throws IOException {
      VirtualFile beforeFile = myBaseMapper.getFile(myPatch, myBeforeName);
      if (! checkExistsAndValid(beforeFile, myBeforeName)) {
        return false;
      }
      addPatch(myPatch, beforeFile);
      FilePath filePath = VcsUtil.getFilePath(beforeFile.getParent(), beforeFile.getName(), beforeFile.isDirectory());
      if (myPatch.isDeletedFile() || myPatch.getAfterName() == null) {
        myDeletedPaths.add(filePath);
      }
      myBeforePaths.add(filePath);
      return true;
    }
  }

  private class CheckAdded extends CheckPath {
    private CheckAdded(FilePatch path) {
      super(path);
    }

    @Override
    protected boolean precheck(VirtualFile beforeFile, VirtualFile afterFile, DelayedPrecheckContext context) {
      if (afterFile != null) {
        context.addOverrideExisting(myPatch, VcsUtil.getFilePath(afterFile));
      }
      return true;
    }

    @Override
    public boolean check() throws IOException {
      String[] pieces = RelativePathCalculator.split(myAfterName);
      VirtualFile parent = makeSureParentPathExists(pieces);
      if (parent == null) {
        setErrorMessage(fileNotFoundMessage(myAfterName));
        return false;
      }
      String name = pieces[pieces.length - 1];
      File afterFile = new File(parent.getPath(), name);
      //if user already accepted overwriting, we shouldn't have created a new one
      VirtualFile file = myDelayedPrecheckContext.getOverridenPaths().contains(VcsUtil.getFilePath(afterFile))
                               ? parent.findChild(name)
                               : createFile(parent, name);
      if (file == null) {
        setErrorMessage(fileNotFoundMessage(myAfterName));
        return false;
      }
      myAddedPaths.add(VcsUtil.getFilePath(file));
      if (! checkExistsAndValid(file, myAfterName)) {
        return false;
      }
      addPatch(myPatch, file);
      return true;
    }
  }

  private class CheckMoved extends CheckPath {
    private CheckMoved(FilePatch path) {
      super(path);
    }

    // before exists; after does not exist
    @Override
    protected boolean precheck(
      VirtualFile beforeFile,
      VirtualFile afterFile,
      DelayedPrecheckContext context
    ) {
      if (beforeFile == null) {
        setErrorMessage(fileNotFoundMessage(myBeforeName));
      } else if (afterFile != null) {
        setErrorMessage(fileAlreadyExists(afterFile.getPath()));
      }
      return (beforeFile != null) && (afterFile == null);
    }

    @Override
    public boolean check() throws IOException {
      String[] pieces = RelativePathCalculator.split(myAfterName);
      VirtualFile afterFileParent = makeSureParentPathExists(pieces);
      if (afterFileParent == null) {
        setErrorMessage(fileNotFoundMessage(myAfterName));
        return false;
      }
      VirtualFile beforeFile = myBaseMapper.getFile(myPatch, myBeforeName);
      if (! checkExistsAndValid(beforeFile, myBeforeName)) {
        return false;
      }
      assert beforeFile != null; // if beforeFile is null then checkExist returned false;
      myMovedFiles.put(beforeFile, new MovedFileData(afterFileParent, beforeFile, myPatch.getAfterFileName()));
      addPatch(myPatch, beforeFile);
      return true;
    }
  }

  private abstract class CheckPath {
    protected final String myBeforeName;
    protected final String myAfterName;
    protected final FilePatch myPatch;
    private String myErrorMessage;

    public CheckPath(FilePatch path) {
      myPatch = path;
      myBeforeName = path.getBeforeName();
      myAfterName = path.getAfterName();
    }

    public String getErrorMessage() {
      return myErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
      myErrorMessage = errorMessage;
    }

    public boolean canBeApplied(DelayedPrecheckContext context) {
      VirtualFile beforeFile = myBaseMapper.getFile(myPatch, myBeforeName);
      VirtualFile afterFile = myBaseMapper.getFile(myPatch, myAfterName);
      return precheck(beforeFile, afterFile, context);
    }

    protected abstract boolean precheck(VirtualFile beforeFile,
                                        VirtualFile afterFile,
                                        DelayedPrecheckContext context);
    protected abstract boolean check() throws IOException;

    protected boolean checkExistsAndValid(VirtualFile file, String name) {
      if (file == null) {
        setErrorMessage(fileNotFoundMessage(name));
        return false;
      }
      return checkModificationValid(file, name);
    }

    protected boolean checkModificationValid(VirtualFile file, String name) {
      if (myProject.getApplication().isUnitTestMode() && myIgnoreContentRootsCheck) return true;
      // security check to avoid overwriting system files with a patch
      if ((file == null) || (!inContent(file)) || (myVcsManager.getVcsRootFor(file) == null)) {
        setErrorMessage("File to patch found outside content root: " + name);
        return false;
      }
      return true;
    }

    private boolean inContent(VirtualFile file) {
      return myVcsManager.isFileInContent(file);
    }

    public FilePatch getPatch() {
      return myPatch;
    }
  }

  private void addPatch(FilePatch patch, VirtualFile file) {
    if (patch instanceof TextFilePatch textFilePatch) {
      myTextPatches.add(Pair.create(file, ApplyFilePatchFactory.create(textFilePatch)));
    } else {
      ApplyFilePatchBase<BinaryType> applyBinaryPatch =
        (ApplyFilePatchBase<BinaryType>)(
          (patch instanceof BinaryFilePatch binaryFilePatch)
            ? ApplyFilePatchFactory.create(binaryFilePatch)
            : ApplyFilePatchFactory.create((ShelvedBinaryFilePatch)patch)
        );
      myBinaryPatches.add(Pair.create(file, applyBinaryPatch));
    }
    myWritableFiles.add(file);
  }

  private static String fileNotFoundMessage(String path) {
    return VcsLocalize.cannotFindFileToPatch(path).get();
  }

  private static String fileAlreadyExists(String path) {
    return VcsLocalize.cannotApplyFileAlreadyExists(path).get();
  }

  private void revert(String errorMessage) {
    PatchApplier.showError(myProject, errorMessage, true);

    // move back
    /*for (MovedFileData movedFile : myMovedFiles) {
      try {
        final VirtualFile current = movedFile.getCurrent();
        final VirtualFile newParent = current.getParent();
        final VirtualFile file;
        if (! Comparing.equal(newParent, movedFile.getOldParent())) {
          file = moveFile(current, movedFile.getOldParent());
        } else {
          file = current;
        }
        if (! Comparing.equal(current.getName(), movedFile.getOldName())) {
          file.rename(PatchApplier.class, movedFile.getOldName());
        }
      }
      catch (IOException e) {
        // ignore: revert as much as possible
      }
    }

    // go back
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        for (int i = myCreatedDirectories.size() - 1; i >= 0; -- i) {
          final VirtualFile file = myCreatedDirectories.get(i);
          try {
            file.delete(PatchApplier.class);
          }
          catch (IOException e) {
            // ignore
          }
        }
      }
    });

    myBinaryPatches.clear();
    myTextPatches.clear();
    myWritableFiles.clear();*/
  }


  private static VirtualFile createFile(VirtualFile parent, String name) throws IOException {
    return parent.createChildData(PatchApplier.class, name);
    /*final Ref<IOException> ioExceptionRef = new Ref<IOException>();
    final Ref<VirtualFile> result = new Ref<VirtualFile>();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        try {
          result.set(parent.createChildData(PatchApplier.class, name));
        }
        catch (IOException e) {
          ioExceptionRef.set(e);
        }
      }
    });
    if (! ioExceptionRef.isNull()) {
      throw ioExceptionRef.get();
    }
    return result.get();*/
  }

  private static VirtualFile moveFile(VirtualFile file, VirtualFile newParent) throws IOException {
    file.move(FilePatch.class, newParent);
    return file;
    /*final Ref<IOException> ioExceptionRef = new Ref<IOException>();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        try {
          file.move(FilePatch.class, newParent);
        }
        catch (IOException e) {
          ioExceptionRef.set(e);
        }
      }
    });
    if (! ioExceptionRef.isNull()) {
      throw ioExceptionRef.get();
    }
    return file;*/
  }

  @Nullable
  private VirtualFile makeSureParentPathExists(String[] pieces) throws IOException {
    VirtualFile child = myBaseDirectory;

    int size = (pieces.length - 1);
    for (int i = 0; i < size; i++) {
      String piece = pieces[i];
      if (StringUtil.isEmptyOrSpaces(piece)) {
        continue;
      }
      if ("..".equals(piece)) {
        child = child.getParent();
        continue;
      }

      VirtualFile nextChild = child.findChild(piece);
      if (nextChild == null) {
        nextChild = VirtualFileUtil.createDirectories(child.getPath() + '/' + piece);
        myCreatedDirectories.add(nextChild);
      }
      child = nextChild;
    }
    return child;
  }

  public List<Pair<VirtualFile, ApplyTextFilePatch>> getTextPatches() {
    return myTextPatches;
  }

  public List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> getBinaryPatches() {
    return myBinaryPatches;
  }

  @Nonnull
  public List<VirtualFile> getWritableFiles() {
    return myWritableFiles;
  }

  @RequiredUIAccess
  public void doMoveIfNeeded(VirtualFile file) throws IOException {
    MovedFileData movedFile = myMovedFiles.get(file);
    if (movedFile != null) {
      myBeforePaths.add(VcsUtil.getFilePath(file));
      myProject.getApplication().runWriteAction((ThrowableComputable<VirtualFile, IOException>)() -> movedFile.doMove());
    }
  }

  private static class MovedFileData {
    private final VirtualFile myNewParent;
    private final VirtualFile myCurrent;
    private final String myNewName;

    private MovedFileData(@Nonnull VirtualFile newParent, @Nonnull VirtualFile current, @Nonnull String newName) {
      myNewParent = newParent;
      myCurrent = current;
      myNewName = newName;
    }

    public VirtualFile getCurrent() {
      return myCurrent;
    }

    public VirtualFile getNewParent() {
      return myNewParent;
    }

    public String getNewName() {
      return myNewName;
    }

    public VirtualFile doMove() throws IOException {
      VirtualFile oldParent = myCurrent.getParent();
      boolean needRename = !Comparing.equal(myCurrent.getName(), myNewName);
      boolean needMove = !myNewParent.equals(oldParent);
      if (needRename) {
        if (needMove) {
          File oldParentFile = VirtualFileUtil.virtualToIoFile(oldParent);
          File targetAfterRenameFile = new File(oldParentFile, myNewName);
          if (targetAfterRenameFile.exists() && myCurrent.exists()) {
            // if there is a conflict during first rename we have to rename to third name, then move, then rename to final target
            performRenameWithConflicts(oldParentFile);
            return myCurrent;
          }
        }
        myCurrent.rename(PatchApplier.class, myNewName);
      }
      if (needMove) {
        myCurrent.move(PatchApplier.class, myNewParent);
      }
      return myCurrent;
    }

    private void performRenameWithConflicts(@Nonnull File oldParent) throws IOException {
      File tmpFileWithUniqueName = FileUtil.createTempFile(oldParent, "tempFileToMove", null, false);
      File newParentFile = VirtualFileUtil.virtualToIoFile(myNewParent);
      File destFile = new File(newParentFile, tmpFileWithUniqueName.getName());
      while (destFile.exists()) {
        destFile = new File(
          newParentFile,
          FileUtil.createTempFile(oldParent, FileUtil.getNameWithoutExtension(destFile.getName()), null, false).getName()
        );
      }
      myCurrent.rename(PatchApplier.class, destFile.getName());
      myCurrent.move(PatchApplier.class, myNewParent);
      myCurrent.rename(PatchApplier.class, myNewName);
    }
  }

  public interface BaseMapper {
    @Nullable
    VirtualFile getFile(FilePatch patch, String path);
    FilePath getPath(FilePatch patch, String path);
  }

  private static class DelayedPrecheckContext {
    private final Map<FilePath, FilePatch> mySkipDeleted;
    private final Map<FilePath, FilePatch> myOverrideExisting;
    private final List<FilePath> myOverridenPaths;
    private final Project myProject;

    private DelayedPrecheckContext(Project project) {
      myProject = project;
      myOverrideExisting = new HashMap<>();
      mySkipDeleted = new HashMap<>();
      myOverridenPaths = new LinkedList<>();
    }

    public void addSkip(FilePath path, FilePatch filePatch) {
      mySkipDeleted.put(path, filePatch);
    }

    public void addOverrideExisting(FilePatch patch, FilePath filePath) {
      if (! myOverrideExisting.containsKey(filePath)) {
        myOverrideExisting.put(filePath, patch);
      }
    }

    // returns those to be skipped
    public Collection<FilePatch> doDelayed() {
      List<FilePatch> result = new LinkedList<>();
      if (! myOverrideExisting.isEmpty()) {
        String title = "Overwrite Existing Files";
        Collection<FilePath> selected = AbstractVcsHelper.getInstance(myProject).selectFilePathsToProcess(
                new ArrayList<>(myOverrideExisting.keySet()), title,
                "\nThe following files should be created by patch, but they already exist.\nDo you want to overwrite them?\n", title,
                "The following file should be created by patch, but it already exists.\nDo you want to overwrite it?\n{0}",
                VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION);
        if (selected != null) {
          for (FilePath path : selected) {
            myOverrideExisting.remove(path);
          }
        }
        result.addAll(myOverrideExisting.values());
        if (selected != null) {
          myOverridenPaths.addAll(selected);
        }
      }
      result.addAll(mySkipDeleted.values());
      return result;
    }

    public List<FilePath> getOverridenPaths() {
      return myOverridenPaths;
    }

    public Collection<FilePath> getAlreadyDeletedPaths() {
      return mySkipDeleted.keySet();
    }
  }

  public void setIgnoreContentRootsCheck(boolean ignoreContentRootsCheck) {
    myIgnoreContentRootsCheck = ignoreContentRootsCheck;
  }
}

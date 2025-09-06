/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.component.persist.scheme.ExternalInfo;
import consulo.component.persist.scheme.ExternalizableScheme;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.shelf.ShelvedBinaryFile;
import consulo.versionControlSystem.change.shelf.ShelvedChangeList;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import org.jdom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author yole
 * @since 2006-11-24
 */
public class ShelvedChangeListImpl implements ShelvedChangeList, JDOMExternalizable, ExternalizableScheme {
    private static final Logger LOG = Logger.getInstance(ShelvedChangeListImpl.class);

    private static final String NAME_ATTRIBUTE = "name";
    private static final String ATTRIBUTE_DATE = "date";
    private static final String ATTRIBUTE_RECYCLED_CHANGELIST = "recycled";
    private static final String ATTRIBUTE_TOBE_DELETED_CHANGELIST = "toDelete";
    private static final String ELEMENT_BINARY = "binary";

    private final ExternalInfo myExternalInfo = new ExternalInfo();

    public String PATH;
    public String DESCRIPTION;
    public Date DATE;
    private List<ShelvedChangeImpl> myChanges;
    private List<ShelvedBinaryFile> myBinaryFiles;
    private boolean myRecycled;
    private boolean myToDelete;
    private String mySchemeName;

    public ShelvedChangeListImpl() {
    }

    public ShelvedChangeListImpl(String path, String description, List<ShelvedBinaryFile> binaryFiles) {
        this(path, description, binaryFiles, System.currentTimeMillis());
    }

    public ShelvedChangeListImpl(String path, String description, List<ShelvedBinaryFile> binaryFiles, long time) {
        PATH = FileUtil.toSystemIndependentName(path);
        DESCRIPTION = description;
        DATE = new Date(time);
        myBinaryFiles = binaryFiles;
        mySchemeName = DESCRIPTION;
    }

    @Override
    public boolean isRecycled() {
        return myRecycled;
    }

    public void setRecycled(boolean recycled) {
        myRecycled = recycled;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
        PATH = FileUtil.toSystemIndependentName(PATH);
        mySchemeName = element.getAttributeValue(NAME_ATTRIBUTE);
        DATE = new Date(Long.parseLong(element.getAttributeValue(ATTRIBUTE_DATE)));
        myRecycled = Boolean.parseBoolean(element.getAttributeValue(ATTRIBUTE_RECYCLED_CHANGELIST));
        myToDelete = Boolean.parseBoolean(element.getAttributeValue(ATTRIBUTE_TOBE_DELETED_CHANGELIST));
        //noinspection unchecked
        List<Element> children = element.getChildren(ELEMENT_BINARY);
        myBinaryFiles = new ArrayList<>(children.size());
        for (Element child : children) {
            ShelvedBinaryFileImpl binaryFile = new ShelvedBinaryFileImpl();
            binaryFile.readExternal(child);

            myBinaryFiles.add(binaryFile);
        }
    }

    @Override
    public void writeExternal(@Nonnull Element element) throws WriteExternalException {
        writeExternal(element, this);
    }

    private static void writeExternal(@Nonnull Element element, @Nonnull ShelvedChangeListImpl shelvedChangeList) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(shelvedChangeList, element);
        element.setAttribute(NAME_ATTRIBUTE, shelvedChangeList.getName());
        element.setAttribute(ATTRIBUTE_DATE, Long.toString(shelvedChangeList.DATE.getTime()));
        element.setAttribute(ATTRIBUTE_RECYCLED_CHANGELIST, Boolean.toString(shelvedChangeList.isRecycled()));
        if (shelvedChangeList.isMarkedToDelete()) {
            element.setAttribute(ATTRIBUTE_TOBE_DELETED_CHANGELIST, Boolean.toString(shelvedChangeList.isMarkedToDelete()));
        }
        
        for (ShelvedBinaryFile file : shelvedChangeList.getBinaryFiles()) {
            Element child = new Element(ELEMENT_BINARY);
            ((ShelvedBinaryFileImpl) file).writeExternal(child);
            element.addContent(child);
        }
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    @Nonnull
    @Override
    public List<ShelvedChangeImpl> getChanges(Project project) {
        if (myChanges == null) {
            try {
                myChanges = new ArrayList<>();
                List<? extends FilePatch> list = ShelveChangesManagerImpl.loadPatchesWithoutContent(project, PATH, null);
                for (FilePatch patch : list) {
                    FileStatus status;
                    if (patch.isNewFile()) {
                        status = FileStatus.ADDED;
                    }
                    else if (patch.isDeletedFile()) {
                        status = FileStatus.DELETED;
                    }
                    else {
                        status = FileStatus.MODIFIED;
                    }
                    myChanges.add(new ShelvedChangeImpl(PATH, patch.getBeforeName(), patch.getAfterName(), status));
                }
            }
            catch (Exception e) {
                LOG.error("Failed to parse the file patch: [" + PATH + "]", e);
            }
        }
        return myChanges;
    }

    public void clearLoadedChanges() {
        myChanges = null;
    }

    @Override
    public List<? extends ShelvedBinaryFile> getBinaryFiles() {
        return myBinaryFiles;
    }

    @Nonnull
    @Override
    public String getName() {
        return mySchemeName;
    }

    @Override
    public String getPath() {
        return PATH;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Nonnull
    @Override
    public ExternalInfo getExternalInfo() {
        return myExternalInfo;
    }

    @Override
    public void setName(@Nonnull String newName) {
        mySchemeName = newName;
    }

    @Override
    public boolean isValid() {
        return new File(PATH).exists();
    }

    public void markToDelete(boolean toDeleted) {
        myToDelete = toDeleted;
    }

    @Override
    public boolean isMarkedToDelete() {
        return myToDelete;
    }

    @Override
    public Date getDate() {
        return DATE;
    }

    /**
     * Update Date while recycle or restore shelvedChangelist
     */
    public void updateDate() {
        DATE = new Date(System.currentTimeMillis());
    }
}

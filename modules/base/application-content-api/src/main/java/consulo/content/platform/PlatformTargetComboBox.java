/*
 * Copyright 2013-2026 consulo.io
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
package consulo.content.platform;

import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Chooses the {@link PlatformTarget} something lives on. A stored id whose target is gone stays selectable as a
 * "missing" entry, so opening an editor never rewrites it silently. Choosing a remote target starts connecting to it.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public final class PlatformTargetComboBox {
    private final PlatformTargetManager myManager;
    private final ComboBox<PlatformTarget> myComboBox;
    private final MutableFlatDataModel<PlatformTarget> myModel;

    @RequiredUIAccess
    public PlatformTargetComboBox(PlatformTargetManager manager) {
        myManager = manager;
        myModel = FlatDataModel.of(manager.getTargets());
        myComboBox = ComboBox.create(myModel);
        myComboBox.setRenderer((presentation, item) -> {
            PlatformTarget target = item.getValue();
            if (target != null) {
                presentation.withIcon(target.getIcon());
                presentation.append(target.getDisplayName());
            }
        });
        myComboBox.setValue(manager.getLocalTarget());
        myComboBox.addValueListener(event -> {
            PlatformTarget target = event.getValue();
            if (target != null && target.getPlatform() == null) {
                target.connect();
            }
        });
    }

    public Component getComponent() {
        return myComboBox;
    }

    /**
     * The id of the chosen target, null for the local machine.
     */
    public @Nullable String getTargetId() {
        PlatformTarget target = myComboBox.getValue();
        return target == null || target == myManager.getLocalTarget() ? null : target.getId();
    }

    @RequiredUIAccess
    public void setTargetId(@Nullable String id) {
        PlatformTarget target = myManager.findTarget(id);
        if (target == null && id != null) {
            target = findOrAddMissing(id);
        }
        myComboBox.setValue(target);
    }

    /**
     * The platform of the chosen target while it is reachable.
     */
    public @Nullable Platform getPlatform() {
        PlatformTarget target = myComboBox.getValue();
        return target == null ? null : target.getPlatform();
    }

    public void addListener(Consumer<@Nullable String> listener) {
        myComboBox.addValueListener(event -> listener.accept(getTargetId()));
    }

    @RequiredUIAccess
    private PlatformTarget findOrAddMissing(String id) {
        for (PlatformTarget target : myModel) {
            if (Comparing.equal(target.getId(), id)) {
                return target;
            }
        }
        PlatformTarget missing = new MissingTarget(id);
        myModel.add(missing);
        return missing;
    }

    private record MissingTarget(String id) implements PlatformTarget {
        @Override
        public String getId() {
            return id;
        }

        @Override
        public LocalizeValue getDisplayName() {
            return ProjectLocalize.platformTargetMissing(id);
        }

        @Override
        public @Nullable Image getIcon() {
            return null;
        }

        @Override
        public @Nullable Platform getPlatform() {
            return null;
        }

        @Override
        public CompletableFuture<Platform> connect() {
            return CompletableFuture.failedFuture(new IllegalStateException(ProjectLocalize.errorPlatformTargetMissing(id).get()));
        }
    }
}

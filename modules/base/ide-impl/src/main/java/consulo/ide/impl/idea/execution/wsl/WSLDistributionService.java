// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution.wsl;


import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.ServiceManager;
import consulo.application.util.AtomicNullableLazyValue;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * Service responsible for keeping list of distributions in the external file, available for user modifications.
 *
 * @apiNote To force IDE to store default values (as an example for users) we use empty list as default and initializing on
 * first read. Configuration available at: {@code HOME\.IntelliJIdea2018.2\config\options\wsl.distributions.xml}
 * <br/>
 * Service automatically adds default descriptors on first invocation.
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "WslDistributionsService", storages = @Storage(value = "wsl.distributions.xml"))
public class WSLDistributionService implements PersistentStateComponent<WSLDistributionService> {
  /**
   * Current service implementation version is necessary for future migrations: fields additions and so on.
   */
  private static final int CURRENT_VERSION = 1;

  /**
   * Persisted service version. Migration should be performed if differs from {@link #CURRENT_VERSION}
   */
  @Attribute("version")
  private int myVersion = 0;

  @Tag("descriptors")
  @Nonnull
  private final Set<WslDistributionDescriptor> myDescriptors = new LinkedHashSet<>();

  private static final List<WslDistributionDescriptor> DEFAULT_DESCRIPTORS =
          Arrays.asList(new WslDistributionDescriptor("DEBIAN", "Debian", "debian.exe", "Debian GNU/Linux"), new WslDistributionDescriptor("KALI", "kali-linux", "kali.exe", "Kali Linux"),
                        new WslDistributionDescriptor("OPENSUSE42", "openSUSE-42", "opensuse-42.exe", "openSUSE Leap 42"),
                        new WslDistributionDescriptor("SLES12", "SLES-12", "sles-12.exe", "SUSE Linux Enterprise Server 12"),
                        new WslDistributionDescriptor("SLES15", "SLES-15", "sles-15.exe", "SUSE Linux Enterprise Server 15"),
                        new WslDistributionDescriptor("OPENSUSE15", "openSUSE-Leap-15", "openSUSE-Leap-15.exe", "openSUSE Leap 15"),
                        new WslDistributionDescriptor("OPENSUSE15-1", "openSUSE-Leap-15-1", "openSUSE-Leap-15-1.exe", "openSUSE Leap 15.1"),
                        new WslDistributionDescriptor("UBUNTU", "Ubuntu", "ubuntu.exe", "Ubuntu"), new WslDistributionDescriptor("UBUNTU1604", "Ubuntu-16.04", "ubuntu1604.exe", "Ubuntu 16.04"),
                        new WslDistributionDescriptor("UBUNTU1804", "Ubuntu-18.04", "ubuntu1804.exe", "Ubuntu 18.04"), new WslDistributionDescriptor("WLINUX", "WLinux", "wlinux.exe", "WLinux"),
                        new WslDistributionDescriptor("PENGWIN", "Pengwin", "pengwin.exe", "Pengwin"), new WslDistributionDescriptor("PENGWIN_ENTERPRISE", "WLE", "wle.exe", "Pengwin Enterprise"),
                        new WslDistributionDescriptor("ARCH", "Arch", "Arch.exe", "Arch Linux"));

  /**
   * Atomic applier of default values: distributions and persisted version.
   * This hack is necessary, because there is no way to force our PersistentStateComponent to save default values
   * It can't be put to {@link #loadState(WSLDistributionService)} or {@link #noStateLoaded()} because of serialization implementations
   * details
   */
  private final AtomicNullableLazyValue<Boolean> myDefaultsApplier = AtomicNullableLazyValue.createValue(() -> {
    myDescriptors.addAll(DEFAULT_DESCRIPTORS);
    myVersion = CURRENT_VERSION;
    return true;
  });


  @Nonnull
  public Collection<WslDistributionDescriptor> getDescriptors() {
    myDefaultsApplier.getValue();
    return myDescriptors;
  }

  @Nullable
  @Override
  public WSLDistributionService getState() {
    return this;
  }

  /**
   * @implSpec migrations if any, should be done here, depending on {@link #myVersion} of {@code state} and {@link #CURRENT_VERSION}
   */
  @Override
  public void loadState(@Nonnull WSLDistributionService state) {
    XmlSerializerUtil.copyBean(state, this);
    myDescriptors.removeIf(it -> !it.isValid());
  }

  @Nonnull
  public static WSLDistributionService getInstance() {
    return ServiceManager.getService(WSLDistributionService.class);
  }
}

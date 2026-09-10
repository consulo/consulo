// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.dependencies;

import com.dynatrace.hash4j.hashing.HashStream64;
import com.dynatrace.hash4j.hashing.Hashing;
import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.RawFileLoader;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Fingerprint (=hash) of the current IDE installation.
 * It used to skip the scanning/indexing process altogether if nothing has changed from the previous IDE session.
 * <p>
 * The defining property of the fingerprint is:
 * 'if (fingerprint is unchanged) AND (files on disk are not changed) =&gt; it should be no reason to update indexes'.
 * I.e. fingerprint should include all the reasons to update indexes _other_ than file(s) content change.
 * <p>
 * Current algorithm to define the fingerprint (i.e. params to include/not include in the fingerprint) is not
 * fundamental, it is just a heuristical attempt to list all the things that could affect the indexing process.
 * The current list contains plugins versions, app build version, file size limits used, and so on. The list may
 * need to be extended in the future.
 */
public record IdeFingerprint(long value) {
    private static final Logger LOG = Logger.getInstance(IdeFingerprint.class);

    private static volatile @Nullable IdeFingerprint ourFingerprint;

    public static IdeFingerprint ideFingerprint() {
        return ideFingerprint(0);
    }

    public static IdeFingerprint ideFingerprint(int debugHelperToken) {
        if (debugHelperToken != 0) {
            return computeIdeFingerprint(debugHelperToken);
        }

        IdeFingerprint fingerprint = ourFingerprint;
        if (fingerprint != null) {
            return fingerprint;
        }

        synchronized (IdeFingerprint.class) {
            IdeFingerprint existing = ourFingerprint;
            if (existing != null) {
                return existing;
            }
            IdeFingerprint computed = computeIdeFingerprint(0);
            ourFingerprint = computed;
            return computed;
        }
    }

    public static IdeFingerprint fromString(String value) throws NumberFormatException {
        return new IdeFingerprint(Long.parseUnsignedLong(value, Character.MAX_RADIX));
    }

    public String asString() {
        return Long.toUnsignedString(value, Character.MAX_RADIX);
    }

    public long asLong() {
        return value;
    }

    @Override
    public String toString() {
        return asString();
    }

    private static long ideBuildStamp(ApplicationInfo appInfo) {
        CodeSource codeSource = IdeFingerprint.class.getProtectionDomain().getCodeSource();
        URL location = codeSource == null ? null : codeSource.getLocation();
        if (location != null) {
            try {
                return Files.getLastModifiedTime(Path.of(location.toURI())).to(TimeUnit.SECONDS);
            }
            catch (Exception e) {
                LOG.warn("Cannot read the platform timestamp from " + location + ", falling back to the build date");
            }
        }
        return appInfo.getBuildDate().getTimeInMillis() / 1000L;
    }

    private static IdeFingerprint computeIdeFingerprint(int debugHelperToken) {
        long startTime = System.currentTimeMillis();

        HashStream64 hasher = Hashing.xxh3_64().hashStream();

        ApplicationInfo appInfo = ApplicationInfo.getInstance();
        long buildTime = ideBuildStamp(appInfo);
        hasher.putLong(buildTime);
        hasher.putString(appInfo.getBuild().asString());

        List<PluginDescriptor> loadedPlugins = PluginManager.getPlugins()
            .stream()
            .filter(descriptor -> !PluginManager.shouldSkipPlugin(descriptor))
            .sorted(Comparator.comparing(descriptor -> descriptor.getPluginId().getIdString()))
            .toList();
        hasher.putInt(loadedPlugins.size());
        for (PluginDescriptor plugin : loadedPlugins) {
            // no need to check bundled plugins - handled by taking build time and version into account
            if (!plugin.isBundled()) {
                addPluginFingerprint(plugin, hasher);
            }
        }

        //RC: we include IDE/plugins versions AND explicit indexes versions into the fingerprint -- because index.version
        //    could change without code change, by sys-properties or other env conditions change.
        //    E.g., sharding: # shards could be changed with system properties and could change automatically if #CPU changed.
        Application.get().getExtensionPoint(FileBasedIndexExtension.class).forEach(extension -> hasher.putInt(extension.getVersion()));

        hasher.putInt(debugHelperToken);

        hasher.putInt(fileSizeLimitsFingerprint());

        IdeFingerprint fingerprint = new IdeFingerprint(hasher.getAsLong());

        long durationMs = System.currentTimeMillis() - startTime;
        LOG.info("Calculated dependencies fingerprint in " + durationMs + " ms "
            + "(hash=" + fingerprint.asString() + ", buildTime=" + buildTime + ", appVersion=" + appInfo.getBuild().asString() + ")");
        return fingerprint;
    }

    private static int fileSizeLimitsFingerprint() {
        RawFileLoader loader = RawFileLoader.getInstance();
        return List.of(loader.getMaxIntellisenseFileSize(), loader.getFileLengthToCacheThreshold(), loader.getUserContentLoadLimit())
            .hashCode();
    }

    private static void addPluginFingerprint(PluginDescriptor plugin, HashStream64 hasher) {
        hasher.putString(plugin.getPluginId().getIdString());
        String version = StringUtil.notNullize(plugin.getVersion());
        hasher.putString(version);
        if (StringUtil.containsIgnoreCase(version, "SNAPSHOT")) {
            hashByFileContent(plugin, hasher);
        }
    }

    private static void hashByFileContent(PluginDescriptor descriptor, HashStream64 hasher) {
        Path pluginPath = descriptor.getNioPath();
        if (pluginPath == null) {
            hasher.putLong(0);
            return;
        }

        try (Stream<Path> walk = Files.walk(pluginPath)) {
            // if the path is not a directory, only "this" file will be visited
            // if the path is a directory, all the regular files will be visited
            //  note that symlinks are not followed
            List<Path> files = walk.sorted().toList();
            hasher.putInt(files.size());
            for (Path file : files) {
                // /tmp/byteBuddyAgent12978532094762450051.jar:1261:2023-09-21T12:46:17.196727952Z <= this file is always different :(
                // see also https://youtrack.jetbrains.com/issue/IJPL-166
                String absolutePathString = file.toAbsolutePath().toString();
                if (!absolutePathString.startsWith("/tmp/byteBuddyAgent")) {
                    hashFile(file, hasher, absolutePathString);
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void hashFile(Path file, HashStream64 hasher, String path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        hasher.putString(path);
        hasher.putLong(attributes.size());
        hasher.putLong(attributes.lastModifiedTime().toMillis());
    }
}

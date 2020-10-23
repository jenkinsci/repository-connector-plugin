package org.jvnet.hudson.plugins.repositoryconnector.util;

import java.util.function.Function;

public class VersionFilter implements Function<String, Boolean> {

    public static final VersionFilter ALL = new VersionFilter(true, true);
    
    private final boolean releases;
    private final boolean snapshots;

    public VersionFilter(boolean releases, boolean snapshots) {
        this.releases = releases;
        this.snapshots = snapshots;
    }

    @Override
    public Boolean apply(String version) {
        if (releases && !isSnapshot(version)) {
            return true;
        }

        if (snapshots && isSnapshot(version)) {
            return true;
        }

        return false;
    }

    public static boolean isSnapshot(String version) {
        return version.endsWith("-SNAPSHOT");
    }
}

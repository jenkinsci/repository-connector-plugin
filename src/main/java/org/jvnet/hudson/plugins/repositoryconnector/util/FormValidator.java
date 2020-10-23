package org.jvnet.hudson.plugins.repositoryconnector.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.jvnet.hudson.plugins.repositoryconnector.Messages;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherException;

import hudson.Util;
import hudson.util.FormValidation;

public final class FormValidator {

    public static FormValidation validateArtifactId(String artifactId) {
        return isNotEmpty(artifactId, "ArtifactId cannot be blank");
    }

    public static FormValidation validateCoordinates(String repositoryId, String groupId, String artifactId, Aether aether,
            VersionFilter filter) {
        // do nothing, other validation will indicate these are missing
        if (Util.fixEmpty(groupId) == null || Util.fixEmpty(artifactId) == null) {
            return FormValidation.ok();
        }

        try {
            // order doesn't matter here
            if (aether.hasAvailableVersions(Util.fixEmpty(repositoryId), groupId, artifactId, filter)) {
                return FormValidation.ok("Success");
            }

            return FormValidation.warning("No versions found for %s:%s", groupId, artifactId);
        } catch (AetherException e) {
            return FormValidation.error(e.getMessage());
        }
    }

    public static FormValidation validateGroupId(String groupId) {
        return isNotEmpty(groupId, "GroupId cannot be blank");
    }

    public static FormValidation validateLocalDirectory(String localDirectory) {
        return Optional.ofNullable(Util.fixEmpty(localDirectory))
                .map(dir -> {
                    Path path = Paths.get(localDirectory);

                    if (!Files.isDirectory(path)) {
                        return FormValidation.error("Path does not represent a valid directory");
                    }

                    if (!Files.isWritable(path)) {
                        return FormValidation.error("Invalid permissions to create directory");
                    }

                    return FormValidation.ok();
                })
                .orElse(FormValidation.ok());
    }

    public static FormValidation validateReleasesAndOrSnapshots(boolean releases, boolean snapshots) {
        if (releases || snapshots) {
            return FormValidation.ok();
        }

        return FormValidation.error(Messages.ReleasesOrSnapshotsRequired());
    }

    public static FormValidation validateVersion(String version) {
        return isNotEmpty(version, "Version cannot be blank");
    }

    public static FormValidation validateVersionLimit(String limit, boolean newestFirst) {
        limit = Util.fixEmpty(limit);

        if (newestFirst && limit != null) {
            try {
                int parsed = Integer.parseInt(limit);
                if (parsed <= 0) {
                    throw new NumberFormatException();
                }
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                return FormValidation.error("Limit must be a number > 0");
            }
        }

        return FormValidation.ok();
    }

    public static FormValidation validateVersionParameterName(String name) {
        return isNotEmpty(name, "Name cannot be blank");
    }

    private static FormValidation isNotEmpty(String value, String message) {
        if (Util.fixEmpty(value) == null) {
            return FormValidation.error(message);
        }

        return FormValidation.ok();
    }
}

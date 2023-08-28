package org.jvnet.hudson.plugins.repositoryconnector.aether;

import hudson.model.InvisibleAction;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.repository.RemoteRepository;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * RecorderAction keeps track of deployed Maven artifacts and metadata files.
 * This is designed as a stapler-exportable object, so that the lists of artifacts
 * show up in API calls in the 'actions' list of a build.
 */
@ExportedBean(defaultVisibility = 999)
public class RecorderAction extends InvisibleAction {

    private final ArrayList<Artifact> artifactsDeployed;

    private final ArrayList<Metadata> metadataDeployed;

    public RecorderAction() {
        artifactsDeployed = new ArrayList<>();
        metadataDeployed = new ArrayList<>();
    }

    public void recordArtifactDeployed(RepositoryEvent event) {
        artifactsDeployed.add(new Artifact(event));
    }

    public void recordMetadataDeployed(RepositoryEvent event) {
        metadataDeployed.add(new Metadata(event));
    }

    @Exported
    public Collection<Artifact> getArtifactsDeployed() {
        return artifactsDeployed;
    }

    @Exported
    public Collection<Metadata> getMetadataDeployed() {
        return metadataDeployed;
    }

    @ExportedBean(defaultVisibility = 999)
    public static class Artifact {
        private final String repositoryId;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String baseVersion;
        private final Boolean isSnapshot;
        private final String classifier;
        private final String extension;
        private final String filePath;
        private final String fileName;
        private final String url;

        public Artifact(RepositoryEvent event) {
            repositoryId = event.getRepository().getId();
            groupId = event.getArtifact().getGroupId();
            artifactId = event.getArtifact().getArtifactId();
            version = event.getArtifact().getVersion();
            baseVersion = event.getArtifact().getBaseVersion();
            isSnapshot = event.getArtifact().isSnapshot();
            classifier = event.getArtifact().getClassifier();
            extension = event.getArtifact().getExtension();
            File file = event.getArtifact().getFile();
            if (file == null) {
                // this shouldn't happen after deploy, but be defensive.
                filePath = fileName = "";
            } else {
                filePath = file.getPath();
                fileName = file.getName();
            }
            // based on maven2 (default) repository layout: https://maven.apache.org/repository/layout.html
            String relativeUrl = groupId.replace(".", "/")
                    + "/" + artifactId
                    + "/" + version
                    + "/" + fileName;
            if (event.getRepository() instanceof RemoteRepository) {
                url = ((RemoteRepository) event.getRepository())
                        .getUrl().replaceAll("/+$", "") + "/"
                        + relativeUrl;
            } else {
                url = relativeUrl;
            }
        }

        @Exported
        public String getRepositoryId() {
            return repositoryId;
        }

        @Exported
        public String getArtifactId() {
            return artifactId;
        }

        @Exported
        public String getGroupId() {
            return groupId;
        }

        @Exported
        public String getVersion() {
            return version;
        }

        @Exported
        public String getBaseVersion() {
            return baseVersion;
        }

        @Exported
        public Boolean isSnapshot() {
            return isSnapshot;
        }

        @Exported
        public String getClassifier() {
            return classifier;
        }

        @Exported
        public String getExtension() {
            return extension;
        }

        @Exported
        public String getFilePath() {
            return filePath;
        }

        @Exported
        public String getFileName() {
            return fileName;
        }

        @Exported
        public String getUrl() {
            return url;
        }
    }

    @ExportedBean(defaultVisibility = 999)
    public static class Metadata {
        private final String repositoryId;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String nature;
        private final String type;
        private final String filePath;
        private final String fileName;
        private final String url;

        public Metadata(RepositoryEvent event) {
            repositoryId = event.getRepository().getId();
            groupId = event.getMetadata().getGroupId();
            artifactId = event.getMetadata().getArtifactId();
            version = event.getMetadata().getVersion();
            nature = event.getMetadata().getNature().toString();
            type = event.getMetadata().getType();
            File file = event.getMetadata().getFile();
            if (file == null) {
                // this shouldn't happen after deploy, but be defensive.
                filePath = fileName = "";
            } else {
                filePath = file.getPath();
                fileName = file.getName();
            }
            // based on maven2 (default) repository layout: https://maven.apache.org/repository/layout.html
            String relativeUrl = groupId.replace(".", "/")
                    + "/" + artifactId
                    + "/" + version
                    + "/" + fileName;
            if (event.getRepository() instanceof RemoteRepository) {
                url = ((RemoteRepository) event.getRepository())
                        .getUrl().replaceAll("/+$", "") + "/"
                        + relativeUrl;
            } else {
                url = relativeUrl;
            }
        }

        @Exported
        public String getRepositoryId() {
            return repositoryId;
        }

        @Exported
        public String getArtifactId() {
            return artifactId;
        }

        @Exported
        public String getGroupId() {
            return groupId;
        }

        @Exported
        public String getVersion() {
            return version;
        }

        @Exported
        public String getNature() {
            return nature;
        }

        @Exported
        public String getType() {
            return type;
        }

        @Exported
        public String getFilePath() {
            return filePath;
        }

        @Exported
        public String getFileName() {
            return fileName;
        }

        @Exported
        public String getUrl() {
            return url;
        }
    }
}

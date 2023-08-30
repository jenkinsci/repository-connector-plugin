package org.jvnet.hudson.plugins.repositoryconnector.aether;

import hudson.model.InvisibleAction;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.net.URI;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
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

    public void recordArtifactDeployed(RepositoryEvent event, RepositoryLayout repositoryLayout) {
        artifactsDeployed.add(new Artifact(event, repositoryLayout));
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

        public Artifact(RepositoryEvent event, RepositoryLayout repositoryLayout) {
            repositoryId = event.getRepository().getId();
            groupId = event.getArtifact().getGroupId();
            artifactId = event.getArtifact().getArtifactId();
            version = event.getArtifact().getVersion();
            baseVersion = event.getArtifact().getBaseVersion();
            isSnapshot = event.getArtifact().isSnapshot();
            classifier = event.getArtifact().getClassifier();
            extension = event.getArtifact().getExtension();

            String downloadPath = "";
            String downloadFileName = "";
            String downloadUrl = "";
            if (repositoryLayout != null && event.getRepository() instanceof RemoteRepository) {
                // As long as we are only recording deployments, this will always be remote.
                try {
                    URI downloadLocation = repositoryLayout.getLocation(event.getArtifact(), false);
                    URI repoUri = new URI(((RemoteRepository) event.getRepository()).getUrl());

                    downloadPath = repoUri.relativize(downloadLocation).getPath();
                    downloadFileName = Paths.get(downloadPath).getFileName().toString();
                    // Set downloadUrl last, so we can use it below to ensure all 3 download* vars are set if it is set.
                    downloadUrl = new URI(
                        downloadLocation.getScheme(),
                        null, // drop any userInfo (user:password)
                        downloadLocation.getHost(),
                        downloadLocation.getPort(),
                        downloadLocation.getPath(),
                        downloadLocation.getQuery(),
                        downloadLocation.getFragment()
                    ).toString();
                } catch (URISyntaxException ignored) {
                }
            }
            if (downloadUrl.isEmpty()) {
                // based on maven2 (default) repository layout: https://maven.apache.org/repository/layout.html
                if (classifier.isEmpty()) {
                    downloadFileName = artifactId + "-" + version + "." + extension;
                } else {
                    downloadFileName = artifactId + "-" + version + "-" + classifier + "." + extension;
                }
                downloadPath = groupId.replace(".", "/")
                        + "/" + artifactId
                        + "/" + version
                        + "/" + downloadFileName;
                // We don't have the remote url, so punt and use that path.
                downloadUrl = downloadPath;
            }
            filePath = downloadPath;
            fileName = downloadFileName;
            url = downloadUrl;
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
            File file = event.getFile();
//            File file = event.getMetadata().getFile();
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

package org.jvnet.hudson.plugins.repositoryconnector.aether;

import hudson.model.InvisibleAction;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
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

    public void recordMetadataDeployed(RepositoryEvent event, RepositoryLayout repositoryLayout) {
        metadataDeployed.add(new Metadata(event, repositoryLayout));
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
            ArtifactRepository repository = event.getRepository();
            org.eclipse.aether.artifact.Artifact artifact = event.getArtifact();

            repositoryId = repository.getId();
            groupId = artifact.getGroupId();
            artifactId = artifact.getArtifactId();
            version = artifact.getVersion();
            baseVersion = artifact.getBaseVersion();
            isSnapshot = artifact.isSnapshot();
            classifier = artifact.getClassifier();
            extension = artifact.getExtension();

            String downloadPath = "";
            String downloadFileName = "";
            String downloadUrl = "";
            if (repositoryLayout != null && repository instanceof RemoteRepository) {
                // As long as we are only recording deployments, this will always be remote.
                try {
                    // getLocation returns a URI instance that ONLY has a path (no scheme, host, ...)
                    URI downloadLocation = repositoryLayout.getLocation(artifact, false);
                    downloadPath = getPathFromUri(downloadLocation);
                    downloadFileName = getFileNameFromPath(downloadPath);

                    URI repoUri = new URI(((RemoteRepository) repository).getUrl());
                    downloadUrl = new URI(
                        repoUri.getScheme(),
                        null, // drop any userInfo (user:password)
                        repoUri.getHost(),
                        repoUri.getPort(),
                        repoUri.resolve(downloadLocation).getPath(),
                        repoUri.getQuery(),
                        repoUri.getFragment()
                    ).toString();
                } catch (URISyntaxException ignored) {
                }
            }
            if (downloadPath.isEmpty() && downloadFileName.isEmpty()) {
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
            }
            if (downloadUrl.isEmpty()) {
                // We don't have the remote url, so punt and use that path.
                downloadUrl = downloadPath;
            }
            filePath = downloadPath;
            fileName = downloadFileName;
            url = downloadUrl;
        }

        private static String getPathFromUri(URI uri) {
            String path = uri.getPath();
            return (path == null) ? "" : path;
        }
        private static String getFileNameFromPath(String path) {
            Path file = Paths.get(path).getFileName();
            return (file == null) ? "" : file.toString();
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

        public Metadata(RepositoryEvent event, RepositoryLayout repositoryLayout) {
            ArtifactRepository repository = event.getRepository();
            org.eclipse.aether.metadata.Metadata metadata = event.getMetadata();

            repositoryId = repository.getId();
            groupId = metadata.getGroupId();
            artifactId = metadata.getArtifactId();
            version = metadata.getVersion();
            nature = metadata.getNature().toString();
            type = metadata.getType();

            String downloadPath = "";
            String downloadFileName = "";
            String downloadUrl = "";
            if (repositoryLayout != null && repository instanceof RemoteRepository) {
                // As long as we are only recording deployments, this will always be remote.
                try {
                    // getLocation returns a URI instance that ONLY has a path (no scheme, host, ...)
                    URI downloadLocation = repositoryLayout.getLocation(metadata, false);
                    downloadPath = downloadLocation.getPath();
                    downloadFileName = Paths.get(downloadPath).getFileName().toString();

                    URI repoUri = new URI(((RemoteRepository) repository).getUrl());
                    downloadUrl = new URI(
                            repoUri.getScheme(),
                            null, // drop any userInfo (user:password)
                            repoUri.getHost(),
                            repoUri.getPort(),
                            repoUri.resolve(downloadLocation).getPath(),
                            repoUri.getQuery(),
                            repoUri.getFragment()
                    ).toString();
                } catch (URISyntaxException ignored) {
                }
            }
            if (downloadPath.isEmpty() && downloadFileName.isEmpty()) {
                downloadFileName = type; // "maven-metadata.xml"
                // based on maven2 (default) repository layout: https://maven.apache.org/repository/layout.html
                if (groupId.isEmpty()) {
                    downloadPath = downloadFileName;
                } else {
                    downloadPath = groupId.replace(".", "/") + "/";
                    if (!artifactId.isEmpty()) {
                        downloadPath += artifactId + "/";
                        if (!version.isEmpty()) {
                            downloadPath += version + "/";
                        }
                    }
                    downloadPath += downloadFileName;
                }
            }
            if (downloadUrl.isEmpty()) {
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

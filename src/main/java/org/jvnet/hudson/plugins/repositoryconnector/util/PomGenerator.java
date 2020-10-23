package org.jvnet.hudson.plugins.repositoryconnector.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.jvnet.hudson.plugins.repositoryconnector.Artifact;

public class PomGenerator {

    public static File generate(Artifact artifact) throws IOException {
        Model model = convertToModel(artifact);
        MavenXpp3Writer writer = new MavenXpp3Writer();

        File pomFile = createTempFile(artifact.getArtifactId(), "xml");
        writer.write(createOutputStream(pomFile), model);

        return pomFile;
    }
    
    private static File createTempFile(String prefix, String suffix) throws IOException {
        return FilePathUtils.createTempFile(prefix, suffix);
    }

    private static OutputStream createOutputStream(File pomFile) throws IOException {
        return FilePathUtils.createOutputStream(pomFile);
    }

    private static Model convertToModel(Artifact artifact) {
        Model model = new Model();

        model.setGroupId(artifact.getGroupId());
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());

        String packaging = artifact.getExtension();
        if (packaging == null) {
            String target = artifact.getTargetFileName();
            // first '.' so we include .tar.gz, etc
            int index = target.indexOf('.');

            packaging = target.substring(index + 1);
        }

        model.setPackaging(packaging);

        return model;
    }
}

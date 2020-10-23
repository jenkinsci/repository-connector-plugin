package org.jvnet.hudson.plugins.repositoryconnector.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.Artifact;

public class PomGeneratorTest {

    @Test
    public void testWithoutPackaging() throws Exception {
        Artifact artifact = createArtifact();

        File pom = createPom(artifact);
        Model model = readPom(pom);

        verifyModel(model, artifact, "jar");
    }

    @Test
    public void testWithPackaging() throws Exception {
        Artifact artifact = createArtifact();
        artifact.setExtension("pom");

        File pom = createPom(artifact);
        Model model = readPom(pom);

        verifyModel(model, artifact, "pom");
    }

    private Artifact createArtifact() {
        Artifact artifact = new Artifact("groupId", "artifactId", "1.0.0");
        artifact.setTargetFileName("target.jar");

        return artifact;
    }

    private File createPom(Artifact artifact) throws IOException {
        File pom = PomGenerator.generate(artifact);
        pom.deleteOnExit();

        return pom;
    }

    private Model readPom(File pom) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileInputStream(pom));
    }

    private void verifyModel(Model model, Artifact artifact, String packaging) {
        assertEquals(artifact.getGroupId(), model.getGroupId());
        assertEquals(artifact.getArtifactId(), model.getArtifactId());
        assertEquals(artifact.getVersion(), model.getVersion());

        assertEquals(packaging, model.getPackaging());
    }
}

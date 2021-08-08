package org.jvnet.hudson.plugins.repositoryconnector.aether;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import java.nio.file.Files;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.Artifact;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.jvnet.hudson.plugins.repositoryconnector.util.PomGenerator;
import org.jvnet.hudson.plugins.repositoryconnector.util.VersionFilter;

import hudson.model.Run;

public class AetherIT {

    private static File localRepository;

    private Aether aether;

    @Before
    public void beforeEach() {
        aether = createAether();
    }

    @Test
    @Ignore("not to be run as part of ci - connects to maven central")
    public void testHasAvailableVersions() throws AetherException {
        Artifact artifact = createResolveableArtifact();
        assertTrue(aether.hasAvailableVersions(null, artifact.getGroupId(), artifact.getArtifactId(), VersionFilter.ALL));
    }

    @Test
    public void testInstall() throws Exception {
        Collection<File> installed = aether.install(createInstallableArtifact());

        assertNotNull(installed);
        assertFalse(installed.isEmpty());
    }

    @Test
    @Ignore("not to be run as part of ci - connects to maven central")
    public void testResolveArtifact() throws AetherException {
        File resolved = aether.resolve(null, createResolveableArtifact());

        assertNotNull(resolved);
        assertTrue(resolved.exists());
    }

    @Test
    @Ignore("not to be run as part of ci - connects to maven central")
    public void testResolveWithDependencies() throws AetherException {
        Collection<File> resolved = aether.resolveWithDependencies(null, createResolveableArtifact(), "compile");

        assertNotNull(resolved);
        assertFalse(resolved.isEmpty());
    }

    private Aether createAether() {
        return new AetherBuilderFactory(null, Repository.MAVEN_CENTRAL)
                .createAetherBuilder((Run<?, ?>) null)
                .setRepositoryLogger(System.out)
                .build();
    }

    private Artifact createInstallableArtifact() throws IOException, URISyntaxException {
        Artifact artifact = new Artifact("repository-connector-test", "test", "0.1.0");
        artifact.setTargetFileName(new File(getClass().getResource("../test.jar").toURI()).getAbsolutePath());

        File pomFile = PomGenerator.generate(artifact);
        artifact.setPomFile(pomFile.getAbsolutePath());

        return artifact;
    }

    private Artifact createResolveableArtifact() {
        return new Artifact("org.junit.jupiter", "junit-jupiter", "RELEASE");
    }

    @BeforeClass
    public static void startup() throws IOException {
        localRepository = Files.createTempDirectory(null).toFile();
    }

    @AfterClass
    public static void teardown() {
        localRepository.delete();
    }
}

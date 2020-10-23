package org.jvnet.hudson.plugins.repositoryconnector.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionFilterTest {

    private static final String RELEASE = "1.0.0";

    private static final String SNAPSHOT = "1.0.0-SNAPSHOT";

    private static final String OTHER = "1.0.0-other";

    @Test
    public void testReleasesAndSnapshots() {
        VersionFilter filter = new VersionFilter(true, true);

        assertTrue(filter.apply(RELEASE));
        assertTrue(filter.apply(SNAPSHOT));
        assertTrue(filter.apply(OTHER));
    }

    @Test
    public void testReleasesOnly() {
        VersionFilter filter = new VersionFilter(true, false);

        assertTrue(filter.apply(RELEASE));
        assertFalse(filter.apply(SNAPSHOT));
        assertTrue(filter.apply(OTHER));
    }

    @Test
    public void testSnapshotsOnly() {
        VersionFilter filter = new VersionFilter(false, true);

        assertFalse(filter.apply(RELEASE));
        assertTrue(filter.apply(SNAPSHOT));
        assertFalse(filter.apply(OTHER));
    }
}

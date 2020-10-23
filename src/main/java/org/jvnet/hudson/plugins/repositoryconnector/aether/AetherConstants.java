package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.util.Arrays;

import org.eclipse.aether.repository.RepositoryPolicy;

public final class AetherConstants {

    private static final String[] CHECKSUM_POLICIES = {
            RepositoryPolicy.CHECKSUM_POLICY_FAIL,
            RepositoryPolicy.CHECKSUM_POLICY_IGNORE,
            RepositoryPolicy.CHECKSUM_POLICY_WARN
    };

    public static final String DEFAULT_CHECKSUM = RepositoryPolicy.CHECKSUM_POLICY_WARN;
    public static final String DEFAULT_UPDATE = RepositoryPolicy.UPDATE_POLICY_DAILY;

    private static final String[] UPDATE_POLICIES = {
            RepositoryPolicy.UPDATE_POLICY_ALWAYS,
            RepositoryPolicy.UPDATE_POLICY_DAILY,
            RepositoryPolicy.UPDATE_POLICY_NEVER
    };
    
    public static String[] getUpdatePolicies() {
        return copy(UPDATE_POLICIES);
    }
    
    public static String[] getChecksumPolicies() {
        return copy(CHECKSUM_POLICIES);
    }
    
    private static String[] copy(String[] array) {
        return Arrays.copyOf(array, array.length);
    }
}

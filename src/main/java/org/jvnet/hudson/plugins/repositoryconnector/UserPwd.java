package org.jvnet.hudson.plugins.repositoryconnector;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.util.Secret;

public class UserPwd {
    private final Secret user;
    private final Secret password;

    @DataBoundConstructor
    public UserPwd(String user, String password) {
        this.user = toSecret(user);
        this.password = toSecret(password);
    }

    public String getUser() {
        return Secret.toString(user);
    }

    public String getPassword() {
        return Secret.toString(password);
    }

    @Override
    public String toString() {
        return "[UserPwd: " + getUser() + "]";
    }

    public static Secret toSecret(String data) {
        return "".equals(data) ? null : Secret.fromString(data);
    }
}

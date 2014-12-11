package org.jvnet.hudson.plugins.repositoryconnector;

import org.kohsuke.stapler.DataBoundConstructor;

public class UserPwd {
    public final String user;
    public final String password;

    @DataBoundConstructor
    public UserPwd(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public String toString() {
        return "[UserPwd: " + user + "]";
    }
}

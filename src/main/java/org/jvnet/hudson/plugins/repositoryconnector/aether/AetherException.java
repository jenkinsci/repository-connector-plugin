package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.io.IOException;

public class AetherException extends IOException {

    private static final long serialVersionUID = -3988370195217810683L;

    public AetherException(String message) {
        super(message);
    }
}

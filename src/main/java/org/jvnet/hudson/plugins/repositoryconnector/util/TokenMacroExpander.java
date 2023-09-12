package org.jvnet.hudson.plugins.repositoryconnector.util;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jvnet.hudson.plugins.repositoryconnector.Artifact;

import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;

public class TokenMacroExpander {

    private final Run<?, ?> context;

    private final TaskListener listener;

    private final FilePath workspace;

    public TokenMacroExpander(Run<?, ?> context, TaskListener listener, FilePath workspace) {
        this.context = context;
        this.listener = listener;
        this.workspace = workspace;
    }

    public String expand(String macro) throws MacroEvaluationException, IOException, InterruptedException {
        return Util.fixEmpty(TokenMacro.expandAll(context, workspace, listener, macro));
    }

    public Artifact expand(Artifact artifact) throws MacroEvaluationException, IOException, InterruptedException {
        String groupId = expand(artifact.getGroupId());
        String artifactId = expand(artifact.getArtifactId());
        String version = expand(artifact.getVersion());

        Artifact expanded = new Artifact(groupId, artifactId, version);
        expanded.setClassifier(expand(artifact.getClassifier()));
        expanded.setExtension(expand(artifact.getExtension()));
        expanded.setPomFile(expand(artifact.getPomFile()));
        expanded.setTargetFileName(expand(artifact.getTargetFileName()));
        
        expanded.setFailOnError(artifact.isFailOnError());
        expanded.setDeployToLocal(artifact.isDeployToLocal());
        expanded.setDeployToRemote(artifact.isDeployToRemote());

        return expanded;
    }
}

package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.util.List;

import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;

public class ExcludeTranisitiveDependencyFilter implements DependencyFilter {

    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        if (parents.size() == 0) {
            return true;
        }
        return false;
    }

}

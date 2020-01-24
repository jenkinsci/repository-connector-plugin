package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.Extension;
import hudson.model.Descriptor;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

/**
 * This class provides the global configuration for the plugin.
 *
 * @author mrumpf
 */
@Extension
public class RepositoryConfiguration extends GlobalConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(RepositoryConfiguration.class.getName());

    private static final Map<String, Repository> DEFAULT_REPOS = new HashMap<String, Repository>();
    static {
        DEFAULT_REPOS.put("central", new Repository("central", "default", "https://repo1.maven.org/maven2", null, null, false));
    }

    private final Map<String, Repository> repos = new HashMap<String, Repository>();

    private String localRepository = "";

    public RepositoryConfiguration() {
        load();
        if (repos.isEmpty()) {
            repos.putAll(DEFAULT_REPOS);
        }
    }

    // Injecting the RepositoryConfiguration into the DescriptorImpl of the ArtifactDeployer or the ArtifactResolver did not work
    public static RepositoryConfiguration get() {
        return GlobalConfiguration.all().get(RepositoryConfiguration.class);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {

        localRepository = formData.getString("localRepository");

        repos.clear();

        final Object repoJson = formData.get("repos");
        if (repoJson instanceof JSONArray) {
            final JSONArray jsonArray = (JSONArray) repoJson;
            for (Object object : jsonArray) {
                addRepo(req, (JSONObject) object);
            }
        } else {
            // there might be only a single repo config
            addRepo(req, (JSONObject) repoJson);
        }

        if (repos.isEmpty()) {
            repos.putAll(DEFAULT_REPOS);
        }

        save();
        return true;
    }

    /**
     * adds a dynamic permission configuration with the data extracted form the jsonObject.
     * 
     */
    private void addRepo(StaplerRequest req, JSONObject jsonObject) {
        final Repository repo = req.bindJSON(Repository.class, jsonObject);
        if (repo != null) {
            if (!StringUtils.isBlank(repo.getUrl())) {
                log.fine("Adding repo " + repo);
                repos.put(repo.getId(), repo);
            }
            else {
                // TODO: Show error -> URL must not be empty
            }
        }
    }

    public String getLocalRepository() {
        return localRepository;
    }
    
    public File getLocalRepoPath() {
        String localRepositoryLocation = localRepository;
        // default to local repo within java temp dir
        if (StringUtils.isBlank(localRepositoryLocation)) {
            localRepositoryLocation = System.getProperty("java.io.tmpdir") + "/repositoryconnector-repo";
        }
        File result = new File(localRepositoryLocation);
        if (!result.exists()) {
            log.fine("create local repo directory: " + result.getAbsolutePath());
            result.mkdirs();
        }
        return result;
    }


    public Collection<Repository> getRepos() {
        List<Repository> r = new ArrayList<Repository>();
        r.addAll(repos.values());
        Collections.sort(r);
        log.fine("repos=" + r);
        return r;
    }
    public Map<String, Repository> getRepositoryMap() {
        log.fine("reposmap=" + repos);
        return repos;
    }

    public static class ValueComparator implements Comparator<String> {

        Map<String, Double> base;
        public ValueComparator(Map<String, Double> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.    
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }
}

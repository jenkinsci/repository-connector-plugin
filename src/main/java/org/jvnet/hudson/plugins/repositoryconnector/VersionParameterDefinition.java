package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.version.Version;

public class VersionParameterDefinition extends SimpleParameterDefinition {

	private static final long serialVersionUID = -147143040052020071L;

	private static final Logger log = Logger.getLogger(VersionParameterDefinition.class.getName());

    private final String groupid;
    private final String repoid;
    private final String artifactid;
    private final boolean reverseOrder;
    private final String propertyName;

    @DataBoundConstructor
    public VersionParameterDefinition(String repoid, String groupid, String artifactid, String propertyName, String description, boolean reverseOrder) {
        super(groupid + "." + artifactid, description);
        this.repoid = repoid;
        this.groupid = groupid;
        this.artifactid = artifactid;
        this.reverseOrder = reverseOrder;
        this.propertyName = propertyName;
    }

    @Override
    public VersionParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof StringParameterValue) {
            // TODO: StringParameterValue value = (StringParameterValue) defaultValue;
            return new VersionParameterDefinition(getRepoid(), getGroupid(), getArtifactid(), "", getDescription(),true);
        } else {
            return this;
        }
    }

    public boolean reverseOrder() {
    	return reverseOrder;
    }
    
    public Collection<Repository> getRepos(String repoid) {
    	List<Repository> out = new ArrayList<Repository>();
    	
    	for(Map.Entry<String,Repository> e : RepositoryConfiguration.get().getRepositoryMap().entrySet()) {
    		if("ALL".equals(repoid) || repoid.equals(e.getKey())) {
    			out.add(e.getValue());
    		}
    	}
    	return out;
    }
    
    
    @Exported
    public List<String> getChoices() {
    	Collection<Repository> repos = getRepos(repoid);

    	List<String> versionStrings = new ArrayList<String>();

    	File localRepo = RepositoryConfiguration.get().getLocalRepoPath();
        log.info("VersionParameterDefinition: local repo "+localRepo.getAbsolutePath());
        
        Aether aether = new Aether(
        		repos, localRepo, null, false, 
        		RepositoryPolicy.UPDATE_POLICY_ALWAYS, 
                RepositoryPolicy.CHECKSUM_POLICY_FAIL, 
                RepositoryPolicy.UPDATE_POLICY_ALWAYS, 
                RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        try {
            List<Version> versions = aether.resolveVersions(groupid, artifactid);
            while(versions.size()>0) {
            	if(reverseOrder) {
            		versionStrings.add(0,versions.remove(0).toString());
            	} else {
            		versionStrings.add(versions.remove(0).toString());
            	}
            }
        } catch (VersionRangeResolutionException ex) {
            log.log(Level.SEVERE, "Could not determine versions", ex);
        }
        if(versionStrings.size()==0) {
        	throw new RuntimeException("no versions found");
        }
        if (!versionStrings.isEmpty()) {
            // reverseorder to have the latest versions on top of the list
        	if(reverseOrder) {
        		Collections.reverse(versionStrings);
        	}
            // add the default parameters
        	// commented this because nexus is a bitch when it comes to dealing with 
        	// these - "latest" is always the latest according to the metadata, which
        	// doesn't really get updated. see 
        	//
        	//  http://stackoverflow.com/questions/14165784/maven-nexus-v-latest-not-working
        	//  http://www.blackpepper.co.uk/nexus-support-for-latest-version/
        	//  http://articles.javatalks.ru/articles/32
        	//
            //versionStrings.add(0, "LATEST");
            //versionStrings.add(0, "RELEASE");
        }
        return versionStrings;
    }

    @Exported
    public String getArtifactid() {
        return artifactid;
    }

    @Exported
    public String getRepoid() {
        return repoid;
    }

    @Exported
    public String getGroupid() {
        return groupid;
    }

    @Exported
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        return new VersionParameterValue(groupid, artifactid, propertyName, jo.getString("value"));
    }

    @Override
    public ParameterValue createValue(String version) {
        // this should never be called
        throw new RuntimeException("Not implemented");
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(VersionParameterDefinition.class);
            load();
        }

        public ListBoxModel doFillRepoidItems() {
        	ListBoxModel repoList = new ListBoxModel();
        	repoList.add("ALL","ALL");
        	for(String v : RepositoryConfiguration.get().getRepositoryMap().keySet()) {
        		repoList.add(v,v);
        	}
            return repoList;
        }

        public Repository getRepo(String id) {
            Repository repo = null;
            RepositoryConfiguration repoConfig = RepositoryConfiguration.get();
            if (repoConfig != null) {
                repo = repoConfig.getRepositoryMap().get(id);
                log.fine("getRepo(" + id + ")=" + repo);
            }
            return repo;
        }

        public Collection<Repository> getRepos() {
            Collection<Repository> repos = null;
            RepositoryConfiguration repoConfig = RepositoryConfiguration.get();
            if (repoConfig != null) {
                repos = repoConfig.getRepos();
                log.fine("getRepos()=" + repos);
            }
            return repos;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            if (formData.has("repo")) {
                try {
                    List l = JSONArray.toList(formData.getJSONArray("repo"), Repository.class);
                } catch (JSONException ex) {
                    Repository r = (Repository) JSONObject.toBean( formData.getJSONObject("repo"), Repository.class);
                }
            }

            save();
            return true;
        }

        public FormValidation doCheckGroupid(@QueryParameter String groupid,
                @QueryParameter String artifactid,
                @QueryParameter String repoid) throws IOException {
            FormValidation result = FormValidation.ok();
            if (groupid == null || groupid.isEmpty()) {
                result = FormValidation.error(Messages.EmptyGroupId());
            }
            return result;
        }

        public FormValidation doCheckRepoid(@QueryParameter String repoid)
                throws IOException {
            FormValidation result = FormValidation.ok();
            if (repoid == null || repoid.isEmpty()) {
                result = FormValidation.error(Messages.EmptyRepositoryName());
            }
            return result;
        }

        public FormValidation doCheckBaseurl(@QueryParameter String baseurl)
                throws IOException {
            FormValidation result = FormValidation.ok();
            if (baseurl == null || baseurl.isEmpty()) {
                result = FormValidation.error(Messages.EmptyBaseURL());
            }
            return result;
        }

        public FormValidation doCheckPassword(@QueryParameter String password,
                @QueryParameter String username) throws IOException {
            FormValidation result = FormValidation.ok();
            if (password != null && !password.isEmpty()
                    && (username == null || username.isEmpty())) {
                result = FormValidation.error(Messages.EmptyUsername());
            }
            return result;
        }

        public FormValidation doTestConnection(@QueryParameter String baseurl, @QueryParameter String username, @QueryParameter String password)
                throws IOException, ServletException {
            try {
            	return FormValidation.ok(Messages.Success());
            } catch (Exception e) {
                log.log(Level.SEVERE, "Client error: " + e.getMessage(), e);
                return FormValidation.error(Messages.ClientError() + e.getMessage());
            }
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getSimpleName());
        sb.append("@[");
        sb.append("name=");
        sb.append(getName());
        sb.append(", description=");
        sb.append(getDescription());
        sb.append(", groupid=");
        sb.append(groupid);
        sb.append(", repoid=");
        sb.append(repoid);
        sb.append(", artifactid=");
        sb.append(artifactid);
        sb.append(']');
        return sb.toString();
    }
}

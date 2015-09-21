/**
 * 
 */
package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents a repository where artifacts can be resolved from or uploaded to.
 * 
 * @author domi
 * 
 */
public class Repository implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;

    final private String url;
    final private String type;
    final private String id;
    final private String user;
    final private String password;
    final private boolean isRepositoryManager;

    @DataBoundConstructor
    public Repository(String id, String type, String url, String user, String password, boolean repositoryManager) {
        this.id = id == null ? "central" : id;
        this.type = type == null ? "default" : type;
        this.url = url;
        this.user = user;
        this.password = password;
        this.isRepositoryManager = repositoryManager;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "[Repository id=" + id + ", type=" + type + ", url=" + url + ", isRepositoryManager=" + isRepositoryManager + "]";
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Repository)) {
            return false;
        }
        Repository other = (Repository) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the isRepositoryManager
     */
    public boolean isRepositoryManager() {
        return isRepositoryManager;
    }

    public int compareTo(Object o) {
        return id.compareTo(((Repository)o).getId());
    }
}

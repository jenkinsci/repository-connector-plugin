package org.jvnet.hudson.plugins.repositoryconnector.aether;

import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.version.Version;

/**
 * An extension of {@link VersionRangeResult} with resolved latest (snapshot and
 * release) and latest release versions.
 * 
 * @author Fabrice Daugan
 */
public class VersionRangeResultWithLatest extends VersionRangeResult {

	/**
	 * The latest version (snapshot or release). May be <code>null</code> is not
	 * resolved.
	 */
	private Version latest;

	/**
	 * The released version . May be <code>null</code> is not resolved.
	 */
	private Version release;

	public VersionRangeResultWithLatest(VersionRangeRequest request) {
		super(request);
	}

	/**
	 * Return the latest version (snapshot or release). May be <code>null</code>
	 * is not resolved.
	 * 
	 * @return Latest version (snapshot or release). May be <code>null</code> is
	 *         not resolved.
	 */
	public Version getLatest() {
		return latest;
	}

	public void setLatest(Version latest) {
		this.latest = latest;
	}

	/**
	 * Return the released version . May be <code>null</code> is not resolved.
	 * 
	 * @return released version . May be <code>null</code> is not resolved.
	 */
	public Version getRelease() {
		return release;
	}

	public void setRelease(Version release) {
		this.release = release;
	}

}

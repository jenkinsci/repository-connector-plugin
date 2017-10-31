package org.jvnet.hudson.plugins.repositoryconnector;

/**
 * Version with optional label.
 * 
 * @author Fabrice Daugan
 */
public class VersionLabel {

	/**
	 * Found version.
	 */
	private final String version;

	/**
	 * Label to display.
	 */
	private final String label;

	public VersionLabel(String version, String label) {
		this.version = version;
		this.label = label;
	}

	public String getVersion() {
		return version;
	}

	public String getLabel() {
		return label;
	}
}

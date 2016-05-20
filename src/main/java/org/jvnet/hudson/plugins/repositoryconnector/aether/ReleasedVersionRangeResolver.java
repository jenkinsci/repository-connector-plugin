package org.jvnet.hudson.plugins.repositoryconnector.aether;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.aether.RepositoryEvent.EventType;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.RequestTrace;
import org.sonatype.aether.SyncContext;
import org.sonatype.aether.impl.MetadataResolver;
import org.sonatype.aether.impl.SyncContextFactory;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;
import org.sonatype.aether.util.DefaultRequestTrace;
import org.sonatype.aether.util.listener.DefaultRepositoryEvent;
import org.sonatype.aether.util.metadata.DefaultMetadata;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;
import org.sonatype.aether.version.VersionScheme;

/**
 * Simple fork of DefaultVersionRangeResolver where the released (not snapshot) versions are resolved. The dedign of forked class required a full copy/past
 * @author Benjamin Bentmann
 * @author Fabrice Daugan
 */
@Component(role = VersionRangeResolver.class)
public class ReleasedVersionRangeResolver implements VersionRangeResolver, Service {

	private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

	@Requirement
	private Logger logger = NullLogger.INSTANCE;

	@Requirement
	private MetadataResolver metadataResolver;

	@Requirement
	private SyncContextFactory syncContextFactory;

	@Override
	public void initService(ServiceLocator locator) {
		setLogger(locator.getService(Logger.class));
		setMetadataResolver(locator.getService(MetadataResolver.class));
		setSyncContextFactory(locator.getService(SyncContextFactory.class));
	}

	public ReleasedVersionRangeResolver setLogger(Logger logger) {
		this.logger = (logger != null) ? logger : NullLogger.INSTANCE;
		return this;
	}

	public ReleasedVersionRangeResolver setMetadataResolver(MetadataResolver metadataResolver) {
		if (metadataResolver == null) {
			throw new IllegalArgumentException("metadata resolver has not been specified");
		}
		this.metadataResolver = metadataResolver;
		return this;
	}

	public ReleasedVersionRangeResolver setSyncContextFactory(SyncContextFactory syncContextFactory) {
		if (syncContextFactory == null) {
			throw new IllegalArgumentException("sync context factory has not been specified");
		}
		this.syncContextFactory = syncContextFactory;
		return this;
	}

	@Override
	public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
			throws VersionRangeResolutionException {
		VersionRangeResultWithLatest result = new VersionRangeResultWithLatest(request);
		VersionScheme versionScheme = new GenericVersionScheme();
		VersionConstraint versionConstraint;
		try {
			versionConstraint = versionScheme.parseVersionConstraint(request.getArtifact().getVersion());
		} catch (InvalidVersionSpecificationException e) {
			result.addException(e);
			throw new VersionRangeResolutionException(result);
		}

		result.setVersionConstraint(versionConstraint);
		if (versionConstraint.getRanges().isEmpty()) {
			result.addVersion(versionConstraint.getVersion());
		} else {
			VersionsAndUpToDates versionsAndUpToDates = getVersions(session, result, request);
			
			// Compute the latest versions
			result.setLatest(getLatest(result, versionScheme, versionConstraint, versionsAndUpToDates.latestCandidates));
			result.setRelease(getLatest(result, versionScheme, versionConstraint, versionsAndUpToDates.releaseCandidates));

			// Build the ordered versions list
			Map<String, ArtifactRepository> versionIndex = versionsAndUpToDates.versions;
			List<Version> versions = new ArrayList<Version>();
			for (Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet()) {
				try {
					Version ver = versionScheme.parseVersion(v.getKey());
					if (versionConstraint.containsVersion(ver)) {
						versions.add(ver);
						result.setRepository(ver, v.getValue());
					}
				} catch (InvalidVersionSpecificationException e) {
					result.addException(e);
				}
			}
			Collections.sort(versions);
			result.setVersions(versions);
		}

		return result;
	}

	/**
	 * Build a parsed {@link Version} from the latest version among the given list.
	 * @param result The target container for exception resolution.
	 * @param versionScheme The version parser.
	 * @param versionConstraint the constraint to honor.
	 * @param versions the resolved version to sort.
	 * @return the latest version according the given {@link VersionScheme}
	 */
	private Version getLatest(VersionRangeResult result, VersionScheme versionScheme, VersionConstraint versionConstraint,
			Collection<String> versions) {
		List<Version> vs = new ArrayList<Version>();
		for (String v : versions) {
			try {
				Version ver = versionScheme.parseVersion(v);
				if (versionConstraint.containsVersion(ver)) {
					vs.add(ver);
				}
			} catch (InvalidVersionSpecificationException e) {
				result.addException(e);
			}
		}
		if (!versions.isEmpty()) {
			Collections.sort(vs);
			return vs.get(0);
		}
		return null;
	}
	
	/**
	 * Internal container to hold versions and resolved latest versions.
	 */
	private class VersionsAndUpToDates {
		private Map<String, ArtifactRepository> versions = new HashMap<String, ArtifactRepository>();

		/**
		 * Latest versions
		 */
		private Set<String> latestCandidates = new LinkedHashSet<String>();

		/**
		 * Latest releases
		 */
		private Set<String> releaseCandidates = new LinkedHashSet<String>();
	}

	protected VersionsAndUpToDates getVersions(RepositorySystemSession session, VersionRangeResult result,
			VersionRangeRequest request) {
		RequestTrace trace = DefaultRequestTrace.newChild(request.getTrace(), request);
		VersionsAndUpToDates versionsAndUpToDates = new VersionsAndUpToDates();
		Map<String, ArtifactRepository> versionIndex = versionsAndUpToDates.versions;
		Metadata metadata = new DefaultMetadata(request.getArtifact().getGroupId(),
				request.getArtifact().getArtifactId(), MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT);
		List<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>(request.getRepositories().size());
		metadataRequests.add(new MetadataRequest(metadata, null, request.getRequestContext()));
		for (RemoteRepository repository : request.getRepositories()) {
			MetadataRequest metadataRequest = new MetadataRequest(metadata, repository, request.getRequestContext());
			metadataRequest.setDeleteLocalCopyIfMissing(true);
			metadataRequest.setTrace(trace);
			metadataRequests.add(metadataRequest);
		}

		List<MetadataResult> metadataResults = metadataResolver.resolveMetadata(session, metadataRequests);
		WorkspaceReader workspace = session.getWorkspaceReader();
		if (workspace != null) {
			List<String> versions = workspace.findVersions(request.getArtifact());
			for (String version : versions) {
				versionIndex.put(version, workspace.getRepository());
			}
		}

		for (MetadataResult metadataResult : metadataResults) {
			result.addException(metadataResult.getException());

			ArtifactRepository repository = metadataResult.getRequest().getRepository();
			if (repository == null) {
				repository = session.getLocalRepository();
			}

			Versioning versioning = readVersions(session, trace, metadataResult.getMetadata(), repository, result);
			versioning.getRelease();
			versioning.getSnapshot();
			for (String version : versioning.getVersions()) {
				if (!versionIndex.containsKey(version)) {
					versionIndex.put(version, repository);
				}

				// Add the discovered latest version
				if (versioning.getLatest() != null && !versionsAndUpToDates.latestCandidates.contains(versioning.getLatest())) {
					versionsAndUpToDates.latestCandidates.add(versioning.getLatest());
				}
				// Add the discovered latest release version
				if (versioning.getRelease() != null && !versionsAndUpToDates.releaseCandidates.contains(versioning.getRelease())) {
					versionsAndUpToDates.releaseCandidates.add(versioning.getRelease());
				}
			}
		}

		return versionsAndUpToDates;
	}

	private Versioning readVersions(RepositorySystemSession session, RequestTrace trace, Metadata metadata,
			ArtifactRepository repository, VersionRangeResult result) {
		Versioning versioning = null;
		FileInputStream fis = null;
		try {
			if (metadata != null) {
				SyncContext syncContext = syncContextFactory.newInstance(session, true);
				try {
					syncContext.acquire(null, Collections.singleton(metadata));
					if (metadata.getFile() != null && metadata.getFile().exists()) {
						fis = new FileInputStream(metadata.getFile());
						org.apache.maven.artifact.repository.metadata.Metadata m = new MetadataXpp3Reader().read(fis,
								false);
						versioning = m.getVersioning();
					}
				} finally {
					syncContext.release();
				}
			}
		} catch (Exception e) {
			invalidMetadata(session, trace, metadata, repository, e);
			result.addException(e);
		} finally {
			IOUtil.close(fis);
		}

		return (versioning != null) ? versioning : new Versioning();
	}

	private void invalidMetadata(RepositorySystemSession session, RequestTrace trace, Metadata metadata,
			ArtifactRepository repository, Exception exception) {
		RepositoryListener listener = session.getRepositoryListener();
		if (listener != null) {
			DefaultRepositoryEvent event = new DefaultRepositoryEvent(EventType.METADATA_INVALID, session, trace);
			event.setMetadata(metadata);
			event.setException(exception);
			event.setRepository(repository);
			listener.metadataInvalid(event);
		}
	}
}

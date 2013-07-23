////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2013 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.service.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.denimgroup.threadfix.data.dao.VulnerabilityDao;
import com.denimgroup.threadfix.data.entities.DataFlowElement;
import com.denimgroup.threadfix.data.entities.Finding;
import com.denimgroup.threadfix.data.entities.GenericVulnerability;
import com.denimgroup.threadfix.data.entities.Scan;
import com.denimgroup.threadfix.data.entities.SurfaceLocation;
import com.denimgroup.threadfix.service.SanitizedLogger;

// TODO maybe move into saveOrUpdate and call it a day
// Not sure yet though because ensureCorrectRelationships might not be what we want in all cases
public class ScanCleanerUtils extends SpringBeanAutowiringSupport {
	
	@Autowired
	private VulnerabilityDao vulnerabilityDao;
	
	private ScanCleanerUtils(){}
	
	private final SanitizedLogger log = new SanitizedLogger("ScanCleanerUtils");
	
	private static final Set<String> VULNS_WITH_PARAMETERS_SET = 
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList(GenericVulnerability.VULNS_WITH_PARAMS)));
	
	public static void clean(Scan scan) {
		ScanCleanerUtils utils = new ScanCleanerUtils();
		utils.ensureCorrectRelationships(scan);
		utils.ensureSafeFieldLengths(scan);
	}
	
	/**
	 * This method ensures that Findings have the correct relationship to the
	 * other objects before being committed to the database.
	 * 
	 * It also makes sure that none of the findings have string lengths that are incompatible with their 
	 * database counterparts.
	 * 
	 * @param scan
	 */
	public void ensureCorrectRelationships(Scan scan) {
		if (scan == null) {
			log.error("The scan processing was unable to complete because the supplied scan was null.");
			return;
		}

		if (scan.getImportTime() == null)
			scan.setImportTime(Calendar.getInstance());

		if (scan.getFindings() == null) {
			log.warn("There were no findings to process.");
			return;
		}

		int numWithoutPath = 0, numWithoutParam = 0;

		// we need to set up appropriate relationships between the scan's many
		// objects.
		SurfaceLocation surfaceLocation = null;
		for (Finding finding : scan.getFindings()) {
			if (finding == null) {
				continue;
			}

			finding.setScan(scan);

			surfaceLocation = finding.getSurfaceLocation();

			if (surfaceLocation != null) {
				surfaceLocation.setFinding(finding);
				if (surfaceLocation.getParameter() == null
						&& finding.getChannelVulnerability() != null
						&& finding.getChannelVulnerability()
								.getGenericVulnerability() != null
						&& VULNS_WITH_PARAMETERS_SET.contains(finding
								.getChannelVulnerability()
								.getGenericVulnerability().getName())) {
					numWithoutParam++;
				}
				if (surfaceLocation.getPath() == null
						|| surfaceLocation.getPath().trim().equals("")) {
					numWithoutPath++;
				}
			}

			if (finding.getDataFlowElements() != null) {
				for (DataFlowElement dataFlowElement : finding
						.getDataFlowElements()) {
					if (dataFlowElement != null) {
						dataFlowElement.setFinding(finding);
					}
				}
			}

			if (finding.getVulnerability() != null) {
				if (finding.getVulnerability().getFindings() == null) {
					finding.getVulnerability().setFindings(new ArrayList<Finding>());
					finding.getVulnerability().getFindings().add(finding);
				}
				finding.getVulnerability().setApplication(
						finding.getScan().getApplication());
				if (finding.getVulnerability().getId() == null) {
					vulnerabilityDao.saveOrUpdate(finding.getVulnerability());
				}

				if ((finding.getVulnerability().getOpenTime() == null)
						|| (finding.getVulnerability().getOpenTime()
								.compareTo(scan.getImportTime()) > 0))
					finding.getVulnerability()
							.setOpenTime(scan.getImportTime());
			}
		}

		if (numWithoutParam > 0) {
			log.warn("There are " + numWithoutParam
					+ " injection-based findings missing parameters. "
					+ "This could indicate a bug in the ThreadFix parser.");
		}

		if (numWithoutPath > 0) {
			log.warn("There are "
					+ numWithoutPath
					+ " findings missing paths. "
					+ "This probably means there is a bug in the ThreadFix parser.");
		}
	}
	
	/**
	 * This method makes sure that the scan's findings don't have any database-incompatible field lengths
	 * 
	 * @param scan
	 */
	public void ensureSafeFieldLengths(Scan scan) {
		if (scan == null || scan.getFindings() == null
				|| scan.getFindings().size() == 0)
			return;

		for (Finding finding : scan.getFindings()) {
			if (finding == null)
				continue;

			finding.setLongDescription(trim(finding.getLongDescription(), Finding.LONG_DESCRIPTION_LENGTH));
			finding.setNativeId(trim(finding.getNativeId(), Finding.NATIVE_ID_LENGTH));
			finding.setSourceFileLocation(trim(finding.getSourceFileLocation(), Finding.SOURCE_FILE_LOCATION_LENGTH));
			

			if (finding.getSurfaceLocation() != null) {
				SurfaceLocation location = finding.getSurfaceLocation();
				
				location.setHost(trim(location.getHost(), SurfaceLocation.HOST_LENGTH));
				location.setParameter(trim(location.getParameter(), SurfaceLocation.PARAMETER_LENGTH));
				location.setPath(trim(location.getPath(), SurfaceLocation.PATH_LENGTH));
				location.setQuery(trim(location.getQuery(), SurfaceLocation.QUERY_LENGTH));

				finding.setSurfaceLocation(location);
			}

			if (finding.getDataFlowElements() != null
					&& finding.getDataFlowElements().size() != 0) {
				for (DataFlowElement dataFlowElement : finding.getDataFlowElements()) {
					dataFlowElement.setLineText(
							trim(dataFlowElement.getLineText(), DataFlowElement.LINE_TEXT_LENGTH));
					dataFlowElement.setSourceFileName(
							trim(dataFlowElement.getSourceFileName(), DataFlowElement.SOURCE_FILE_NAME_LENGTH));
				}
			}
		}
	}
	
	private static String trim(String inputString, int length) {
		String returnString = inputString;
		
		if (returnString != null && returnString.length() > length) {
			returnString = returnString.substring(0, length - 1);
		}
		
		return returnString;
	}
}

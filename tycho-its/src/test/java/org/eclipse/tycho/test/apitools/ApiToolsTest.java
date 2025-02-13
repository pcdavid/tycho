/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *     Hannes Wellman - add verify test case
 *******************************************************************************/
package org.eclipse.tycho.test.apitools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;

public class ApiToolsTest extends AbstractTychoIntegrationTest {
	@Test
	public void testGenerate() throws Exception {
		Verifier verifier = getVerifier("api-tools", true, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File descriptionFile = new File(verifier.getBasedir(), "bundle1/target/.api_description");
		assertTrue(descriptionFile.getAbsoluteFile() + " not found", descriptionFile.isFile());
		Document document = XMLParser.parse(descriptionFile);
		assertEquals("api-bundle-1_0.0.1-SNAPSHOT", document.getRootElement().getAttribute("name").getValue());
		// TODO enhance project and assert more useful things...
	}

	@Test
	public void testVerify() throws Exception {
		Verifier verifier = getVerifier("api-tools", true, true);
		File repo = ResourceUtil.resolveTestResource("repositories/api-tools");
		verifier.addCliOption("-DbaselineRepo=" + repo.toURI());

		assertThrows(VerificationException.class, () -> verifier.executeGoals(List.of("clean", "verify")), () -> {
			String msg = "No API errors where detected!";
			try {
				return msg + System.lineSeparator()
						+ verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false).stream()
								.collect(Collectors.joining(System.lineSeparator()));
			} catch (VerificationException e) {
				return msg;
			}
		});
		// check summary output
		verifier.verifyTextInLog("4 API ERRORS");
		verifier.verifyTextInLog("0 API warnings");
		// check error output has source references and lines
		verifier.verifyTextInLog(
				"File ApiInterface.java at line 2: The type bundle.ApiInterface has been removed from api-bundle");
		verifier.verifyTextInLog("File ClassA.java at line 5: The type bundle.ClassA has been removed from api-bundle");
		verifier.verifyTextInLog(
				"File MANIFEST.MF at line 0: The type bundle.InterfaceA has been removed from api-bundle");
		verifier.verifyTextInLog(
				"File MANIFEST.MF at line 5: The major version should be incremented in version 0.0.1, since API breakage occurred since version 0.0.1");
		// now check for the build error output
		verifier.verifyTextInLog("on project api-bundle-1: There are API errors:");
		verifier.verifyTextInLog(
				"src/bundle/ApiInterface.java:2 The type bundle.ApiInterface has been removed from api-bundle");
		verifier.verifyTextInLog(
				"src/bundle/ClassA.java:5 The type bundle.ClassA has been removed from api-bundle-1_0.0.1");
		verifier.verifyTextInLog("META-INF/MANIFEST.MF:0 The type bundle.InterfaceA has been removed from api-bundle");
		verifier.verifyTextInLog(
				"META-INF/MANIFEST.MF:5 The major version should be incremented in version 0.0.1, since API breakage occurred since version 0.0.1");
		// TODO: check with api-filter
		// TODO: check with second plugin with BREE?
	}
}

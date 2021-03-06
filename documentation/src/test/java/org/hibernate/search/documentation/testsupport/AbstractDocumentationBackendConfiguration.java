/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

abstract class AbstractDocumentationBackendConfiguration implements BackendConfiguration {

	@Override
	public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setupWithName(C setupContext,
			String backendName, TestConfigurationProvider configurationProvider) {
		return setupContext
				.withBackendProperties(
						backendName,
						configurationProvider.interpolateProperties( getBackendProperties() )
				);
	}

	protected abstract Map<String, Object> getBackendProperties();

}

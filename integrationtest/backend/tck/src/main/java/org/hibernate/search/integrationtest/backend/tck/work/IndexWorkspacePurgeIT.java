/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;

public class IndexWorkspacePurgeIT extends AbstractIndexWorkspaceSimpleOperationIT {
	@Override
	protected boolean operationWillFailIfAppliedToDeletedIndex() {
		return true;
	}

	@Override
	protected CompletableFuture<?> executeAsync(IndexWorkspace workspace) {
		return workspace.purge( Collections.emptySet() );
	}

	@Override
	protected void afterInitData(StubMappingIndexManager indexManager) {
		// Make sure to flush the index, otherwise the test won't fail as expected with Lucene,
		// probably because the index writer optimizes purges when changes are not committed yet.
		indexManager.createWorkspace().flush();
	}

	@Override
	protected void assertPreconditions(StubMappingIndexManager indexManager) {
		indexManager.createWorkspace().refresh().join();
		long count = indexManager.createScope().query().where( f -> f.matchAll() )
				.fetchTotalHitCount();
		assertThat( count ).isGreaterThan( 0 );
	}

	@Override
	protected void assertSuccess(StubMappingIndexManager indexManager) {
		indexManager.createWorkspace().refresh().join();
		long count = indexManager.createScope().query().where( f -> f.matchAll() )
				.fetchTotalHitCount();
		assertThat( count ).isEqualTo( 0 );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.backend.index.spi.StreamIndexWorker;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.impl.LuceneBackendImplementor;
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.StubLuceneIndexWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Guillaume Smet
 */
// TODO in the end the IndexManager won't implement ReaderProvider as it's far more complex than that
public class LuceneLocalDirectoryIndexManager implements LuceneIndexManager, ReaderProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneBackendImplementor backend;
	private final String indexName;
	private final LuceneIndexModel model;
	private final LuceneWorkFactory workFactory;
	private final LuceneIndexWorkOrchestrator changesetOrchestrator;
	private final LuceneIndexWorkOrchestrator streamOrchestrator;
	private final IndexWriter indexWriter;

	public LuceneLocalDirectoryIndexManager(LuceneBackendImplementor backend, String indexName, LuceneIndexModel model, IndexWriter indexWriter) {
		this.backend = backend;
		this.indexName = indexName;
		this.model = model;
		this.workFactory = backend.getWorkFactory();
		this.changesetOrchestrator = new StubLuceneIndexWorkOrchestrator( indexWriter );
		this.streamOrchestrator = new StubLuceneIndexWorkOrchestrator( indexWriter );
		this.indexWriter = indexWriter;
	}

	@Override
	public String getName() {
		return indexName;
	}

	@Override
	public LuceneIndexModel getModel() {
		return model;
	}

	@Override
	public ChangesetIndexWorker<LuceneRootDocumentBuilder> createWorker(SessionContext sessionContext) {
		backend.getMultiTenancyStrategy().checkTenantId( backend, sessionContext.getTenantIdentifier() );

		return new LuceneChangesetIndexWorker( workFactory, changesetOrchestrator, indexName, backend.getMultiTenancyStrategy(), sessionContext );
	}

	@Override
	public StreamIndexWorker<LuceneRootDocumentBuilder> createStreamWorker(SessionContext sessionContext) {
		backend.getMultiTenancyStrategy().checkTenantId( backend, sessionContext.getTenantIdentifier() );

		return new LuceneStreamIndexWorker( workFactory, streamOrchestrator, indexName, backend.getMultiTenancyStrategy(), sessionContext );
	}

	@Override
	public IndexSearchTargetBuilder createSearchTarget() {
		return new LuceneIndexSearchTargetBuilder( backend, this );
	}

	@Override
	public void addToSearchTarget(IndexSearchTargetBuilder searchTargetBuilder) {
		if ( ! (searchTargetBuilder instanceof LuceneIndexSearchTargetBuilder ) ) {
			throw log.cannotMixLuceneSearchTargetWithOtherType( searchTargetBuilder, this );
		}

		LuceneIndexSearchTargetBuilder luceneSearchTargetBuilder = (LuceneIndexSearchTargetBuilder) searchTargetBuilder;
		luceneSearchTargetBuilder.add( backend, this );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( indexName )
				.append( "]")
				.toString();
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( indexWriter::close );
			closer.push( changesetOrchestrator::close );
			closer.push( streamOrchestrator::close );
		}
		catch (IOException | RuntimeException e) {
			throw new SearchException( "Failed to shut down the Lucene index manager", e );
		}
	}

	@Override
	public ReaderProvider getReaderProvider() {
		return this;
	}

	@Override
	public IndexReader openIndexReader() {
		try {
			return DirectoryReader.open( indexWriter );
		}
		catch (IOException e) {
			throw log.unableToCreateIndexReader( backend, indexName, e );
		}
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			log.unableToCloseIndexReader( backend, indexName, e );
		}
	}
}
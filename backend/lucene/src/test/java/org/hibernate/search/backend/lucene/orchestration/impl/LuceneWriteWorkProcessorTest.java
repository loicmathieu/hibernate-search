/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWorkExecutionContext;
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.engine.common.spi.ContextualErrorHandler;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.test.FutureAssert;
import org.hibernate.search.util.impl.test.powermock.PowerMockSupport;

import org.junit.Test;

import org.apache.lucene.index.IndexWriter;
import org.easymock.Capture;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({ IndexWriter.class }) // To intercept commit() calls
public class LuceneWriteWorkProcessorTest extends PowerMockSupport {

	private static final String INDEX_NAME = "SomeIndexName";

	private EventContext indexEventContext = EventContexts.fromIndexName( INDEX_NAME );
	private IndexWriter indexWriterMock = PowerMock.createStrictMock( IndexWriter.class );
	private ErrorHandler errorHandlerMock = createStrictMock( ErrorHandler.class );
	private ContextualErrorHandler contextualErrorHandlerMock = createMock( ContextualErrorHandler.class );

	private LuceneWriteWorkProcessor processor =
			new LuceneWriteWorkProcessor( indexEventContext, indexWriterMock, errorHandlerMock );

	@Test
	public void simple() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 4, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true );
		testSuccessfulWorkSet( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, true );
		testSuccessfulWorkSet( 1, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );

		resetAll();
		// There was no commit in the last workset, there must be one here
		expect( indexWriterMock.commit() ).andReturn( 1L );
		replayAll();
		processor.endBatch();
		verifyAll();

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet( 3, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 4, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE, true );
		testSuccessfulWorkSet( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 5, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE, true );
		testSuccessfulWorkSet( 1, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, false );
		testSuccessfulWorkSet( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE, true );

		resetAll();
		// The last workset triggered a commit: no need for a commit here
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_workExecute_commitNone() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a workset where one work fails
		RuntimeException workException = new RuntimeException( "Some message" );
		testWorkSetBeginning( 2, DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
		testFailingWork( workException );
		testSkippedWorks( 4 );
		testWorkSetEndingAfterWorkFailure( workException );

		// Subsequent worksets must be executed regardless of previous failures in the same batch
		testSuccessfulWorkSet(
				3,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// A work may have failed, but there were still successful changes, before and after the failure: these must be committed
		resetAll();
		expect( indexWriterMock.commit() ).andReturn( 1L );
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	/**
	 * Test that there is no workset commit after a failure, even if the commit strategy is FORCE.
	 */
	@Test
	public void error_workExecute_commitForce() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute a workset where one work fails
		RuntimeException workException = new RuntimeException( "Some message" );
		testWorkSetBeginning( 3, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE );
		testFailingWork( workException );
		testSkippedWorks( 5 );
		testWorkSetEndingAfterWorkFailure( workException );

		// Subsequent worksets must be executed regardless of previous failures in the same batch
		testSuccessfulWorkSet(
				3,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// A work may have failed, but there still were successful changes, before and after the failure: these must be committed
		resetAll();
		expect( indexWriterMock.commit() ).andReturn( 1L );
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_workSetCommit() throws IOException {
		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		testSuccessfulWorkSet(
				4,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Start a new workset
		testWorkSetBeginning(
				6,
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);

		// Fail upon workset commit

		RuntimeException commitException = new RuntimeException( "Some message" );

		Capture<Throwable> exceptionCapture = Capture.newInstance();
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		Object workSetResult = new Object();
		resetAll();
		expect( indexWriterMock.commit() ).andThrow( commitException );
		expect( errorHandlerMock.createContextualHandler() ).andReturn( contextualErrorHandlerMock );
		contextualErrorHandlerMock.addThrowable( capture( exceptionCapture ) );
		contextualErrorHandlerMock.handle();
		// We don't expect any commit when a workset fails
		replayAll();
		processor.afterWorkSet( workSetFuture, workSetResult );
		verifyAll();

		assertThat( exceptionCapture.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to commit" )
				.hasMessageContaining( INDEX_NAME )
				.hasCause( commitException );

		FutureAssert.assertThat( workSetFuture ).isFailed( exceptionCapture.getValue() );

		resetAll();
		replayAll();
		processor.endBatch();
		verifyAll();
	}

	@Test
	public void error_batchCommit() throws IOException {
		RuntimeException commitException = new RuntimeException( "Some message" );

		resetAll();
		replayAll();
		processor.beginBatch();
		verifyAll();

		// Execute worksets
		testSuccessfulWorkSet(
				4,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);
		testSuccessfulWorkSet(
				6,
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
				false
		);

		// Fail upon batch commit

		Capture<String> stringCapture = Capture.newInstance();
		Capture<Throwable> exceptionCapture = Capture.newInstance();
		resetAll();
		expect( indexWriterMock.commit() ).andThrow( commitException );
		errorHandlerMock.handleException( capture( stringCapture ), capture( exceptionCapture ) );
		replayAll();
		processor.endBatch();
		verifyAll();

		assertThat( exceptionCapture.getValue() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unable to commit" )
				.hasMessageContaining( INDEX_NAME )
				.hasCause( commitException );

		assertThat( stringCapture.getValue() )
				.isEqualTo( exceptionCapture.getValue().getMessage() );

	}

	private void testSuccessfulWorkSet(int workCount,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			boolean expectCommit) throws IOException {
		testWorkSetBeginning( workCount, commitStrategy, refreshStrategy );

		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		Object workSetResult = new Object();
		resetAll();
		if ( expectCommit ) {
			expect( indexWriterMock.commit() ).andReturn( 1L );
		}
		replayAll();
		processor.afterWorkSet( workSetFuture, workSetResult );
		verifyAll();
		FutureAssert.assertThat( workSetFuture ).isSuccessful( workSetResult );
	}

	private void testFailingWork(RuntimeException workException) throws IOException {
		Capture<LuceneWriteWorkExecutionContext> contextCapture = Capture.newInstance();

		LuceneWriteWork<Object> failingWork = work( "failing" );
		Object failingWorkInfo = new Object();

		resetAll();
		expect( failingWork.execute( capture( contextCapture ) ) ).andThrow( workException );
		expect( failingWork.getInfo() ).andReturn( failingWorkInfo );
		expect( errorHandlerMock.createContextualHandler() ).andReturn( contextualErrorHandlerMock );
		contextualErrorHandlerMock.markAsFailed( failingWorkInfo, workException );
		replayAll();
		assertThat( processor.submit( failingWork ) ).isNull();
		verifyAll();
		testContext( contextCapture.getValue() );
	}

	private void testWorkSetBeginning(int workCount,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) throws IOException {
		Capture<LuceneWriteWorkExecutionContext> contextCapture = Capture.newInstance();

		resetAll();
		replayAll();
		processor.beforeWorkSet( commitStrategy, refreshStrategy );
		verifyAll();

		for ( int i = 0; i < workCount; ++i ) {
			LuceneWriteWork<Object> work = work( "begin_" + i );
			Object workResult = new Object();

			resetAll();
			expect( work.execute( capture( contextCapture ) ) ).andReturn( workResult );
			replayAll();
			assertThat( processor.submit( work ) ).isEqualTo( workResult );
			verifyAll();
			testContext( contextCapture.getValue() );
		}
	}

	private void testSkippedWorks(int workCount) throws IOException {
		for ( int i = 0; i < workCount; ++i ) {
			LuceneWriteWork<Object> work = work( "skipped_" + i );
			Object workInfo = new Object();

			resetAll();
			expect( work.getInfo() ).andReturn( workInfo );
			contextualErrorHandlerMock.markAsSkipped( workInfo );
			replayAll();
			assertThat( processor.submit( work ) ).isNull();
			verifyAll();
		}
	}

	private void testWorkSetEndingAfterWorkFailure(Throwable workException) throws IOException {
		CompletableFuture<Object> workSetFuture = new CompletableFuture<>();
		Object workSetResult = new Object();
		resetAll();
		contextualErrorHandlerMock.handle();
		// We don't expect any commit when a workset fails
		replayAll();
		processor.afterWorkSet( workSetFuture, workSetResult );
		verifyAll();
		FutureAssert.assertThat( workSetFuture ).isFailed( workException );
	}

	private void testContext(LuceneWriteWorkExecutionContext context) throws IOException {
		assertThat( context )
				.returns( indexWriterMock, LuceneWriteWorkExecutionContext::getIndexWriter );
	}

	private <T> LuceneWriteWork<T> work(String name) {
		return createStrictMock( "work_" + name, LuceneWriteWork.class );
	}

}
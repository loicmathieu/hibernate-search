/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <E> The encoded type.
 * @param <C> The codec type.
 * @see LuceneStandardFieldCodec
 */
public abstract class AbstractLuceneStandardMatchPredicateBuilder<F, E, C extends LuceneStandardFieldCodec<F, E>>
		extends AbstractLuceneSearchNestedPredicateBuilder
		implements MatchPredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	protected final String absoluteFieldPath;

	private final DslConverter<?, ? extends F> converter;
	private final DslConverter<F, ? extends F> rawConverter;
	private final LuceneCompatibilityChecker converterChecker;
	protected final C codec;

	protected E value;

	protected AbstractLuceneStandardMatchPredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			LuceneCompatibilityChecker converterChecker, C codec) {
		super( nestedPathHierarchy );
		this.searchContext = searchContext;
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.converterChecker = converterChecker;
		this.codec = codec;
	}

	@Override
	public void fuzzy(int maxEditDistance, int exactPrefixLength) {
		throw log.textPredicatesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public void analyzer(String analyzerName) {
		throw log.textPredicatesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public void skipAnalysis() {
		throw log.textPredicatesNotSupportedByFieldType( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public void value(Object value, ValueConvert convert) {
		DslConverter<?, ? extends F> dslToIndexConverter = getDslToIndexConverter( convert );
		try {
			F converted = dslToIndexConverter.convertUnknown( value, searchContext.getToDocumentFieldValueConvertContext() );
			this.value = codec.encode( converted );
		}
		catch (RuntimeException e) {
			throw log.cannotConvertDslParameter(
					e.getMessage(), e, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
			);
		}
	}

	private DslConverter<?, ? extends F> getDslToIndexConverter(ValueConvert convert) {
		switch ( convert ) {
			case NO:
				return rawConverter;
			case YES:
			default:
				converterChecker.failIfNotCompatible();
				return converter;
		}
	}
}

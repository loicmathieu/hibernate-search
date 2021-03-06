/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

import org.apache.lucene.analysis.Analyzer;

public final class LuceneTextFieldPredicateBuilderFactory<F>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, LuceneTextFieldCodec<F>> {

	private final Analyzer analyzerOrNormalizer;

	public LuceneTextFieldPredicateBuilderFactory(boolean searchable,
			DslConverter<?, ? extends F> converter, DslConverter<F, ? extends F> rawConverter,
			LuceneTextFieldCodec<F> codec, Analyzer analyzerOrNormalizer) {
		super( searchable, converter, rawConverter, codec );
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	@Override
	public LuceneTextMatchPredicateBuilder<?> createMatchPredicateBuilder(LuceneSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			LuceneCompatibilityChecker converterChecker, LuceneCompatibilityChecker analyzerChecker) {
		checkSearchable( absoluteFieldPath );
		return new LuceneTextMatchPredicateBuilder<>(
				searchContext, absoluteFieldPath, nestedPathHierarchy, converter, rawConverter, converterChecker, codec, analyzerOrNormalizer, analyzerChecker
		);
	}

	@Override
	public LuceneTextRangePredicateBuilder<?> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy, LuceneCompatibilityChecker converterChecker) {
		checkSearchable( absoluteFieldPath );
		return new LuceneTextRangePredicateBuilder<>( searchContext, absoluteFieldPath, nestedPathHierarchy, converter, rawConverter, converterChecker, codec );
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(LuceneSearchContext searchContext,
			String absoluteFieldPath, List<String> nestedPathHierarchy, LuceneCompatibilityChecker analyzerChecker) {
		checkSearchable( absoluteFieldPath );
		return new LuceneTextPhrasePredicateBuilder( searchContext, absoluteFieldPath, nestedPathHierarchy, codec, analyzerOrNormalizer, analyzerChecker );
	}

	@Override
	public WildcardPredicateBuilder<LuceneSearchPredicateBuilder> createWildcardPredicateBuilder(
			String absoluteFieldPath, List<String> nestedPathHierarchy) {
		checkSearchable( absoluteFieldPath );
		return new LuceneTextWildcardPredicateBuilder( absoluteFieldPath, nestedPathHierarchy, analyzerOrNormalizer );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldContext(
			String absoluteFieldPath) {
		checkSearchable( absoluteFieldPath );
		return new LuceneSimpleQueryStringPredicateBuilderFieldState(
				analyzerOrNormalizer
		);
	}

	@Override
	public boolean hasCompatibleAnalyzer(LuceneFieldPredicateBuilderFactory other) {
		if ( !( other instanceof LuceneTextFieldPredicateBuilderFactory ) ) {
			return false;
		}

		LuceneTextFieldPredicateBuilderFactory castedOther = (LuceneTextFieldPredicateBuilderFactory) other;
		return Objects.equals( analyzerOrNormalizer, castedOther.analyzerOrNormalizer );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStringFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

class LuceneStringMatchPredicateBuilder extends AbstractLuceneMatchPredicateBuilder<String> {

	private final LuceneStringFieldCodec codec;

	private final QueryBuilder queryBuilder;

	LuceneStringMatchPredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends String> converter,
			LuceneStringFieldCodec codec,
			QueryBuilder queryBuilder) {
		super( searchContext, absoluteFieldPath, converter );
		this.codec = codec;
		this.queryBuilder = queryBuilder;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		if ( queryBuilder != null ) {
			Query analyzed = queryBuilder.createBooleanQuery( absoluteFieldPath, value );
			if ( analyzed == null ) {
				// Either the value was an empty string
				// or the analysis removed all tokens (that can happen if the value contained only stopwords, for example)
				// In any case, use the same behavior as Elasticsearch: don't match anything
				analyzed = new MatchNoDocsQuery( "No tokens after analysis of the value to match" );
			}
			return analyzed;
		}
		else {
			// we are in the case where we a have a normalizer here as the analyzer case has already been treated by
			// the queryBuilder case above

			return new TermQuery( new Term( absoluteFieldPath, codec.normalize( absoluteFieldPath, value ) ) );
		}
	}
}
/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.formatter.impl;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldFormatter;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerUtils;

public final class StringFieldFormatter implements LuceneFieldFormatter<String> {

	private final Analyzer analyzerOrNormalizer;

	public StringFieldFormatter(Analyzer analyzerOrNormalizer) {
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	@Override
	public String format(Object value) {
		return (String) value;
	}

	public String normalize(String absoluteFieldPath, String value) {
		if ( value == null ) {
			return null;
		}

		if ( analyzerOrNormalizer == null ) {
			return value;
		}

		return AnalyzerUtils.analyzeSortableValue( analyzerOrNormalizer, absoluteFieldPath, value );
	}
}
/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldMoreStep;
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class WildcardPredicateFieldMoreStepImpl<B>
		implements WildcardPredicateFieldMoreStep, AbstractBooleanMultiFieldPredicateCommonState.FieldSetState<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<WildcardPredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	WildcardPredicateFieldMoreStepImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.wildcard( absoluteFieldPath ) );
		}
	}

	@Override
	public WildcardPredicateFieldMoreStep orFields(String... absoluteFieldPaths) {
		return new WildcardPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public WildcardPredicateFieldMoreStep boostedTo(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public WildcardPredicateOptionsStep matching(String wildcard) {
		return commonState.matching( wildcard );
	}

	@Override
	public List<String> getAbsoluteFieldPaths() {
		return absoluteFieldPaths;
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( WildcardPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<B>, B, WildcardPredicateFieldMoreStepImpl<B>>
			implements WildcardPredicateOptionsStep {

		CommonState(SearchPredicateBuilderFactory<?, B> builderFactory) {
			super( builderFactory );
		}

		private WildcardPredicateOptionsStep matching(String wildcardPattern) {
			if ( wildcardPattern == null ) {
				throw log.wildcardPredicateCannotMatchNullPattern( getEventContext() );
			}
			for ( WildcardPredicateFieldMoreStepImpl<B> fieldSetState : getFieldSetStates() ) {
				for ( WildcardPredicateBuilder<B> predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.pattern( wildcardPattern );
				}
			}
			return this;
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}

}
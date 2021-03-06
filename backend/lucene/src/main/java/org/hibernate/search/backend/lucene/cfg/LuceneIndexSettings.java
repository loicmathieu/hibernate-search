/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cfg;

import org.hibernate.search.backend.lucene.lowlevel.index.IOStrategyName;

/**
 * Configuration properties for Lucene indexes.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key;
 * see {@link org.hibernate.search.engine.cfg.IndexSettings} for details.
 */
public final class LuceneIndexSettings {

	private LuceneIndexSettings() {
	}

	/**
	 * The prefix for I/O-related property keys.
	 */
	public static final String IO_PREFIX = "io.";

	/**
	 * The I/O strategy, deciding how indexes are written to and read from.
	 * <p>
	 * Expects a {@link IOStrategyName} value, or a String representation of such value.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#IO_STRATEGY}.
	 */
	public static final String IO_STRATEGY = IO_PREFIX + IORadicals.STRATEGY;

	/**
	 * How much time may pass after an index change until the change is committed.
	 * <p>
	 * Only available for the "near-real-time" I/O strategy.
	 * <p>
	 * This effectively defines how long changes may be in an "unsafe" state,
	 * where a crash or power loss will result in data loss. For example:
	 * <ul>
	 *   <li>if set to 0, changes are safe as soon as the background process finishes treating a batch of changes.</li>
	 *   <li>if set to 1000, changes may not be safe for an additional 1 second
	 *   after the background process finishes treating a batch.
	 * 	 There is a benefit, though: index changes occurring less than 1 second after another change may execute faster.</li>
	 * </ul>
	 * <p>
	 * Note that individual write operations may trigger a forced commit
	 * (for example with the "committed" and "searchable" automatic indexing synchronization strategies in the ORM mapper),
	 * in which case you will only benefit from a non-zero commit interval during intensive indexing (mass indexer, ...).
	 * <p>
	 * Note that saving is <strong>not</strong> necessary to make changes visible to search queries:
	 * the two concepts are unrelated. See {@link #IO_REFRESH_INTERVAL}.
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 1000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#IO_COMMIT_INTERVAL}.
	 */
	public static final String IO_COMMIT_INTERVAL = IO_PREFIX + IORadicals.COMMIT_INTERVAL;

	/**
	 * How much time may pass after an index write
	 * until the index reader is considered stale and re-created.
	 * <p>
	 * Only available for the "near-real-time" I/O strategy.
	 * <p>
	 * This effectively defines how out-of-date search query results may be. For example:
	 * <ul>
	 *   <li>If set to 0, search results will always be completely in sync with the index writes.</li>
	 *   <li>If set to 1000, search results may reflect the state of the index at most 1 second ago.
	 * 	 There is a benefit, though: in situations where the index is being frequently written to,
	 * 	 search queries executed less than 1 second after another query may execute faster.</li>
	 * </ul>
	 * <p>
	 * Note that individual write operations may trigger a forced refresh
	 * (for example with the "searchable" automatic indexing synchronization strategy in the ORM mapper),
	 * in which case you will only benefit from a non-zero refresh interval during intensive indexing (mass indexer, ...).
	 * <p>
	 * Expects a positive Integer value in milliseconds, such as {@code 1000},
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#IO_REFRESH_INTERVAL}.
	 */
	public static final String IO_REFRESH_INTERVAL = IO_PREFIX + IORadicals.REFRESH_INTERVAL;

	/**
	 * The prefix for sharding-related property keys.
	 */
	public static final String SHARDING_PREFIX = "sharding.";

	/**
	 * The sharding strategy, deciding the number of shards, their identifiers,
	 * and how to translate a routing key into a shard identifier.
	 * <p>
	 * Expects a String, such as "hash".
	 * See the reference documentation for a list of available values.
	 * <p>
	 * Defaults to {@link LuceneIndexSettings.Defaults#SHARDING_STRATEGY} (no sharding).
	 */
	public static final String SHARDING_STRATEGY = SHARDING_PREFIX + ShardingRadicals.STRATEGY;

	/**
	 * The number of shards to create for the index,
	 * i.e. the number of "physical" indexes, each holding a part of the index data.
	 * <p>
	 * Only available for the "hash" sharding strategy.
	 * <p>
	 * Expects a strictly positive Integer value, such as 4,
	 * or a String that can be parsed into such Integer value.
	 * <p>
	 * No default: this property must be set when using the "hash" sharding strategy.
	 */
	public static final String SHARDING_NUMBER_OF_SHARDS = SHARDING_PREFIX + ShardingRadicals.NUMBER_OF_SHARDS;

	/**
	 * The list of shard identifiers to accept for the index.
	 * <p>
	 * Only available for the "explicit" sharding strategy.
	 * <p>
	 * Expects either a String containing multiple shard identifiers separated by commas (','),
	 * or a {@code Collection<String>} containing such shard identifiers.
	 * <p>
	 * No default: this property must be set when using the "explicit" sharding strategy.
	 */
	public static final String SHARDING_SHARD_IDENTIFIERS = SHARDING_PREFIX + ShardingRadicals.SHARD_IDENTIFIERS;

	/**
	 * The prefix for indexing-related property keys.
	 */
	public static final String INDEXING_PREFIX = "indexing.";

	/**
	 * The number of indexing queues assigned to each index (or each shard of each index, when sharding is enabled).
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed to such integer value.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_QUEUE_COUNT}.
	 * <p>
	 * See the reference documentation, section "Lucene backend - Indexing",
	 * for more information about this setting and its implications.
	 */
	public static final String INDEXING_QUEUE_COUNT = INDEXING_PREFIX + IndexingRadicals.QUEUE_COUNT;

	/**
	 * The size of indexing queues.
	 * <p>
	 * Expects a strictly positive integer value,
	 * or a string that can be parsed to such integer value.
	 * <p>
	 * Defaults to {@link Defaults#INDEXING_QUEUE_SIZE}.
	 * <p>
	 * See the reference documentation, section "Lucene backend - Indexing",
	 * for more information about this setting and its implications.
	 */
	public static final String INDEXING_QUEUE_SIZE = INDEXING_PREFIX + IndexingRadicals.QUEUE_SIZE;

	/**
	 * Configuration property keys for I/O, without the {@link #IO_PREFIX prefix}.
	 */
	public static final class IORadicals {

		private IORadicals() {
		}

		public static final String STRATEGY = "strategy";
		public static final String COMMIT_INTERVAL = "commit_interval";
		public static final String REFRESH_INTERVAL = "refresh_interval";
	}

	/**
	 * Configuration property keys for sharding, without the {@link #SHARDING_PREFIX prefix}.
	 */
	public static final class ShardingRadicals {

		private ShardingRadicals() {
		}

		public static final String STRATEGY = "strategy";
		public static final String NUMBER_OF_SHARDS = "number_of_shards";
		public static final String SHARD_IDENTIFIERS = "shard_identifiers";
	}

	/**
	 * Configuration property keys for indexing, without the {@link #INDEXING_PREFIX prefix}.
	 */
	public static final class IndexingRadicals {

		private IndexingRadicals() {
		}

		public static final String QUEUE_COUNT = "queue_count";
		public static final String QUEUE_SIZE = "queue_size";
	}

	/**
	 * Default values for the different settings if no values are given.
	 */
	public static final class Defaults {

		private Defaults() {
		}

		public static final String SHARDING_STRATEGY = "none";
		public static final IOStrategyName IO_STRATEGY = IOStrategyName.NEAR_REAL_TIME;
		public static final int IO_COMMIT_INTERVAL = 1000;
		public static final int IO_REFRESH_INTERVAL = 0;
		public static final int INDEXING_QUEUE_COUNT = 10;
		public static final int INDEXING_QUEUE_SIZE = 1000;
	}
}

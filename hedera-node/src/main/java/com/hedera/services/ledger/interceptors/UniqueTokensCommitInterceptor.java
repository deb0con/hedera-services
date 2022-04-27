package com.hedera.services.ledger.interceptors;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2022 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.hedera.services.context.SideEffectsTracker;
import com.hedera.services.ledger.CommitInterceptor;
import com.hedera.services.ledger.EntityChangeSet;
import com.hedera.services.ledger.properties.NftProperty;
import com.hedera.services.state.merkle.MerkleUniqueToken;
import com.hedera.services.store.models.NftId;

/**
 * Placeholder for upcoming work.
 */
public class UniqueTokensCommitInterceptor implements CommitInterceptor<NftId, MerkleUniqueToken, NftProperty> {
	public UniqueTokensCommitInterceptor(final SideEffectsTracker sideEffectsTracker) {
		// No-op
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void preview(final EntityChangeSet<NftId, MerkleUniqueToken, NftProperty> pendingChanges) {
		// No-op
	}
}
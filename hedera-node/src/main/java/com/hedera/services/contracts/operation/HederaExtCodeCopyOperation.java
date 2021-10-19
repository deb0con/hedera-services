package com.hedera.services.contracts.operation;

/*
 * -
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 */

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.operation.ExtCodeCopyOperation;

import javax.inject.Inject;

import static org.hyperledger.besu.evm.internal.Words.clampedToLong;

/**
 * Hedera adapted version of the {@link ExtCodeCopyOperation}.
 *
 * Performs an existence check on the requested {@link Address}
 * Halts the execution of the EVM transaction with {@link HederaExceptionalHaltReason#INVALID_SOLIDITY_ADDRESS} if
 * the account does not exist or it is deleted.
 *
 */
public class HederaExtCodeCopyOperation extends ExtCodeCopyOperation {

	@Inject
	public HederaExtCodeCopyOperation(GasCalculator gasCalculator) {
		super(gasCalculator);
	}

	@Override
	public OperationResult execute(MessageFrame frame, EVM evm) {
		final long memOffset = clampedToLong(frame.getStackItem(1));
		final long numBytes = clampedToLong(frame.getStackItem(3));

		return HederaOperationUtil.addressCheckExecution(
				frame,
				() -> frame.getStackItem(0),
				() -> cost(frame, memOffset, numBytes, true),
				() -> super.execute(frame, evm)
		);
	}
}
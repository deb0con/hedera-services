package com.hedera.services.bdd.suites.file;

/*-
 * ‌
 * Hedera Services Test Clients
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.infrastructure.meta.ContractResources;
import com.hedera.services.bdd.spec.keys.KeyShape;
import com.hedera.services.bdd.suites.HapiApiSuite;
import com.swirlds.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.keys.KeyShape.SIMPLE;
import static com.hedera.services.bdd.spec.keys.KeyShape.listOf;
import static com.hedera.services.bdd.spec.keys.KeyShape.threshOf;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getContractBytecode;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileInfo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCall;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileDelete;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.systemFileDelete;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.newKeyNamed;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.updateLargeFile;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;

/**
 * Client that creates a state with at least one of every type of blob; and a bit of diversity within type.
 *
 * It then writes a JSON metadata file that {@link DiverseStateValidation} can use to validate this state
 * is as expected (e.g. after a migration).
 */
public final class DiverseStateCreation extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(DiverseStateCreation.class);

	private static byte[] SMALL_CONTENTS;
	private static byte[] MEDIUM_CONTENTS;
	private static byte[] LARGE_CONTENTS;

	public static final long SOMETIME = 1_635_780_626L;
	public static final long FUSE_EXPIRY_TIME = 1_111_111L + SOMETIME;
	public static final long MULTI_EXPIRY_TIME = 2_222_222L + SOMETIME;
	public static final long SMALL_EXPIRY_TIME = 1_000_000L + SOMETIME;
	public static final long MEDIUM_EXPIRY_TIME = 10_000_000L + SOMETIME;
	public static final long LARGE_EXPIRY_TIME = 100_000_000L + SOMETIME;

	public static final Integer EXPECTED_LUCKY_NO = 1326;

	public static final String SMALL_FILE = "smallFile";
	public static final String MEDIUM_FILE = "mediumFile";
	public static final String LARGE_FILE = "largeFile";
	public static final String FUSE_INITCODE = "fuseInitcode";
	public static final String FUSE_BYTECODE = "fuseBytecode";
	public static final String MULTI_INITCODE = "multiInitcode";
	public static final String FUSE_CONTRACT = "fuseContract";
	public static final String MULTI_CONTRACT = "multiContract";

	private final Map<String, Long> entityNums = new HashMap<>();
	private final Map<String, String> keyReprs = new HashMap<>();
	private final Map<String, String> hexedBytecode = new HashMap<>();
	public static final String ENTITY_NUM_KEY = "entityNums";
	public static final String KEY_REPRS_KEY = "keyReprs";
	public static final String HEXED_BYTECODE_KEY = "hexedBytecode";

	public static final String SMALL_CONTENTS_LOC = "src/main/resource/testfiles/small.txt";
	public static final String MEDIUM_CONTENTS_LOC = "src/main/resource/testfiles/medium.txt";
	public static final String LARGE_CONTENTS_LOC = "src/main/resource/testfiles/large.txt";
	public static final String STATE_META_JSON_LOC = "src/main/resource/testfiles/diverseBlobsInfo.json";

	public static void main(String... args) throws IOException {
		new DiverseStateCreation().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		try {
			SMALL_CONTENTS = Files.newInputStream(Paths.get(SMALL_CONTENTS_LOC)).readAllBytes();
			MEDIUM_CONTENTS = Files.newInputStream(Paths.get(MEDIUM_CONTENTS_LOC)).readAllBytes();
			LARGE_CONTENTS = Files.newInputStream(Paths.get(LARGE_CONTENTS_LOC)).readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return List.of(
				createDiverseState()
		);
	}

	private HapiApiSpec createDiverseState() {
		final KeyShape SMALL_SHAPE = listOf(
				threshOf(1, 3));
		final KeyShape MEDIUM_SHAPE = listOf(
				SIMPLE,
				threshOf(2, 3));
		final KeyShape LARGE_SHAPE = listOf(
				SIMPLE,
				threshOf(1, listOf(SIMPLE, threshOf(1, 2), SIMPLE)),
				threshOf(2, threshOf(1, SIMPLE, listOf(SIMPLE, SIMPLE)), SIMPLE));
		final var smallKey = "smallKey";
		final var mediumKey = "mediumKey";
		final var largeKey = "largeKey";

		final var fuseInitcode = "fuseInitcode";
		final var multiInitcode = "multiInitcode";
		final var fuseContract = "fuseContract";
		final var multiContract = "multiContract";

		return defaultHapiSpec("CreateDiverseState").given(
				newKeyNamed(smallKey).shape(SMALL_SHAPE),
				newKeyNamed(mediumKey).shape(MEDIUM_SHAPE),
				newKeyNamed(largeKey).shape(LARGE_SHAPE)
		).when(
				/* Create some well-known files */
				fileCreate(SMALL_FILE)
						.contents(SMALL_CONTENTS)
						.key(smallKey)
						.expiry(SMALL_EXPIRY_TIME)
						.exposingNumTo(num -> entityNums.put(SMALL_FILE, num)),
				fileCreate(MEDIUM_FILE)
						.contents("")
						.key(mediumKey)
						.expiry(MEDIUM_EXPIRY_TIME)
						.exposingNumTo(num -> entityNums.put(MEDIUM_FILE, num)),
				updateLargeFile(
						GENESIS,
						MEDIUM_FILE, ByteString.copyFrom(MEDIUM_CONTENTS),
						false, OptionalLong.of(ONE_HBAR)),
				fileDelete(MEDIUM_FILE),
				fileCreate(LARGE_FILE)
						.contents("")
						.key(largeKey)
						.expiry(LARGE_EXPIRY_TIME)
						.exposingNumTo(num -> entityNums.put(LARGE_FILE, num)),
				updateLargeFile(
						GENESIS,
						LARGE_FILE, ByteString.copyFrom(LARGE_CONTENTS),
						false, OptionalLong.of(ONE_HBAR)),
				/* Create some bytecode files */
				fileCreate(fuseInitcode)
						.expiry(FUSE_EXPIRY_TIME)
						.path(ContractResources.FUSE_BYTECODE_PATH)
						.exposingNumTo(num -> entityNums.put(FUSE_INITCODE, num)),
				fileCreate(multiInitcode)
						.expiry(MULTI_EXPIRY_TIME)
						.path(ContractResources.MULTIPURPOSE_BYTECODE_PATH)
						.exposingNumTo(num -> entityNums.put(MULTI_INITCODE, num)),
				contractCreate(fuseContract)
						.bytecode(fuseInitcode)
						.exposingNumTo(num -> entityNums.put(FUSE_CONTRACT, num)),
				contractCreate(multiContract)
						.bytecode(multiInitcode)
						.exposingNumTo(num -> entityNums.put(MULTI_CONTRACT, num)),
				contractCall(multiContract, ContractResources.BELIEVE_IN_ABI, EXPECTED_LUCKY_NO)
		).then(
				systemFileDelete(fuseInitcode).payingWith(GENESIS),
				systemFileDelete(multiInitcode).payingWith(GENESIS),
				getFileInfo(SMALL_FILE)
						.exposingKeyReprTo(repr -> keyReprs.put(SMALL_FILE, repr)),
				getFileInfo(MEDIUM_FILE)
						.exposingKeyReprTo(repr -> keyReprs.put(MEDIUM_FILE, repr)),
				getFileInfo(LARGE_FILE)
						.exposingKeyReprTo(repr -> keyReprs.put(LARGE_FILE, repr)),
				getContractBytecode(FUSE_CONTRACT)
						.exposingBytecodeTo(code -> hexedBytecode.put(FUSE_BYTECODE, CommonUtils.hex(code))),
				withOpContext((spec, opLog) -> {
					final var toSerialize = Map.of(
							ENTITY_NUM_KEY, entityNums,
							KEY_REPRS_KEY, keyReprs,
							HEXED_BYTECODE_KEY, hexedBytecode);
					final var om = new ObjectMapper();
					om.writeValue(Files.newOutputStream(Paths.get(STATE_META_JSON_LOC)), toSerialize);
				})
		);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
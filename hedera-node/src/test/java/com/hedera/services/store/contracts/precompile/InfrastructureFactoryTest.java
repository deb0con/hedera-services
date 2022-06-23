package com.hedera.services.store.contracts.precompile;

import com.hedera.services.context.SideEffectsTracker;
import com.hedera.services.context.properties.GlobalDynamicProperties;
import com.hedera.services.ledger.SigImpactHistorian;
import com.hedera.services.ledger.TransactionalLedger;
import com.hedera.services.ledger.TransferLogic;
import com.hedera.services.ledger.backing.BackingStore;
import com.hedera.services.ledger.ids.EntityIdSource;
import com.hedera.services.ledger.properties.AccountProperty;
import com.hedera.services.ledger.properties.NftProperty;
import com.hedera.services.ledger.properties.TokenRelProperty;
import com.hedera.services.records.RecordsHistorian;
import com.hedera.services.state.merkle.MerkleAccount;
import com.hedera.services.state.merkle.MerkleToken;
import com.hedera.services.state.merkle.MerkleTokenRelStatus;
import com.hedera.services.state.merkle.MerkleUniqueToken;
import com.hedera.services.store.AccountStore;
import com.hedera.services.store.TypedTokenStore;
import com.hedera.services.store.contracts.HederaStackedWorldStateUpdater;
import com.hedera.services.store.contracts.precompile.codec.DecodingFacade;
import com.hedera.services.store.contracts.precompile.codec.EncodingFacade;
import com.hedera.services.store.contracts.precompile.proxy.RedirectGasCalculator;
import com.hedera.services.store.contracts.precompile.proxy.RedirectViewExecutor;
import com.hedera.services.store.models.NftId;
import com.hedera.services.store.tokens.HederaTokenStore;
import com.hedera.services.txns.crypto.ApproveAllowanceLogic;
import com.hedera.services.txns.crypto.DeleteAllowanceLogic;
import com.hedera.services.txns.crypto.validators.ApproveAllowanceChecks;
import com.hedera.services.txns.crypto.validators.DeleteAllowanceChecks;
import com.hedera.services.txns.token.AssociateLogic;
import com.hedera.services.txns.token.BurnLogic;
import com.hedera.services.txns.token.CreateLogic;
import com.hedera.services.txns.token.DissociateLogic;
import com.hedera.services.txns.token.MintLogic;
import com.hedera.services.txns.token.process.DissociationFactory;
import com.hedera.services.txns.token.validators.CreateChecks;
import com.hedera.services.txns.validation.OptionValidator;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.TokenID;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InfrastructureFactoryTest {
	@Mock
	private EntityIdSource ids;
	@Mock
	private EncodingFacade encoder;
	@Mock
	private DecodingFacade decoder;
	@Mock
	private OptionValidator validator;
	@Mock
	private RecordsHistorian recordsHistorian;
	@Mock
	private SigImpactHistorian sigImpactHistorian;
	@Mock
	private DissociationFactory dissociationFactory;
	@Mock
	private GlobalDynamicProperties dynamicProperties;
	@Mock
	private TransactionalLedger<NftId, NftProperty, MerkleUniqueToken> nftsLedger;
	@Mock
	private TransactionalLedger<Pair<AccountID, TokenID>, TokenRelProperty, MerkleTokenRelStatus> tokenRelsLedger;
	@Mock
	private TransactionalLedger<AccountID, AccountProperty, MerkleAccount> accounts;
	@Mock
	private BackingStore<TokenID, MerkleToken> tokens;
	@Mock
	private BackingStore<NftId, MerkleUniqueToken> uniqueTokens;
	@Mock
	private BackingStore<Pair<AccountID, TokenID>, MerkleTokenRelStatus> tokenRels;
	@Mock
	private MessageFrame frame;
	@Mock
	private RedirectGasCalculator gasCalculator;
	@Mock
	private HederaStackedWorldStateUpdater worldStateUpdater;

	private InfrastructureFactory subject;

	@BeforeEach
	void setUp() {
		subject = new InfrastructureFactory(
				ids,
				encoder, decoder, validator,
				recordsHistorian, sigImpactHistorian, dissociationFactory, dynamicProperties);
	}

	@Test
	void canCreateSideEffects() {
		assertInstanceOf(SideEffectsTracker.class, subject.newSideEffects());
	}

	@Test
	void canCreateAccountStore() {
		assertInstanceOf(AccountStore.class, subject.newAccountStore(accounts));
	}

	@Test
	void canCreateNewTokenStore() {
		assertInstanceOf(TypedTokenStore.class, subject.newTokenStore(
				subject.newAccountStore(accounts), subject.newSideEffects(), tokens, uniqueTokens, tokenRels));
	}

	@Test
	void canCreateNewHederaTokenStore() {
		assertInstanceOf(HederaTokenStore.class, subject.newHederaTokenStore(
				subject.newSideEffects(), tokens, nftsLedger, tokenRelsLedger));
	}

	@Test
	void canCreateNewBurnLogic() {
		final var accountStore = subject.newAccountStore(accounts);
		assertInstanceOf(BurnLogic.class, subject.newBurnLogic(
				accountStore,
				subject.newTokenStore(accountStore, subject.newSideEffects(), tokens, uniqueTokens, tokenRels)));
	}

	@Test
	void canCreateNewMintLogic() {
		final var accountStore = subject.newAccountStore(accounts);
		assertInstanceOf(MintLogic.class, subject.newMintLogic(
				accountStore,
				subject.newTokenStore(accountStore, subject.newSideEffects(), tokens, uniqueTokens, tokenRels)));
	}

	@Test
	void canCreateNewAssociateLogic() {
		final var accountStore = subject.newAccountStore(accounts);
		assertInstanceOf(AssociateLogic.class, subject.newAssociateLogic(
				accountStore,
				subject.newTokenStore(accountStore, subject.newSideEffects(), tokens, uniqueTokens, tokenRels)));
	}

	@Test
	void canCreateNewDissociateLogic() {
		final var accountStore = subject.newAccountStore(accounts);
		assertInstanceOf(DissociateLogic.class, subject.newDissociateLogic(
				accountStore,
				subject.newTokenStore(accountStore, subject.newSideEffects(), tokens, uniqueTokens, tokenRels)));
	}

	@Test
	void canCreateNewTokenCreateLogic( ) {
		final var accountStore = subject.newAccountStore(accounts);
		assertInstanceOf(CreateLogic.class, subject.newTokenCreateLogic(
				accountStore,
				subject.newTokenStore(accountStore, subject.newSideEffects(), tokens, uniqueTokens, tokenRels)));
	}

	@Test
	void canCreateNewTransferLogic() {
		final var sideEffects = subject.newSideEffects();
		assertInstanceOf(TransferLogic.class, subject.newTransferLogic(
				subject.newHederaTokenStore(sideEffects, tokens, nftsLedger, tokenRelsLedger),
				sideEffects, nftsLedger, accounts, tokenRelsLedger));
	}

	@Test
	void canCreateNewRedirectExecutor() {
		given(frame.getWorldUpdater()).willReturn(worldStateUpdater);

		assertInstanceOf(RedirectViewExecutor.class, subject.newRedirectExecutor(Bytes.EMPTY, frame, gasCalculator));
	}

	@Test
	void canCreateNewApproveAllowanceLogic() {
		final var accountStore = subject.newAccountStore(accounts);
		final var tokenStore = subject.newTokenStore(accountStore, subject.newSideEffects(), tokens,
				uniqueTokens, tokenRels);
		assertInstanceOf(ApproveAllowanceLogic.class, subject.newApproveAllowanceLogic(accountStore, tokenStore));
	}

	@Test
	void canCreateNewDeleteAllowanceLogic() {
		final var accountStore = subject.newAccountStore(accounts);
		final var tokenStore = subject.newTokenStore(accountStore, subject.newSideEffects(), tokens,
				uniqueTokens, tokenRels);
		assertInstanceOf(DeleteAllowanceLogic.class, subject.newDeleteAllowanceLogic(accountStore, tokenStore));
	}

	@Test
	void canCreateNewCreateChecks() {
		assertInstanceOf(CreateChecks.class, subject.newCreateChecks());
	}

	@Test
	void canCreateNewApproveAllowanceChecks() {
		assertInstanceOf(ApproveAllowanceChecks.class, subject.newApproveAllowanceChecks());
	}

	@Test
	void canCreateNewDeleteAllowanceChecks() {
		assertInstanceOf(DeleteAllowanceChecks.class, subject.newDeleteAllowanceChecks());
	}
}
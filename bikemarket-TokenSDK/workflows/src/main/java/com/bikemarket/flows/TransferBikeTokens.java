package com.bikemarket.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokensHandler;
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken;
import com.bikemarket.states.FrameTokenState;
import com.bikemarket.states.WheelsTokenState;
import kotlin.Unit;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

public class TransferBikeTokens {
    public TransferBikeTokens() {
    }
    @InitiatingFlow
    @StartableByRPC
    public static class TransferBikeToken extends FlowLogic<String> {

        private final String frameModel;
        private final String wheelsModel;
        private final Party holder;

        public TransferBikeToken(String frameModel, String wheelsModel, Party holder) {
            this.frameModel = frameModel;
            this.wheelsModel = wheelsModel;
            this.holder = holder;
        }

        @Suspendable
        @Override
        public String call() throws FlowException {
            //Step 1: Frame Token
            //get frame states on ledger
            StateAndRef<FrameTokenState> frameStateAndRef = getServiceHub().getVaultService().
                    queryBy(FrameTokenState.class).getStates().stream()
                    .filter(sf -> sf.getState().getData().getModelNum().equals(this.frameModel)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("StockState symbol=\"" + this.frameModel + "\" not found from vault"));

            //get the TokenType object
            FrameTokenState frametokentype = frameStateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer frametokenPointer = frametokentype.toPointer(frametokentype.getClass());

            PartyAndToken partyAndFrameToken = new PartyAndToken(holder, frametokenPointer);

            SignedTransaction stx1 = (SignedTransaction) subFlow(new MoveNonFungibleTokens(partyAndFrameToken));

            //Step 2: Wheels Token
            StateAndRef<WheelsTokenState> wheelStateStateAndRef = getServiceHub().getVaultService().
                    queryBy(WheelsTokenState.class).getStates().stream().filter(sf -> sf.getState().getData().getModelNum().equals(this.wheelsModel)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("StockState symbol=\"" + this.wheelsModel + "\" not found from vault"));

            //get the TokenType object
            WheelsTokenState wheeltokentype = wheelStateStateAndRef.getState().getData();

            //get the pointer pointer to the house
            TokenPointer wheeltokenPointer = wheeltokentype.toPointer(wheeltokentype.getClass());

            PartyAndToken partyAndWheelToken = new PartyAndToken(holder, wheeltokenPointer);

            SignedTransaction stx2 = (SignedTransaction) subFlow(new MoveNonFungibleTokens(partyAndWheelToken));

            return "\nTransfer ownership of a bike (Frame serial#: "+ this.frameModel + ", Wheels serial#: " + this.wheelsModel + ") to "
                    + this.holder.getName().getOrganisation() + "\nTransaction IDs: "
                    + stx1.getId() + ", " + stx2.getId();
        }
    }

    @InitiatedBy(TransferBikeToken.class)
    public static class MoveNonFungibleHouseTokenFlowResponder extends FlowLogic<Unit> {

        private FlowSession counterSession;

        public MoveNonFungibleHouseTokenFlowResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveNonFungibleTokensHandler(counterSession));
        }
    }
}
// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.0;
pragma experimental ABIEncoderV2;

import "./interfaces/IPtcVerifier.sol";

contract CommitteePtcVerifier is IPtcVerifier {

    function verifyTpBta(PTCVerifyAnchor calldata va, TpBta calldata tpBta) external override returns (bool) {
        return true;
    }

    function verifyTpProof(TpBta memory tpBta, ThirdPartyProof memory tpProof) external override returns (bool) {
        return true;
    }

    function myPtcType() external pure override returns (PTCTypeEnum) {
        return PTCTypeEnum.COMMITTEE;
    }
}
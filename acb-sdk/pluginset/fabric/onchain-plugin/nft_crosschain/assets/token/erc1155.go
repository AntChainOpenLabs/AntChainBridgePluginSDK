package token

import (
	"github.com/hyperledger/fabric-chaincode-go/shim"
	"github.com/hyperledger/fabric-protos-go/peer"
)

type TransferSingleEvent struct {
	Operator []byte `json:"operator"`
	From     []byte `json:"from"`
	To       []byte `json:"to"`
	Id       []byte `json:"id"`
	Amount   []byte `json:"amount"`
}

type TransferBatchEvent struct {
	Operator []byte   `json:"operator"`
	From     []byte   `json:"from"`
	To       []byte   `json:"to"`
	Ids      [][]byte `json:"ids"`
	Amounts  [][]byte `json:"amounts"`
}

type ERC1155 interface {
	/**
	 * @dev Returns the amount of tokens of token type `id` owned by `account`.
	 *
	 * Requirements:
	 *
	 * - `account` cannot be the zero address.
	 */
	balanceOf(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response

	/**
	 * @dev xref:ROOT:erc1155.adoc#batch-operations[Batched] version of {balanceOf}.
	 *
	 * Requirements:
	 *
	 * - `accounts` and `ids` must have the same length.
	 */
	balanceOfBatch(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response

	/**
	 * @dev Grants or revokes permission to `operator` to transfer the caller's tokens, according to `approved`,
	 *
	 * Emits an {ApprovalForAll} event.
	 *
	 * Requirements:
	 *
	 * - `operator` cannot be the caller.
	 */
	setApprovalForAll(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response

	/**
	 * @dev Returns true if `operator` is approved to transfer ``account``'s tokens.
	 *
	 * See {setApprovalForAll}.
	 */
	isApprovedForAll(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response

	/**
	 * @dev Transfers `amount` tokens of token type `id` from `from` to `to`.
	 *
	 * Emits a {TransferSingle} event.
	 *
	 * Requirements:
	 *
	 * - `to` cannot be the zero address.
	 * - If the caller is not `from`, it must have been approved to spend ``from``'s tokens via {setApprovalForAll}.
	 * - `from` must have a balance of tokens of type `id` of at least `amount`.
	 * - If `to` refers to a smart contract, it must implement {ERC1155Receiver-onERC1155Received} and return the
	 * acceptance magic value.
	 */
	safeTransferFrom(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response

	/**
	 * @dev xref:ROOT:erc1155.adoc#batch-operations[Batched] version of {safeTransferFrom}.
	 *
	 * Emits a {TransferBatch} event.
	 *
	 * Requirements:
	 *
	 * - `ids` and `amounts` must have the same length.
	 * - If `to` refers to a smart contract, it must implement {ERC1155Receiver-onERC1155BatchReceived} and return the
	 * acceptance magic value.
	 */
	safeBatchTransferFrom(stub shim.ChaincodeStubInterface, args [][]byte) peer.Response
}

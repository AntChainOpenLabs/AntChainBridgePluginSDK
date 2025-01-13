package wrapstub

import (
	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/hyperledger/fabric-chaincode-go/shim"
	"github.com/hyperledger/fabric-chaincode-go/shimtest"
	pb "github.com/hyperledger/fabric-protos-go/peer"
)

type MockWrapStub struct {
	stub             *shimtest.MockStub
	cacheState       map[string][]byte
	cachePrivateData map[string][]byte
}

func NewMockWrapStub(stub *shimtest.MockStub) shim.ChaincodeStubInterface {
	return &MockWrapStub{
		stub,
		make(map[string][]byte),
		make(map[string][]byte),
	}
}

func (s *MockWrapStub) GetArgs() [][]byte {
	return s.stub.GetArgs()
}

// func (s *MockWrapStub) GetStringArgs returns the arguments intended for the chaincode Init and
// Invoke as a string array. Only use func (s *MockWrapStub) GetStringArgs if the client passes
// arguments intended to be used as strings.
func (s *MockWrapStub) GetStringArgs() []string {
	return s.stub.GetStringArgs()
}

// func (s *MockWrapStub) GetFunctionAndParameters returns the first argument as the function
// name and the rest of the arguments as parameters in a string array.
// Only use func (s *MockWrapStub) GetFunctionAndParameters if the client passes arguments intended
// to be used as strings.
func (s *MockWrapStub) GetFunctionAndParameters() (string, []string) {
	return s.stub.GetFunctionAndParameters()
}

// func (s *MockWrapStub) GetArgsSlice returns the arguments intended for the chaincode Init and
// Invoke as a byte array
func (s *MockWrapStub) GetArgsSlice() ([]byte, error) {
	return s.stub.GetArgsSlice()
}

// func (s *MockWrapStub) GetTxID returns the tx_id of the transaction proposal, which is unique per
// transaction and per client. See ChannelHeader in protos/common/common.proto
// for further details.
func (s *MockWrapStub) GetTxID() string {
	return s.stub.GetTxID()
}

// func (s *MockWrapStub) GetChannelID returns the channel the proposal is sent to for chaincode to process.
// This would be the channel_id of the transaction proposal (see ChannelHeader
// in protos/common/common.proto) except where the chaincode is calling another on
// a different channel
func (s *MockWrapStub) GetChannelID() string {
	return s.stub.GetChannelID()
}

// InvokeChaincode locally calls the specified chaincode `Invoke` using the
// same transaction context; that is, chaincode calling chaincode doesn't
// create a new transaction message.
// If the called chaincode is on the same channel, it simply adds the called
// chaincode read set and write set to the calling transaction.
// If the called chaincode is on a different channel,
// only the Response is returned to the calling chaincode; any PutState calls
// from the called chaincode will not have any effect on the ledger; that is,
// the called chaincode on a different channel will not have its read set
// and write set applied to the transaction. Only the calling chaincode's
// read set and write set will be applied to the transaction. Effectively
// the called chaincode on a different channel is a `Query`, which does not
// participate in state validation checks in subsequent commit phase.
// If `channel` is empty, the caller's channel is assumed.
func (s *MockWrapStub) InvokeChaincode(chaincodeName string, args [][]byte, channel string) pb.Response {
	return s.stub.InvokeChaincode(chaincodeName, args, channel)
}

// func (s *MockWrapStub) GetState returns the value of the specified `key` from the
// ledger. Note that func (s *MockWrapStub) GetState doesn't read data from the writeset, which
// has not been committed to the ledger. In other words, func (s *MockWrapStub) GetState doesn't
// consider data modified by PutState that has not been committed.
// If the key does not exist in the state database, (nil, nil) is returned.
func (s *MockWrapStub) GetState(key string) ([]byte, error) {

	if s.cacheState != nil {
		if v, ok := s.cacheState[key]; ok {
			return v, nil
		}
	}
	return s.stub.GetState(key)
}

// PutState puts the specified `key` and `value` into the transaction's
// writeset as a data-write proposal. PutState doesn't effect the ledger
// until the transaction is validated and successfully committed.
// Simple keys must not be an empty string and must not start with null
// character (0x00), in order to avoid range query collisions with
// composite keys, which internally get prefixed with 0x00 as composite
// key namespace.
func (s *MockWrapStub) PutState(key string, value []byte) error {
	if s.cacheState != nil {
		s.cacheState[key] = value
	}
	return s.stub.PutState(key, value)
}

// DelState records the specified `key` to be deleted in the writeset of
// the transaction proposal. The `key` and its value will be deleted from
// the ledger when the transaction is validated and successfully committed.
func (s *MockWrapStub) DelState(key string) error {
	return s.stub.DelState(key)
}

// func (s *MockWrapStub) SetStateValidationParameter sets the key-level endorsement policy for `key`.
func (s *MockWrapStub) SetStateValidationParameter(key string, ep []byte) error {
	return s.stub.SetStateValidationParameter(key, ep)
}

// func (s *MockWrapStub) GetStateValidationParameter retrieves the key-level endorsement policy
// for `key`. Note that this will introduce a read dependency on `key` in
// the transaction's readset.
func (s *MockWrapStub) GetStateValidationParameter(key string) ([]byte, error) {
	return s.stub.GetStateValidationParameter(key)
}

// func (s *MockWrapStub) GetStateByRange returns a range iterator over a set of keys in the
// ledger. The iterator can be used to iterate over all keys
// between the startKey (inclusive) and endKey (exclusive).
// However, if the number of keys between startKey and endKey is greater than the
// totalQueryLimit (defined in core.yaml), this iterator cannot be used
// to fetch all keys (results will be capped by the totalQueryLimit).
// The keys are returned by the iterator in lexical order. Note
// that startKey and endKey can be empty string, which implies unbounded range
// query on start or end.
// Call Close() on the returned StateQueryIteratorInterface object when done.
// The query is re-executed during validation phase to ensure result set
// has not changed since transaction endorsement (phantom reads detected).
func (s *MockWrapStub) GetStateByRange(startKey, endKey string) (shim.StateQueryIteratorInterface, error) {
	return s.stub.GetStateByRange(startKey, endKey)
}

// func (s *MockWrapStub) GetStateByRangeWithPagination returns a range iterator over a set of keys in the
// ledger. The iterator can be used to fetch keys between the startKey (inclusive)
// and endKey (exclusive).
// When an empty string is passed as a value to the bookmark argument, the returned
// iterator can be used to fetch the first `pageSize` keys between the startKey
// (inclusive) and endKey (exclusive).
// When the bookmark is a non-emptry string, the iterator can be used to fetch
// the first `pageSize` keys between the bookmark (inclusive) and endKey (exclusive).
// Note that only the bookmark present in a prior page of query results (ResponseMetadata)
// can be used as a value to the bookmark argument. Otherwise, an empty string must
// be passed as bookmark.
// The keys are returned by the iterator in lexical order. Note
// that startKey and endKey can be empty string, which implies unbounded range
// query on start or end.
// Call Close() on the returned StateQueryIteratorInterface object when done.
// This call is only supported in a read only transaction.
func (s *MockWrapStub) GetStateByRangeWithPagination(startKey, endKey string, pageSize int32,
	bookmark string) (shim.StateQueryIteratorInterface, *pb.QueryResponseMetadata, error) {
	return s.stub.GetStateByRangeWithPagination(startKey, endKey, pageSize, bookmark)
}

// func (s *MockWrapStub) GetStateByPartialCompositeKey queries the state in the ledger based on
// a given partial composite key. This function returns an iterator
// which can be used to iterate over all composite keys whose prefix matches
// the given partial composite key. However, if the number of matching composite
// keys is greater than the totalQueryLimit (defined in core.yaml), this iterator
// cannot be used to fetch all matching keys (results will be limited by the totalQueryLimit).
// The `objectType` and attributes are expected to have only valid utf8 strings and
// should not contain U+0000 (nil byte) and U+10FFFF (biggest and unallocated code point).
// See related functions SplitCompositeKey and CreateCompositeKey.
// Call Close() on the returned StateQueryIteratorInterface object when done.
// The query is re-executed during validation phase to ensure result set
// has not changed since transaction endorsement (phantom reads detected).
func (s *MockWrapStub) GetStateByPartialCompositeKey(objectType string, keys []string) (shim.StateQueryIteratorInterface, error) {
	return s.stub.GetStateByPartialCompositeKey(objectType, keys)
}

// func (s *MockWrapStub) GetStateByPartialCompositeKeyWithPagination queries the state in the ledger based on
// a given partial composite key. This function returns an iterator
// which can be used to iterate over the composite keys whose
// prefix matches the given partial composite key.
// When an empty string is passed as a value to the bookmark argument, the returned
// iterator can be used to fetch the first `pageSize` composite keys whose prefix
// matches the given partial composite key.
// When the bookmark is a non-emptry string, the iterator can be used to fetch
// the first `pageSize` keys between the bookmark (inclusive) and the last matching
// composite key.
// Note that only the bookmark present in a prior page of query result (ResponseMetadata)
// can be used as a value to the bookmark argument. Otherwise, an empty string must
// be passed as bookmark.
// The `objectType` and attributes are expected to have only valid utf8 strings
// and should not contain U+0000 (nil byte) and U+10FFFF (biggest and unallocated
// code point). See related functions SplitCompositeKey and CreateCompositeKey.
// Call Close() on the returned StateQueryIteratorInterface object when done.
// This call is only supported in a read only transaction.
func (s *MockWrapStub) GetStateByPartialCompositeKeyWithPagination(objectType string, keys []string,
	pageSize int32, bookmark string) (shim.StateQueryIteratorInterface, *pb.QueryResponseMetadata, error) {
	return s.stub.GetStateByPartialCompositeKeyWithPagination(objectType, keys, pageSize, bookmark)
}

// CreateCompositeKey combines the given `attributes` to form a composite
// key. The objectType and attributes are expected to have only valid utf8
// strings and should not contain U+0000 (nil byte) and U+10FFFF
// (biggest and unallocated code point).
// The resulting composite key can be used as the key in PutState().
func (s MockWrapStub) CreateCompositeKey(objectType string, attributes []string) (string, error) {
	return s.stub.CreateCompositeKey(objectType, attributes)
}

// SplitCompositeKey splits the specified key into attributes on which the
// composite key was formed. Composite keys found during range queries
// or partial composite key queries can therefore be split into their
// composite parts.
func (s *MockWrapStub) SplitCompositeKey(compositeKey string) (string, []string, error) {
	return s.stub.SplitCompositeKey(compositeKey)
}

// func (s *MockWrapStub) GetQueryResult performs a "rich" query against a state database. It is
// only supported for state databases that support rich query,
// e.g.CouchDB. The query string is in the native syntax
// of the underlying state database. An iterator is returned
// which can be used to iterate over all keys in the query result set.
// However, if the number of keys in the query result set is greater than the
// totalQueryLimit (defined in core.yaml), this iterator cannot be used
// to fetch all keys in the query result set (results will be limited by
// the totalQueryLimit).
// The query is NOT re-executed during validation phase, phantom reads are
// not detected. That is, other committed transactions may have added,
// updated, or removed keys that impact the result set, and this would not
// be detected at validation/commit time.  Applications susceptible to this
// should therefore not use func (s *MockWrapStub) GetQueryResult as part of transactions that update
// ledger, and should limit use to read-only chaincode operations.
func (s *MockWrapStub) GetQueryResult(query string) (shim.StateQueryIteratorInterface, error) {
	return s.stub.GetQueryResult(query)
}

// func (s *MockWrapStub) GetQueryResultWithPagination performs a "rich" query against a state database.
// It is only supported for state databases that support rich query,
// e.g., CouchDB. The query string is in the native syntax
// of the underlying state database. An iterator is returned
// which can be used to iterate over keys in the query result set.
// When an empty string is passed as a value to the bookmark argument, the returned
// iterator can be used to fetch the first `pageSize` of query results.
// When the bookmark is a non-emptry string, the iterator can be used to fetch
// the first `pageSize` keys between the bookmark and the last key in the query result.
// Note that only the bookmark present in a prior page of query results (ResponseMetadata)
// can be used as a value to the bookmark argument. Otherwise, an empty string
// must be passed as bookmark.
// This call is only supported in a read only transaction.
func (s *MockWrapStub) GetQueryResultWithPagination(query string, pageSize int32,
	bookmark string) (shim.StateQueryIteratorInterface, *pb.QueryResponseMetadata, error) {
	return s.stub.GetQueryResultWithPagination(query, pageSize, bookmark)
}

// func (s *MockWrapStub) GetHistoryForKey returns a history of key values across time.
// For each historic key update, the historic value and associated
// transaction id and timestamp are returned. The timestamp is the
// timestamp provided by the client in the proposal header.
// func (s *MockWrapStub) GetHistoryForKey requires peer configuration
// core.ledger.history.enableHistoryDatabase to be true.
// The query is NOT re-executed during validation phase, phantom reads are
// not detected. That is, other committed transactions may have updated
// the key concurrently, impacting the result set, and this would not be
// detected at validation/commit time. Applications susceptible to this
// should therefore not use func (s *MockWrapStub) GetHistoryForKey as part of transactions that
// update ledger, and should limit use to read-only chaincode operations.
func (s *MockWrapStub) GetHistoryForKey(key string) (shim.HistoryQueryIteratorInterface, error) {
	return s.stub.GetHistoryForKey(key)
}

// func (s *MockWrapStub) GetPrivateData returns the value of the specified `key` from the specified
// `collection`. Note that func (s *MockWrapStub) GetPrivateData doesn't read data from the
// private writeset, which has not been committed to the `collection`. In
// other words, func (s *MockWrapStub) GetPrivateData doesn't consider data modified by PutPrivateData
// that has not been committed.
func (s *MockWrapStub) GetPrivateData(collection, key string) ([]byte, error) {

	if s.cachePrivateData != nil {
		if v, ok := s.cachePrivateData[key]; ok {
			return v, nil
		}
	}
	return s.stub.GetPrivateData(collection, key)
}

// func (s *MockWrapStub) GetPrivateDataHash returns the hash of the value of the specified `key` from the specified
// `collection`
func (s *MockWrapStub) GetPrivateDataHash(collection, key string) ([]byte, error) {
	return s.stub.GetPrivateDataHash(collection, key)
}

// PutPrivateData puts the specified `key` and `value` into the transaction's
// private writeset. Note that only hash of the private writeset goes into the
// transaction proposal response (which is sent to the client who issued the
// transaction) and the actual private writeset gets temporarily stored in a
// transient store. PutPrivateData doesn't effect the `collection` until the
// transaction is validated and successfully committed. Simple keys must not be
// an empty string and must not start with null character (0x00), in order to
// avoid range query collisions with composite keys, which internally get
// prefixed with 0x00 as composite key namespace.
func (s *MockWrapStub) PutPrivateData(collection string, key string, value []byte) error {
	if s.cachePrivateData != nil {
		s.cachePrivateData[key] = value
	}
	return s.stub.PutPrivateData(collection, key, value)
}

// DelPrivateData records the specified `key` to be deleted in the private writeset
// of the transaction. Note that only hash of the private writeset goes into the
// transaction proposal response (which is sent to the client who issued the
// transaction) and the actual private writeset gets temporarily stored in a
// transient store. The `key` and its value will be deleted from the collection
// when the transaction is validated and successfully committed.
func (s *MockWrapStub) DelPrivateData(collection, key string) error {
	return s.stub.DelPrivateData(collection, key)
}

// func (s *MockWrapStub) SetPrivateDataValidationParameter sets the key-level endorsement policy
// for the private data specified by `key`.
func (s *MockWrapStub) SetPrivateDataValidationParameter(collection, key string, ep []byte) error {
	return s.stub.SetPrivateDataValidationParameter(collection, key, ep)
}

// func (s *MockWrapStub) GetPrivateDataValidationParameter retrieves the key-level endorsement
// policy for the private data specified by `key`. Note that this introduces
// a read dependency on `key` in the transaction's readset.
func (s *MockWrapStub) GetPrivateDataValidationParameter(collection, key string) ([]byte, error) {
	return s.stub.GetPrivateDataValidationParameter(collection, key)
}

// func (s *MockWrapStub) GetPrivateDataByRange returns a range iterator over a set of keys in a
// given private collection. The iterator can be used to iterate over all keys
// between the startKey (inclusive) and endKey (exclusive).
// The keys are returned by the iterator in lexical order. Note
// that startKey and endKey can be empty string, which implies unbounded range
// query on start or end.
// Call Close() on the returned StateQueryIteratorInterface object when done.
// The query is re-executed during validation phase to ensure result set
// has not changed since transaction endorsement (phantom reads detected).
func (s *MockWrapStub) GetPrivateDataByRange(collection, startKey, endKey string) (shim.StateQueryIteratorInterface, error) {
	return s.stub.GetPrivateDataByRange(collection, startKey, endKey)
}

// func (s *MockWrapStub) GetPrivateDataByPartialCompositeKey queries the state in a given private
// collection based on a given partial composite key. This function returns
// an iterator which can be used to iterate over all composite keys whose prefix
// matches the given partial composite key. The `objectType` and attributes are
// expected to have only valid utf8 strings and should not contain
// U+0000 (nil byte) and U+10FFFF (biggest and unallocated code point).
// See related functions SplitCompositeKey and CreateCompositeKey.
// Call Close() on the returned StateQueryIteratorInterface object when done.
// The query is re-executed during validation phase to ensure result set
// has not changed since transaction endorsement (phantom reads detected).
func (s *MockWrapStub) GetPrivateDataByPartialCompositeKey(collection, objectType string, keys []string) (shim.StateQueryIteratorInterface, error) {
	return s.stub.GetPrivateDataByPartialCompositeKey(collection, objectType, keys)
}

// func (s *MockWrapStub) GetPrivateDataQueryResult performs a "rich" query against a given private
// collection. It is only supported for state databases that support rich query,
// e.g.CouchDB. The query string is in the native syntax
// of the underlying state database. An iterator is returned
// which can be used to iterate (next) over the query result set.
// The query is NOT re-executed during validation phase, phantom reads are
// not detected. That is, other committed transactions may have added,
// updated, or removed keys that impact the result set, and this would not
// be detected at validation/commit time.  Applications susceptible to this
// should therefore not use func (s *MockWrapStub) GetPrivateDataQueryResult as part of transactions that update
// ledger, and should limit use to read-only chaincode operations.
func (s *MockWrapStub) GetPrivateDataQueryResult(collection, query string) (shim.StateQueryIteratorInterface, error) {
	return s.stub.GetPrivateDataQueryResult(collection, query)
}

// func (s *MockWrapStub) GetCreator returns `SignatureHeader.Creator` (e.g. an identity)
// of the `SignedProposal`. This is the identity of the agent (or user)
// submitting the transaction.
func (s *MockWrapStub) GetCreator() ([]byte, error) {
	return s.stub.GetCreator()
}

// func (s *MockWrapStub) GetTransient returns the `ChaincodeProposalPayload.Transient` field.
// It is a map that contains data (e.g. cryptographic material)
// that might be used to implement some form of application-level
// confidentiality. The contents of this field, as prescribed by
// `ChaincodeProposalPayload`, are supposed to always
// be omitted from the transaction and excluded from the ledger.
func (s *MockWrapStub) GetTransient() (map[string][]byte, error) {
	return s.stub.GetTransient()
}

// func (s *MockWrapStub) GetBinding returns the transaction binding, which is used to enforce a
// link between application data (like those stored in the transient field
// above) to the proposal itself. This is useful to avoid possible replay
// attacks.
func (s *MockWrapStub) GetBinding() ([]byte, error) {
	return s.stub.GetBinding()
}

// func (s *MockWrapStub) GetDecorations returns additional data (if applicable) about the proposal
// that originated from the peer. This data is set by the decorators of the
// peer, which append or mutate the chaincode input passed to the chaincode.
func (s *MockWrapStub) GetDecorations() map[string][]byte {
	return s.stub.GetDecorations()
}

// func (s *MockWrapStub) GetSignedProposal returns the SignedProposal object, which contains all
// data elements part of a transaction proposal.
func (s *MockWrapStub) GetSignedProposal() (*pb.SignedProposal, error) {
	return s.stub.GetSignedProposal()
}

// func (s *MockWrapStub) GetTxTimestamp returns the timestamp when the transaction was created. This
// is taken from the transaction ChannelHeader, therefore it will indicate the
// client's timestamp and will have the same value across all endorsers.
func (s *MockWrapStub) GetTxTimestamp() (*timestamp.Timestamp, error) {
	return s.stub.GetTxTimestamp()
}

// func (s *MockWrapStub) SetEvent allows the chaincode to set an event on the response to the
// proposal to be included as part of a transaction. The event will be
// available within the transaction in the committed block regardless of the
// validity of the transaction.
func (s *MockWrapStub) SetEvent(name string, payload []byte) error {
	return s.stub.SetEvent(name, payload)
}

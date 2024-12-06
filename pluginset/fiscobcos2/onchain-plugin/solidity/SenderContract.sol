pragma solidity ^0.4.22;

interface ProtocolInterface {
    function sendMessage(
        string _destination_domain,
        bytes32 _receiver,
        bytes _message
    ) external;

    function sendUnorderedMessage(
        string _destination_domain,
        bytes32 _receiver,
        bytes _message
    ) external;
}

contract SenderContract {
    address sdp_address;

    function setSdpMSGAddress(address _sdp_address) public {
        sdp_address = _sdp_address;
    }

    function send(
        bytes32 receiver,
        string memory domain,
        bytes memory _msg
    ) public {
        ProtocolInterface sdp = ProtocolInterface(sdp_address);
        sdp.sendMessage(domain, receiver, _msg);
    }

    function sendUnordered(
        bytes32 receiver,
        string memory domain,
        bytes memory _msg
    ) public {
        ProtocolInterface sdp = ProtocolInterface(sdp_address);
        sdp.sendUnorderedMessage(domain, receiver, _msg);
    }
}
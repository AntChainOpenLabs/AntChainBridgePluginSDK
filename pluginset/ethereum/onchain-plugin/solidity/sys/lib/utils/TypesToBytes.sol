pragma solidity ^0.8.0;

/**
 * @title TypesToBytes
 * @dev The TypesToBytes contract converts the standard solidity types to the byte array
 * @author pouladzade@gmail.com
 */

library TypesToBytes {

    function addressToBytes(uint _offset, address _input, bytes memory _output) internal pure {

        assembly {
            mstore(add(_output, _offset), _input)
        }
    }

    function addressToBytes32(address _input) internal pure returns (bytes32 _output) {
        bytes memory out = new bytes(32);
        assembly {
            mstore(add(out, 32), _input)
            _output := mload(add(out, 32))
        }
    }

    function bytes32ToBytes(uint _offset, bytes32 _input, bytes memory _output) internal pure {

        assembly {
            mstore(add(_output, _offset), _input)
        }
    }

    function boolToBytes(uint _offset, bool _input, bytes memory _output) internal pure {
        uint8 x = _input == false ? 0 : 1;
        assembly {
            mstore(add(_output, _offset), x)
        }
    }

    function varBytesToBytes(uint _offset, bytes memory _input, bytes memory _output) internal pure {
        uint32 body_len = uint32(_input.length);
        TypesToBytes.uintToBytes(_offset, body_len, _output);
        _offset -= 4;

        require(
            _offset >= body_len,
            "varBytesToBytes: offset less than the input length"
        );

        _offset -= body_len;

        assembly{
            switch iszero(body_len)
            case 0 {
                let prev_word_pos := add(_output, _offset)
                let prev_word := mload(prev_word_pos)

                let lengthmod := and(body_len, 31)
                let buff_cnt := add(add(_input, lengthmod), mul(0x20, iszero(lengthmod)))
                let end := add(buff_cnt, body_len)

                for {
                    let output_cnt := add(add(prev_word_pos, lengthmod), mul(0x20, iszero(lengthmod)))
                } lt(buff_cnt, end) {
                    buff_cnt := add(buff_cnt, 0x20)
                    output_cnt := add(output_cnt, 0x20)
                } {
                    mstore(output_cnt, mload(buff_cnt))
                }

                mstore(prev_word_pos, prev_word)
            }
        }
    }

    function varBytesToBytesBigEndian(uint _offset, bytes memory _input, bytes memory _output) internal pure {
        uint32 body_len = uint32(_input.length);
        require(
            _offset + body_len < _output.length,
            "varBytesToBytesBigEndian: offset is greater than the output length"
        );

        assembly{
            switch iszero(body_len)
            case 0 {
                let prev_word_pos := add(_output, _offset)
                let prev_word := mload(prev_word_pos)

                let lengthmod := and(body_len, 31)
                let buff_cnt := add(add(_input, lengthmod), mul(0x20, iszero(lengthmod)))
                let end := add(buff_cnt, body_len)

                for {
                    let output_cnt := add(add(prev_word_pos, lengthmod), mul(0x20, iszero(lengthmod)))
                } lt(buff_cnt, end) {
                    buff_cnt := add(buff_cnt, 0x20)
                    output_cnt := add(output_cnt, 0x20)
                } {
                    mstore(output_cnt, mload(buff_cnt))
                }

                mstore(prev_word_pos, prev_word)
            }
        }
    }

    function stringToBytes(uint _offset, bytes memory _input, bytes memory _output) internal pure {
        uint256 stack_size = _input.length / 32;
        if(_input.length % 32 > 0) stack_size++;

        assembly {
            let index := 0

            for
                {stack_size := add(stack_size,1)} //adding because of 32 first bytes memory as the length
                lt(index,stack_size)
                {index := add(index ,1)}
            {
                mstore(add(_output, _offset), mload(add(_input,mul(index,32))))
                _offset := sub(_offset , 32)
            }
        }
    }

    function intToBytes(uint _offset, int _input, bytes memory  _output) internal pure {

        assembly {
            mstore(add(_output, _offset), _input)
        }
    }

    function uintToBytes(uint _offset, uint _input, bytes memory _output) internal pure {

        assembly {
            mstore(add(_output, _offset), _input)
        }
    }

    function uint16ToBytes(uint _offset, uint16 _input, bytes memory _output) internal pure {

        assembly {
            let prev_word_pos := add(_output, _offset)
            let prev_word := mload(prev_word_pos)

            mstore(add(prev_word_pos, 2), _input)

            mstore(prev_word_pos, prev_word)
        }
    }

    function uint32ToBytes(uint _offset, uint32 _input, bytes memory _output) internal pure {

        assembly {
            let prev_word_pos := add(_output, _offset)
            let prev_word := mload(prev_word_pos)

            mstore(add(prev_word_pos, 4), _input)

            mstore(prev_word_pos, prev_word)
        }
    }

}

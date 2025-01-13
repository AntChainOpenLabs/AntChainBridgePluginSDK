#pragma once
#include <eosio/eosio.hpp>
#include <string>
#include "utils.hpp"

namespace crosschain
{
  void BytesToUint64(const std::string &byte_array, uint32_t &offset, uint64_t &num)
  {
    eosio::check(8 <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");

    for (int i = 8; i > 0; i--)
    {
      num = (num << 8) + static_cast<unsigned char>(byte_array.at(offset - i));
    }
    offset -= 8;
  }

  void BytesToName(const std::string &byte_array, uint32_t &offset, eosio::name &id)
  {
    eosio::check(crosschain::NameLength <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");

    uint64_t name_value;
    BytesToUint64(byte_array, offset, name_value);
    id = eosio::name{name_value};

    offset -= 24;
  }

  void BytesToByte32(const std::string &byte_array, uint32_t &offset, string &id)
  {
    eosio::check(crosschain::NameLength <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");
    
    id = string(32, 0);
    std::memcpy(id.data(), byte_array.data() + offset - crosschain::NameLength, crosschain::NameLength);

    offset -= crosschain::NameLength;
  }

  void BytesToUint32(const std::string &byte_array, uint32_t &offset, uint32_t &num)
  {
    eosio::check(4 <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");
    for (int i = 4; i > 0; i--)
    {
      num = (num << 8) + static_cast<unsigned char>(byte_array.at(offset - i));
    }
    offset -= 4;
  }

  void BytesToString(const std::string &byte_array, uint32_t &offset, std::string &str)
  {
    // 存放字符串长度的32字节，高位28字节为空，低位4字节存储长度
    eosio::check(32 <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");
    uint32_t str_len;
    BytesToUint32(byte_array, offset, str_len);
    offset -= 28; // 跳过剩余28空白字节

    eosio::check(BytesReserveForString(str_len) <= (offset + 32), "CROSSCHAIN_ERROR: offset not enough for data");
    str.clear();
    str.resize(str_len, 0);
    std::string::iterator it = str.begin();
    std::string::const_iterator bytes_head = byte_array.begin();
    while (it != str.end())
    {
      if ((it + 32) <= str.end())
      {
        str.replace(it, it + 32, bytes_head + offset - 32, bytes_head + offset);
        it += 32;
      }
      else
      {
        str.replace(it, it + (str_len % 32),
                    bytes_head + offset - 32, bytes_head + offset - 32 + (str_len % 32));
        it += (str_len % 32);
      }
      offset -= 32;
    }
  }

  // 顺序读取
  void SequentialBytesToUint32(const std::string &byte_array, uint32_t &offset, uint32_t &num)
  {
    uint32_t offset_end = offset + 4;
    BytesToUint32(byte_array, offset_end, num);
    offset += 4;
  }

  void SequentialBytesToString(const std::string &byte_array, uint32_t &offset, std::string &str)
  {
    uint32_t str_len;
    SequentialBytesToUint32(byte_array, offset, str_len);
    // Sequential string
    str.replace(str.begin(), str.end(), byte_array.begin() + offset, byte_array.begin() + offset + str_len);
    offset += str_len;
  }

  void SequentialBytesToString32BytesFormated(const std::string &byte_array, uint32_t &offset, std::string &str)
  {
    uint32_t str_len;
    offset += 28;
    SequentialBytesToUint32(byte_array, offset, str_len);
    // Sequential string
    str.replace(str.begin(), str.end(), byte_array.begin() + offset, byte_array.begin() + offset + str_len);
    offset += BytesReserveForString(str_len) - 32;
  }
}
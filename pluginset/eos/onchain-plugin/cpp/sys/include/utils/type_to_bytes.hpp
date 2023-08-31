#pragma once
#include <string>
#include <eosio/eosio.hpp>
#include <eosio/print.hpp>
#include "utils.hpp"

// offset所指位置为右侧开区间，填充范围[offset-len, offset)
// 所有位置填充完毕后，offset值为0

namespace crosschain
{

  void Uint64ToBytes(const uint64_t &num, std::string &byte_array, uint32_t &offset)
  {
    eosio::check(8 <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");
    for (uint64_t i = 0; i < 8; i++)
    {
      byte_array.replace(offset - 1, 1, 1, static_cast<unsigned char>(num >> (i * 8)));
      offset -= 1;
    }
  }

  // todo: eos为小端字节序，待验证
  void NameToBytes(const eosio::name &id, std::string &byte_array, uint32_t &offset)
  {
    eosio::check(crosschain::NameLength <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");
    uint64_t name_value = id.value;
    Uint64ToBytes(name_value, byte_array, offset);
    std::string tmp(24, 0);
    byte_array.replace(offset - 24, 24, tmp);
    offset -= 24;
  }

  void Checksum256ToBytes(const eosio::checksum256 &id, std::string &byte_array, uint32_t &offset)
  {
    eosio::check(crosschain::NameLength <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");

    std::string tmp((char *)id.get_array().begin(), id.get_array().size());
    byte_array.replace(offset - crosschain::NameLength, crosschain::NameLength, tmp);
    offset -= crosschain::NameLength;
  }

  void Byte32ToBytes(const std::string &id, std::string &byte_array, uint32_t &offset)
  {
    eosio::check(crosschain::NameLength <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");

    byte_array.replace(offset - crosschain::NameLength, crosschain::NameLength, id);
    offset -= crosschain::NameLength;
  }

  void Uint32ToBytes(const uint32_t &num, std::string &byte_array, uint32_t &offset)
  {
    eosio::check(4 <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");
    for (uint32_t i = 0; i < 4; i++)
    {
      byte_array.replace(offset - 1, 1, 1, static_cast<unsigned char>(num >> (i * 8)));
      offset -= 1;
    }
  }

  void StringToBytes(const std::string &str, std::string &byte_array, uint32_t &offset)
  {
    // 与Fabric一致，假设string size不超过uint32
    eosio::check(str.size() <= UINT32_MAX, "CROSSCHAIN_ERROR: string length exceeds uint32");

    uint32_t set_zero_len = BytesReserveForString(str);
    eosio::check(set_zero_len <= offset, "CROSSCHAIN_ERROR: offset not enough for data");
    eosio::check(offset <= byte_array.size(), "CROSSCHAIN_ERROR: offset exceeds bytes array size");

    // string填充区域预置0
    byte_array.replace(offset - set_zero_len, set_zero_len, set_zero_len, 0);

    // 迁就EVM 256位对齐，使用32字节保存string长度
    uint32_t str_len = str.size();
    // 高地址填4字节
    Uint32ToBytes(str_len, byte_array, offset);
    // 填充空白28字节
    byte_array.replace(offset - 28, 28, 28, 0);
    offset -= 28;

    // 每32字节为一个单位，逆序填充string内容
    std::string::const_iterator it = str.begin();
    std::string::iterator bytes_head = byte_array.begin();
    while (it != str.end())
    {
      if ((it + 32) <= str.end())
      {
        byte_array.replace(bytes_head + offset - 32, bytes_head + offset, it, it + 32);
        it += 32;
      }
      else
      {
        byte_array.replace(
            bytes_head + offset - 32, bytes_head + offset - 32 + (str_len % 32), it, it + (str_len % 32));
        it += (str_len % 32);
      }
      offset -= 32;
    }
  }
}

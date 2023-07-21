#pragma once
#include <cstddef>
#include <string>
#include <eosio/crypto.hpp>
#include <eosio/print.hpp>

namespace crosschain
{
  constexpr uint32_t HashLength = 32;
  constexpr uint32_t IdentityLength = 32;
  // eos 的 name 固定为8个字节 todo：前面补零
  constexpr uint32_t NameLength = 32;

  const uint32_t value = 0;
  const uint32_t gas = 0;

  uint32_t BytesReserveForString(const uint32_t &str_len)
  {
    eosio::check(str_len <= UINT32_MAX, "CROSSCHAIN_ERROR: string length exceeds uint32");
    uint32_t reserve = (0 == str_len) ? 0 : (str_len - 1) / 32 + 1;
    return ++reserve * 32;
  }

  uint32_t BytesReserveForString(const std::string &str)
  {
    return BytesReserveForString(str.size());
  }

  bool ReadUint8(const std::string &str, const uint32_t &offset, uint8_t &val)
  {
    if (offset >= str.length())
    {
      return false;
    }
    val = static_cast<unsigned char>(str.at(offset + 0));
    return true;
  }

  bool ReadUint16LittleEndian(const std::string &str, const uint32_t &offset, uint16_t &val)
  {
    if (offset >= str.length())
    {
      return false;
    }
    uint32_t ch1 = static_cast<unsigned char>(str.at(offset + 1));
    uint32_t ch2 = static_cast<unsigned char>(str.at(offset + 0));
    val = ((ch1 << 8) + (ch2 << 0));
    return true;
  }

  bool ReadUint32LittleEndian(const std::string &str, const uint32_t &offset, uint32_t &val)
  {
    if (offset + 3 >= str.length())
    {
      return false;
    }
    uint32_t ch1 = static_cast<unsigned char>(str.at(offset + 3));
    uint32_t ch2 = static_cast<unsigned char>(str.at(offset + 2));
    uint32_t ch3 = static_cast<unsigned char>(str.at(offset + 1));
    uint32_t ch4 = static_cast<unsigned char>(str.at(offset + 0));
    val = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    return true;
  }

  bool ReadUint32(const std::string &str, const uint32_t &offset, uint32_t &val)
  {
    if (offset + 3 >= str.length())
    {
      return false;
    }
    uint32_t ch1 = static_cast<unsigned char>(str.at(offset + 0));
    uint32_t ch2 = static_cast<unsigned char>(str.at(offset + 1));
    uint32_t ch3 = static_cast<unsigned char>(str.at(offset + 2));
    uint32_t ch4 = static_cast<unsigned char>(str.at(offset + 3));
    val = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    return true;
  }

  bool StartsWithCaseInsensitive(std::string mainStr, std::string toMatch)
  {
    // Convert mainStr to lower case
    std::transform(mainStr.begin(), mainStr.end(), mainStr.begin(), ::tolower);
    // Convert toMatch to lower case
    std::transform(toMatch.begin(), toMatch.end(), toMatch.begin(), ::tolower);

    if (mainStr.find(toMatch) == 0)
      return true;
    else
      return false;
  }

  int HexStringToBytes(const char *hexStr,
                       char *output,
                       size_t outputLen)
  {
    size_t len = strlen(hexStr);
    if (len % 2 != 0)
    {
      return -1;
    }
    size_t finalLen = len / 2;

    eosio::check(outputLen == finalLen, "HexStringToBytes: incorrect len");

    for (size_t inIdx = 0, outIdx = 0; outIdx < finalLen; inIdx += 2, outIdx++)
    {
      if ((hexStr[inIdx] - 48) <= 9 && (hexStr[inIdx + 1] - 48) <= 9)
      {
        goto convert;
      }
      else
      {
        if (((hexStr[inIdx] - 65) <= 5 && (hexStr[inIdx + 1] - 65) <= 5) || ((hexStr[inIdx] - 97) <= 5 && (hexStr[inIdx + 1] - 97) <= 5))
        {
          goto convert;
        }
        else
        {
          return -1;
        }
      }
    convert:
      output[outIdx] =
          (hexStr[inIdx] % 32 + 9) % 25 * 16 + (hexStr[inIdx + 1] % 32 + 9) % 25;
    }
    // output[finalLen] = '\0';
    return 0;
  }

  void BytesToHexString(const char *data, size_t datalen, std::string &res)
  {
    const std::string hex = "0123456789abcdef";
    res.clear();
    res.resize(datalen * 2, 0);

    for (std::string::size_type i = 0; i < datalen; ++i)
    {
      res[2 * i] = hex[(unsigned char)data[i] >> 4];
      res[2 * i + 1] = hex[(unsigned char)data[i] & 0xf];
    }
  }
}
#pragma once
#include <cstddef>
#include <string>
#include <eosio/eosio.hpp>
#include "utils.hpp"

namespace crosschain {
    struct TLVItem {
        uint16_t tag;
        uint32_t len;
        std::string value;
    };
    struct LVItem {
        uint32_t len;
        std::string value;
    };

    bool ParseTLVItem(const std::string& raw_data, uint32_t& offset, TLVItem& item){
        eosio::check(raw_data.length() > offset, "CROSSCHAIN_ERROR: length of raw data less than offset");
        eosio::check(offset >= 6, "CROSSCHAIN_ERROR: illegal offset");

        if(!ReadUint16LittleEndian(raw_data, offset, item.tag)){ return false; }
        offset = offset + 2;

        if(!ReadUint32LittleEndian(raw_data, offset, item.len)){ return false; }
        offset = offset + 4;

        item.value =  raw_data.substr(offset, item.len);
        offset = offset + item.len;

        return true;
    }

    bool ParseLVItem(const std::string& raw_data, uint32_t& offset, LVItem& item){
        eosio::check(raw_data.length() > offset, "CROSSCHAIN_ERROR: length of raw data less than offset");
        eosio::check(offset >= 6, "CROSSCHAIN_ERROR: illegal offset");

        if(!ReadUint32LittleEndian(raw_data, offset, item.len)){ return false; }
        offset = offset + 4;

        item.value =  raw_data.substr(offset, item.len);
        offset = offset + item.len;

        return true;
    }
}
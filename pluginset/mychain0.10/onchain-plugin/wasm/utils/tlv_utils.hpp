#ifndef TLV_UTILS_HPP
#define TLV_UTILS_HPP

#pragma once
#include <cstddef>
#include <string>
#include <mychainlib/developer_api.hpp>
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
        mychain::Require(raw_data.length() > offset, "CROSSCHAIN_ERROR: length of raw data less than offset");
        mychain::Require(offset >= 6, "CROSSCHAIN_ERROR: illegal offset");

        if(!ReadUint16LittleEndian(raw_data, offset, item.tag)){ return false; }
        offset = offset + 2;

        if(!ReadUint32LittleEndian(raw_data, offset, item.len)){ return false; }
        offset = offset + 4;

        item.value =  raw_data.substr(offset, item.len);
        offset = offset + item.len;

        return true;
    }

    bool ParseLVItem(const std::string& raw_data, uint32_t& offset, LVItem& item){
        mychain::Require(raw_data.length() > offset, "CROSSCHAIN_ERROR: length of raw data less than offset");
        mychain::Require(offset >= 6, "CROSSCHAIN_ERROR: illegal offset");

        if(!ReadUint32LittleEndian(raw_data, offset, item.len)){ return false; }
        offset = offset + 4;

        item.value =  raw_data.substr(offset, item.len);
        offset = offset + item.len;

        return true;
    }
}

#endif
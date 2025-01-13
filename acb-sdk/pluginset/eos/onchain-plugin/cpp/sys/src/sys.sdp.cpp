#include <eosio/eosio.hpp>
#include <eosio/print.hpp>
#include <algorithm>
#include <string>
#include <string.h>
#include "../include/sys.sdp/sys.sdp.hpp"
#include "../include/sys.am/sys.am.hpp"
#include "../include/utils/bytes_to_type.hpp"
#include "../include/utils/type_to_bytes.hpp"
#include "../include/utils/utils.hpp"

using namespace eosio;
using namespace std;
using namespace crosschain;

ACTION syssdp::init(const name &initializer)
{
}

ACTION syssdp::setam(const name &invoker, const name &am_contract_account)
{
    require_auth(invoker);

    auto data_itr = tbl_sdpinitinfo.find(SDP_INIT_INFO_TABLE_KEY);

    if (data_itr == tbl_sdpinitinfo.end())
    {
        tbl_sdpinitinfo.emplace(invoker, [&](auto &row)
                                {
            row.sdp_init_key = SDP_INIT_INFO_TABLE_KEY;
            row.am_contract_account = am_contract_account;
            print("P2PMSG_INFO: set am contract\n"); });
    }
    else
    {
        tbl_sdpinitinfo.modify(data_itr, invoker, [&](auto &row)
                               {
            row.am_contract_account = am_contract_account;
            print("P2PMSG_INFO: modify am contract\n"); });
    }
}

ACTION syssdp::setdomain(const name &invoker, const string &local_domain)
{
    require_auth(invoker);

    auto data_itr = tbl_sdpinitinfo.find(SDP_INIT_INFO_TABLE_KEY);

    if (data_itr == tbl_sdpinitinfo.end())
    {
        tbl_sdpinitinfo.emplace(invoker, [&](auto &row)
                                {
            row.sdp_init_key = SDP_INIT_INFO_TABLE_KEY;
            row.local_domain = local_domain;
            print("P2PMSG_INFO: set localdomain\n"); });
    }
    else
    {
        tbl_sdpinitinfo.modify(data_itr, invoker, [&](auto &row)
                               {
            row.local_domain = local_domain;
            print("P2PMSG_INFO: modify localdomain\n"); });
    }
}

ACTION syssdp::getdomain(const name &invoker)
{
    require_auth(invoker);

    auto data_itr = tbl_sdpinitinfo.find(SDP_INIT_INFO_TABLE_KEY);

    if (data_itr == tbl_sdpinitinfo.end())
    {
        print("");
    }
    print(data_itr->local_domain);
}

ACTION syssdp::recvmsg(
    const name &invoker,
    const string &sender_domain,
    const string &sender_id,
    const string &pkg)
{
    require_auth(invoker);
    only_amclient(invoker);

    // print("P2PMSG_INFO: recv msg\n");

    // 从pkg中解析出 receiver_id msg recv_seq
    name receiver_id;
    string msg;
    uint32_t recv_seq;
    parse_message(pkg, receiver_id, msg, recv_seq);

    name callback_action;
    if (UNORDERED_SEQUENCE == recv_seq)
    {
        callback_action = UNORDERED_CALLBACK_ACTION;
    }
    else
    {
        // 有序消息，需要判断消息序号是否正确
        string raw_id_b32(32, 0);
        uint32_t offset = 32;
        NameToBytes(receiver_id, raw_id_b32, offset);

        auto msg_seq_key = get_sdp_msg_key(sender_domain, sender_id, raw_id_b32);
        check(recv_seq == get_sequence(invoker, msg_seq_key), "P2PMSG_ERROR: invalid receiving sequence");
        // 序号自增
        set_sequence(invoker, msg_seq_key, 1 + recv_seq);

        callback_action = ORDERED_CALLBACK_ACTION;
    }

    std::string s_id;
    BytesToHexString(sender_id.c_str(), sender_id.size(), s_id);

    print_f("r: % , s_d: % , s_id: % , m: % ", receiver_id.to_string(), sender_domain, s_id, msg);

    // 跨合约调用业务合约
    action(
        permission_level{_self, name("active")},
        receiver_id,
        callback_action,
        std::make_tuple(get_self(), sender_domain, sender_id, msg))
        .send();
}

ACTION syssdp::sendmsg(
    const name &invoker,
    const string &receiver_domain,
    const string &receiver_id,
    const string &msg)
{
    require_auth(invoker);

    string pkg;
    uint32_t send_seq;
    build_message_ordered(invoker, receiver_domain, receiver_id, msg, pkg, send_seq);

    // 跨合约调用AM合约
    sysam::recvprotocol_action recvprotocol(get_am_contract(), {get_self(), "active"_n});
    recvprotocol.send(
        get_self(),
        invoker,
        pkg);
}

ACTION syssdp::sendunmsg(
    const name &invoker,
    const string &receiver_domain,
    const string &receiver_id,
    const string &msg)
{
    require_auth(invoker);

    string pkg;
    uint32_t send_seq;
    build_message_unordered(invoker, receiver_domain, receiver_id, msg, pkg, send_seq);

    // 跨合约调用AM合约
    sysam::recvprotocol_action recvprotocol(get_am_contract(), {get_self(), "active"_n});
    recvprotocol.send(
        get_self(),
        invoker,
        pkg);
}

ACTION syssdp::upseq(
    const name &invoker,
    const string &sender_domain,
    const string &sender_id,
    const string &pkg)
{
    require_auth(invoker);
    only_amclient(invoker);

    name receiver_id;
    string msg;
    uint32_t recv_seq;
    parse_message(pkg, receiver_id, msg, recv_seq);

    if (UNORDERED_SEQUENCE == recv_seq)
    {
        print_f("not expect that update unorder seq");
        return;
    }

    string raw_id_b32(32, 0);
    uint32_t offset = 32;
    NameToBytes(receiver_id, raw_id_b32, offset);

    auto msg_seq_key = get_sdp_msg_key(sender_domain, sender_id, raw_id_b32);
    check(recv_seq == get_sequence(invoker, msg_seq_key), "UP_SEQ: invalid receiving sequence");
    // 序号自增
    set_sequence(invoker, msg_seq_key, 1 + recv_seq);
}

// 如果不存在会初始化一个0
uint32_t syssdp::get_sequence(const name &invoker, checksum256 key)
{
    auto seq_idx = tbl_sdpmsgseq.get_index<name("sdpmsgkey")>();
    auto data_itr = seq_idx.find(key);

    if (data_itr == seq_idx.end())
    {
        tbl_sdpmsgseq.emplace(invoker, [&](auto &row)
                              {
                                  row.sdp_msg_count = getcount(invoker);
                                  addcount(invoker); // seq表的主键值，每添加一条seq记录，主键自增

                                  row.sdp_msg_key = key;
                                  row.sdp_msg_seq = 0; });
        data_itr = seq_idx.find(key);
        check(data_itr != seq_idx.end(), "init sequence error");
    }

    print_f("get_sequence: % |", data_itr->sdp_msg_seq);
    return data_itr->sdp_msg_seq;
}

void syssdp::set_sequence(const name &invoker, checksum256 key, uint32_t seq)
{
    auto seq_idx = tbl_sdpmsgseq.get_index<name("sdpmsgkey")>();
    auto data_itr = seq_idx.find(key);

    if (data_itr == seq_idx.end())
    {
        tbl_sdpmsgseq.emplace(invoker, [&](auto &row)
                              {
            row.sdp_msg_count = getcount(invoker);
            addcount(invoker);

            row.sdp_msg_key = key;
            row.sdp_msg_seq = seq;
            print_f("P2PMSG_INFO: set sequence, count: %, key: %, seq: %\n", row.sdp_msg_count, row.sdp_msg_key, row.sdp_msg_seq); });
    }
    else
    {
        seq_idx.modify(data_itr, invoker, [&](auto &row)
                       {
                           row.sdp_msg_key = key;
                           row.sdp_msg_seq = seq; });
    }
}

// 不可以在初始化时调用，不存在会直接报错
string syssdp::get_this_domain()
{
    auto data_itr = tbl_sdpinitinfo.find(SDP_INIT_INFO_TABLE_KEY);

    check(data_itr != tbl_sdpinitinfo.end() && !data_itr->local_domain.empty(),
          "P2PMSG_ERROR: local domain does not exist, please set it before using the contract");

    return data_itr->local_domain;
}

// 不可以在初始化时调用，不存在会直接报错
name syssdp::get_am_contract()
{
    auto data_itr = tbl_sdpinitinfo.find(SDP_INIT_INFO_TABLE_KEY);

    check(data_itr != tbl_sdpinitinfo.end() && data_itr->am_contract_account.value != 0,
          "P2PMSG_ERROR: am contract account does not exist, please set it before using the contract");

    return data_itr->am_contract_account;
}

void syssdp::only_amclient(const name &invoker)
{
    name am = get_am_contract();
    check(am.value == invoker.value, "P2PMSG_ERROR: the invoker should be am contract");
}

void syssdp::parse_message(const string &byte_array, name &receiver_id, string &msg, uint32_t &recv_seq)
{
    uint32_t offset = byte_array.size();
    string receiver_domain;
    BytesToString(byte_array, offset, receiver_domain);
    check(receiver_domain == get_this_domain(), "P2PMSG_ERROR: domain name mismatch");

    BytesToName(byte_array, offset, receiver_id);
    BytesToUint32(byte_array, offset, recv_seq);
    BytesToString(byte_array, offset, msg);
    check(offset == 0, "P2PMSG_ERROR: ParseMessage taking out failed");
}

void syssdp::build_message_ordered(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg, string &byte_array, uint32_t &send_seq)
{
    checksum256 msg_seq_key = get_sdp_msg_key(invoker.to_string(), receiver_domain, receiver_id);
    uint32_t msg_seq = get_sequence(invoker, msg_seq_key);
    build_message_core(msg_seq, receiver_domain, receiver_id, msg, byte_array);
    set_sequence(invoker, msg_seq_key, 1 + msg_seq);
}

void syssdp::build_message_unordered(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg, string &byte_array, uint32_t &send_seq)
{
    build_message_core(UNORDERED_SEQUENCE, receiver_domain, receiver_id, msg, byte_array);
    send_seq = UNORDERED_SEQUENCE;
}

void syssdp::build_message_core(const uint32_t this_send_seq, const string &receiver_domain, const string &receiver_id, const string &msg, string &byte_array)
{
    uint32_t offset = BytesReserveForString(msg) + 4 + 32 + BytesReserveForString(receiver_domain);
    byte_array.clear();
    byte_array.resize(offset, 0);

    StringToBytes(receiver_domain, byte_array, offset);
    Byte32ToBytes(receiver_id, byte_array, offset);
    Uint32ToBytes(this_send_seq, byte_array, offset);
    StringToBytes(msg, byte_array, offset);
    check(offset == 0, "P2PMSG_ERROR: buildMessage filling failed");
}

void syssdp::addcount(const name &invoker)
{
    auto entry_stored = tbl_sdpmsgcount.get_or_create(invoker, countrow);
    entry_stored.count = entry_stored.count + 1;
    tbl_sdpmsgcount.set(entry_stored, invoker);
}

uint64_t syssdp::getcount(const name &invoker)
{
    auto entry_stored = tbl_sdpmsgcount.get_or_create(invoker, countrow);
    return entry_stored.count;
}

checksum256 syssdp::get_sdp_msg_key(const string &var1, const string &var2, const string &receiver_id)
{
    string str = var1 + "-" + var2 + "-" + receiver_id;
    auto hash = eosio::sha256(str.c_str(), str.size());

    return hash;
}

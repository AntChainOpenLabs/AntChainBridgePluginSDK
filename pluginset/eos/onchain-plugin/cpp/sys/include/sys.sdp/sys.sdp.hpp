#pragma once

#include <eosio/eosio.hpp>
#include <eosio/singleton.hpp>
#include "../utils/type_to_bytes.hpp"

using namespace eosio;
using namespace std;
using namespace crosschain;

// 无序消息标识
#define UNORDERED_SEQUENCE -1

// 业务合约实现的回调接口action名
#define UNORDERED_CALLBACK_ACTION "recvunmsg"_n
#define ORDERED_CALLBACK_ACTION "recvmsg"_n

// 合约初始化信息表的唯一键值
#define SDP_INIT_INFO_TABLE_KEY 1

// sdpmsgseq表的singleton结构序号
#define SDP_MSG_SEQ_COUNT_SIGNLETON_KEY 11

class [[eosio::contract("sys.sdp")]] syssdp : public contract
{
public:
    using contract::contract;

    // 名称最长12个字符
    ACTION init(const name &initializer);

    ACTION setam(const name &invoker, const name &am_contract_account);

    ACTION setdomain(const name &invoker, const string &local_domain);

    ACTION getdomain(const name &invoker);

    ACTION recvmsg(
        const name &invoker,
        const string &sender_domain,
        const string &sender_id,
        const string &pkg);

    ACTION sendmsg(
        const name &invoker,
        const string &receiver_domain,
        const string &receiver_id,
        const string &msg);

    ACTION sendunmsg(
        const name &invoker,
        const string &receiver_domain,
        const string &receiver_id,
        const string &msg);

    ACTION upseq(
        const name &invoker,
        const string &sender_domain,
        const string &sender_id,
        const string &pkg);

    using recvmsg_action = action_wrapper<"recvmsg"_n, &syssdp::recvmsg>;
    using sendmsg_action = action_wrapper<"sendmsg"_n, &syssdp::sendmsg>;
    using sendunmsg_action = action_wrapper<"sendunmsg"_n, &syssdp::sendunmsg>;
    using upseq_action = action_wrapper<"upseq"_n, &syssdp::upseq>;

private:
    // 合约初始化信息表：am、localdomain
    TABLE s_sdpinitinfo
    {
        uint64_t sdp_init_key;
        name am_contract_account;
        string local_domain;
        uint64_t primary_key() const { return sdp_init_key; }
    };
    typedef multi_index<name("sdpinitinfo"), s_sdpinitinfo> t_sdpinitinfo;

    // 有序消息序号表序号
    TABLE counttable
    {
        uint64_t count;
    }
    countrow;
    typedef singleton<name("sdpmsgcount"), counttable> t_sdpmsgcount;

    // 有序消息序号表
    TABLE s_sdpmsgseq
    {
        uint64_t sdp_msg_count;
        eosio::checksum256 sdp_msg_key;
        uint32_t sdp_msg_seq;
        uint64_t primary_key() const { return sdp_msg_count; }
        eosio::checksum256 by_sdp_msg_key() const { return sdp_msg_key; }
    };
    typedef multi_index<name("sdpmsgseq"), s_sdpmsgseq,
                        indexed_by<name("sdpmsgkey"), const_mem_fun<s_sdpmsgseq, eosio::checksum256, &s_sdpmsgseq::by_sdp_msg_key>>>
        t_sdpmsgseq;

    t_sdpinitinfo tbl_sdpinitinfo = t_sdpinitinfo(get_self(), get_self().value);
    t_sdpmsgcount tbl_sdpmsgcount = t_sdpmsgcount(get_self(), SDP_MSG_SEQ_COUNT_SIGNLETON_KEY);
    t_sdpmsgseq tbl_sdpmsgseq = t_sdpmsgseq(get_self(), get_self().value);

    uint32_t get_sequence(const name &invoker, checksum256 key);
    void set_sequence(const name &invoker, checksum256 key, uint32_t seq);
    string get_this_domain();
    name get_am_contract();
    void only_amclient(const name &invoker);
    void parse_message(const string &byte_array, name &receiver_id, string &msg, uint32_t &recv_seq);
    void build_message_ordered(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg, string &byte_array, uint32_t &send_seq);
    void build_message_unordered(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg, string &byte_array, uint32_t &send_seq);
    void build_message_core(const uint32_t this_send_seq, const string &receiver_domain, const string &receiver_id, const string &msg, string &byte_array);

    void addcount(const name &invoker);
    uint64_t getcount(const name &invoker);
    eosio::checksum256 get_sdp_msg_key(const string &invoker, const string &receiver_domain, const string &receiver_id);
};

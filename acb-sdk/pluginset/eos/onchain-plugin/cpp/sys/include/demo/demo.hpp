#pragma once

#include <eosio/eosio.hpp>
#include <eosio/singleton.hpp>

using namespace eosio;
using namespace std;

class [[eosio::contract("demo")]] demo : public contract
{
public:
    using contract::contract;

    ACTION recvunmsg(const name &invoker, const string &sender_domain, const string &sender_id, const string &msg);

    ACTION recvmsg(const name &invoker, const string &sender_domain, const string &sender_id, const string &msg);

    ACTION setsdp(const name &invoker, const name &sdp_contract);

    ACTION sendunmsg(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg);

    ACTION sendmsg(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg);

private:
    typedef singleton<name("sdpcontract"), uint64_t> sdpcontract_record;
    typedef singleton<name("latestmsg"), std::string> latestmsg_record;
    typedef singleton<name("latestunmsg"), std::string> latestunmsg_record;

    sdpcontract_record sdpcontract_record_instance = sdpcontract_record(get_self(), get_self().value);
    latestmsg_record latestmsg_record_instance = latestmsg_record(get_self(), get_self().value);
    latestunmsg_record latestunmsg_record_instance = latestunmsg_record(get_self(), get_self().value);
};
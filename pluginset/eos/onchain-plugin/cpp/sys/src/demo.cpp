#include <eosio/print.hpp>
#include "../include/demo/demo.hpp"
#include "../include/utils/utils.hpp"
#include "../include/sys.sdp/sys.sdp.hpp"

using namespace crosschain;

ACTION demo::recvunmsg(const name &invoker, const std::string &sender_domain, const std::string &sender_id, const std::string &msg)
{
    require_auth(invoker);

    std::string s_id;
    BytesToHexString(sender_id.c_str(), sender_id.size(), s_id);
    print_f("invoker: % , sender_domain: % , sender_id: % , msg: % ", invoker.to_string(), sender_domain, s_id, msg);

    latestunmsg_record_instance.set(msg, invoker);
}

ACTION demo::recvmsg(const name &invoker, const string &sender_domain, const string &sender_id, const string &msg)
{
    require_auth(invoker);

    std::string s_id;
    BytesToHexString(sender_id.c_str(), sender_id.size(), s_id);
    print_f("invoker: % , sender_domain: % , sender_id: % , msg: % ", invoker.to_string(), sender_domain, s_id, msg);

    latestmsg_record_instance.set(msg, invoker);
}

ACTION demo::setsdp(const name &invoker, const name &sdp_contract)
{
    require_auth(invoker);
    sdpcontract_record_instance.set(sdp_contract.value, invoker);
}

ACTION demo::sendunmsg(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg)
{
    require_auth(invoker);

    std::string raw_receiver(32, 0);
    check(HexStringToBytes(receiver_id.c_str(), raw_receiver.data(), raw_receiver.size()) != -1, "failed to convert receiver from hex string");

    check(sdpcontract_record_instance.exists(), "sdp contract not set");

    syssdp::sendunmsg_action sendunmsg(sdpcontract_record_instance.get(), {get_self(), "active"_n});
    sendunmsg.send(
        get_self(),
        receiver_domain,
        raw_receiver,
        msg);
}

ACTION demo::sendmsg(const name &invoker, const string &receiver_domain, const string &receiver_id, const string &msg)
{
    require_auth(invoker);

    std::string raw_receiver(32, 0);
    check(HexStringToBytes(receiver_id.c_str(), raw_receiver.data(), raw_receiver.size()) != -1, "failed to convert receiver from hex string");

    check(sdpcontract_record_instance.exists(), "sdp contract not set");

    syssdp::sendmsg_action sendmsg(sdpcontract_record_instance.get(), {get_self(), "active"_n});
    sendmsg.send(
        get_self(),
        receiver_domain,
        raw_receiver,
        msg);
}
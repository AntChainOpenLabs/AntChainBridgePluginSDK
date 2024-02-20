#pragma once

#include <eosio/eosio.hpp>
#include <vector>
#include "../utils/type_to_bytes.hpp"

using namespace eosio;
using namespace std;
using namespace crosschain;

// 合约基本信息表的唯一键值
#define AM_INFO_TABLE_KEY 1
#define PROTOCOL_CALLBACK_ACTION "recvmsg"_n

class [[eosio::contract("sys.am")]] sysam : public contract
{
public:
  using contract::contract;

  ACTION init(const name &initializer);

  ACTION addrelayer(const name &invoker, const name &relayer_account);

  ACTION setprotocol(const name &invoker, const name &protocol_account, const uint32_t &protocol_type);

  ACTION rmprotocol(const name &invoker, const uint32_t &protocol_type);

  // recv from protocol
  ACTION recvprotocol(const name &invoker, const name &sender_account, const string &msg);

  // recv from relayer
  ACTION recvrelayer(const name &invoker, const string &pkg);

  ACTION recvrelayerx(const name &invoker, const string &pkg_hex);

  ACTION rejectamx(const name &invoker, const string &pkg_hex);

  // 跨链事件
  ACTION crossing(const name &invoker, const string &msg_hex);

  using recvprotocol_action = action_wrapper<"recvprotocol"_n, &sysam::recvprotocol>;

private:
  // 合约relayer信息表
  TABLE s_relayers
  {
    name relayer;
    uint64_t primary_key() const { return relayer.value; }
  };
  typedef multi_index<name("relayerinfo"), s_relayers> t_relayers;

  // protocol信息表 type -> account
  TABLE s_protocols_by_type
  {
    name protocol_account;
    uint32_t protocol_type;
    uint64_t primary_key() const { return (uint64_t)protocol_type; }
    uint64_t by_protocol_account() const { return protocol_account.value; }
  };
  typedef multi_index<name("prottype"), s_protocols_by_type, indexed_by<name("protacckey"), const_mem_fun<s_protocols_by_type, uint64_t, &s_protocols_by_type::by_protocol_account>>> t_protocols_by_type;

  t_relayers tbl_relayers = t_relayers(get_self(), get_self().value);
  t_protocols_by_type tbl_protocols_by_type = t_protocols_by_type(get_self(), get_self().value);

  uint32_t get_protocol_type_by_contract(const name &protocol_account);
  name get_protocol_account_by_type(uint32_t protocol_type);

  void only_relayers(const name &invoker);
  void only_protocols(const name &invoker);

  bool decode_proof(const std::string &raw_proof, std::string &domain, std::string &pkg);

  void parse_am_pkg(const string &pkg, string &sender_id, uint32_t &protocol_type, string &msg);
  void do_udag_proof_data(const string &proof, const string &hints, bool if_verify);
  void forward_am_pkg(const string &domain_name, const string &pkg, bool if_reject);
  void RecvPkgFromRelayerCore(const string &pkg, bool if_verify);
  void process_am(const string &pkg, bool if_reject);
};
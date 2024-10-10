#!/usr/bin/env python3

import json
import os
import sys
import argparse

def read_pem(file_path):
    """读取PEM文件内容并格式化为JSON字符串中的换行符"""
    try:
        with open(file_path, 'r') as f:
            pem_content = f.read()
        # Escape backslashes and double quotes
        pem_content = pem_content.replace('\\', '\\\\').replace('"', '\\"')
        # Replace newlines with \n
        # pem_content = pem_content.replace('\n', '\\n')
        return pem_content
    except Exception as e:
        print(f"Error reading PEM file {file_path}: {e}")
        return ""

def extract_org_from_peer(hostname):
    """
    从主机名中提取组织名。
    例如，peer0.org1.example.com -> org1.example.com
    """
    parts = hostname.split('.')
    if len(parts) < 4:
        print(f"Unexpected peer hostname format: {hostname}")
        return ""
    return '.'.join(parts[1:])

def extract_org_from_orderer(orderer_location):
    """
    从ordererLocation中提取组织名。
    例如，grpcs://orderer.example.com:7050 -> example.com
    """
    try:
        host_port = orderer_location.split('://')[1]
        host = host_port.split(':')[0]
        parts = host.split('.')
        if len(parts) < 3:
            print(f"Unexpected orderer hostname format: {host}")
            return ""
        return '.'.join(parts[1:])  # example.com
    except IndexError:
        print(f"Unexpected ordererLocation format: {orderer_location}")
        return ""

def update_peers(conf_data, section, crypto_config):
    """
    更新discoveryPeers或validatorPeers中的pemBytes和signCert字段
    """
    peers = conf_data.get(section, [])
    for peer in peers:
        peer_props = peer.get('peerProperties', {})
        ssl_target = peer_props.get('ssl-target-name-override', '')
        if not ssl_target:
            print("Missing ssl-target-name-override, skipping peer.")
            continue

        org = extract_org_from_peer(ssl_target)
        if not org:
            print(f"Could not extract org from {ssl_target}, skipping.")
            continue

        # 构建TLS CA证书路径
        tlsca_cert_path = os.path.join(crypto_config, 'peerOrganizations', org, 'peers', ssl_target, 'tls', 'ca.crt')
        if not os.path.isfile(tlsca_cert_path):
            print(f"Warning: TLS CA cert not found for org {org} at {tlsca_cert_path}")
            continue

        # 读取并格式化pemBytes
        pem_bytes = read_pem(tlsca_cert_path)
        peer_props['pemBytes'] = pem_bytes

        print(f"Updated pemBytes for {ssl_target} in {section}")

        if section == "validatorPeers":
            # 对于validatorPeers，还需要更新signCert字段
            signcert_path = os.path.join(crypto_config, 'peerOrganizations', org, 'peers', ssl_target, 'msp', 'signcerts', f'{ssl_target}-cert.pem')
            if not os.path.isfile(signcert_path):
                print(f"Warning: signCert not found for {ssl_target} at {signcert_path}")
                continue

            sign_cert = read_pem(signcert_path)
            peer_props['signCert'] = sign_cert

            print(f"Updated signCert for {ssl_target} in {section}")

def update_orderers(conf_data, crypto_config):
    """
    更新orderers中的pemBytes字段
    """
    orderers = conf_data.get('orderers', [])
    for orderer in orderers:
        orderer_props = orderer.get('ordererProperties', {})
        orderer_location = orderer.get('ordererLocation', '')
        if not orderer_location:
            print("Missing ordererLocation, skipping orderer.")
            continue

        org = extract_org_from_orderer(orderer_location)
        if not org:
            print(f"Could not extract org from {orderer_location}, skipping.")
            continue

        # 构建TLS CA证书路径
        tlsca_cert_path = os.path.join(crypto_config, 'ordererOrganizations', org, 'tlsca', f'tlsca.{org}-cert.pem')
        if not os.path.isfile(tlsca_cert_path):
            print(f"Warning: TLS CA cert not found for orderer org {org} at {tlsca_cert_path}")
            continue

        # 读取并格式化pemBytes
        pem_bytes = read_pem(tlsca_cert_path)
        orderer_props['pemBytes'] = pem_bytes

        print(f"Updated pemBytes for orderer {org} in orderers")

def update_user_cert_and_key(conf_data, crypto_config):
    """
    更新conf.json中user字段的cert和key内容
    :param conf_data: 解析的conf.json数据
    :param crypto_config: crypto-config目录的路径
    """
    user = conf_data.get('user', {})
    if not user:
        print("No 'user' section found in conf.json")
        return

    # 从user字段中提取组织和用户名
    org = user.get('mspId', '').lower().replace('msp', '') + '.example.com'  # 组织名称，假设域名为 example.com
    username = user.get('name', '')
    if not org or not username:
        print("Invalid 'user' section, missing 'mspId' or 'name'")
        return

    # 构建cert和key文件路径
    cert_path = os.path.join(crypto_config, 'peerOrganizations', org, 'users', username, 'msp', 'signcerts', f'{username}-cert.pem')
    key_path = os.path.join(crypto_config, 'peerOrganizations', org, 'users', username, 'msp', 'keystore')

    # 获取key文件夹中的私钥文件（通常只有一个文件）
    try:
        key_file = next(f for f in os.listdir(key_path) if f.endswith('_sk'))
        key_path = os.path.join(key_path, key_file)
    except StopIteration:
        print(f"Could not find private key file in {key_path}")
        return

    # 读取并更新cert
    if os.path.isfile(cert_path):
        user['cert'] = read_pem(cert_path)
        print(f"Updated cert for user {username} in user section")
    else:
        print(f"Cert file not found at {cert_path}")

    # 读取并更新key
    if os.path.isfile(key_path):
        user['key'] = read_pem(key_path)
        print(f"Updated key for user {username} in user section")
    else:
        print(f"Private key file not found at {key_path}")
        
def main():
    parser = argparse.ArgumentParser(description="自动填充conf.json中的pemBytes、signCert以及user的cert和key字段")
    parser.add_argument('conf_json', help="conf.json文件的路径")
    parser.add_argument('crypto_config', help="crypto-config目录的路径")
    args = parser.parse_args()

    conf_json_path = args.conf_json
    crypto_config_path = args.crypto_config

    # 检查conf.json是否存在
    if not os.path.isfile(conf_json_path):
        print(f"Error: {conf_json_path} does not exist.")
        sys.exit(1)

    # 检查crypto-config目录是否存在
    if not os.path.isdir(crypto_config_path):
        print(f"Error: {crypto_config_path} directory does not exist.")
        sys.exit(1)

    # 读取conf.json
    try:
        with open(conf_json_path, 'r') as f:
            conf_data = json.load(f)
    except Exception as e:
        print(f"Error reading conf.json: {e}")
        sys.exit(1)

    # 更新discoveryPeers
    if 'discoveryPeers' in conf_data:
        update_peers(conf_data, 'discoveryPeers', crypto_config_path)
    else:
        print("No discoveryPeers found in conf.json")

    # 更新validatorPeers
    if 'validatorPeers' in conf_data:
        update_peers(conf_data, 'validatorPeers', crypto_config_path)
    else:
        print("No validatorPeers found in conf.json")

    # 更新orderers
    if 'orderers' in conf_data:
        update_orderers(conf_data, crypto_config_path)
    else:
        print("No orderers found in conf.json")

    # 更新用户的cert和key字段
    update_user_cert_and_key(conf_data, crypto_config_path)

    # 写回conf.json
    try:
        with open(conf_json_path, 'w') as f:
            json.dump(conf_data, f, indent=2)
        print("conf.json has been successfully updated with pemBytes, signCert, cert, and key.")
    except Exception as e:
        print(f"Error writing to conf.json: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
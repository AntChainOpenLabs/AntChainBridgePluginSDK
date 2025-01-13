/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.commons.exception;

import lombok.Getter;

/**
 * Error code for {@code antchain-bridge-relayer}
 *
 * <p>
 *     The {@code errorCode} field supposed to be hex and has two bytes.
 *     First byte represents the space code for project.
 *     Last byte represents the specific error scenarios.
 * </p>
 *
 * <p>
 *     Space code interval for {@code antchain-bridge-commons} is from 00 to 3f.
 * </p>
 */
@Getter
public enum RelayerErrorCodeEnum {

    /**
     *
     */
    DAL_ANCHOR_HEIGHTS_ERROR("0101", "wrong heights state"),

    DAL_BLOCKCHAIN_ERROR("0102", "wrong blockchain state"),

    DAL_PLUGINSERVER_ERROR("0103", "wrong pluginserver state"),

    DAL_CROSSCHAIN_MSG_ERROR("0104", "crosschain message error"),

    DAL_RELAYER_NETWORK_ERROR("0105", "relayer net data error"),

    DAL_RELAYER_NODE_ERROR("0106", "relayer node data error"),

    DAL_DT_ACTIVE_NODE_ERROR("0107", "dt active node data error"),

    DAL_DT_TASK_ERROR("0108", "distributed task data error"),

    DAL_CROSSCHAIN_MSG_ACL_ERROR("0109", "acl data error"),

    DAL_SYSTEM_CONFIG_ERROR("010a", "sys config data error"),

    DAL_DOMAIN_SPACE_ERROR("010b", "domain space cert data error"),

    DAL_BCDNS_ERROR("010c", "bcdns data error"),

    DAL_PTC_SERVICE_ERROR("010d", "ptc service data error"),

    CORE_BLOCKCHAIN_ERROR("0201", "blockchain error"),

    CORE_BLOCKCHAIN_CLIENT_INIT_ERROR("0202", "blockchain client init error"),

    CORE_PLUGIN_SERVER_ERROR("0203", "plugin server error"),

    CORE_BBC_CALL_ERROR("0204", "call bbc error"),

    CORE_RELAYER_NETWORK_ERROR("0205", "relayer network error"),

    CORE_BCDNS_MANAGER_ERROR("0206", "bcdns manager error"),

    CORE_UNKNOWN_RELAYER_FOR_DEST_DOMAIN("0207", "unknown relayer for dest domain error"),

    CORE_RELAYER_HANDSHAKE_FAILED("0208", "relayer handshake failed"),

    CORE_SEND_AM_TO_REMOTE_RELAYER_FAILED("0209", "send am to relayer failed"),

    CORE_CREATE_NEW_CROSSCHAIN_CHANNEL_FAILED("0210", "create new crosschain channel failed"),

    CORE_CROSSCHAIN_CHANNEL_NOT_EXIST_DOMAIN("0211", "crosschain channel not exist"),

    CORE_GOV_ACL_ERROR("0212", "acl error"),

    CORE_ADD_DOMAIN_ROUTER_ERROR("0213", "add domain router error"),

    CORE_PTC_SERVICE_MANAGE_ERROR("0214", "failed to manage ptc service"),

    CORE_PTC_SERVICE_TYPE_NOT_SUPPORT("0215", "ptc service type not support"),

    CORE_PTC_SERVICE_CONFIG_INVALID("0216", "ptc service config invalid"),

    CORE_PTC_SERVICE_VERIFY_CONSENSUS_STATE_FAILED("0217", "verify consensus state failed"),

    CORE_AM_PROCESS_ERROR("0218", "process am failed"),

    CORE_BBC_INTERFACE_NOT_SUPPORT("0219", "bbc interface not support"),

    SERVICE_CORE_PROCESS_AUTH_MSG_PROCESS_FAILED("0301", "process auth msg failed"),

    SERVICE_CORE_PROCESS_PROCESS_CCMSG_FAILED("0302", "process ccmsg failed"),

    SERVICE_COMMITTER_PROCESS_CCMSG_FAILED("0303", "commit ccmsg failed"),

    SERVICE_COMMITTER_PROCESS_COMMIT_SDP_FAILED("0304", "commit ccmsg to blockchain failed"),

    SERVICE_MULTI_ANCHOR_PROCESS_START_ANCHOR_FAILED("0305", "start anchor for blockchain failed"),

    SERVICE_MULTI_ANCHOR_PROCESS_POLLING_TASK_FAILED("0306", "polling block task failed"),

    SERVICE_MULTI_ANCHOR_PROCESS_SYNC_TASK_FAILED("0307", "sync block task failed"),

    SERVICE_MULTI_ANCHOR_PROCESS_REMOTE_AM_PROCESS_FAILED("0308", "remote am process failed"),

    SERVICE_ARCHIVE_PRECESS_FAILED("0309", "archive process failed"),

    SERVICE_MULTI_ANCHOR_PROCESS_NOTIFY_TASK_FAILED("030a", "notify task failed"),

    SERVICE_VALIDATION_UCP_VERIFY_EXCEPTION("030b", "verify ucp failed"),

    SERVER_REQUEST_FROM_RELAYER_REJECT("0401", "relayer request rejected"),

    SERVER_ADMIN_COMMAND_NOT_EXIST("0402", "command not exist"),

    SERVER_ADMIN_UNEXPECTED_ERROR("0403", "unexpected error"),

    SERVER_RELAYER_HELLO_ERROR("0404", "relayer hello error"),

    SERVICE_VALIDATION_BS_VERIFY_EXCEPTION("0501", "verify block state failed"),

    SERVICE_HANDLE_TIMEOUT_MESSAGE_EXCEPTION("0502", "handle timeout message failed"),

    /**
     *
     */
    UNKNOWN_INTERNAL_ERROR("0001", "internal error");

    /**
     * Error code for errors happened in project {@code antchain-bridge-relayer}
     */
    private final String errorCode;

    /**
     * Every code has a short message to describe the error stuff
     */
    private final String shortMsg;

    RelayerErrorCodeEnum(String errorCode, String shortMsg) {
        this.errorCode = errorCode;
        this.shortMsg = shortMsg;
    }
}

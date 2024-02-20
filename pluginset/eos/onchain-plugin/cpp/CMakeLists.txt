# This file is intended to help including headers

cmake_minimum_required(VERSION 3.19)

project(acb_bbc_eos VERSION "0.1")

set(CMAKE_CXX_STANDARD 11)

if (NOT EOS_INCLUDE_PATH)
    MESSAGE(FATAL_ERROR "you need to set -DEOS_INCLUDE_PATH")
endif ()

MESSAGE(STATUS "your EOS include path is ${EOS_INCLUDE_PATH}")

FILE(GLOB_RECURSE SYS_SRCS
        "${CMAKE_SOURCE_DIR}/*.cpp"
        "${CMAKE_SOURCE_DIR}/*.hpp"
)

ADD_LIBRARY(
        SRC SHARED
        ${SYS_SRCS}
)

TARGET_INCLUDE_DIRECTORIES(
        SRC PRIVATE
        ${EOS_INCLUDE_PATH}/eosiolib/capi
        ${EOS_INCLUDE_PATH}/eosiolib/contracts
        ${EOS_INCLUDE_PATH}/eosiolib/core
        ${EOS_INCLUDE_PATH}/eosiolib/native
)
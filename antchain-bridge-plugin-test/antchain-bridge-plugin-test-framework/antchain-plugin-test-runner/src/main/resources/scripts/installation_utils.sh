#!/bin/bash

check_installation() {
    for package in "$@"; do
        dpkg -l | grep -q "$package"
        if [ $? -eq 0 ]; then
            echo "$package is already installed."
        else
            echo "$package is not installed. Installing..."
            apt install -y "$package"
        fi
    done
}
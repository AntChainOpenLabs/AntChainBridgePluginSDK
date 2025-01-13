#!/bin/bash

#
# Copyright 2023 Ant Group
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo "install ACB SDK ..."
git clone -b release/v0.2.3 https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK.git
cd AntChainBridgePluginSDK
mvn install -Dmaven.test.skip=true
echo "install ACB SDK finished"
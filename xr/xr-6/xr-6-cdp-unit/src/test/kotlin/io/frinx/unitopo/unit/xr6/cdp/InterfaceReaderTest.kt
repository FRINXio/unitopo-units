/*
 * Copyright © 2018 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.xr6.cdp

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.InterfaceKey

class InterfaceReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/cdp-oper.xml")

    @Test
    fun testAllIds() {
        val expected = listOf("GigabitEthernet0/0/0/3", "MgmtEth0/0/CPU0/0", "GigabitEthernet0/0/0/2")
                .map { InterfaceKey(it) }
        val real = InterfaceReader.parseInterfaceIds(
                InterfaceReader.parseInterfaces(parseGetCfgResponse(DATA_NODES, InterfaceReader.CDP_OPER)))
        Assert.assertEquals(expected.size, real.size)
        Assert.assertTrue(expected.containsAll(real))
    }
}
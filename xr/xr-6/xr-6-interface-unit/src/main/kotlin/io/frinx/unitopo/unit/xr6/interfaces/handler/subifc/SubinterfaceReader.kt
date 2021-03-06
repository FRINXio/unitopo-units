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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc

import io.frinx.unitopo.ifc.base.handler.subinterfaces.AbstractSubinterfaceReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.Util
import io.frinx.unitopo.unit.xr6.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150730._interface.properties.DataNodes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Ipv6AddressReader as Ipv6AddressRev150730Reader
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303.Ipv6AddressConfigReader as Ipv6AddressRev170303Reader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey as Ipv4AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.addresses.AddressKey as Ipv6AddressKey

class SubinterfaceReader(underlayAccess: UnderlayAccess) : AbstractSubinterfaceReader<DataNodes>(underlayAccess) {

    private val ifaceReader = InterfaceReader(underlayAccess)

    override fun readIid(ifcName: String): InstanceIdentifier<DataNodes> = InterfaceReader.DATA_NODES_ID

    override val readDSType: LogicalDatastoreType = LogicalDatastoreType.OPERATIONAL

    override fun parseSubInterfaceIds(data: DataNodes, ifcName: String): List<SubinterfaceKey> {
        val subIfcKeys = ifaceReader.getInterfaceIds()
            .filter { Util.isSubinterface(it.name) }
            .filter { it.name.startsWith(ifcName) }
            .map { Util.getSubinterfaceKey(it.name) }

        val ipv4Keys = mutableListOf<Ipv4AddressKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName) { Ipv4AddressReader.extractAddresses(it, ipv4Keys) }

        val ipv6Keys = mutableListOf<Ipv6AddressKey>()
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName) {
            Ipv6AddressRev150730Reader.extractAddresses(it, ipv6Keys)
        }
        InterfaceReader.readInterfaceCfg(underlayAccess, ifcName) {
            Ipv6AddressRev170303Reader.extractAddresses(it, ipv6Keys)
        }

        return if (ipv4Keys.isNotEmpty() || ipv6Keys.isNotEmpty())
            subIfcKeys.plus(SubinterfaceKey(Util.ZERO_SUBINTERFACE_ID)) else
            subIfcKeys
    }
}
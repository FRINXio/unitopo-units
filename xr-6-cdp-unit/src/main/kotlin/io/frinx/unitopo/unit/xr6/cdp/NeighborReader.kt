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

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.cdp.oper.rev150730.cdp.nodes.node.neighbors.summaries.Summary
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp._interface.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.NeighborsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.lldp.rev160516.lldp.neighbor.top.neighbors.NeighborKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborReader(private val underlayAccess: UnderlayAccess) : OperListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder> {

    override fun merge(builder: Builder<out DataObject>, readData: MutableList<Neighbor>) {
        (builder as NeighborsBuilder).neighbor = readData
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<Neighbor>, builder: NeighborBuilder, ctx: ReadContext) {
        builder.id = id.firstKeyOf(Neighbor::class.java).id
    }

    override fun getAllIds(id: InstanceIdentifier<Neighbor>, context: ReadContext): List<NeighborKey> {
        return parseDeviceIds(InterfaceReader.readInterfaceNeighbors(underlayAccess, id.firstKeyOf(Interface::class.java).name))
    }

    override fun getBuilder(id: InstanceIdentifier<Neighbor>) = NeighborBuilder()

    companion object {
        fun parseDeviceIds(summaries: List<Summary>): List<NeighborKey> {
            return summaries
                    .map { it.deviceId }
                    .map { NeighborKey(it) }
                    .toList()
        }
    }
}

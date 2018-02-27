/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.handlers.ospf.OspfWriter
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaConfigWriter.Companion.getAreaAddressIdentifier
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaConfigWriter.Companion.getAreaIdIdentifier
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.NameScopes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.name.scopes.NameScope
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.name.scopes.NameScopeBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.area.table.area.addresses.area.content.name.scopes.NameScopeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.DEFAULTINSTANCE
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.types.rev170228.L3VRF
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.types.rev160512.OSPF
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AreaInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : OspfWriter<Config> {

    override fun updateCurrentAttributesForType(iid: IID<Config>, dataBefore: Config, dataAfter: Config, writeContext: WriteContext) {
        deleteCurrentAttributesForType(iid, dataBefore, writeContext)
        writeCurrentAttributesForType(iid, dataAfter, writeContext)
    }

    override fun writeCurrentAttributesForType(id: IID<Config>, dataAfter: Config, wtx: WriteContext) {
        wtx.readAfter(IIDs.NETWORKINSTANCES).orNull()
                ?.networkInstance.orEmpty()
                .filter { it.config?.type == L3VRF::class.java || it.config?.type == DEFAULTINSTANCE::class.java }
                .flatMap { it.protocols?.protocol.orEmpty() }
                .filter { it.config?.identifier == OSPF::class.java }
                .flatMap { it.ospfv2?.areas?.area.orEmpty().flatMap { it.interfaces?.`interface`.orEmpty() } }

        val (underlayId, underlayData) = getData(id, dataAfter)

        underlayAccess.merge(underlayId, underlayData)
    }

    override fun deleteCurrentAttributesForType(id: IID<Config>, dataBefore: Config, wtx: WriteContext) {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val ifaceIid = getNameScopeIdentifier(processIid, vrfName, id)

        underlayAccess.delete(ifaceIid)
    }

    private fun getData(id: IID<Config>, data: Config): Pair<IID<NameScope>, NameScope> {
        val (processIid, vrfName) = GlobalConfigWriter.getIdentifiers(id)
        val ifName = id.firstKeyOf(Interface::class.java).id
        val cost = data.metric?.value
        val ifaceIid = getNameScopeIdentifier(processIid, vrfName, id)

        val iface = NameScopeBuilder()
                .setKey(NameScopeKey(InterfaceName(ifName)))
                .setRunning(true).let {
            if (cost != null) {
                it.cost = cost.toLong()
            }
            it
        }

        return Pair(ifaceIid, iface.build())
    }

    companion object {
        public fun getNameScopeIdentifier(processIid: IID<Process>, vrfName: String, iid: IID<Config>): IID<NameScope> {
            val areaId = iid.firstKeyOf(Area::class.java).identifier
            val ifName = iid.firstKeyOf(Interface::class.java).id

            return if (areaId.uint32 != null) {
                getAreaIdIdentifier(processIid, vrfName, areaId.uint32.toInt()).child(NameScopes::class.java)
                        .child(NameScope::class.java, NameScopeKey(InterfaceName(ifName)))
            } else {
                getAreaAddressIdentifier(processIid, vrfName, areaId.dottedQuad).child(NameScopes::class.java)
                        .child(NameScope::class.java, NameScopeKey(InterfaceName(ifName)))
            }
        }
    }
}

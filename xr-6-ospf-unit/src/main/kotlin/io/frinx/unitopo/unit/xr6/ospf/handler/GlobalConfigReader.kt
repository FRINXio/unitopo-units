/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.common.OspfReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.Ospf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.Processes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.ospf.cfg.rev151109.ospf.processes.Process
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.Config
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.ospfv2.rev170228.ospfv2.global.structural.global.ConfigBuilder
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class GlobalConfigReader(private val access: UnderlayAccess) : OspfReader.OspfConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(id: IID<Config>) = ConfigBuilder()

    override fun readCurrentAttributesForType(id: IID<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val vrfName = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)

        try {
            readProcess(access, protKey, { builder.fromUnderlay(it, vrfName.name) })
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(id, e)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: Config) {
        (parentBuilder as GlobalBuilder).config = readValue
    }

    companion object {
        private val UNDERLAY_OSPF_PROCESSES = IID.create(Ospf::class.java).child(Processes::class.java)

        fun readProcess(access: UnderlayAccess, protKey: ProtocolKey, handler: (Process) -> Unit) {
            getProcess(access, protKey)
                    ?.let(handler)
        }

        fun getProcess(access: UnderlayAccess, protKey: ProtocolKey): Process? {
            return access.read(UNDERLAY_OSPF_PROCESSES)
                    .checkedGet()
                    .orNull()
                    ?.process.orEmpty()
                    .find { it.processName.value == protKey.name }
        }

        fun getRouterId(vrfName: String, p: Process): DottedQuad? {
            // Set router ID for appropriate VRF
            var routerId: DottedQuad? = null
            if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                p.defaultVrf?.routerId?.value?.let { routerId = DottedQuad(it) }
            } else {
                p.vrfs?.vrf.orEmpty()
                        .find { it.vrfName.value == vrfName }
                        ?.let { routerId = DottedQuad(it.routerId.value) }
            }
            return routerId
        }
    }
}

private fun ConfigBuilder.fromUnderlay(p: Process, vrfName: String) {
    GlobalConfigReader.getRouterId(vrfName, p)?.let {
        routerId = it
    }
}

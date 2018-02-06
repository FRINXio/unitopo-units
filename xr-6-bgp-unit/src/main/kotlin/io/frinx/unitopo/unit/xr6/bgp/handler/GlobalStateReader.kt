/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.bgp.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.Bgp
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827._default.vrf.DefaultVrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.Instances
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.bgp.instances.instance.InstanceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.global.process.info.GlobalProcessInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.vrf.table.Vrfs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.vrf.table.vrfs.Vrf
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.oper.rev150827.vrf.table.vrfs.VrfKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.State
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.global.base.StateBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.top.bgp.GlobalBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.yang.rev170403.DottedQuad
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class GlobalStateReader(private val access: UnderlayAccess) : BgpReader.BgpOperReader<State, StateBuilder> {

    override fun getBuilder(id: InstanceIdentifier<State>) = StateBuilder()

    override fun readCurrentAttributesForType(id: InstanceIdentifier<State>, builder: StateBuilder, ctx: ReadContext) {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(getId(protKey, vrfKey), LogicalDatastoreType.OPERATIONAL)
                .checkedGet()
                .orNull()

        builder.fromUnderlay(data)
    }

    override fun merge(parentBuilder: Builder<out DataObject>, readValue: State) {
        (parentBuilder as GlobalBuilder).state = readValue
    }

    companion object {

        fun getId(protKey: ProtocolKey, vrfName: NetworkInstanceKey): InstanceIdentifier<GlobalProcessInfo> {
            return if (vrfName == NetworInstance.DEFAULT_NETWORK) {
                InstanceIdentifier.create(Bgp::class.java)
                        .child(Instances::class.java)
                        .child(Instance::class.java, InstanceKey(CiscoIosXrString(protKey.name)))
                        .child(InstanceActive::class.java)
                        .child(DefaultVrf::class.java)
                        .child(GlobalProcessInfo::class.java)
            } else {
                return InstanceIdentifier.create(Bgp::class.java)
                        .child(Instances::class.java)
                        .child(Instance::class.java, InstanceKey(CiscoIosXrString(protKey.name)))
                        .child(InstanceActive::class.java)
                        .child(Vrfs::class.java)
                        .child(Vrf::class.java, VrfKey(CiscoIosXrString(vrfName.name)))
                        .child(GlobalProcessInfo::class.java)
            }

        }
    }
}

@VisibleForTesting
fun StateBuilder.fromUnderlay(data: GlobalProcessInfo?) {
    data?.let {
        it.global?.let {
            `as`= AsNumber(it.localAs)
        }
        it.vrf?.let {
            routerId = DottedQuad(it.routerId.value)
        }
    }
}
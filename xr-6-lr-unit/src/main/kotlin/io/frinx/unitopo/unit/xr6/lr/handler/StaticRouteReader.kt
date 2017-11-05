/*
 * Copyright © 2017 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.lr.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.lr.common.LrListReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.RouterStatic
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.address.family.AddressFamily
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.prefix.table.VrfPrefixes
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev150910.vrf.prefix.table.vrf.prefixes.VrfPrefix
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top.StaticRoutesBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.Static
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticBuilder
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev170515.local._static.top._static.routes.StaticKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.IpPrefix
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.Ipv4Prefix
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.types.inet.rev170403.Ipv6Prefix
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class StaticRouteReader(private val access: UnderlayAccess) : LrListReader.LrConfigListReader<Static, StaticKey, StaticBuilder> {

    override fun getAllIdsForType(id: InstanceIdentifier<Static>, context: ReadContext): List<StaticKey> {
        val vrfName = id.firstKeyOf<NetworkInstance, NetworkInstanceKey>(NetworkInstance::class.java).`name`
        return getStaticKeys(getAddressFamily(access, vrfName))
    }

    override fun merge(builder: Builder<out DataObject>, readData: List<Static>) {
        (builder as StaticRoutesBuilder).static = readData
    }

    override fun getBuilder(id: InstanceIdentifier<Static>): StaticBuilder = StaticBuilder()

    override fun readCurrentAttributesForType(id: InstanceIdentifier<Static>, builder: StaticBuilder, ctx: ReadContext) {
        val protKey = id.firstKeyOf(Protocol::class.java)
        val prefix = id.firstKeyOf(Static::class.java).prefix
        builder.prefix = prefix
    }

    companion object {

        @VisibleForTesting
        val ROUTE_STATIC_IID = InstanceIdentifier.create(RouterStatic::class.java)!!

        fun getAddressFamily(access: UnderlayAccess, vrfName: String): AddressFamily? {
            return access.read(ROUTE_STATIC_IID).checkedGet().orNull()?.let {
                if (NetworInstance.DEFAULT_NETWORK_NAME == vrfName) {
                    it.defaultVrf?.addressFamily
                } else {
                    it.vrfs?.vrf.orEmpty()
                            .find { it.vrfName.value == vrfName }
                            ?.addressFamily
                }
            }
        }

        @VisibleForTesting
        fun getStaticKeys(family: AddressFamily?) : List<StaticKey> {
           val keys = ArrayList<StaticKey>()
           family?.let {
                findKeys(keys, it.vrfipv4?.vrfUnicast?.vrfPrefixes)
                findKeys(keys, it.vrfipv4?.vrfMulticast?.vrfPrefixes)
                findKeys(keys, it.vrfipv6?.vrfUnicast?.vrfPrefixes)
                findKeys(keys, it.vrfipv6?.vrfMulticast?.vrfPrefixes)
            }
            return keys
        }

        private fun findKeys(keys: MutableList<StaticKey>, prefixes: VrfPrefixes?) {
            keys.addAll(prefixes?.vrfPrefix.orEmpty().map(this::convertVrfKeyToStaticKey))
        }

        private fun convertVrfKeyToStaticKey(prefix: VrfPrefix) = StaticKey(prefix.ipAddressToPrefix())
    }
}


fun VrfPrefix.ipAddressToPrefix() : IpPrefix {
    prefix.ipv4AddressNoZone?.let {
        return IpPrefix(Ipv4Prefix(StringBuilder(prefix.ipv4AddressNoZone.value).append("/").append(prefixLength).toString()))
    }
    return IpPrefix(Ipv6Prefix(StringBuilder(prefix.ipv6AddressNoZone.value).append("/").append(prefixLength).toString()))
}
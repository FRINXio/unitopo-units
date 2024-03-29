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

package io.frinx.unitopo.unit.junos17.bgp.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.PeerType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.IpAddress
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.Ipv4Address
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException as MdSalReadFailedException
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group as JunosGroup
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.Group.Type as JunosGroupType
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.protocols.bgp.group.Neighbor as JunosNeighbor

class BgpNeighborConfigReader(private val access: UnderlayAccess) : ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(id: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<Config>,
        builder: ConfigBuilder,
        ctx: ReadContext
    ) {
        try {
            val neighborAddress = id.firstKeyOf(Neighbor::class.java).neighborAddress.ipv4Address.value
            access.read(BgpProtocolReader.UNDERLAY_PROTOCOL_BGP).checkedGet().orNull()
                    ?.group.orEmpty()
                        .forEach { group: JunosGroup? -> group?.neighbor
                            ?.find { neighbor -> neighbor.name.value == neighborAddress }
                                ?.let { builder.fromUnderlay(it, group) } }
        } catch (e: MdSalReadFailedException) {
            throw ReadFailedException(id, e)
        }
    }

    override fun merge(parentBuilder: Builder<out DataObject>, data: Config) {
        (parentBuilder as NeighborBuilder).config = data
    }
}

@VisibleForTesting
fun ConfigBuilder.fromUnderlay(underlay: JunosNeighbor, group: JunosGroup) {
    isEnabled = true // neighbor must exist if this function is called
    neighborAddress = IpAddress(Ipv4Address(underlay.name?.value))
    peerAs = AsNumber(underlay.peerAs?.toLongOrNull())
    peerGroup = group.name
    peerType = parsePeerType(group.type)
}

fun parsePeerType(type: JunosGroupType?): PeerType? {
    return when (type) {
        JunosGroupType.Internal -> PeerType.INTERNAL
        JunosGroupType.External -> PeerType.EXTERNAL
        else -> null
    }
}
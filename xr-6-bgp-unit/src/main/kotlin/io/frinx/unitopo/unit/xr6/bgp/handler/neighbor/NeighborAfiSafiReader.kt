package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpListReader
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import io.frinx.unitopo.unit.xr6.bgp.handler.toOpenconfig
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafi
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.AfiSafiKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.afi.safi.list.afi.safi.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.AfiSafisBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborAfiSafiReader(private val access: UnderlayAccess) : BgpListReader.BgpConfigListReader<AfiSafi, AfiSafiKey, AfiSafiBuilder> {

    override fun getAllIdsForType(id: InstanceIdentifier<AfiSafi>, readContext: ReadContext): List<AfiSafiKey> {
        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val neighborKey = id.firstKeyOf(Neighbor::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        return parseAfiSafis(data, vrfKey, neighborKey)
    }

    override fun merge(builder: Builder<out DataObject>, list: List<AfiSafi>) {
        (builder as AfiSafisBuilder).afiSafi = list
    }

    override fun readCurrentAttributesForType(id: InstanceIdentifier<AfiSafi>, afiSafiBuilder: AfiSafiBuilder, readContext: ReadContext) {
        afiSafiBuilder.afiSafiName = id.firstKeyOf(AfiSafi::class.java).afiSafiName
        afiSafiBuilder.config = ConfigBuilder()
                .setAfiSafiName(afiSafiBuilder.afiSafiName)
                .build()
    }

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<AfiSafi>) = AfiSafiBuilder()

    companion object {
        fun parseAfiSafis(data: Instance?, vrfKey: NetworkInstanceKey, neighborKey: NeighborKey): List<AfiSafiKey> {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(data)

            val afs = if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                NeighborConfigReader.getNeighbor(fourByteAs, neighborKey)
                        ?.neighborAfs
                        ?.neighborAf.orEmpty()
                        .map { it.afName }

            } else {
                NeighborConfigReader.getVrfNeighbor(fourByteAs, vrfKey, neighborKey)
                        ?.vrfNeighborAfs
                        ?.vrfNeighborAf.orEmpty()
                        .map { it.afName }
            }

            return afs
                    .map { it.toOpenconfig() }
                    .filterNotNull()
                    .map { AfiSafiKey(it) }
                    .toList()
        }
    }
}
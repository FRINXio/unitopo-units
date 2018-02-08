package io.frinx.unitopo.unit.xr6.bgp.handler.neighbor

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.openconfig.network.instance.NetworInstance
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.network.instance.protocol.bgp.common.BgpReader
import io.frinx.unitopo.unit.xr6.bgp.UnderlayNeighbor
import io.frinx.unitopo.unit.xr6.bgp.UnderlayVrfNeighbor
import io.frinx.unitopo.unit.xr6.bgp.common.As.Companion.asFromDotNotation
import io.frinx.unitopo.unit.xr6.bgp.handler.BgpProtocolReader
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.REMOTEAS
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.SHUTDOWN
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.Instance
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.InstanceKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.bgp.cfg.rev150827.bgp.instance.instance.`as`.FourByteAs
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.base.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.Neighbor
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.rev170202.bgp.neighbor.list.NeighborKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.ProtocolKey
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NeighborConfigReader(private val access: UnderlayAccess) : BgpReader.BgpConfigReader<Config, ConfigBuilder> {

    override fun merge(parentBuilder: Builder<out DataObject>, config: Config) {
        (parentBuilder as NeighborBuilder).config = config
    }

    override fun getBuilder(p0: InstanceIdentifier<Config>) = ConfigBuilder()

    override fun readCurrentAttributesForType(id: InstanceIdentifier<Config>, builder: ConfigBuilder, readContext: ReadContext) {
        val neighborKey = id.firstKeyOf(Neighbor::class.java)
        builder.neighborAddress = neighborKey.neighborAddress

        val protKey = id.firstKeyOf<Protocol, ProtocolKey>(Protocol::class.java)
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)

        val data = access.read(BgpProtocolReader.UNDERLAY_BGP.child(Instance::class.java, InstanceKey(CiscoIosXrString(protKey.name))))
                .checkedGet()
                .orNull()

        parseNeighbor(data, vrfKey, neighborKey, builder)
    }

    companion object {
        fun parseNeighbor(underlayInstance: Instance?, vrfKey: NetworkInstanceKey, neighborKey: NeighborKey, builder: ConfigBuilder) {
            val fourByteAs = BgpProtocolReader.getFirst4ByteAs(underlayInstance)

            if (vrfKey == NetworInstance.DEFAULT_NETWORK) {
                getNeighbor(fourByteAs, neighborKey)
                        ?.let { builder.fromUnderlay(it) }
            } else {
                getVrfNeighbor(fourByteAs, vrfKey, neighborKey)
                        ?.let { builder.fromUnderlay(it) }
            }
        }

        fun getVrfNeighbor(fourByteAs: FourByteAs?, vrfKey: NetworkInstanceKey, neighborKey: NeighborKey) =
                NeighborReader.getVrfNeighbors(fourByteAs, vrfKey)
                        .find { it.neighborAddress == neighborKey.neighborAddress.toNoZone() }

        fun getNeighbor(fourByteAs: FourByteAs?, neighborKey: NeighborKey) =
                NeighborReader.getNeighbors(fourByteAs)
                        .find { it.neighborAddress == neighborKey.neighborAddress.toNoZone() }
    }
}

private fun ConfigBuilder.fromUnderlay(neighbor: UnderlayNeighbor?) {
    neighbor?.let {
        neighborAddress = neighbor.neighborAddress.toIp()
        fromCommonUnderlay(neighbor)
    }
}

private fun <T> ConfigBuilder.fromCommonUnderlay(neighbor: T?)
        where T : REMOTEAS,
              T : SHUTDOWN {
    neighbor?.let {
        isEnabled = true
        neighbor.isShutdown?.let {
            isEnabled = false
        }
        peerAs = asFromDotNotation(neighbor.remoteAs.asXx.value, neighbor.remoteAs.asYy.value)
    }
}

private fun ConfigBuilder.fromUnderlay(neighbor: UnderlayVrfNeighbor?) {
    neighbor?.let {
        neighborAddress = neighbor.neighborAddress.toIp()
        fromCommonUnderlay(neighbor)
    }
}

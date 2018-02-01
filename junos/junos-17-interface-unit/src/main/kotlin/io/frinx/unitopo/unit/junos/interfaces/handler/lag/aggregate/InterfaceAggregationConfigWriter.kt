/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler.lag.aggregate

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader.Companion.LAG_PREFIX
import io.frinx.unitopo.unit.junos.interfaces.handler.parseIfcType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.aggregation.logical.top.aggregation.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.IfLagJuniperAug
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.juniper.rev171024.JuniperIfAggregateConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions as JunosAggregEthOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions.MinimumLinks as JunosMinimumLinks
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptionsBuilder as JunosAggregEthOptionsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection as JunosBfdLivenessDetection
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetectionBuilder as JunosBfdLivenessDetectionBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.Family as JunosInterfaceUnitFamily
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.Inet as JunosInterfaceUnitFamilyInet
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitFamilyInetAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.AddressBuilder as JunosInterfaceUnitFamilyInetAddressBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.AddressKey as JunosInterfaceUnitFamilyInetAddressKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey


class InterfaceAggregationConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        val (underlayAggrEthOptId, underlayAggrEthOpt) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayAggrEthOptId),
                "Write: Aggregation Config is not supported for: %s", id)
        try {
            underlayAccess.put(underlayAggrEthOptId, underlayAggrEthOpt)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        val underlayAggrEthOptId = getUnderlayId(id)
        Preconditions.checkArgument(isSupportedForInterface(underlayAggrEthOptId),
                "Delete: Aggregation Config is not supported for: %s", id)
        try {
            underlayAccess.delete(underlayAggrEthOptId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>,
                                         dataBefore: Config, dataAfter: Config,
                                         writeContext: WriteContext) {
        val (underlayAggrEthOptId, underlayAggrEthOpt) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayAggrEthOptId),
                "Update: Aggregation Config is not supported for: %s", id)
        try {
             underlayAccess.merge(underlayAggrEthOptId, underlayAggrEthOpt)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosAggregEthOptions>, JunosAggregEthOptions> {
        val underlayAggrEthOptId = getUnderlayId(id)

        val aggregatedEtherOptionsBuilder = JunosAggregEthOptionsBuilder()
        aggregatedEtherOptionsBuilder.minimumLinks = JunosMinimumLinks(dataAfter.minLinks)
        aggregatedEtherOptionsBuilder.linkSpeed =
                parseLinkSpeedJunos(dataAfter.getAugmentation(IfLagJuniperAug::class.java)?.linkSpeed)

        return Pair(underlayAggrEthOptId, aggregatedEtherOptionsBuilder.build())
    }

    private fun parseLinkSpeedJunos(linkSpeed: JuniperIfAggregateConfig.LinkSpeed?): JunosAggregEthOptions.LinkSpeed? {
        return when (linkSpeed){
            JuniperIfAggregateConfig.LinkSpeed._10M -> JunosAggregEthOptions.LinkSpeed._10m
            JuniperIfAggregateConfig.LinkSpeed._100M -> JunosAggregEthOptions.LinkSpeed._100m
            JuniperIfAggregateConfig.LinkSpeed._1G -> JunosAggregEthOptions.LinkSpeed._1g
            JuniperIfAggregateConfig.LinkSpeed._2G -> JunosAggregEthOptions.LinkSpeed._2g
            JuniperIfAggregateConfig.LinkSpeed._5G -> JunosAggregEthOptions.LinkSpeed._5g
            JuniperIfAggregateConfig.LinkSpeed._8G -> JunosAggregEthOptions.LinkSpeed._8g
            JuniperIfAggregateConfig.LinkSpeed._10G -> JunosAggregEthOptions.LinkSpeed._10g
            JuniperIfAggregateConfig.LinkSpeed._25G -> JunosAggregEthOptions.LinkSpeed._25g
            JuniperIfAggregateConfig.LinkSpeed._40G -> JunosAggregEthOptions.LinkSpeed._40g
            JuniperIfAggregateConfig.LinkSpeed._50G -> JunosAggregEthOptions.LinkSpeed._50g
            JuniperIfAggregateConfig.LinkSpeed._80G -> JunosAggregEthOptions.LinkSpeed._80g
            JuniperIfAggregateConfig.LinkSpeed._100G -> JunosAggregEthOptions.LinkSpeed._100g
            JuniperIfAggregateConfig.LinkSpeed.OC192 -> JunosAggregEthOptions.LinkSpeed.Oc192
            JuniperIfAggregateConfig.LinkSpeed.MIXED -> JunosAggregEthOptions.LinkSpeed.Mixed
            else -> null
        }
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): InstanceIdentifier<JunosAggregEthOptions> {
        val ifcName = id.firstKeyOf(Interface::class.java).name.removePrefix(LAG_PREFIX)

        return InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosAggregEthOptions::class.java)
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<JunosAggregEthOptions>): Boolean {
        return when (parseIfcType(deviceId.firstKeyOf(JunosInterface::class.java).name)) {
            Ieee8023adLag::class.java -> true
            else -> false
        }
    }
}
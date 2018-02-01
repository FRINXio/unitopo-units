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
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bfd.rev171024.bfd.top.bfd.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Ipaddr
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.AggregatedEtherOptions as JunosAggregEthOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitBuilder as JunosInterfaceUnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitKey as JunosInterfaceUnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection as JunosBfdLivenessDetection
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection.MinimumInterval as JunosMinInterval
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetection.Multiplier as JunosMultiplier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.aggregated.ether.options.BfdLivenessDetectionBuilder as JunosBfdLivenessDetectionBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.Family as JunosInterfaceUnitFamily
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.Inet as JunosInterfaceUnitFamilyInet
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.Address as JunosInterfaceUnitFamilyInetAddress
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.AddressBuilder as JunosInterfaceUnitFamilyInetAddressBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.inet.AddressKey as JunosInterfaceUnitFamilyInetAddressKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder as JunosInterfaceBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceAggregationBfdConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, dataAfter: Config, writeContext: WriteContext) {
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayBfdLivenessDetectionId),
                "Write: Aggregation Bfd Config is not supported for: %s", id)
        try {
            underlayAccess.put(underlayBfdLivenessDetectionId, underlayBfdLivenessDetectionCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, config: Config, context: WriteContext) {
        val underlayBfdLivenessDetectionId = getUnderlayId(id)
        Preconditions.checkArgument(isSupportedForInterface(underlayBfdLivenessDetectionId),
                "Delete: Aggregation Bfd Config is not supported for: %s", id)
        try {
            underlayAccess.delete(underlayBfdLivenessDetectionId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(id: InstanceIdentifier<Config>,
                                         dataBefore: Config, dataAfter: Config,
                                         writeContext: WriteContext) {
        val (underlayBfdLivenessDetectionId, underlayBfdLivenessDetection) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayBfdLivenessDetectionId),
                "Update: Aggregation Bfd Config is not supported for: %s", id)
        try {
             underlayAccess.merge(underlayBfdLivenessDetectionId, underlayBfdLivenessDetection)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<JunosBfdLivenessDetection>, JunosBfdLivenessDetection> {
        val underlayBfdLivenessDetectionId = getUnderlayId(id)

        val bfdLivenessDetectionBuilder = JunosBfdLivenessDetectionBuilder()
        bfdLivenessDetectionBuilder.localAddress = Ipaddr(dataAfter.localAddress?.ipv4Address?.value)
        bfdLivenessDetectionBuilder.neighbor = Ipaddr(dataAfter.destinationAddress?.ipv4Address?.value)
        bfdLivenessDetectionBuilder.multiplier = JunosMultiplier(dataAfter.multiplier)
        bfdLivenessDetectionBuilder.minimumInterval = JunosMinInterval(dataAfter.minInterval)

        return Pair(underlayBfdLivenessDetectionId, bfdLivenessDetectionBuilder.build())
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config>): InstanceIdentifier<JunosBfdLivenessDetection> {
        val ifcName = id.firstKeyOf(Interface::class.java).name.removePrefix(LAG_PREFIX)

        return InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosAggregEthOptions::class.java)
                .child(JunosBfdLivenessDetection::class.java)
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<JunosBfdLivenessDetection>): Boolean {
        return when (parseIfcType(deviceId.firstKeyOf(JunosInterface::class.java).name)) {
            Ieee8023adLag::class.java -> true
            else -> false
        }
    }
}
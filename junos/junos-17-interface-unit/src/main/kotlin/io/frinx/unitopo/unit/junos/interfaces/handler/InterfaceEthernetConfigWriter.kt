/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos.interfaces.handler

import com.google.common.base.Preconditions
import io.fd.honeycomb.translate.read.ReadFailedException
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.aggregate.rev161222.Config1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Ieee8023adLag
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Other
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.GigetherOptions as JunosGigEthOptions
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023ad as JunosGigEthIeee8023ad
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023ad.Bundle as JunosGigEthIeee8023adBundle
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.gigether.options.Ieee8023adBuilder as JunosGigEthIeee8023adBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey as JunosInterfaceKey

class InterfaceEthernetConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config1> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config1>, dataAfter: Config1, writeContext: WriteContext) {
        val (underlayGigEthIeee8023adId, underlayGigEthIeee8023ad) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayGigEthIeee8023adId),
                "Write: Ethernet configuration is not supported for: %s", id)

        try {
            underlayAccess.put(underlayGigEthIeee8023adId, underlayGigEthIeee8023ad)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config1>,
                                         dataBefore: Config1,
                                         writeContext: WriteContext) {
        val (_, underlayGigEthIeee8023adId) = getUnderlayId(id)
        Preconditions.checkArgument(isSupportedForInterface(underlayGigEthIeee8023adId),
                "Delete: Ethernet configuration is not supported for: %s", id)

        try {
            underlayAccess.delete(underlayGigEthIeee8023adId)
        } catch (e: Exception) {
            throw ReadFailedException(id, e)
        }
    }

    override fun updateCurrentAttributes(id: InstanceIdentifier<Config1>,
                                         dataBefore: Config1, dataAfter: Config1,
                                         writeContext: WriteContext) {
        val (underlayGigEthIeee8023adId, underlayGigEthIeee8023ad) = getData(id, dataAfter)
        Preconditions.checkArgument(isSupportedForInterface(underlayGigEthIeee8023adId),
                "Update: Ethernet configuration is not supported for: %s", id)

        try {
            underlayAccess.merge(underlayGigEthIeee8023adId, underlayGigEthIeee8023ad)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config1>, dataAfter: Config1): Pair<InstanceIdentifier<JunosGigEthIeee8023ad>, JunosGigEthIeee8023ad> {
        val (_, underlayGigEthIeee8023adId) = getUnderlayId(id)

        val gigEthIeee8023adBuilder = JunosGigEthIeee8023adBuilder()
                .setBundle(JunosGigEthIeee8023adBundle(dataAfter.aggregateId.removePrefix(InterfaceReader.LAG_PREFIX)))
                .build()

        return Pair(underlayGigEthIeee8023adId, gigEthIeee8023adBuilder)
    }

    private fun getUnderlayId(id: InstanceIdentifier<Config1>): Pair<String, InstanceIdentifier<JunosGigEthIeee8023ad>> {
        val ifcName = id.firstKeyOf(Interface::class.java).name.removePrefix(InterfaceReader.LAG_PREFIX)
        val underlayGigEthIeee8023adId = InterfaceReader.IFCS.child(JunosInterface::class.java, JunosInterfaceKey(ifcName))
                .child(JunosGigEthOptions::class.java).child(JunosGigEthIeee8023ad::class.java)

        return Pair(ifcName, underlayGigEthIeee8023adId)
    }

    private fun isSupportedForInterface(deviceId: InstanceIdentifier<JunosGigEthIeee8023ad>): Boolean {
        return when (parseIfcType(deviceId.firstKeyOf(JunosInterface::class.java).name)) {
            Ieee8023adLag::class.java -> false
            Other::class.java -> false
            else -> true
        }
    }
}

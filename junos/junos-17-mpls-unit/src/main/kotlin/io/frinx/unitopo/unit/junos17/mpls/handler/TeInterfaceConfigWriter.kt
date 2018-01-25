/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.mpls.handler

import com.google.common.collect.Lists
import io.fd.honeycomb.translate.spi.write.WriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import io.fd.honeycomb.translate.write.WriteFailedException
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te._interface.attributes.top._interface.Config
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.FamilyBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.unit.family.MplsBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.mpls.rev170824.te._interface.attributes.top.Interface as OcInterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class TeInterfaceConfigWriter(private val underlayAccess: UnderlayAccess) : WriterCustomizer<Config> {

    override fun writeCurrentAttributes(id: InstanceIdentifier<Config>, data: Config, writeContext: WriteContext) {
        val (underlayId, underlayIfcCfg) = getData(id, data)

        try {
            underlayAccess.put(underlayId, underlayIfcCfg)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    override fun deleteCurrentAttributes(id: InstanceIdentifier<Config>, data: Config, writeContext: WriteContext) {
        val underlayId = getId(id)

        try {
            underlayAccess.delete(underlayId)
        } catch (e: Exception) {
            throw WriteFailedException(id, e)
        }
    }

    private fun getData(id: InstanceIdentifier<Config>, dataAfter: Config):
            Pair<InstanceIdentifier<Interface>, Interface> {
        val underlayId = getId(id)

        // TODO: are we sure we don't want to preserve existing units? If we do, how do we determine unit name?
        val unitBuilder = UnitBuilder().setName("0")
                .setFamily(FamilyBuilder().setMpls(MplsBuilder().build()).build())

        val underlayIfcCfg = InterfaceBuilder()
                .setName(dataAfter.interfaceId.value)
                .setUnit(Lists.newArrayList(unitBuilder.build()))
                .build()
        return Pair(underlayId, underlayIfcCfg)
    }

    private fun getId(id: InstanceIdentifier<Config>):
            InstanceIdentifier<Interface> {
        val ifcName = id.firstKeyOf(OcInterface::class.java).interfaceId.value
        return TeInterfaceReader.INTERFACES.child(Interface::class.java, InterfaceKey(ifcName))
    }
}
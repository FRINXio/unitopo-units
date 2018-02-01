/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.junos17.policy.forwarding.handler

import com.google.common.annotations.VisibleForTesting
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigListReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.InterfaceId
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.Interface as OcInterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.Configuration
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper._class.of.service.options.Interfaces
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper._class.of.service.options.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.ClassOfService
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class PolicyForwardingInterfaceReader(private val underlayAccess: UnderlayAccess) : ConfigListReaderCustomizer<OcInterface, InterfaceKey, InterfaceBuilder> {

    override fun getBuilder(p0: InstanceIdentifier<OcInterface>): InterfaceBuilder = InterfaceBuilder()

    override fun getAllIds(id: InstanceIdentifier<OcInterface>, context: ReadContext): List<InterfaceKey> = getInterfaceIds(underlayAccess)

    override fun merge(builder: Builder<out DataObject>, readData: List<OcInterface>) {
        (builder as InterfacesBuilder).`interface` = readData
    }

    override fun readCurrentAttributes(id: InstanceIdentifier<OcInterface>, builder: InterfaceBuilder,
                                       ctx: ReadContext) {
        readSpecificInterface(underlayAccess, id.firstKeyOf(OcInterface::class.java).interfaceId.value)?.let {
            builder.interfaceId = InterfaceId(it.name)
        }
    }

    companion object {

        val CLASS_OF_SERVICE = InstanceIdentifier.create(Configuration::class.java).child(ClassOfService::class.java).child(Interfaces::class.java)

        fun getInterfaceIds(underlayAccess: UnderlayAccess): List<InterfaceKey> {
            return underlayAccess.read(CLASS_OF_SERVICE, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet()
                    .orNull()
                    ?.let { parseInterfaceIds(it) }.orEmpty()
        }

        @VisibleForTesting
        fun parseInterfaceIds(cos: Interfaces): List<InterfaceKey> {
            return cos.`interface`.orEmpty().map {
                InterfaceKey(InterfaceId(it.name))
            }.toList()
        }

        fun readSpecificInterface(underlayAccess: UnderlayAccess, ifcName: String) : Interface? {
            return underlayAccess.read(CLASS_OF_SERVICE, LogicalDatastoreType.OPERATIONAL)
                    .checkedGet().orNull()
                    ?.`interface`.orEmpty().firstOrNull { it.name == ifcName }
        }
    }
}
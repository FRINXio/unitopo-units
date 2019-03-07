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

package io.frinx.unitopo.unit.junos.interfaces.handler.subinterfaces

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.ifc.base.handler.subinterfaces.AbstractSubinterfaceConfigReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos.interfaces.handler.InterfaceReader
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.subinterface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.UnitKey
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.InterfaceKey
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.interfaces_type.Unit as JunosInterfaceUnit
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.juniper.config.interfaces.Interface as JunosInterface

class SubinterfaceConfigReader(underlayAccess: UnderlayAccess)
    : AbstractSubinterfaceConfigReader<JunosInterfaceUnit>(underlayAccess) {

    override fun readCurrentAttributes(
        instanceIdentifier: InstanceIdentifier<Config>,
        configBuilder: ConfigBuilder,
        readContext: ReadContext
    ) {
        val ifcName = instanceIdentifier.firstKeyOf(Interface::class.java).name
        val subIfcIndex = instanceIdentifier.firstKeyOf(Subinterface::class.java).index

        readData(underlayAccess.read(readIid(ifcName, subIfcIndex),
            LogicalDatastoreType.CONFIGURATION).checkedGet().orNull(),
            configBuilder, ifcName, subIfcIndex)
    }

    override fun readData(data: JunosInterfaceUnit?, configBuilder: ConfigBuilder, ifcName: String, subIfcIndex: Long) {
        data?.let { configBuilder.index = data.name.toLong() }
    }

    override fun readIid(ifcName: String, subIfcIndex: Long): InstanceIdentifier<JunosInterfaceUnit> =
        InterfaceReader.IFCS.child(JunosInterface::class.java, InterfaceKey(ifcName))
            .child(JunosInterfaceUnit::class.java, UnitKey(subIfcIndex.toString()))
}
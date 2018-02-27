/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.xr6.ospf.handler

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.handlers.ospf.OspfReader
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.ospf.handler.AreaInterfaceReader.Companion.findAreaNameScopes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstance
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.protocols.Protocol
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospf.types.rev170228.OspfMetric
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces.InterfaceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.area.interfaces.structure.interfaces._interface.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.ospfv2.rev170228.ospfv2.top.ospfv2.areas.Area
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier as IID

class AreaInterfaceConfigReader(private val access: UnderlayAccess) :
        OspfReader.OspfConfigReader<Config, ConfigBuilder> {

    override fun getBuilder(id: IID<Config>): ConfigBuilder = ConfigBuilder()

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as InterfaceBuilder).`config` = config
    }

    override fun readCurrentAttributesForType(id: IID<Config>, config: ConfigBuilder, readContext: ReadContext) {
        val vrfKey = id.firstKeyOf(NetworkInstance::class.java)
        val protKey = id.firstKeyOf(Protocol::class.java)
        val areaKey = id.firstKeyOf(Area::class.java)
        val ifaceId = id.firstKeyOf(Interface::class.java).id

        val areas = OspfAreaReader.getAreas(access, protKey, vrfKey.name)
        findAreaNameScopes(areas, areaKey)
                ?.nameScope.orEmpty()
                .find { it.interfaceName.value == ifaceId }
                ?.let {
                    config.id = ifaceId
                    it.cost?.let {
                        config.metric = OspfMetric(it.toInt())
                    }
                }
    }
}

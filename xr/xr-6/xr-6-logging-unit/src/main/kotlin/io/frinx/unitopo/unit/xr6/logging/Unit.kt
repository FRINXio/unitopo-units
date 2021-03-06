/*
 * Copyright © 2019 Frinx and others.
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

package io.frinx.unitopo.unit.xr6.logging

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericListWriter
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NoopListWriter
import io.frinx.unitopo.unit.xr6.init.Unit
import io.frinx.unitopo.unit.xr6.logging.handler.LoggingInterfacesConfigWriter
import io.frinx.unitopo.unit.xr6.logging.handler.LoggingInterfacesReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.top.LoggingBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import io.frinx.openconfig.openconfig.logging.IIDs as LG_IIDS
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730.`$YangModuleInfoImpl` as UnderlayInterfacesYangInfo
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.l2.eth.infra.datatypes.rev151109.`$YangModuleInfoImpl` as Underlayinfradatatypes
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.event.types.rev171024.`$YangModuleInfoImpl` as EventTypeYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.`$YangModuleInfoImpl` as InterfaceYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.`$YangModuleInfoImpl` as LoggingYangInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.Interfaces as LoggingInterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.logging.rev171024.logging.interfaces.structural.interfaces._interface.Config as LoggingInterfaceConfig

class Unit(private val registry: TranslationUnitCollector) : Unit() {
    private var reg: TranslationUnitCollector.Registration? = null
    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        reg?.let { reg!!.close() }
    }

    override fun getYangSchemas() = setOf(
        InterfaceYangInfo.getInstance(),
        LoggingYangInfo.getInstance(),
        EventTypeYangInfo.getInstance())

    override fun getUnderlayYangSchemas() = UNDERLAY_SCHEMAS

    override fun getRpcs(underlayAccess: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(writeRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        writeRegistry.add(GenericListWriter(LG_IIDS.LO_IN_INTERFACE, NoopListWriter()))
        writeRegistry.subtreeAddAfter(setOf(
            RWUtils.cutIdFromStart(LG_IIDS.LO_IN_IN_CO_ENABLEDLOGGINGFOREVENT, IFC_CFG_ID)),
            GenericWriter(LG_IIDS.LO_IN_IN_CONFIG, LoggingInterfacesConfigWriter(underlayAccess)),
            IIDs.IN_IN_SU_SU_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(LG_IIDS.LOGGING, LoggingBuilder::class.java)
        rRegistry.subtreeAdd(setOf(
            RWUtils.cutIdFromStart(LG_IIDS.LO_IN_INTERFACE, IFCS_ID),
            RWUtils.cutIdFromStart(LG_IIDS.LO_IN_IN_CONFIG, IFCS_ID),
            RWUtils.cutIdFromStart(LG_IIDS.LO_IN_IN_CO_ENABLEDLOGGINGFOREVENT, IFCS_ID)),
            GenericConfigReader(LG_IIDS.LO_INTERFACES, LoggingInterfacesReader(underlayAccess)))
    }

    override fun toString(): String = "LG_IIDS@2018-06-15:logging translate unit"

    companion object {
        private val IFCS_ID = InstanceIdentifier.create(LoggingInterfaces::class.java)
        private val IFC_CFG_ID = InstanceIdentifier.create(LoggingInterfaceConfig::class.java)
        private val UNDERLAY_SCHEMAS = setOf(
            UnderlayInterfacesYangInfo.getInstance(),
            Underlayinfradatatypes.getInstance()
        )
    }
}
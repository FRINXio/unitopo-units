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

package io.frinx.unitopo.unit.junos17.platform

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericOperListReader
import io.fd.honeycomb.translate.impl.read.GenericOperReader
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.frinx.openconfig.openconfig.platform.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.platform.handler.ComponentConfigReader
import io.frinx.unitopo.unit.junos17.platform.handler.ComponentReader
import io.frinx.unitopo.unit.junos17.platform.handler.ComponentStateReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.platform.component.top.ComponentsBuilder
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.platform.rev161222.`$YangModuleInfoImpl` as OpenconfigPlatformModule
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jrpc.show.system.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as JunosYangInfoimport

class PlatformUnit(private val registry: TranslationUnitCollector) : TranslateUnit {
    private var reg: TranslationUnitCollector.Registration? = null

    // Invoked by blueprint
    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    // Invoked by blueprint
    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getYangSchemas() = setOf(OpenconfigPlatformModule.getInstance())

    override fun getUnderlayYangSchemas(): MutableSet<YangModuleInfo> = setOf(
            JunosYangInfoimport.getInstance()
    ).toMutableSet()

    override fun getRpcs(context: UnderlayAccess) = emptySet<RpcService<*, *>>()

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        access: UnderlayAccess
    ) {
        provideReaders(rRegistry, access)
        provideWriters(wRegistry, access)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, access: UnderlayAccess) {
        // no-op
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, access: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.COMPONENTS, ComponentsBuilder::class.java)
        rRegistry.add(GenericOperListReader(IIDs.CO_COMPONENT, ComponentReader(access)))
        rRegistry.add(GenericOperReader(IIDs.CO_CO_CONFIG, ComponentConfigReader()))
        rRegistry.add(GenericOperReader(IIDs.CO_CO_STATE, ComponentStateReader(access)))
    }

    override fun toString(): String {
        return "Junos platform"
    }
}
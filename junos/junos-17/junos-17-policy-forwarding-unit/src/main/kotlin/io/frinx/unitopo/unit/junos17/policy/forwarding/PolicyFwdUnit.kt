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

package io.frinx.unitopo.unit.junos17.policy.forwarding

import io.fd.honeycomb.rpc.RpcService
import io.fd.honeycomb.translate.impl.read.GenericConfigListReader
import io.fd.honeycomb.translate.impl.read.GenericConfigReader
import io.fd.honeycomb.translate.impl.write.GenericWriter
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareReadRegistryBuilder
import io.fd.honeycomb.translate.spi.builder.CustomizerAwareWriteRegistryBuilder
import io.fd.honeycomb.translate.util.RWUtils
import io.frinx.openconfig.openconfig.network.instance.IIDs
import io.frinx.unitopo.registry.api.TranslationUnitCollector
import io.frinx.unitopo.registry.spi.TranslateUnit
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.junos17.policy.forwarding.handler.PolicyForwardingInterfaceConfigReader
import io.frinx.unitopo.unit.junos17.policy.forwarding.handler.PolicyForwardingInterfaceConfigWriter
import io.frinx.unitopo.unit.junos17.policy.forwarding.handler.PolicyForwardingInterfaceReader
import io.frinx.unitopo.unit.utils.NoopListWriter
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.InterfacesBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.pf.interfaces.structural.interfaces._interface.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.policy.forwarding.top.PolicyForwardingBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.YangModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.pf.interfaces.extension.juniper.rev171109.`$YangModuleInfoImpl` as PfExtensionModuleInfo
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.policy.forwarding.rev170621.`$YangModuleInfoImpl` as PolicyModuleInfo
import org.opendaylight.yang.gen.v1.http.yang.juniper.net.yang._1._1.jc.configuration.junos._17._3r1._10.rev170101.`$YangModuleInfoImpl` as JunosYangInfo

class PolicyFwdUnit(private val registry: TranslationUnitCollector) : TranslateUnit {

    private var reg: TranslationUnitCollector.Registration? = null

    fun init() {
        reg = registry.registerTranslateUnit(this)
    }

    fun close() {
        if (reg != null) {
            reg!!.close()
        }
    }

    override fun getRpcs(context: UnderlayAccess): Set<RpcService<*, *>> = emptySet()

    override fun getYangSchemas(): Set<YangModuleInfo> = setOf(
        PolicyModuleInfo.getInstance(),
        PfExtensionModuleInfo.getInstance()
    )

    override fun getUnderlayYangSchemas(): Set<YangModuleInfo> = setOf(JunosYangInfo.getInstance())

    override fun provideHandlers(
        rRegistry: CustomizerAwareReadRegistryBuilder,
        wRegistry: CustomizerAwareWriteRegistryBuilder,
        underlayAccess: UnderlayAccess
    ) {
        provideReaders(rRegistry, underlayAccess)
        provideWriters(wRegistry, underlayAccess)
    }

    private fun provideWriters(wRegistry: CustomizerAwareWriteRegistryBuilder, underlayAccess: UnderlayAccess) {
        wRegistry.add(GenericWriter(IIDs.NE_NE_PO_IN_INTERFACE, NoopListWriter()))
        wRegistry.subtreeAddAfter(JUNIPER_AUG_SUBTREE, GenericWriter(IIDs.NE_NE_PO_IN_IN_CONFIG,
            PolicyForwardingInterfaceConfigWriter(underlayAccess)),
                /*handle after ifc configuration*/ io.frinx.openconfig.openconfig.interfaces.IIDs.IN_IN_CONFIG)
    }

    private fun provideReaders(rRegistry: CustomizerAwareReadRegistryBuilder, underlayAccess: UnderlayAccess) {
        rRegistry.addStructuralReader(IIDs.NE_NE_POLICYFORWARDING, PolicyForwardingBuilder::class.java)
        rRegistry.addStructuralReader(IIDs.NE_NE_PO_INTERFACES, InterfacesBuilder::class.java)
        rRegistry.add(GenericConfigListReader(IIDs.NE_NE_PO_IN_INTERFACE,
            PolicyForwardingInterfaceReader(underlayAccess)))
        rRegistry.subtreeAdd(JUNIPER_AUG_SUBTREE, GenericConfigReader(IIDs.NE_NE_PO_IN_IN_CONFIG,
            PolicyForwardingInterfaceConfigReader(underlayAccess)))
    }

    override fun toString(): String {
        return "Junos 17.3 Policy Forwarding translate unit"
    }

    companion object {
        private val PF_IFC_CFG_ROOT_ID = InstanceIdentifier.create(Config::class.java)

        private val JUNIPER_AUG_SUBTREE = setOf(
            RWUtils.cutIdFromStart<Config>(IIDs.NE_NE_PO_IN_IN_CO_AUG_NIPFIFJUNIPERAUG, PF_IFC_CFG_ROOT_ID),
            RWUtils.cutIdFromStart<Config>(IIDs.NE_NE_PO_IN_IN_CO_AUG_NIPFIFJUNIPERAUG_CLASSIFIERS, PF_IFC_CFG_ROOT_ID),
            RWUtils.cutIdFromStart<Config>(IIDs.NE_NE_PO_IN_IN_CO_AUG_NIPFIFJUNIPERAUG_CL_EXP, PF_IFC_CFG_ROOT_ID),
            RWUtils.cutIdFromStart<Config>(IIDs.NE_NE_PO_IN_IN_CO_AUG_NIPFIFJUNIPERAUG_CL_INETPRECEDENCE,
                PF_IFC_CFG_ROOT_ID))
    }
}
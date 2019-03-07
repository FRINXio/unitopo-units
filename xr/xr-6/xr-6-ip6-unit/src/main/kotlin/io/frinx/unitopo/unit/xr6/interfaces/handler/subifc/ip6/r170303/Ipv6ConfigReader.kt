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

package io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r170303

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.xr6.interfaces.handler.subifc.ip6.r150730.Ipv6AddressReader.Companion.readInterfaceCfg
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150730._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv6.ma.cfg.rev170303.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv6.top.ipv6.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class Ipv6ConfigReader(private val underlayAccess: UnderlayAccess) :
        ConfigReaderCustomizer<Config, ConfigBuilder> {

    override fun readCurrentAttributes(id: InstanceIdentifier<Config>, builder: ConfigBuilder, ctx: ReadContext) {
        val ifcName = id.firstKeyOf(Interface::class.java).name
        val ifcIndex = id.firstKeyOf(Subinterface::class.java).index
        val subIfcName = when (ifcIndex) {
            ZERO_SUBINTERFACE_ID -> ifcName
            else -> getSubIfcName(ifcName, ifcIndex)
        }
        readInterfaceCfg(underlayAccess, subIfcName, { extractAddress(it, builder) })
    }

    companion object {
        const val ZERO_SUBINTERFACE_ID = 0L
        fun getSubIfcName(ifcName: String, subifcIdx: Long) = ifcName + "." + subifcIdx

        private fun extractAddress(ifcCfg: InterfaceConfiguration, builder: ConfigBuilder) {
            ifcCfg.getAugmentation(InterfaceConfiguration1::class.java)?.let {
                it.ipv6Network?.let {
                    it.addresses?.let {
                        if (null != it.autoConfiguration) {
                            builder.setEnabled(it.autoConfiguration.isEnable)
                        } else {
                            builder.setEnabled(false)
                        }
                    }
                }
            }
        }
    }
}
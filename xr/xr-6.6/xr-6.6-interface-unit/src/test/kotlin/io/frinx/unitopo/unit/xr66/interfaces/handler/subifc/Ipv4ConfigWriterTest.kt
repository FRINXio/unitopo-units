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

package io.frinx.unitopo.unit.xr66.interfaces.handler.subifc

import io.fd.honeycomb.translate.write.WriteContext
import io.frinx.openconfig.openconfig.interfaces.IIDs
import io.frinx.unitopo.registry.spi.UnderlayAccess
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceActive
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907.InterfaceConfigurations
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfiguration
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev170907._interface.configurations.InterfaceConfigurationKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111.InterfaceConfiguration1
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111._interface.configurations._interface.configuration.Ipv4Network
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111._interface.configurations._interface.configuration.ipv4.network.Addresses as NativeIpv4Addresses
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111._interface.configurations._interface.configuration.ipv4.network.addresses.Primary
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ipv4.io.cfg.rev180111._interface.configurations._interface.configuration.ipv4.network.addresses.PrimaryBuilder
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev180629.InterfaceName
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.Interface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.interfaces.top.interfaces.InterfaceKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.Subinterface1
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.Ipv4
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.Addresses
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.Address
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.AddressKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.Config as AddressConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.ip.rev161222.ipv4.top.ipv4.addresses.address.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.Subinterfaces
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.Subinterface
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.interfaces.rev161222.subinterfaces.top.subinterfaces.SubinterfaceKey
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class Ipv4ConfigWriterTest {
    @Mock
    private lateinit var writeContext: WriteContext

    private lateinit var underlayAccess: UnderlayAccess

    private lateinit var target: Ipv4ConfigWriter

    companion object {
        private val NC_HELPER = NetconfAccessHelper("/data_nodes.xml")
        private val NATIVE_ACT = InterfaceActive("act")
        private val IF_NAME = "GigabitEthernet0/0/0/0"
        val ETH_ADDRESS = "10.1.5.28"
        val ETH_NETMASK = 16

        private val NATIVE_IF_NAME = InterfaceName(IF_NAME)
        private val NATIVE_ADDRESS = ETH_ADDRESS
        private val NATIVE_NETMASK = "255.255.0.0"

        private val IID_CONFIG = IIDs.INTERFACES
                .child(Interface::class.java, InterfaceKey(IF_NAME))
                .child(Subinterfaces::class.java)
                .child(Subinterface::class.java, SubinterfaceKey(0))
                .augmentation(Subinterface1::class.java)
                .child(Ipv4::class.java)
                .child(Addresses::class.java)
                .child(Address::class.java, AddressKey(Ipv4AddressNoZone(ETH_ADDRESS)))
                .child(AddressConfig::class.java)

        private val CONFIG = ConfigBuilder()
                .setIp(Ipv4AddressNoZone(ETH_ADDRESS))
                .setPrefixLength(ETH_NETMASK.toShort())
            .build()

        private val NATIVE_IID = InstanceIdentifier.create(InterfaceConfigurations::class.java)
                .child(InterfaceConfiguration::class.java, InterfaceConfigurationKey(NATIVE_ACT, NATIVE_IF_NAME))
                .augmentation(InterfaceConfiguration1::class.java)
                .child(Ipv4Network::class.java)
                .child(NativeIpv4Addresses::class.java)
                .child(Primary::class.java)

        private val DATA_ETHER_IFC = NC_HELPER.read(NATIVE_IID).checkedGet().get()

        private val NATIVE_CONFIG = PrimaryBuilder(DATA_ETHER_IFC)
                .setAddress(Ipv4AddressNoZone(NATIVE_ADDRESS))
                .setNetmask(Ipv4AddressNoZone(NATIVE_NETMASK))
            .build()
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        underlayAccess = Mockito.spy(NC_HELPER)
        target = Ipv4ConfigWriter(underlayAccess)
    }

    @Test
    fun testWriteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
            .build()
        val expectedConfig = PrimaryBuilder(NATIVE_CONFIG) // not customize
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Primary>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<Primary>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.writeCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Primary>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }

    @Test
    fun testDeleteCurrentAttributes() {
        val id = IID_CONFIG
        val config = ConfigBuilder(CONFIG) // not customize
            .build()
        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Primary>>

        Mockito.doNothing().`when`(underlayAccess).delete(Mockito.any())

        // test
        target.deleteCurrentAttributes(id, config, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).delete(idCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Primary>>
        )
    }

    @Test
    fun testUpdateCurrentAttributes() {
        val AFTER_ETH_ADDRESS = "10.1.12.88"

        val configBefore = ConfigBuilder(CONFIG) // not customize
            .build()
        val configAfter = ConfigBuilder(CONFIG)
            .setIp(Ipv4AddressNoZone(AFTER_ETH_ADDRESS))
            .build()
        val expectedConfig = PrimaryBuilder()
            .setAddress(Ipv4AddressNoZone(AFTER_ETH_ADDRESS))
            .setNetmask(Ipv4AddressNoZone(NATIVE_NETMASK))
            .build()

        val idCap = ArgumentCaptor
            .forClass(InstanceIdentifier::class.java) as ArgumentCaptor<InstanceIdentifier<Primary>>
        val dataCap = ArgumentCaptor
            .forClass(DataObject::class.java) as ArgumentCaptor<Primary>

        Mockito.doNothing().`when`(underlayAccess).merge(Mockito.any(), Mockito.any())

        // test
        target.updateCurrentAttributes(IID_CONFIG, configBefore, configAfter, writeContext)

        // capture
        Mockito.verify(underlayAccess, Mockito.times(1)).merge(idCap.capture(), dataCap.capture())

        // verify capture-length
        Assert.assertThat(idCap.allValues.size, CoreMatchers.`is`(1))
        Assert.assertThat(dataCap.allValues.size, CoreMatchers.`is`(1))

        // verify captured values
        Assert.assertThat(
            idCap.allValues[0],
            CoreMatchers.equalTo(NATIVE_IID) as Matcher<in InstanceIdentifier<Primary>>
        )
        Assert.assertThat(
            dataCap.allValues[0],
            CoreMatchers.equalTo(expectedConfig)
        )
    }
}
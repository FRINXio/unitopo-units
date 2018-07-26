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

package io.frinx.unitopo.unit.xr6.routing.policy.handlers

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.infra.rsi.cfg.rev150730.Vrfs
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ExtCommunitySetConfig
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySetBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ExtCommunitySetKey
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.policy.rev170730.ext.community.set.top.ext.community.sets.ext.community.set.ConfigBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.bgp.types.rev170202.BgpExtCommunityType
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ExtCommunitySetReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/vrf-conf.xml")

    @Test
    fun testAllIds() {

        val vrfs = parseGetCfgResponse(DATA_NODES, InstanceIdentifier.create(Vrfs::class.java))

        Assert.assertTrue(listOf("abcd-route-target-export-set", "abcd-route-target-import-set",
                "abcd3-route-target-import-set")
                .map { ExtCommunitySetKey(it) }.containsAll(ExtCommunitySetReader.parseAllIds(vrfs)))
    }

    @Test
    fun testRead() {
        val vrfs = parseGetCfgResponse(DATA_NODES, InstanceIdentifier.create(Vrfs::class.java))

        var builder = ExtCommunitySetBuilder()
        ExtCommunitySetReader.parseCurrentAttributes(vrfs, builder, "abcd-route-target-export-set")
        var real = builder.build()
        var expected = ExtCommunitySetBuilder()
                .setExtCommunitySetName("abcd-route-target-export-set")
                .setConfig(ConfigBuilder()
                        .setExtCommunitySetName("abcd-route-target-export-set")
                        .setExtCommunityMember(listOf("8585:4343", "1:1").map {
                            ExtCommunitySetConfig.ExtCommunityMember(BgpExtCommunityType(it)) })
                        .build())
                .build()

        Assert.assertEquals(real.extCommunitySetName, expected.extCommunitySetName)
        Assert.assertEquals(real.config.extCommunityMember!!.size, expected.config.extCommunityMember!!.size)
        Assert.assertTrue(real.config.extCommunityMember!!.containsAll(expected.config.extCommunityMember!!.toList()))

        builder = ExtCommunitySetBuilder()
        ExtCommunitySetReader.parseCurrentAttributes(vrfs, builder, "abcd-route-target-import-set")

        real = builder.build()
        expected = ExtCommunitySetBuilder()
                .setExtCommunitySetName("abcd-route-target-import-set")
                .setConfig(ConfigBuilder()
                        .setExtCommunitySetName("abcd-route-target-import-set")
                        .setExtCommunityMember(setOf("6500:4", "5445444:1").map {
                            ExtCommunitySetConfig.ExtCommunityMember(BgpExtCommunityType(it)) })
                        .build())
                .build()

        Assert.assertEquals(real.extCommunitySetName, expected.extCommunitySetName)
        Assert.assertEquals(real.config.extCommunityMember!!.size, expected.config.extCommunityMember!!.size)
        Assert.assertTrue(real.config.extCommunityMember!!.containsAll(expected.config.extCommunityMember!!.toList()))

        val builder2 = ExtCommunitySetBuilder()
        ExtCommunitySetReader.parseCurrentAttributes(vrfs, builder2, "NONEXISTING")

        Assert.assertEquals(
                ExtCommunitySetBuilder().build(),
                builder2.build())
    }
}
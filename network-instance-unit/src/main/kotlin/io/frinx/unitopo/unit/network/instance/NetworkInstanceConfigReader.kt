/*
 * Copyright © 2018 Frinx and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.frinx.unitopo.unit.network.instance

import io.fd.honeycomb.translate.spi.read.ConfigReaderCustomizer
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer
import io.frinx.cli.registry.common.CompositeReader
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.NetworkInstanceBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.Config
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.network.instance.rev170228.network.instance.top.network.instances.network.instance.ConfigBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

open class NetworkInstanceConfigReader(readers : ArrayList<ReaderCustomizer<Config, ConfigBuilder>>) :
        ConfigReaderCustomizer<Config, ConfigBuilder>, CompositeReader<Config, ConfigBuilder>(readers), ReaderCustomizer<Config, ConfigBuilder> {

    override fun getBuilder(instanceIdentifier: InstanceIdentifier<Config>): ConfigBuilder {
        return ConfigBuilder()
    }

    override fun merge(builder: Builder<out DataObject>, config: Config) {
        (builder as NetworkInstanceBuilder).config = config
    }
}
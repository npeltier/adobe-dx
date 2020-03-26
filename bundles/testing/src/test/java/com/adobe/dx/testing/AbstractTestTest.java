/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.dx.testing;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class AbstractTestTest {

    protected final AemContext context = new AemContextBuilder()
        .plugin(CACONFIG)
        .build();

    @Test
    public void test() {
        context.build().resource(AbstractTest.CONTENT_ROOT, "foo", "bar", "blah", 2).commit();
        AbstractTest test = new AbstractTest();
        ValueMap properties = test.getVM(AbstractTest.CONTENT_ROOT);
        assertTrue(properties.containsKey("foo"));
    }
}
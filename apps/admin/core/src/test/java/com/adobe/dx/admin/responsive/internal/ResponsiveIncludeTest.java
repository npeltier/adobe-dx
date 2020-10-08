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
package com.adobe.dx.admin.responsive.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.adobe.dx.testing.AbstractTest;
import com.adobe.dx.testing.extensions.ResponsiveContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ResponsiveContext.class)
class ResponsiveIncludeTest extends AbstractTest {

    ResponsiveInclude include;

    @BeforeEach
    public void setup() {
        String path = "/mnt/override/apps/dx/component/cq:dialog/content/items/tabs";
        context.build().resource("/mnt/override/apps/dx/component/cq:dialog/content/items/tabs");
        context.currentResource(path);
        include = new ResponsiveInclude();
        context.registerInjectActivateService(include);
    }

    @Test
    public void testPath() {
        assertEquals("/var/dx/admin/responsiveinclude/apps/dx/component/cq:dialog/content/items/tabs",
            include.getIncludePath(context.request()));
    }
}
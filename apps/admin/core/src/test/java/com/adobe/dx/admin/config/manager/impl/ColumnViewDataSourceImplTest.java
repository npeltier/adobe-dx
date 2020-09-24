/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.dx.admin.config.manager.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.adobe.dx.admin.config.manager.ColumnViewDataSource;
import com.adobe.dx.admin.config.manager.ColumnViewItem;
import com.adobe.dx.admin.config.manager.internal.ColumnViewDataSourceImpl;
import com.adobe.dx.testing.AbstractTest;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.models.factory.ModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ColumnViewDataSourceImplTest extends AbstractTest {
    @BeforeEach
    private void setup() {
        context.load().json("/mocks/admin.configmanager/configuration-tree.json", CONF_ROOT);
        context.currentResource(CONF_ROOT);
        context.addModelsForClasses(ColumnViewItem.class, ColumnViewDataSourceImpl.class, ColumnViewDataSource.class);
    }

    @Test
    public void testBrowsing() throws ReflectiveOperationException {
        ColumnViewDataSource ds = context.getService(ModelFactory.class).createModel(context.request(), ColumnViewDataSource.class);
        List<ColumnViewItem> firstChildren = ds.getItems();
        assertNotNull(firstChildren);
        assertEquals(1, firstChildren.size());
        ColumnViewItem root = firstChildren.get(0);
        assertEquals("folder", root.getIconType());
        assertNotNull(root.getChildren());
        List<ColumnViewItem> children = root.getChildren();
        assertArrayEquals(new String[] {"whatever", "anotherPage"},
            children.stream()
            .map(ColumnViewItem::getName)
            .collect(Collectors.toList()).toArray());
        ColumnViewItem pageItem = children.get(1);
        assertTrue(pageItem.getIsPage());
        assertEquals(CONF_ROOT + "/anotherPage", pageItem.getPath());
        assertEquals("Some Random page", pageItem.getLabel());
        assertEquals("config", pageItem.getIconType());
    }
}
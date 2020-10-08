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

import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_GET;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;

import com.adobe.dx.responsive.Breakpoint;
import com.adobe.dx.utils.RequestUtil;
import com.adobe.granite.ui.components.htl.ComponentHelper;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.script.Bindings;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {Servlet.class, EventHandler.class}, configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        SLING_SERVLET_RESOURCE_TYPES + "=" + ResponsiveInclude.RESOURCE_TYPE,
        SLING_SERVLET_METHODS + "=" + METHOD_GET})
public class ResponsiveInclude extends SlingSafeMethodsServlet implements EventHandler {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String RESOURCE_TYPE = "dx/admin/responsive/include";
    private static final String PN_TYPE = "resourceType";
    private static final String PN_LOOP = "dxResponsiveItem";
    private static final String PN_NAME = "name";
    private static final String PN_TITLE = "jcr:title";
    private static final String PN_DESCRIPTION = "jcr:description";
    private static final String SLING_FOLDER = "sling:Folder";
    private static final String PN_RESOURCE_TYPE = "sling:" + PN_TYPE;
    private static final String MNT_PREFIX = "/mnt/override";

    Configuration configuration;

    @Reference
    ResourceResolverFactory factory;

    @Activate
    public void activate(Configuration configuration) {
        this.configuration = configuration;
    }

    static final class Referrer extends ComponentHelper {

        SlingHttpServletRequest request;
        Resource targetResource;

        Referrer(SlingHttpServletRequest request, Resource targetResource) {
            this.request = request;
            this.targetResource = targetResource;
            init((Bindings)request.getAttribute(SlingBindings.class.getName()));
        }
        @Override
        protected void activate() {
        }

        String include() throws ServletException, IOException {
            return super.include(targetResource, targetResource.getResourceType(), StringUtils.EMPTY, getOptions());
        }
    }

    void scanRoots(ResourceResolver resolver) {

    }

    /**
     * Write properties from the configuration to the target resource,
     * instantiating both property names & values
     *
     * @param conf configured resource that holds all properties to write (and subpipes)
     * @param target target resource on which configured values will be written
     */
    private void copyProperties(@Nullable Resource conf, Resource target, Breakpoint breakpoint)  {
        ValueMap writeMap = conf != null ? conf.adaptTo(ValueMap.class) : null;
        ModifiableValueMap properties = target.adaptTo(ModifiableValueMap.class);

        //writing current node
        if (properties != null && writeMap != null) {
            for (Map.Entry<String, Object> entry : writeMap.entrySet()) {
                Object value = breakpoint != null && PN_NAME.equals(entry.getKey()) ? entry.getValue() + breakpoint.propertySuffix() : entry.getValue();
                properties.put(entry.getKey(), value);
            }
        }
    }

    /**
     * loop over breakpoints and write current tree that many times
     * @param request
     * @param loop
     * @param target
     * @throws RepositoryException
     */
    private void loopTree(SlingHttpServletRequest request, Node loop, Resource target) throws RepositoryException {
        Node targetNode = target.adaptTo(Node.class);
        for (Breakpoint breakpoint : RequestUtil.getBreakpoints(request)) {
            Node child = targetNode.addNode(loop.getName() + breakpoint.propertySuffix(), loop.getPrimaryNodeType().getName());
            child.setProperty(PN_TITLE, breakpoint.getLabel());
            logger.debug("writing responsive tree {}", child.getPath());
            writeTree(request, loop, target.getResourceResolver().getResource(child.getPath()), breakpoint);
        }
    }

    /**
     * @param referrer source JCR tree to dub to the cache
     * @param target target resource to write
     * @param breakpoint current breakpoint under which this tree is written
     */
    private void writeTree(SlingHttpServletRequest request, Node referrer, Resource target, Breakpoint breakpoint) throws RepositoryException {
        if (breakpoint == null && referrer.hasProperty(PN_LOOP)) {
            loopTree(request, referrer, target);
        }
        ResourceResolver resolver = target.getResourceResolver();
        copyProperties(resolver.getResource(referrer.getPath()), target, breakpoint);
        NodeIterator childrenConf = referrer.getNodes();
        if (childrenConf.hasNext()){
            Node targetNode = target.adaptTo(Node.class);
            if (targetNode != null) {
                logger.debug("dubbing {} at {}", referrer.getPath(), target.getPath());
                while (childrenConf.hasNext()) {
                    Node childConf = childrenConf.nextNode();
                    String name = childConf.getName();
                    Node childTarget = targetNode.hasNode(name) ? targetNode.getNode(name) : targetNode.addNode(name, childConf.getPrimaryNodeType().getName());
                    logger.debug("writing tree {}", childTarget.getPath());
                    writeTree(request, childConf, resolver.getResource(childTarget.getPath()), breakpoint);
                }
            }
        }
    }

    Resource buildInclude(SlingHttpServletRequest request) {
        String path = getIncludePath(request);
        Resource referrer = request.getResourceResolver().getResource(getRawPath(request.getResource()));
        try (ResourceResolver writeResolver = factory.getAdministrativeResourceResolver(null)) {
            Map<String, Object> properties = new HashMap<>();
            String targetType = referrer.getValueMap().get(PN_TYPE, String.class);
            if (targetType != null) {
                properties.put(PN_RESOURCE_TYPE, targetType);
            }
            properties.put(JCR_LASTMODIFIED, new Date());
            Resource target = ResourceUtil.getOrCreateResource(writeResolver, path, properties, SLING_FOLDER,false);
            writeTree(request, referrer.adaptTo(Node.class), target , null);
            writeResolver.commit();
        } catch (LoginException | RepositoryException e) {
            logger.error("unable to login for a write session", e);
        } catch (PersistenceException e) {
            logger.error("unable to login for a write session", e);
         }
        return request.getResourceResolver().getResource(path);
    }


    boolean isValidInclude(Resource resource) {
        return resource != null;
    }

    String getRawPath(Resource resource) {
        return StringUtils.substringAfter(resource.getPath(), MNT_PREFIX);
    }

    String getIncludePath(SlingHttpServletRequest request) {
        Resource resource = request.getResource();
        return configuration.cacheRoot() + getRawPath(resource);
    }

    Resource getIncludeResource(SlingHttpServletRequest request) {
        String includePath = getIncludePath(request);
        Resource resource = request.getResource();
        Resource includeResource = resource.getResourceResolver().getResource(includePath);
        if (isValidInclude(includeResource)) {
            return includeResource;
        }
        return buildInclude(request);
    }

    void include(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        Resource targetResource = getIncludeResource(request);
        if (targetResource != null) {
            Referrer referrer = new Referrer(request, targetResource);
            response.getWriter().print(referrer.include());
        }
    }

    @Override
    public void handleEvent(Event event) {

    }

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        include(request, response);
    }

    @ObjectClassDefinition(name = "Adobe DX Admin Responsive Include")
    public @interface Configuration {

        @AttributeDefinition(name = "Dialog roots", description = "only responsive includes under those roots will be inspected"
        )
        String[] dialogRoots() default { "/apps/dx" };

        @AttributeDefinition(name = "Root of where responsive include are stored",
            description = "should tag components on creation or copy"
        )
        String cacheRoot() default "/var/dx/admin/responsiveinclude";
    }
}

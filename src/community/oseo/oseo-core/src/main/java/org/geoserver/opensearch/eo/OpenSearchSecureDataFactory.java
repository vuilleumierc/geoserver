/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredObjectFactory;

/**
 * Just to make sure the code can recognize OpenSearchAccess, no need for wrapping it at the moment
 *
 * <p>TODO: figure out a better setup for OpenSearchAccess object
 */
public class OpenSearchSecureDataFactory implements SecuredObjectFactory {

    @Override
    public boolean canSecure(Class clazz) {
        return OpenSearchAccess.class.isAssignableFrom(clazz);
    }

    @Override
    public Object secure(Object object, WrapperPolicy policy) {
        // null check
        if (object == null) return null;

        // wrapping check
        Class clazz = object.getClass();
        if (!canSecure(clazz))
            throw new IllegalArgumentException(
                    "Don't know how to wrap objects of class " + object.getClass());

        // return as is, implementations of OpenSearchAccess are read only
        return object;
    }

    /** Returns {@link ExtensionPriority#LOWEST} since the wrappers generated by this factory */
    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}

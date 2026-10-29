package org.etrange.towards.data.desktop;

import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.portal.Location")
public interface LocationPortal extends DBusInterface {
    DBusPath CreateSession(Map<String, Variant<?>> options);

    DBusPath Start(DBusPath sessionHandle, String parentWindow, Map<String, Variant<?>> options);

    class LocationUpdated extends DBusSignal {
        private final DBusPath sessionHandle;
        private final Map<String, Variant<?>> location;

        public LocationUpdated(
            String path,
            DBusPath sessionHandle,
            Map<String, Variant<?>> location
        ) throws DBusException {
            super(path, sessionHandle, location);
            this.sessionHandle = sessionHandle;
            this.location = location;
        }

        public DBusPath getSessionHandle() {
            return sessionHandle;
        }

        public Map<String, Variant<?>> getLocation() {
            return location;
        }
    }
}

final class PortalOptions {
    /** Street-level portal accuracy. The closest documented step to a 100 meter fix. */
    private static final int STREET_ACCURACY = 4;
    private static final int DISTANCE_THRESHOLD_METERS = 100;

    private PortalOptions() {
    }

    static Map<String, Variant<?>> createSession(String sessionToken) {
        Map<String, Variant<?>> options = new HashMap<String, Variant<?>>();
        options.put("session_handle_token", new Variant<String>(sessionToken, "s"));
        options.put("distance-threshold", new Variant<UInt32>(new UInt32(DISTANCE_THRESHOLD_METERS), "u"));
        options.put("accuracy", new Variant<UInt32>(new UInt32(STREET_ACCURACY), "u"));
        return options;
    }

    static Map<String, Variant<?>> start(String requestToken) {
        Map<String, Variant<?>> options = new HashMap<String, Variant<?>>();
        options.put("handle_token", new Variant<String>(requestToken, "s"));
        return options;
    }

    static Double variantDouble(Map<String, Variant<?>> values, String key) {
        if (values == null) {
            return null;
        }
        Variant<?> variant = values.get(key);
        if (variant == null) {
            return null;
        }
        Object value = variant.getValue();
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}

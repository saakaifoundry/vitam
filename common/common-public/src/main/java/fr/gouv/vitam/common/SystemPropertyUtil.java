/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * A collection of utility methods to retrieve and parse the values of the Java system properties.
 */
public final class SystemPropertyUtil {
    // Since logger could be not available yet, one must not declare there a Logger

    private static final String USING_THE_DEFAULT_VALUE = "using the default value: ";
    /**
     * Default File encoding field
     */
    public static final String FILE_ENCODING = "file.encoding";
    /**
     * Property Vitam Config Folder
     */
    protected static final String VITAM_CONFIG_FOLDER = "vitam.config.folder";
    /**
     * Property Vitam Data Folder
     */
    protected static final String VITAM_DATA_FOLDER = "vitam.data.folder";
    /**
     * Property Vitam Log Folder
     */
    protected static final String VITAM_LOG_FOLDER = "vitam.log.folder";
    /**
     * Property Vitam Tmp Folder
     */
    protected static final String VITAM_TMP_FOLDER = "vitam.tmp.folder";
    /**
     * Default Vitam Config Folder
     */
    private static final String VITAM_CONFIG_FOLDER_DEFAULT = "/vitam/conf";
    /**
     * Default Vitam Config Folder
     */
    private static final String VITAM_DATA_FOLDER_DEFAULT = "/vitam/data";
    /**
     * Default Vitam Config Folder
     */
    private static final String VITAM_LOG_FOLDER_DEFAULT = "/vitam/log";
    /**
     * Default Vitam Config Folder
     */
    // TODO change to /vitam/tmp when configured on the PIC
    private static final String VITAM_TMP_FOLDER_DEFAULT = "/vitam/data/tmp";

    private static final Properties PROPS = new Properties();

    private static Platform m_os = null;

    // Retrieve all system properties at once so that there's no need to deal
    // with
    // security exceptions from next time. Otherwise, we might end up with
    // logging every
    // security exceptions on every system property access or introducing more
    // complexity
    // just because of less verbose logging.
    static {
        refresh();
    }

    private SystemPropertyUtil() {
        // Unused
    }

    /**
     * Re-retrieves all system properties so that any post-launch properties updates are retrieved.
     */
    public static void refresh() {
        Properties newProps = null;
        try {
            newProps = System.getProperties();
        } catch (final SecurityException e) { // NOSONAR
            // Since logger could be not available yet
            System.err.println( // NOSONAR
                "Unable to retrieve the system properties; default values will be used: " + e.getMessage());
            newProps = new Properties();
        }

        synchronized (PROPS) {
            PROPS.clear();
            PROPS.putAll(newProps);
        }
        if (!contains(FILE_ENCODING) || !get(FILE_ENCODING).equalsIgnoreCase(CharsetUtils.UTF_8)) {
            try {
                // Try to set UTF-8 as default file encoding: use
                // -Dfile.encoding=UTF-8 as java command argument to ensure
                // correctness
                System.setProperty(FILE_ENCODING, CharsetUtils.UTF_8);
                final Field charset = Charset.class.getDeclaredField("defaultCharset");
                charset.setAccessible(true);
                charset.set(null, null);
                synchronized (PROPS) {
                    PROPS.clear();
                    PROPS.putAll(newProps);
                }
            } catch (final Exception e1) { // NOSONAR
                // Since logger could be not available yet
                // ignore since it is a security issue and -Dfile.encoding=UTF-8
                // should be used
                System.err // NOSONAR
                    .println(
                        "Issue while trying to set UTF-8 as default file encoding: use -Dfile.encoding=UTF-8 as java command argument: " +
                            e1.getMessage());
                System.err.println("Currently file.encoding is: " + get(FILE_ENCODING)); // NOSONAR
            }
        }
    }

    /**
     *
     * @return True if Encoding is Correct
     */
    public static boolean isFileEncodingCorrect() {
        return contains(FILE_ENCODING) && get(FILE_ENCODING).equalsIgnoreCase(CharsetUtils.UTF_8);
    }

    /**
     *
     * @return the VitamConfigFolder path
     */
    public static String getVitamConfigFolder() {
        if (contains(VITAM_CONFIG_FOLDER)) {
            return get(VITAM_CONFIG_FOLDER);
        }
        return VITAM_CONFIG_FOLDER_DEFAULT;
    }

    /**
     *
     * @return the VitamDataFolder path
     */
    public static String getVitamDataFolder() {
        if (contains(VITAM_DATA_FOLDER)) {
            return get(VITAM_DATA_FOLDER);
        }
        return VITAM_DATA_FOLDER_DEFAULT;
    }

    /**
     *
     * @return the VitamLogFolder path
     */
    public static String getVitamLogFolder() {
        if (contains(VITAM_LOG_FOLDER)) {
            return get(VITAM_LOG_FOLDER);
        }
        return VITAM_LOG_FOLDER_DEFAULT;
    }

    /**
     *
     * @return the VitamTmpFolder path
     */
    public static String getVitamTmpFolder() {
        if (contains(VITAM_TMP_FOLDER)) {
            return get(VITAM_TMP_FOLDER);
        }
        return VITAM_TMP_FOLDER_DEFAULT;
    }

    /**
     * Returns {@code true} if and only if the system property with the specified {@code key} exists.
     *
     * @param key
     * @return True if the key is contained
     * @throws IllegalArgumentException key null
     */
    public static final boolean contains(final String key) {
        ParametersChecker.checkParameter("Key", key);
        return PROPS.containsKey(key);
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to {@code null}
     * if the property access fails.
     *
     * @param key
     * @return the property value or {@code null}
     * @throws IllegalArgumentException key null
     */
    public static final String get(final String key) {
        return get(key, null);
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key null
     */
    public static final String get(final String key, final String def) {
        ParametersChecker.checkParameter("Key", key);
        final String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key null
     */
    public static boolean get(final String key, final boolean def) {
        ParametersChecker.checkParameter("Key", key);
        String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }

        System.err.println("Unable to parse the boolean system property '" + key + "':" + value + " - " + // NOSONAR
            USING_THE_DEFAULT_VALUE + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key null
     */
    public static int get(final String key, final int def) {
        ParametersChecker.checkParameter("Key", key);
        String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Integer.parseInt(value);
            } catch (final Exception e) { // NOSONAR
                // Since logger could be not available yet
                // Ignore
            }
        }

        System.err.println("Unable to parse the integer system property '" + key + "':" + value + " - " + // NOSONAR
            USING_THE_DEFAULT_VALUE + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key null
     */
    public static long get(final String key, final long def) {
        ParametersChecker.checkParameter("Key", key);
        String value = PROPS.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Long.parseLong(value);
            } catch (final Exception e) { // NOSONAR
                // Since logger could be not available yet
                // Ignore
            }
        }

        System.err.println("Unable to parse the long integer system property '" + key + "':" + value + " - " + // NOSONAR
            USING_THE_DEFAULT_VALUE + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key or def null
     */
    public static String getAndSet(String key, String def) {
        ParametersChecker.checkParameter("Key", key);
        if (def == null) {
            throw new IllegalArgumentException("Def cannot be null");
        }
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, def);
            refresh();
            return def;
        }
        return PROPS.getProperty(key);
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key null
     */
    public static boolean getAndSet(String key, boolean def) {
        ParametersChecker.checkParameter("Key", key);
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, Boolean.toString(def));
            refresh();
            return def;
        }
        return get(key, def);
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key null
     */
    public static int getAndSet(String key, int def) {
        ParametersChecker.checkParameter("Key", key);
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, Integer.toString(def));
            refresh();
            return def;
        }
        return get(key, def);
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified
     * default value if the property access fails.
     *
     * @param key
     * @param def
     * @return the property value. {@code def} if there's no such property or if an access to the specified property is
     *         not allowed.
     * @throws IllegalArgumentException key null
     */
    public static long getAndSet(String key, long def) {
        ParametersChecker.checkParameter("Key", key);
        if (!PROPS.containsKey(key)) {
            System.setProperty(key, Long.toString(def));
            refresh();
            return def;
        }
        return get(key, def);
    }

    /**
     * Set the value of the Java system property with the specified {@code key} to the specified default value.
     *
     * @param key
     * @param def
     * @return the ancient value.
     * @throws IllegalArgumentException key or def null
     */
    public static String set(String key, String def) {
        ParametersChecker.checkParameter("Key", key);
        if (def == null) {
            throw new IllegalArgumentException("Def cannot be null");
        }
        String old = null;
        if (PROPS.containsKey(key)) {
            old = PROPS.getProperty(key);
        }
        System.setProperty(key, def);
        refresh();
        return old;
    }

    /**
     * Set the value of the Java system property with the specified {@code key} to the specified default value.
     *
     * @param key
     * @param def
     * @return the ancient value.
     * @throws IllegalArgumentException key null
     */
    public static boolean set(String key, boolean def) {
        ParametersChecker.checkParameter("Key", key);
        boolean old = false;
        if (PROPS.containsKey(key)) {
            old = get(key, def);
        }
        System.setProperty(key, Boolean.toString(def));
        refresh();
        return old;
    }

    /**
     * Set the value of the Java system property with the specified {@code key} to the specified default value.
     *
     * @param key
     * @param def
     * @return the ancient value.
     * @throws IllegalArgumentException key null
     */
    public static int set(String key, int def) {
        ParametersChecker.checkParameter("Key", key);
        int old = 0;
        if (PROPS.containsKey(key)) {
            old = get(key, def);
        }
        System.setProperty(key, Integer.toString(def));
        refresh();
        return old;
    }

    /**
     * Set the value of the Java system property with the specified {@code key} to the specified default value.
     *
     * @param key
     * @param def
     * @return the ancient value.
     * @throws IllegalArgumentException key null
     */
    public static long set(String key, long def) {
        ParametersChecker.checkParameter("Key", key);
        long old = 0;
        if (PROPS.containsKey(key)) {
            old = get(key, def);
        }
        System.setProperty(key, Long.toString(def));
        refresh();
        return old;
    }

    /**
     * Remove the key of the Java system property with the specified {@code key}.
     *
     * @param key
     * @throws IllegalArgumentException key null
     */
    public static void clear(String key) {
        ParametersChecker.checkParameter("Key", key);
        if (PROPS.containsKey(key)) {
            PROPS.remove(key);
        }
        System.clearProperty(key);
        refresh();
    }

    /**
     * Print to System.out the content of the properties
     *
     * @param out the output stream to be used
     * @throws IllegalArgumentException out null
     */
    public static void debug(PrintStream out) {
        ParametersChecker.checkParameter("Out", out);
        PROPS.list(out);
    }

    /**
     * Inspired from http://commons.apache.org/lang/api-2.4/org/apache/commons/lang/ SystemUtils.html
     */
    public enum Platform {
        /**
         * Windows
         */
        WINDOWS,
        /**
         * Mac
         */
        MAC,
        /**
         * Unix
         */
        UNIX,
        /**
         * Solaris
         */
        SOLARIS,
        /**
         * Unsupported
         */
        UNSUPPORTED
    }

    /**
     * @return the Platform
     */
    public static Platform getOS() {
        if (m_os == null) {
            m_os = Platform.UNSUPPORTED;
            String os = "";
            try {
                os = System.getProperty("os.name").toLowerCase();
            } catch (Exception e) {// NOSONAR ignore
                // ignore
            }
            if (os.indexOf("win") >= 0) {
                m_os = Platform.WINDOWS;
                // Windows
            }
            if (os.indexOf("mac") >= 0) {
                m_os = Platform.MAC;
                // Mac
            }
            if (os.indexOf("nux") >= 0) {
                m_os = Platform.UNIX;
                // Linux
            }
            if (os.indexOf("nix") >= 0) {
                m_os = Platform.UNIX;
                // Unix
            }
            if (os.indexOf("sunos") >= 0) {
                m_os = Platform.SOLARIS; 
                // Solaris
            }
        }
        return m_os;
    }

    /**
     * @return True if Windows
     */
    public static boolean isWindows() {
        return getOS() == Platform.WINDOWS;
    }

    /**
     * @return True if Mac
     */
    public static boolean isMac() {
        return getOS() == Platform.MAC;
    }

    /**
     * @return True if Unix
     */
    public static boolean isUnix() {
        return getOS() == Platform.UNIX;
    }

    /**
     * @return True if Solaris
     */
    public static boolean isSolaris() {
        return getOS() == Platform.SOLARIS;
    }
}
package org.oucho.mpdclient.mpd;

import org.oucho.mpdclient.mpd.exception.InvalidResponseException;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Tools {

    public static final int KEY = 0;

    public static final int VALUE = 1;

    private Tools() {
        super();
    }

    private static String convertToHex(final byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder(data.length);
        for (byte aData : data) {
            int halfbyte = (aData >>> 4) & 0x0F;
            int twoHalves = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buffer.append((char) ('0' + halfbyte));
                } else {
                    buffer.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = aData & 0x0F;
            } while (twoHalves++ < 1);
        }

        return buffer.toString();
    }

    public static String getExtension(final String path) {
        final int index = path.lastIndexOf('.');
        final int extLength = path.length() - index - 1;
        final int extensionShort = 2;
        final int extensionLong = 4;
        String result = null;

        if (extLength >= extensionShort && extLength <= extensionLong) {
            result = path.substring(index + 1);
        }

        return result;
    }

    public static String getHashFromString(final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            final MessageDigest hashEngine = MessageDigest.getInstance("MD5");
            hashEngine.update(value.getBytes("iso-8859-1"), 0, value.length());
            return convertToHex(hashEngine.digest());
        } catch (final Exception e) {
            return null;
        }
    }

    public static boolean isNotEqual(final Object[][] arrays) {
        boolean result = false;

        for (final Object[] array : arrays) {
            if (isNotEqual(array[0], array[1])) {
                result = true;
                break;
            }
        }

        return result;
    }

    public static boolean isNotEqual(final Object objectA, final Object objectB) {
        final boolean isEqual;

        if (objectA == null) {
            isEqual = objectB == null;
        } else {
            isEqual = objectA.equals(objectB);
        }

        return !isEqual;
    }

    public static boolean isNotEqual(final int[][] arrays) {
        boolean result = false;

        for (final int[] array : arrays) {
            if (array[0] != array[1]) {
                result = true;
                break;
            }
        }

        return result;
    }

    public static List<String> parseResponse(final Collection<String> response, final String type) {
        final List<String> result = new ArrayList<>(response.size());

        for (final String[] lines : splitResponse(response)) {
            if (lines[KEY].equals(type)) {
                result.add(lines[VALUE]);
            }
        }

        return result;
    }

    static String[][] splitResponse(final String... list) {
        final String[][] results = new String[list.length][];
        int iterator = 0;

        for (final String line : list) {
            results[iterator] = splitResponse(line);
            iterator++;
        }

        return results;
    }

    public static String[][] splitResponse(final Collection<String> list) {
        final String[][] results = new String[list.size()][];
        int iterator = 0;

        for (final String line : list) {
            results[iterator] = splitResponse(line);
            iterator++;
        }

        return results;
    }

    private static String[] splitResponse(final String line) {
        final int delimiterIndex = line.indexOf(':');
        final String[] result = new String[2];

        if (delimiterIndex == -1) {
            throw new InvalidResponseException("Failed to parse server response key for line: " +
                    line);
        }

        result[0] = line.substring(0, delimiterIndex);

        /* Skip ': ' */
        result[1] = line.substring(delimiterIndex + 2);

        return result;
    }
}

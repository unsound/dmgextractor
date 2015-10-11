/*-
 * Copyright (C) 2008 Erik Larsson
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This code was written from studying vfdecrypt, Copyright (c) 2006
 *   Ralf-Philipp Weinmann <ralf@coderpunks.org>
 *   Jacob Appelbaum <jacob@appelbaum.net>
 *   Christian Fromme <kaner@strace.org>
 *
 * [I'm not sure if their copyright and license terms need to be applied,
 *  but in case they do, the original license terms are reprinted below
 *  as required by the license.]
 *
 * The vfdecrypt license says:
 *   Permission is hereby granted, free of charge, to any person
 *   obtaining a copy of this software and associated documentation
 *   files (the "Software"), to deal in the Software without
 *   restriction, including without limitation the rights to use,
 *   copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following
 *   conditions:
 *
 *   The above copyright notice and this permission notice shall be
 *   included in all copies or substantial portions of the Software.
 */

package org.catacombae.dmg.encrypted;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.LinkedList;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import org.catacombae.dmgextractor.Util;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public abstract class CommonCEncryptedEncodingHeader {
    public static CommonCEncryptedEncodingHeader create(V1Header header) {
        return new V1Implementation(header);
    }

    public static CommonCEncryptedEncodingHeader create(V2Header header) {
        return new V2Implementation(header);
    }

    public abstract int getBlockSize();
    public abstract long getBlockDataStart();

    public abstract KeyData[] getKeys();

    /**
     * Returns the amount of bytes at the end of the stream that are not part
     * of the block data area.
     * @return the amount of bytes at the end of the stream that are not part
     * of the block data area.
     */
    public abstract long getTrailingReservedBytes();

    /**
     * Returns the length of the data that has been encrypted. This length may
     * not be aligned with the encryption block size, in which case the last
     * encryption block will have been padded at the end
     * @return the length of the data that has been encrypted.
     */
    public abstract long getEncryptedDataLength();

    public static class KeySet {
        private final byte[] aesKey;
        private final byte[] hmacSha1Key;

        private KeySet(byte[] aesKey, byte[] hmacSha1Key) {
            this.aesKey = aesKey;
            this.hmacSha1Key = hmacSha1Key;
        }

        public byte[] getAesKey() { return aesKey; }
        public byte[] getHmacSha1Key() { return hmacSha1Key; }

        public void clearData() {
            Util.zero(aesKey);
            Util.zero(hmacSha1Key);
        }
    }

    public static abstract class KeyData {
        /**
         * Returns the salt for the key derivation function.
         * @return the salt for the key derivation function.
         */
        public abstract byte[] getKdfSalt();

        /**
         * Returns the iteration count for the key derivation function.
         * @return the iteration count for the key derivation function.
         */
        public abstract int getKdfIterationCount();


        /**
         * Returns the initialization vector for the key decryption cipher.
         * @return the initialization vector for the key decryption cipher.
         */
        public abstract byte[] getUnwrapInitializationVector();

        public abstract KeySet unwrapKeys(Key derivedKey, Cipher cph)
                throws GeneralSecurityException, InvalidKeyException,
                InvalidAlgorithmParameterException;
    }

    private static class V1Implementation extends CommonCEncryptedEncodingHeader {
        private final V1Header header;
        private final V1KeyDataImplementation keyData;

        public V1Implementation(V1Header header) {
            this.header = header;
            this.keyData = new V1KeyDataImplementation(header);
        }

        @Override
        public int getBlockSize() {
            return header.getBlockSize();
        }

        @Override
        public long getBlockDataStart() {
            return 0; // By design (I think).
        }

        @Override
        public long getTrailingReservedBytes() {
            return header.length();
        }

        @Override
        public long getEncryptedDataLength() {
            return header.getDecryptedDataLength(); // Confusion!
        }

        @Override
        public KeyData[] getKeys() {
            return new KeyData[] { keyData };
        }
    }

    private static class V1KeyDataImplementation extends KeyData {
        private V1Header header;

        private V1KeyDataImplementation(V1Header header) {
            this.header = header;
        }

        @Override
        public byte[] getKdfSalt() {
            return Util.createCopy(header.getKdfSalt(), 0, header.getKdfSaltLen());
        }

        @Override
        public int getKdfIterationCount() {
            return header.getKdfIterationCount();
        }

        @Override
        public byte[] getUnwrapInitializationVector() {
            return header.getUnwrapIv();
        }

        @Override
        public KeySet unwrapKeys(Key derivedKey, Cipher cph)
                throws GeneralSecurityException, InvalidKeyException,
                InvalidAlgorithmParameterException {
            byte[] aesKey = unwrapIndividualKey(derivedKey, cph,
                    Util.createCopy(header.getWrappedAesKey(), 0, header.getLenWrappedAesKey()));

            byte[] hmacSha1Key = unwrapIndividualKey(derivedKey, cph,
                    Util.createCopy(header.getWrappedHmacSha1Key(), 0, header.getLenWrappedHmacSha1Key()));
            return new KeySet(aesKey, hmacSha1Key);
        }

        public byte[] unwrapIndividualKey(Key key, Cipher cph, byte[] wrappedKey)
                throws InvalidKeyException, InvalidAlgorithmParameterException,
                GeneralSecurityException {
            Debug.print("unwrapIndividualKey(" + key + ", " + cph + ", byte[" + wrappedKey.length + "]);");
            Debug.print("  wrappedKey: 0x" + Util.byteArrayToHexString(wrappedKey));

            final byte[] initialIv = new byte[] {
                (byte) 0x4a, (byte) 0xdd, (byte) 0xa2, (byte) 0x2c,
                (byte) 0x79, (byte) 0xe8, (byte) 0x21, (byte) 0x05
            };
            // irX = intermediate result X

            byte[] ir1 = new byte[wrappedKey.length];
            cph.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(initialIv));
            int ir1Len = cph.doFinal(wrappedKey, 0, wrappedKey.length, ir1, 0);
            Debug.print("  ir1: 0x" + Util.byteArrayToHexString(ir1, 0, ir1Len));
            Debug.print("  ir1Len: " + ir1Len);

            byte[] ir2 = new byte[ir1Len];
            for(int i = 0; i < ir1Len; ++i)
                ir2[i] = ir1[ir1Len-1-i];
            Debug.print("  ir2: 0x" + Util.byteArrayToHexString(ir2));
            Debug.print("  ir2.length: " + ir2.length);

            byte[] ir3 = new byte[ir2.length-8];
            cph.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ir2, 0, 8));
            int ir3Len = cph.doFinal(ir2, 8, ir2.length-8, ir3, 0);
            Debug.print("  ir3: 0x" + Util.byteArrayToHexString(ir3, 0, ir3Len));
            Debug.print("  ir3Len: " + ir3Len);

            byte[] result = Util.createCopy(ir3, 4, ir3Len-4);
            Util.zero(ir1, ir2, ir3);
            return result;
        }
    }

    private static class V2Implementation extends CommonCEncryptedEncodingHeader {
        private final V2Header header;
        private final V2KeyDataImplementation[] keys;

        public V2Implementation(V2Header header) {
            this.header = header;

            LinkedList<V2KeyDataImplementation> keyList =
                    new LinkedList<V2KeyDataImplementation>();
            for(V2Header.KeyData kd : header.getKeys()) {
                if(kd instanceof V2Header.UserKeyData) {
                    keyList.add(new V2KeyDataImplementation(header,
                            (V2Header.UserKeyData) kd));
                }
            }
            this.keys = keyList.toArray(
                    new V2KeyDataImplementation[keyList.size()]);
        }

        @Override
        public int getBlockSize() {
            return header.getBlockSize();
        }

        @Override
        public long getBlockDataStart() {
            return header.getOffsetToDataStart();
        }

        @Override
        public long getTrailingReservedBytes() {
            return 0;
        }


        @Override
        public long getEncryptedDataLength() {
            return header.getEncryptedDataLength();
        }

        @Override
        public KeyData[] getKeys() {
            return Util.arrayCopy(keys, new KeyData[keys.length]);
        }
    }

    private static class V2KeyDataImplementation extends KeyData {
        private final V2Header header;
        private final V2Header.UserKeyData keyData;

        private V2KeyDataImplementation(V2Header header,
                V2Header.UserKeyData keyData)
        {
            this.header = header;
            this.keyData = keyData;
        }

        @Override
        public byte[] getKdfSalt() {
            return Util.createCopy(keyData.getKdfSalt(), 0,
                    keyData.getKdfSaltLen());
        }

        @Override
        public int getKdfIterationCount() {
            return keyData.getKdfIterationCount();
        }

        @Override
        public byte[] getUnwrapInitializationVector() {
            return Util.createCopy(keyData.getBlobEncIv(), 0,
                    keyData.getBlobEncIvSize());
        }

        private byte[] getEncryptedKeyBlob() {
            return Util.createCopy(keyData.getEncryptedKeyblob(), 0,
                    keyData.getEncryptedKeyblobSize());
        }

        @Override
        public KeySet unwrapKeys(Key derivedKey, Cipher cph)
                throws InvalidKeyException, InvalidAlgorithmParameterException, GeneralSecurityException {

            final int keyBytes = (header.getKeyBits() + 7) / 8;

            Debug.print("V2Implementation.unwrapKeys(" + derivedKey + ", " + cph + ");");
            cph.init(Cipher.DECRYPT_MODE, derivedKey, new IvParameterSpec(getUnwrapInitializationVector()));

            byte[] encryptedKeyBlob = getEncryptedKeyBlob();
            Debug.print("  encryptedKeyBlob.length=" + encryptedKeyBlob.length);
            byte[] decryptedKeyBlob = new byte[encryptedKeyBlob.length];
            Debug.print("  doing update....");
            int bp = cph.update(encryptedKeyBlob, 0, encryptedKeyBlob.length, decryptedKeyBlob);
            Debug.print("    bp == " + bp);
            Debug.print("  doing final....");
            bp += cph.doFinal(decryptedKeyBlob, bp);
            Debug.print("    bp == " + bp);

            Debug.print("  decryptedKeyBlob: 0x" + Util.byteArrayToHexString(decryptedKeyBlob));
            byte[] aesKey = new byte[keyBytes];
            byte[] hmacSha1Key = new byte[20];
            System.arraycopy(decryptedKeyBlob, 0, aesKey, 0, keyBytes);
            Debug.print("  aesKey: 0x" + Util.byteArrayToHexString(aesKey));
            System.arraycopy(decryptedKeyBlob, keyBytes, hmacSha1Key, 0, 20);
            Debug.print("  hmacSha1Key: 0x" + Util.byteArrayToHexString(hmacSha1Key));

            Util.zero(decryptedKeyBlob); // No unused secret data in memory.
            Debug.print("  decryptedKeyBlob: 0x" + Util.byteArrayToHexString(decryptedKeyBlob));

            Debug.print("returning from V2Implementation.unwrapKeys...");
            return new KeySet(aesKey, hmacSha1Key);
        }
    }
}

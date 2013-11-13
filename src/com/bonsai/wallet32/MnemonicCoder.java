// Copyright (C) 2013  Bonsai Software, Inc.
// 
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.bonsai.wallet32;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.spongycastle.crypto.engines.RijndaelEngine;
import org.spongycastle.crypto.params.KeyParameter;

import android.content.Context;

public class MnemonicCoder {

    private ArrayList<String>	mWordList;

    MnemonicCoder(Context context) throws IOException {
        // Initialize the words array.
        InputStream wis = context.getAssets().open("wordlist/english.txt");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(wis, "UTF-8"));
        String word;
        mWordList = new ArrayList<String>();
        while ((word = br.readLine()) != null)
            mWordList.add(word);
        br.close();
    }

    public List<String> encode(byte[] seed) throws RuntimeException {
        int len = seed.length * 8;
        
        if (len != 128 && len != 192 && len != 256)
            throw new RuntimeException("seed not 128, 192 or 256 bits");

        byte[] data = stretch(len, seed);

        boolean[] bb = new boolean[data.length * 8];
        for (int ii = 0; ii < data.length; ++ii)
            for (int jj = 0; jj < 8; ++jj)
                bb[(ii * 8) + jj] = (data[ii] & (1 << (7 - jj))) != 0;

        boolean[] cc = checksum(bb);

        boolean[] ee = new boolean[bb.length + cc.length];
        for (int ii = 0; ii < bb.length; ++ii)
            ee[ii] = bb[ii];
        for (int ii = 0; ii < cc.length; ++ii)
            ee[bb.length + ii] = cc[ii];

        ArrayList<String> words = new ArrayList<String>();
        int nwords = ee.length / 11;
        for (int ii = 0; ii < nwords; ++ii) {
            int ndx = 0;
            for (int jj = 0; jj < 11; ++jj) {
                ndx <<= 1;
                if (ee[(ii * 11) + jj])
                    ndx |= 0x1;
            }
            words.add(mWordList.get(ndx));
        }
            
        return words;
    }

    public byte[] decode(List<String> words) throws RuntimeException {
        int nwords = words.size();

        if (nwords != 12 && nwords != 18 && nwords != 24)
            throw new RuntimeException("Mnemonic code not 12, 18 or 24 words");

        int len = nwords * 11;

        boolean[] ee = new boolean[len];
        int kk = 0;
        for (String word : words) {
            int ndx = Collections.binarySearch(mWordList, word);
            if (ndx < 0)
                throw new RuntimeException("\"" + word + "\" invalid");
            for (int jj = 0; jj < 11; ++jj)
                ee[(kk * 11) + jj] = (ndx & (1 << (10 - jj))) != 0;
            ++kk;
        }

        int bblen = (len / 33) * 32;
        int cclen = len - bblen;
        
        boolean[] bb = new boolean[bblen];
        for (int ii = 0; ii < bblen; ++ii)
            bb[ii] = ee[ii];

        boolean[] cc = new boolean[cclen];
        for (int ii = 0; ii < cclen; ++ii)
            cc[ii] = ee[bblen + ii];

        boolean[] ncc = checksum(bb);

        if (!Arrays.equals(ncc, cc))
            throw new RuntimeException("checksum error");

        byte[] data = new byte[bblen / 8];
        for (int ii = 0; ii < data.length; ++ii)
            for (int jj = 0; jj < 8; ++jj)
                if (bb[(ii * 8) + jj])
                    data[ii] |= 1 << (7 - jj);

        byte[] seed = unstretch(bblen, data);

        return seed;
    }

    private byte[] stretch(int len, byte[] data) throws RuntimeException {
        byte[] mnemonic = {'m', 'n', 'e', 'm', 'o', 'n', 'i', 'c'};
        byte[] key;
        try {
            key = MessageDigest.getInstance("SHA-256").digest(mnemonic);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);		// Can't happen.
        }
        RijndaelEngine cipher = new RijndaelEngine(len);
        cipher.init(true, new KeyParameter(key));
        for (int ii = 0; ii < 10000; ++ii)
            cipher.processBlock(data, 0, data, 0);
        return data;
    }

    private byte[] unstretch(int len, byte[] data) throws RuntimeException {
        byte[] mnemonic = {'m', 'n', 'e', 'm', 'o', 'n', 'i', 'c'};
        byte[] key;
        try {
            key = MessageDigest.getInstance("SHA-256").digest(mnemonic);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);		// Can't happen.
        }
        RijndaelEngine cipher = new RijndaelEngine(len);
        cipher.init(false, new KeyParameter(key));
        for (int ii = 0; ii < 10000; ++ii)
            cipher.processBlock(data, 0, data, 0);
        return data;
    }

    private boolean[] checksum(boolean[] bits) {
        int ll = bits.length / 32;
        boolean[] cc = new boolean[ll];
        for (int ii = 0; ii < 32; ++ii)
            for (int jj = 0; jj < ll; ++jj)
                cc[jj] ^= bits[(ii * ll) + jj];
        return cc;
    }
}

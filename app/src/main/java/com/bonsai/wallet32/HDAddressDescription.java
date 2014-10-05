// Copyright (C) 2014  Bonsai Software, Inc.
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

// Used to return an HDAddress and it's location in the HDWallet.
public class HDAddressDescription {
    public HDAccount	hdAccount;
    public HDChain		hdChain;
    public HDAddress	hdAddress;

    public HDAddressDescription(HDChain chain, HDAddress addr) {
        this.hdAccount = null;
        this.hdChain = chain;
        this.hdAddress = addr;
    }

    public void setHDAccount(HDAccount hdAccount) {
        this.hdAccount = hdAccount;
    }

    public String toString() {
        return String.format("%s:%s:%s:%s",
                             hdAccount.getName(),
                             hdChain.isReceive() ? "R" : "C",
                             hdAddress.getPath(),
                             hdAddress.getAbbrev());
    }
}

// Local Variables:
// mode: java
// c-basic-offset: 4
// tab-width: 4
// End:

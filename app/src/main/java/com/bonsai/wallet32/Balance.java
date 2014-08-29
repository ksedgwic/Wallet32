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

public class Balance {
    public final int accountId;
    public final String accountName;
    public final long balance;
    public final long available;
    public Balance(int id, String name, long balance, long available) {
        this.accountId = id;
        this.accountName = name;
        this.balance = balance;
        this.available = available;
    }
}

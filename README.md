![Wallet32](walrus-256.png)

A BIP-0032 Hierarchical Deterministic Bitcoin Wallet
====================================================

This wallet uses BIP-0032 to implement a hierarchical deterministic
wallet.

Features include:

* Multiple logical "accounts" within each wallet.
* Fresh receive and change addresses are used for each transfer.
* Wallet only needs to be backed up once, on initial creation.
* Wallet backup consists of simple list of 12 common words (BIP-0039).
* Same wallet may be securely accessed concurrently from multiple devices.
* Wallet data is protected by scrypt passcode.

Alpha Test Version
================

An alpha test version of Wallet32 is available in the google play store.
You'll need to join the Wallet32 Google+ community to get access to the
pre-release version:

    https://plus.google.com/u/0/communities/112340435878616981465

Instructions for installing the alpha version are in the first post ...

Building Wallet32
===============

### Setup

Clone the project source from github:

    git clone git@github.com:ksedgwic/Wallet32.git

Import the project into Eclipse:

File -> Import ... -> General -> Existing Projects into Workspace

Add v7 appcompat to Eclipse:

http://developer.android.com/tools/support-library/setup.html#libs-with-res


About Wallet32
================

Wallet32

Version: 0.1.11 (30-Jan-2014)

Source:  https://github.com/ksedgwic/Wallet32

Author:  Ken Sedgwick <ken@bonsai.com>

Wallet32 is made possible by:
* bitcoinj    https://code.google.com/p/bitcoinj/
* zxscanlib   https://github.com/LivotovLabs/zxscanlib

Donate: 1DjvS5TGhTaTaQj8Q6Tnw24irHKhaCbAbk

Copyright (C) 2013-2014 Bonsai Software, Inc.  All rights reserved.

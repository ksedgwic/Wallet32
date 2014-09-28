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

Wallet32 is available in the Google play store:

[https://play.google.com/store/apps/details?id=com.bonsai.wallet32](https://play.google.com/store/apps/details?id=com.bonsai.wallet32)

Beta Test Version
================

A beta test version of Wallet32 is available in the google play store
to members of the bitcoinj google group.  If you are in the bitcoinj
google group you can click on this link to install the beta:

[https://play.google.com/apps/testing/com.bonsai.wallet32](https://play.google.com/apps/testing/com.bonsai.wallet32)

Alpha Test Version
================

An alpha test version of Wallet32 is available in the google play store.
You'll need to join the
[Wallet32 Google+ Community](https://plus.google.com/u/0/communities/112340435878616981465) to get access to the
pre-release version.

Instructions for installing the alpha version are in the first post ...

Building Wallet32
===============

### Install zxscanlib to local maven repository

    git clone git@github.com:LivotovLabs/zxscanlib.git

    cd zxscanlib

    patch -p1 <<"EOF"
    --- a/build.gradle
    +++ b/build.gradle
    @@ -1,12 +1,38 @@
     apply plugin: 'android-library'
     
    +apply plugin: 'maven'
    +
    +uploadArchives {
    +    repositories {
    +        mavenDeployer {
    +            repository(url: mavenLocal().url)
    +            pom.groupId = 'eu.livotov'
    +            pom.artifactId = 'zxscan'
    +            pom.version = '1.1'
    +        }
    +    }
    +}
    +
    +buildscript {  
    +    repositories {
    +        mavenCentral()
    +    }
    +    dependencies {
    +        classpath 'com.android.tools.build:gradle:0.10.+'
    +    }
    +}
    +
     dependencies {
         compile fileTree(dir: 'libs', include: '*.jar')
     }
     
     android {
         compileSdkVersion 17
    -    buildToolsVersion "18.0.1"
    +    buildToolsVersion "19.1.0"
    +
    +    lintOptions {
    +        abortOnError false
    +    }
     
         sourceSets {
             main {
    EOF

    gradle uploadArchives     

### Build Wallet32 with Gradle

    git clone git@github.com:ksedgwic/Wallet32.git

    cd Wallet32

    ./gradlew clean app:assembleDebug

### Build Wallet32 with Android Studio

    git clone git@github.com:ksedgwic/Wallet32.git

    # Run Android Studio

    # Import Project ...", select Wallet32 top-level directory.


About Wallet32
================

Wallet32

Version: 0.3.14 (28-Sep-2014)

Source:  https://github.com/ksedgwic/Wallet32

Author:  Ken Sedgwick <ken@bonsai.com>

With contributions from:
* Karel Bílek
* Harald Hoyer

Wallet32 is made possible by:
* [bitcoinj](https://code.google.com/p/bitcoinj/)
* [bitcoin-wallet](https://github.com/schildbach/bitcoin-wallet)
* [ZXScanLib](https://github.com/LivotovLabs/zxscanlib)
* [blockchain.info](https://blockchain.info)
* [Bitstamp](https://www.bitstamp.net)

Data Provided by [WINKDEX&#8480;](http://www.winkdex.com/)

Powered by [CoinDesk](http://www.coindesk.com/price)

Donate: [14qJ7PAfKCBtcMHzQNerHwUtgt4HVSZFRL](bitcoin:14qJ7PAfKCBtcMHzQNerHwUtgt4HVSZFRL)

Copyright (C) 2013-2014 Bonsai Software, Inc.  All rights reserved.

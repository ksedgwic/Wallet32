Bugs
----------------------------------------------------------------

Needed
----------------------------------------------------------------

* Explore adding space in value formats. eg:   1 028 543.06

* Make additional passcode entry based on timeout instead of always.

* Advanced Features Setting (send change to account, private key ops)

* Multiple wallets

* Testnet build.

* Electrum compatibility.

* Embelish messages and directions.

* Send dump feature.

* Tune for slow platforms.

* Make passcode creation estimate of brute-force difficulty.

* Add transaction label support.

* Auto logout of service after idle for N minutes.

* Non-USD fiat support.

* Font selections for tables.

* Factor wallet file prefix into single location.


Checklist
----------------------------------------------------------------

Update AndroidManifest.xml

Update README.txt version.

Update res/values/strings.xml version.

Commit, tag, git push --tag

Clean project.

Smoke test on attached device.

Build Wallet32.apk.

scp ~/bonsai/workspace/Wallet32.apk sl0:htdocs/ken/public/

Upload to Google Play Developer Console.

Update Recent Changes.

----------------------------------------------------------------

http://stackoverflow.com/questions/15770215/ccats-on-ios-appstore-and-encryption

https://www.bis.doc.gov/index.php/policy-guidance/encryption

Is the item 
publicly available 
encryption 
source code? 

Self Classify as 5D002. See 
License Exception TSU (740.13(e)) 
for notification requirement 

http://www.apache.org/licenses/exports/

ERN0001

Title	Encryption Registration ERN0001 Accepted with ERN R106015
Type	Acknowledgement
Date of Message	 12/19/2013
Reference Number	 ERN0001
Content	
Thank you for submitting the information in Supplement 5 to Part 742.
Your ERN is R106015.

Based on this submission you are authorized to export or reexport
encryption products described in Section 740.17(b)(1) of the EAR and
mass market encryption products described in Section 742.15(b)(1) of
the EAR.  You are required to submit an annual self-classification
report (Supplement 8 to Part 742) for these products pursuant to the
requirements in Section 742.15(c) of the EAR.

----------------------------------------------------------------

* Improved initial startup speed.
* Updated to bitcoinj-0.11-SNAPSHOT.
* Added sync completion estimate.
* Fixed missing addresses bug in add-account.

----------------------------------------------------------------

Kenstigator: yes, that's usually caused by protobufs being code hungry.
7:24 Kenstigator: do you have any custom proto types there? i'd be surprised if for some reason you hit that but Andreas never did
7:24 Kenstigator: you can try switching to the lite protobuf runtime, and make sure the protobufs are being optimized for code size
7:24 Kenstigator: but try asking goonie

----------------------------------------------------------------

Create New Wallet

    Abort during sync, restart.

    Receive 0.0023 to acct0.

    Attempt send to acct2 before cleared (insufficient funds expected).

    Add acct2.

    Send 0.001 to acct2.

    Send 0.001 from acct0 back to external.

    Verify margin in each account.

    Logout, re-login.


Restore Wallet

    Restore using word list w/ 3 accounts.

    Verify margin in each account.

    Rename acct0, acct1, acct2.

    Send 0.001 from acct2 to external.

    Change passcode.

    Logout, re-login w/ new passcode.

    Verify account names.

    Verify margin in each account.

Rescan blockchain.

Restore Big Wallet (hex seed this time).

    Confirm margin (likely not good).

    Rescan to fix missing addresses.

----------------------------------------------------------------

devrandom
11:01 something like this:
11:01         Sha256Hash hash = tx.hashForSignature(inputIndex, redeemScript.getProgram(), Transaction.SigHash.ALL, false);
11:01         TransactionSignature signature = new TransactionSignature(key.sign(hash), Transaction.SigHash.ALL, false);
11:02 actually, let me check if it's easier to import the keys

devrandom
11:03 Kenstigator: yes, it might be easier to import the key, then you can do:
devrandom
11:03 tx.signInputs(Transaction.SigHash.ALL, wallet)

Kenstigator: so the inputScripts are available from the unspent too (hex decode, pass to Script)

Kenstigator: the code shouldn't be so hard to figure out - would you
mind doing a couple of things for me? (1) writing up your experience
and what you found confusing/hard, so i can write better docs. and (2)
maybe post your code so we can integrate it into the library

----------------------------------------------------------------

http://brainwallet.org/#tx

https://blockchain.info/pushtx
https://blockchain.info/decode-tx

----------------------------------------------------------------

https://play.google.com/apps/testing/com.bonsai.wallet32

----------------------------------------------------------------

Wallet32: A Hierarchical Deterministic Bitcoin Wallet for Android

Wallet32 is a BIP-0032 hierarchical deterministic bitcoin wallet for
android. Features include:

* Multiple logical "accounts" within each wallet.
* Fresh receive and change addresses are used for each transfer.
* Wallet only needs to be backed up once, on initial creation.
* Wallet backup consists of simple list of 12 common words (BIP-0039).
* Same wallet may be securely accessed concurrently from multiple devices.
* Wallet data is protected by scrypt passcode.

The wallet is available in the Google play store at:

https://play.google.com/store/apps/details?id=com.bonsai.wallet32

Source code available here:

https://github.com/ksedgwic/Wallet32

Please let me know what you think!

Ken

----------------------------------------------------------------

Old Donation Address

1DjvS5TGhTaTaQj8Q6Tnw24irHKhaCbAbk

----------------------------------------------------------------

7. What is "T" an "Path" in the account overview? Does the user need to
know that?

The "T" column is the number of transactions for each address.  It is
pretty important in an HD Wallet because "holes" (sequential groups of
addresses without any transactions) make wallet recovery more
difficult.

The "Path" column is the HD tree location of the address.  It's useful
when cross-referencing addresses in the view transaction activity.

16. I haven't tried, but there should be a waring about a too low/high fee.

There are warnings for fee too high and fee too low.

25. Sweep view asks for private key or address. What is the use of an
address there?

I wanted a way to verify funds on a public identifier without actually
sweeping the private key.  I shoehorned the function here.  I agree
that it is not well documented and confusing.  I think I should make
a separate "scan public identifier" activity instead.

----------------------------------------------------------------
Fixed

2. Decrypt dialog shouldn't be dismissable by tapping outside. Doing so
and entering a wrong code afterwards leads to decrypt, then force
close. After the crash it tries to start again and crashes with a NPE
at com.bonsai.wallet32.WalletService.onStartCommand(WalletService.java:533)

3. I think limiting the passcode length isn't a bad idea. You can push
everything out of view with huge codes. Props for making 4 digit
blocks though!

9. I'd consider copying a private key to clipboard a super risky
operation. Pattern matching malware watching the clipboard?

10. Click on "Edit" for account name should give focus to the text field.

12. Adding an account gives no progress feedback. Again UI looks
unresponsive. Not sure if Settings is the right place to have this
too.

13. Scanning a non bitcoin QR crashes the wallet with a NPE at
com.bonsai.wallet32.SweepKeyActivity$5.handleMessage(SweepKeyActivity.java:484)
(Same crash loop as mentioned before afterwards)

14. Start app -> Menu -> About -> Menu -> Settings. Doesn't show anything
sensible, but still shouldn't be accessible that way.

18. More than 8 accounts start pushing out of the view

19. QR scanner repsonse sound is way too loud

20. QR scanner needs a target area as it won't scan on the whole image

----------------------------------------------------------------
Issues

11. Settings -> View Seed is doubling of the menu. Also, Pressing back
from this screen doesn't go back to settings, but the main screen

Back from View Seed goes to wherever Settings was selected from.  I
don't think anything from Settings goes back to Settings.  Not sure
what the consistent thing to do here is.

----------------------------------------------------------------

1. Del button in bottom right is a bad place imho as there are usually
the Submit buttons. Maybe get rid of the "Clr" button?

4. Sync Progress dialog takes half a minute until it starts
processing. Can confuse users

5. Progress bar in said dialog is in old Android style

6. PIN change should ask for the old PIN first, even if the wallet was
unlocked before.

8. Hitting a specific address is pretty hard in that list.

15. You can create receiving requests with more than 21mil coins.

17. Soft keyboard goes over the activity. Instead, it should push it up so
it doesn't cover the UI

21. The wallet doesn't lock itself right away when using the home
button. It also stays in the recent apps list potentially showing
sensitive info on the preview image

22. Starting activities via command line bypasses PIN, but doesn't show
anything sensitive as the wallet is encrypted. (e.g. adb shell am
start -n com.bonsai.wallet32/.MainActivity)

23. Just my opinion: Overall design wastes a lot of screen real estate. It
also look too "clean". Not a fan of having averything centered.

24. Showing a toast on each and every character input at the address field
is too much.

25. Sweep view asks for private key or address. What is the use of an
address there? Also, trying to manually enter anything crashes with a
NPE at com.bonsai.wallet32.SweepKeyActivity$5.handleMessage(SweepKeyActivity.java:484)

----------------------------------------------------------------

    <string name="balance_header_btc">BTC</string>
	android:id="@+id/header_btc"

    <string name="address_header_btc">BTC</string>
	android:id="@+id/header_btc"

    <string name="send_header_btc">BTC</string>
      android:id="@+id/header_btc"

    <string name="receive_header_btc">BTC</string>
      android:id="@+id/header_btc"

    <string name="transaction_header_btc">BTC</string>
	android:id="@+id/header_btc"

    <string name="transputs_header_btc">BTC</string>
	android:id="@+id/header_btc"

    <string name="btc_label">BTC</string>
	      android:id="@+id/amount_btc_label"
    	      android:id="@+id/amount_btc_label"
    	      android:id="@+id/fee_btc_label"
    	      android:id="@+id/balance_btc_label"
    	      android:id="@+id/fee_btc_label"
	    android:id="@+id/header_btc"

    <string name="wallet_service_note_sent_title">BTC Sent</string>
    <string name="wallet_service_note_sent_msg">%1$s BTC sent</string>
    <string name="wallet_service_note_rcvd_title">BTC Received</string>
    <string name="wallet_service_note_rcvd_msg">%1$s BTC received</string>
    <string name="wallet_service_note_scnf_title">Send Confirmed</string>
    <string name="wallet_service_note_scnf_msg">%1$s BTC confirmed</string>
    <string name="wallet_service_note_rcnf_title">Receive Confirmed</string>
    <string name="wallet_service_note_rcnf_msg">%1$s BTC confirmed</string>
    <string name="wallet_service_note_rdead_title">Receive Dead</string>
    <string name="wallet_service_note_rdead_msg">%1$s BTC dead</string>
    <string name="wallet_service_note_sdead_title">Send Dead</string>
    <string name="wallet_service_note_sdead_msg">%1$s BTC dead</string>

----------------------------------------------------------------

			Wallet Format 0.4
      	 		Standard Account Derivation
			bip39_version = V0_6
			acct_derive = HDSV_STDV0
30-Mar-2014	v0.2.2
			Wallet Format 0.3
			Level 0 Private Account Derivation
			bip39_version = V0_6
			acct_derive = HDSV_L0PRV
14-Mar-2014	v0.1.18
			Wallet Format 0.2
			Level 0 Public Account Derivation
			bip39_version = V0_6
			acct_derive = HDSV_L0PUB
19-Jan-2014	v0.1.8
			Wallet Format 0.1
			Legacy BIP-0039
			bip39_version = V0_5
			acct_derive = HDSV_L0PUB

================================================================

https://code.google.com/p/maven-android-plugin/wiki/GettingStarted

rgladwell.github.io/m2e-android/

stand.spree.de/wiki_details_maven_archetypes

    export JAVA_HOME=/usr/lib/jvm/java
    export ANDROID_HOME=/usr/local/adt-bundle-linux-x86_64/sdk
    export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH

    mvn archetype:generate \
      -DarchetypeArtifactId=android-quickstart \
      -DarchetypeGroupId=de.akquinet.android.archetypes \
      -DarchetypeVersion=1.0.11 \
      -DgroupId=com.bonsai \
      -DartifactId=Wallet32 \
      -Dplatform=18 \
      -Dpackage=com.bonsai.wallet32

    <platform.version>4.1.1.4</platform.version>

    <android.plugin.version>3.8.0</android.plugin.version>

    mvn clean install android:deploy

    slf4j-api-1.7.6.jar
    logback-android-1.1.1-1.jar
    bitcoinj-0.12-SNAPSHOT-bundl

    

    mvn install android:deploy android:run

    mvn install -Dmaven.test.skip=true

Tracing
----------------------------------------------------------------

    <!-- DEBUG TRACING -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    import android.os.Debug;

    Debug.startMethodTracing("w32");
    ...
    Debug.stopMethodTracing();

zxscanlib
----------------------------------------------------------------

Installed gradle:

    cd /usr/local
    unzip gradle-1.12-all.zip

cloned zxscanlib:

    cd /tmp
    git clone git@github.com:LivotovLabs/zxscanlib.git

patched build.gradle:

    cd /tmp/zxscanlib

    diff --git a/build.gradle b/build.gradle
    index 12b9428..f09fc1e 100644
    --- a/build.gradle
    +++ b/build.gradle
    @@ -1,12 +1,27 @@
     apply plugin: 'android-library'
     
    +apply plugin: 'maven'
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
        
built library:

    cd /tmp/zxscanlib

    /usr/local/gradle-1.12/bin/gradle install

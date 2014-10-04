Bugs
----------------------------------------------------------------

Needed
----------------------------------------------------------------

* Add transaction label support.

* Non-USD fiat support.

* Factor wallet file prefix into single location.

* Investigate Slush's Sync Hang.

* Consider 18 and 24 word wallet seeds.

* Smash case on wallet word lists.

* Set lower-case mode on keyboard during word list entry.

* Fix wallet encryption on bitcoinj-hdw branch.

* Fix P2SH transaction scoring (see log below).


Checklist
----------------------------------------------------------------

Update versions in app/build.gradle

Update versions in AndroidManifest.xml

Update version, date in README.txt.

Update version, date, in res/values/strings.xml.

Commit, tag, git push --tag

Generate signed apk in Android Studio.

scp app/app-release.apk sl0:htdocs/ken/public/Wallet32.apk

Smoke test downloaded

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
               
build install into local maven repository:

    cd /tmp/zxscanlib

    /usr/local/gradle-1.12/bin/gradle uploadArchives

ZipAlign
----------------------------------------------------------------

Clashes w/ late-model android.plugin.version.

Late-model android.plugin.version needed to avoid multiple BuildConfig
issue w/ gradle written aar files.

      <plugin>
        <groupId>com.jayway.maven.plugins.android.generation2</groupId>
        <artifactId>maven-android-plugin</artifactId>
        <inherited>true</inherited>
        <configuration>
          <sign>
            <debug>false</debug>
          </sign>
          <zipalign>
            <verbose>true</verbose>
            <inputApk>${project.build.directory}/${project.artifactId}.apk</inputApk>
            <outputApk>${project.build.directory}/${project.artifactId}-signed-aligned.apk
            </outputApk>
          </zipalign>
        </configuration>
        <executions>
          <execution>
            <id>alignApk</id>
            <phase>package</phase>
            <goals>
              <goal>zipalign</goal>
            </goals>
          </execution>
        </executions>
      </plugin>


Wallet32 Issue w/ P2SH Stacktrace
----------------------------------------------------------------

Wallet restoration broken on HDW branch.  Check wallet create.
Fix wallet encryption on bitcoinj-hdw branch.
> 1. Incorporate KeyCrypter.
> 2. Disable 100 key intrinsic chain.
> 3. Remove scan time workaround key.


Investigate Slush's Sync Hang.

Smash case on wallet word lists.

Set lower-case mode on keyboard during word list entry.

06-21 19:14:59.711 I/MemoryPool(28320): [NioClientManager] [176.223.201.250]:8333: Peer announced new transaction [1] 6d803430f02fdd7d5cb659d9e0cbc978033273d04d686453bf3e1b1ede7ba02d
06-21 19:14:59.711 W/System.err(28320): 	at com.bonsai.wallet32.HDWallet.applyAllTransactions(HDWallet.java:578)
06-21 19:14:59.711 W/System.err(28320): 	at com.bonsai.wallet32.WalletService$SetupWalletTask.doInBackground(WalletService.java:566)
06-21 19:14:59.711 W/System.err(28320): 	at com.bonsai.wallet32.WalletService$SetupWalletTask.doInBackground(WalletService.java:442)
06-21 19:14:59.711 W/System.err(28320): 	at android.os.AsyncTask$2.call(AsyncTask.java:288)
06-21 19:14:59.711 W/System.err(28320): 	at java.util.concurrent.FutureTask.run(FutureTask.java:237)
06-21 19:14:59.711 W/System.err(28320): 	at android.os.AsyncTask$SerialExecutor$1.run(AsyncTask.java:231)
06-21 19:14:59.711 W/System.err(28320): 	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1112)
06-21 19:14:59.711 W/System.err(28320): 	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:587)
06-21 19:14:59.711 W/System.err(28320): 	at java.lang.Thread.run(Thread.java:841)
06-21 19:14:59.721 W/System.err(28320): com.google.bitcoin.core.ScriptException: Script not of right size, expecting 2 but got 4
06-21 19:14:59.731 W/System.err(28320): 	at com.google.bitcoin.script.Script.getPubKey(Script.java:277)
06-21 19:14:59.731 W/System.err(28320): 	at com.bonsai.wallet32.HDWallet.applyAllTransactions(HDWallet.java:578)
06-21 19:14:59.731 W/System.err(28320): 	at com.bonsai.wallet32.WalletService$SetupWalletTask.doInBackground(WalletService.java:566)
06-21 19:14:59.731 W/System.err(28320): 	at com.bonsai.wallet32.WalletService$SetupWalletTask.doInBackground(WalletService.java:442)
06-21 19:14:59.731 W/System.err(28320): 	at android.os.AsyncTask$2.call(AsyncTask.java:288)
06-21 19:14:59.731 W/System.err(28320): 	at java.util.concurrent.FutureTask.run(FutureTask.java:237)
06-21 19:14:59.731 W/System.err(28320): 	at android.os.AsyncTask$SerialExecutor$1.run(AsyncTask.java:231)
06-21 19:14:59.731 W/System.err(28320): 	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1112)
06-21 19:14:59.731 W/System.err(28320): 	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:587)
06-21 19:14:59.731 W/System.err(28320): 	at java.lang.Thread.run(Thread.java:841)


Married Wallet Notes
----------------------------------------------------------------

Design and implement framework for wallet cosigners:

    https://code.google.com/p/bitcoinj/issues/detail?id=540

Married HD wallets: introduce shadow keychain notion:

    https://github.com/bitcoinj/bitcoinj/pull/99

Bounties:

    https://github.com/Btc4All/bitcoinj-bounties/issues?direction=asc&page=1&sort=created&state=open


Scrypt Parameters
----------------------------------------------------------------

message ScryptParameters {
  required bytes salt = 1;                        // Salt to use in generation of the wallet password (8 bytes)
  optional int64 n = 2 [default = 16384];         // CPU/ memory cost parameter
  optional int32 r = 3 [default = 8];             // Block size parameter
  optional int32 p = 4 [default = 1];             // Parallelisation parameter
}

n = 16384
r = 8
p = 1

http://stackoverflow.com/questions/11126315/what-are-optimal-scrypt-work-factors


https://blog.damballa.com/archives/330

100K hosts * 24 hours is $200

2.314e-8 $/host-sec

43 200 000 per 1$

4 320 000 000 10mSec scrypt / 1$

4^9 scrypt per $

1 1234 5678 9012

1^12 combinations


C - cost per host second
R - scrypt/sec per host

(1*10^len) * C / R

----------------------------------------------------------------

https://github.com/mosabua/maven-android-sdk-deployer

----------------------------------------------------------------

Merging with devrandom's changes on a different fork:

    git remote add devrandom git@github.com:devrandom/bitcoinj.git
    git fetch devrandom
    git merge devrandom/signer

----------------------------------------------------------------

    ./gradlew desktop:run
    ./gradlew android:installDebug android:run

    ./gradlew android:AssembleDebug android:

    /usr/local/android-studio-studio/bin/studio.sh

----------------------------------------------------------------

[INFO] Verifying dependencies
[INFO] Verifying URN: org.bitcoinj:orchid:1.0:jar:null:compile:bd98285f39f88875bb91bde940d6ca2d020edaa4
[INFO] Verifying URN: cglib:cglib-nodep:2.2:jar:null:test:59afed7ab65e7ec6585d5bc60556c3cbd203532b
[INFO] Verifying URN: com.google.code.findbugs:jsr305:2.0.1:jar:null:compile:516c03b21d50a644d538de0f0369c620989cd8f0
[INFO] Verifying URN: com.google.guava:guava:16.0.1:jar:null:compile:5fa98cd1a63c99a44dd8d3b77e4762b066a5d0c5
[INFO] Verifying URN: com.google.protobuf:protobuf-java:2.5.0:jar:null:compile:a10732c76bfacdbd633a7eb0f7968b1059a65dfa
[INFO] Verifying URN: com.h2database:h2:1.3.167:jar:null:compile:d3867d586f087e53eb12fc65e5693d8ee9a5da17
[INFO] Verifying URN: com.lambdaworks:scrypt:1.4.0:jar:null:compile:906506b74f30c8c20bccd9ed4a11112d8941fe87
[INFO] Verifying URN: com.madgag.spongycastle:core:1.51.0.0:jar:null:compile:0f642963312ea0e615ad65f28adc5a5b3a2a0862
[INFO] Verifying URN: junit:junit:4.11:jar:null:test:4e031bb61df09069aeb2bffb4019e7a5034a4ee0
[INFO] Verifying URN: net.jcip:jcip-annotations:1.0:jar:null:compile:afba4942caaeaf46aab0b976afd57cc7c181467e
[INFO] Verifying URN: org.apache.maven.plugins:maven-clean-plugin:2.5:maven-plugin:null:runtime:75653decaefa85ca8114ff3a4f869bb2ee6d605d
[INFO] Verifying URN: org.apache.maven.plugins:maven-compiler-plugin:3.1:maven-plugin:null:runtime:9977a8d04e75609cf01badc4eb6a9c7198c4c5ea
[INFO] Verifying URN: org.apache.maven.plugins:maven-dependency-plugin:2.8:maven-plugin:null:runtime:04c8dedf3d9b2a3f45f3daa93e11ca547d2063ca
[INFO] Verifying URN: org.apache.maven.plugins:maven-deploy-plugin:2.7:maven-plugin:null:runtime:6dadfb75679ca010b41286794f737088ebfe12fd
[INFO] Verifying URN: org.apache.maven.plugins:maven-enforcer-plugin:1.2:maven-plugin:null:runtime:6b755a9a0d618f8f57c0b5c4a0737a012e710a46
[INFO] Verifying URN: org.apache.maven.plugins:maven-install-plugin:2.5.1:maven-plugin:null:runtime:b6f5a4b621b9c26699c8deadb20fdc35ce568e35
[INFO] Verifying URN: org.apache.maven.plugins:maven-jar-plugin:2.5:maven-plugin:null:runtime:344d667f5ec8b90d03d698d096a1147672fc522f
[INFO] Verifying URN: org.apache.maven.plugins:maven-resources-plugin:2.6:maven-plugin:null:runtime:dd093ff6a4b680eae7ae83b5ab04310249fc6590
[INFO] Verifying URN: org.apache.maven.plugins:maven-shade-plugin:2.3:maven-plugin:null:runtime:d136adc7abccc9c12adcad6ae7a9bc51b2b7184b
[INFO] Verifying URN: org.apache.maven.plugins:maven-site-plugin:3.3:maven-plugin:null:runtime:77ba1752b1ac4c4339d6f11554800960a56a4ae1
[INFO] Verifying URN: org.apache.maven.plugins:maven-source-plugin:2.1.2:maven-plugin:null:runtime:35154aa8e6e0e84c2b5c10c3d5220d65670ba984
[INFO] Verifying URN: org.apache.maven.plugins:maven-surefire-plugin:2.12.4:maven-plugin:null:runtime:2b435f7f77777d2e62354fdc690da3f1dc47a26b
[INFO] Verifying URN: org.codehaus.mojo:cobertura-maven-plugin:2.6:maven-plugin:null:runtime:5204735a0642b42f5647d8ec876d4301e328c0d5
[INFO] Verifying URN: org.easymock:easymock:3.0:jar:null:test:f28a4c31c330f95c9acbf1108cea19952b5c496f
[INFO] Verifying URN: org.hamcrest:hamcrest-core:1.3:jar:null:test:42a25dc3219429f0e5d060061f71acb49bf010a0
[INFO] Verifying URN: org.objenesis:objenesis:1.2:jar:null:test:bfcb0539a071a4c5a30690388903ac48c0667f2a
[INFO] Verifying URN: org.slf4j:slf4j-api:1.7.6:jar:null:compile:562424e36df3d2327e8e9301a76027fca17d54ea
[INFO] Verifying URN: org.slf4j:slf4j-jdk14:1.7.6:jar:null:test:1a3301a32ea7d90c3d33e9d60edbfdc9589fc748
[INFO] Verifying URN: com.fasterxml.jackson.core:jackson-databind:2.4.2:jar:null:test:8e31266a272ad25ac4c089734d93e8d811652c1f
[INFO] Verifying URN: com.fasterxml.jackson.core:jackson-core:2.4.2:jar:null:test:ceb72830d95c512b4b300a38f29febc85bdf6e4b
[INFO] Verifying URN: com.fasterxml.jackson.core:jackson-annotations:2.4.2:jar:null:test:6bb52af09372d5064206d47d7887d41671f00f7d

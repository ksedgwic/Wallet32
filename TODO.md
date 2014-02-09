
Bugs
----------------------------------------------------------------

* Pausing Passcode activity while it is "encrypting wallet" crashes.

* Need to handle precision of BTC values much better then 4 fixed places.

* Disable Settings actions on everything prior to MainActivity.

* I don't think the Home "Up" feature is working from the ViewSeed page,
  at least on initial create.

Needed
----------------------------------------------------------------

* Testnet build.

* View Seed needs to be "pre-main" when started from create wallet
  and BaseWalletActivity when called from main.

* Electrum compatibility.

* Embelish messages and directions.

* Add file logging.

* Send dump feature.


Next
----------------------------------------------------------------

* mBTC Support

* Tune for slow platforms.

* Make passcode creation estimate of brute-force difficulty.

* Add coindesk index rate updater.

* Add transaction label support.

* Move view seed from main to settings.

* Auto logout of service after idle for N minutes.

* Non-USD fiat support.

* Font selections for tables.

* Sweep Activity.

* Create "adjust" dialog when amount > avail.

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

scp ~/Wallet32.apk sl0:htdocs/ken/public/

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

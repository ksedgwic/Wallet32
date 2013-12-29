
Bugs
----------------------------------------------------------------

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

* Add change passcode.

* Embelish messages and directions.

* Add file logging.

* Send dump feature.

* bitcoinj update.

* Make Send balances the available amount.

* Figure out reasonable minimum fee calculation.


Recover Wallet
----------------------------------------------------------------

* Make recover check for more accounts.

* Make recover auto-rescan when needed.

* Notify user when rescan required.


Next
----------------------------------------------------------------

* mBTC Support

* Tune for slow platforms.

* Make passcode creation estimate of brute-force difficulty.

* Add coindesk index rate updater.

* Add transaction label support.

* Move view seed from main to settings.

* Auto logout of service after idle for N minutes.

* Audio/Vibration notification of wallet changes.

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

Build Wallet32.apk

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

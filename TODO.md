
Bugs
----------------------------------------------------------------

* View Transactions Account Filter "slipping" back to "All Accounts".

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

* Make SendBitcoinActivity address field accept URI format (prefill amount).


Checklist
----------------------------------------------------------------

Update README.txt version.

Update values/strings.xml version.

Tag repository.


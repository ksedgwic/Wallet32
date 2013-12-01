
Bugs
----------------------------------------------------------------

* On HTC, "back" from Main activity and then resume crashes ...

* View Transactions too quickly crashes the client.

* View Transactions Account Filter "slipping" back to "All Accounts".

* On relaunch of runninc wallet need to skip passcode and service spinup.

* Need to handle precision of BTC values much better then 4 fixed places.


Needed
----------------------------------------------------------------

* Add an About screen from Settings.

* Electrum compatibility.

* Add change passcode.

* Add syncing progress screen between lobby and main. Also use on rescan.

* Embelish messages and directions.

* Account View Activity

* Tune logging ... SECURITY!

* Add file logging.

* Send dump feature.

* bitcoinj update.

* Make Send balances the available amount.

* BIP-0039 update.

* Figure out reasonable minimum fee calculation.


Recover Wallet
----------------------------------------------------------------

* Add getEarliestKeyCreationTime override in MyWalletAppKit when
  performing recovery.  Needs to be set much earlier ...

* Make recover check for more accounts.

* Make recover auto-rescan when needed.

* Notify user when rescan required.


Next
----------------------------------------------------------------

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


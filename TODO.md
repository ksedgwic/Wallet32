
* Add transaction label support.

* Move view seed from main to settings.

* Notify user when rescan required.

* Add getEarliestKeyCreationTime override in MyWalletAppKit when
  performing recovery.  Needs to be set much earlier ...

* Make recover check for more accounts.

* Make recover auto-rescan when needed.

* Add change passcode.

* Add syncing progress screen between lobby and main. Also use on rescan.

* Auto logout of service after idle for N minutes.

* Audio/Vibration notification of wallet changes.

* Non-USD fiat support.

* Font selections for tables.

* Factor all common stuff into BaseActivity.

* Make status more prominent (color?)

* Sweep Activity.

* Account View Activity

* Tune logging ... SECURITY!

* Add file logging.

* Create "adjust" dialog when amount > avail.

* Make Send balances the available amount.

* Add notification bar icon when WalletService is running.

* Factor wallet file prefix into single location.

* Make SendBitcoinActivity address field accept URI format (prefill amount).

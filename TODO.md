
Bugs
----------------------------------------------------------------

* On HTC, "back" from Main activity and then resume crashes ...

* View Transactions too quickly crashes the client.

* View Transactions Account Filter "slipping" back to "All Accounts".

* On relaunch of running wallet need to skip passcode and service spinup.

* Need to handle precision of BTC values much better then 4 fixed places.

* Disable Settings actions on everything prior to MainActivity.

* I don't think the Home "Up" feature is working from the ViewSeed page,
  at least on initial create.

* The rescan interface sucks:
> 1. The preferences GUI is lousy; nothing happens after you pick it.
> 2. We seem to go to some places and then end up on a dead MainActivity.

Needed
----------------------------------------------------------------

* View Seed needs to be "pre-main" when started from create wallet
  and BaseWalletActivity when called from main.

* Add an About screen from Settings.

* Electrum compatibility.

* Add change passcode.

* Embelish messages and directions.

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


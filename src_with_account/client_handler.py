from datetime import datetime
import imp, os, time

Config = imp.load_source("module.name", './record.py')
ut = imp.load_source("module.name", './autoETUtils.py')
trade_port = Config.trading_port[0]

os.system(f"java_99 -jar ../etoro-api/src_with_account/build/libs/etoro-api-0.1.4.8130.jar ")

while True:
    if not ut.renewAvailable():
        ut.send_text_message("Renew the session~")
        os.system(f"pkill -9 -f etoro-api-0.1.4.8130.jar");
        time.sleep(1)
        os.system(f"java_99 -jar ../etoro-api/src_with_account/build/libs/etoro-api-0.1.4.8130.jar ")
    time.sleep(60)






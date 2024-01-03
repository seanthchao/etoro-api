echo "$1"
# sudo ln -fP /opt/google/chrome/chrome /opt/google/chrome/chrome_"$i"
sudo ln -fP /usr/bin/java /usr/bin/java_"$1"
java_"$1" -jar ../etoro-api/src_no_account/build/libs/etoro-api-0.1.4.free_after_login."$1".jar &
#sleep 20s
#java -jar build/libs/etoro-api-0.1.4.8087.jar
# java -jar build/libs/etoro-api-0.1.4.8088.jar
# java -jar build/libs/etoro-api-0.1.4.8091.jar
#java -jar build/libs/etoro-api-0.1.4.80.jar

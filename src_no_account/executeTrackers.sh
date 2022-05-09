for i in $(seq $1 $2)
do
    echo "$i"
    # sudo ln -fP /opt/google/chrome/chrome /opt/google/chrome/chrome_"$i"
    sudo ln -fP /opt/brave.com/brave/brave /opt/brave.com/brave/brave_"$i"
    sudo ln -fP /usr/bin/java /usr/bin/java_"$i"
    ln -fP ./chromedriver ./drivers/ubuntu/chromedriver_"$i"
    # ln -fP /home/sean/etoro/etoro_api/etoro-api/drivers/ubuntu/chromedriver /home/sean/etoro/etoro-api/drivers/ubuntu/chromedriver_"$i"
    # ln -fP /home/sean/etoro/etoro_api/etoro-api/python/drivers/ubuntu/chromedriver /home/sean/etoro/etoro-api_no_account/drivers/ubuntu/chromedriver_"$i"
    java_"$i" -jar ../etoro-api/src_no_account/build/libs/etoro-api-0.1.4."$i".jar &
    sleep 10s
done
#java -jar build/libs/etoro-api-0.1.4.8087.jar
# java -jar build/libs/etoro-api-0.1.4.8088.jar
# java -jar build/libs/etoro-api-0.1.4.8091.jar
#java -jar build/libs/etoro-api-0.1.4.80.jar

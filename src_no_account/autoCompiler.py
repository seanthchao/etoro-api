import yaml, io, os, sys
import xml.etree.ElementTree as ET

# dir(xml.etree.__package__)

# pom_path = '/home/sean/etoro/etoro-api/pom.xml'
gradle_kts_path = '/home/sean/etoro/etoroAuto_test/etoro-api/src_no_account/build.gradle.kts'
application_path = '/home/sean/etoro/etoroAuto_test/etoro-api/src_no_account/src/main/resources/application.yaml'

port_range = range(8066, 8125)
for p in port_range:
    # p=port_range[0]
    with open(application_path, "r") as stream:
        data = yaml.safe_load(stream)
        data['server']['port'] = p

        # yaml.dump(data, stream, default_flow_style=False, allow_unicode=True)
    with io.open(application_path, 'w', encoding='utf8') as outfile:
        yaml.dump(data, outfile, default_flow_style=False, allow_unicode=True)

    kts = open(gradle_kts_path, 'r')
    cont = kts.readlines()
    # version = "0.1.4"\n
    kts.close()
    cont[11] = f'version = "0.1.4.{p}"\n'
    kts = open(gradle_kts_path, 'w')
    new_cont = "".join(cont)
    kts.write(new_cont)
    kts.close()
    os.system('/home/sean/etoro/etoro-api/gradlew build')
    # !cd /home/sean/etoro/etoro-api | gradlew build

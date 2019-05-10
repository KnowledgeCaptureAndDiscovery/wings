git clone https://github.com/KnowledgeCaptureAndDiscovery/WINGS-OPMW-Mapper.git -b master opmm-tmp
rsync -avzP opmm-tmp/src/main/java/edu/isi/wings/opmm/ opmm/src/main/java/edu/isi/wings/opmm/
rm -rf opmm-tmp


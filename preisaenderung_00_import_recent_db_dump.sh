#!/bin/bash

dump_file=$(ncftpls -o useMLSD=0 -x "-lt" -u USERNAME -p PASSWORD ftp://server.domain.com/path/to/folder | head -1 | awk '{print $9}')
cd /tmp
ncftpget -u USERNAME -p PASSWORD ftp://server.domain.com/path/to/folder/$dump_file
mysql --local-infile -hlocalhost -ukassenadmin -p -e "source $dump_file" kasse

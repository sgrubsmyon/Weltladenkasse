#!/bin/bash

# Check if mysql is executable
run_check() {
    sudo mysql -u root --execute="quit"
    mysql_runs_ok=$(echo $?)
    echo $mysql_runs_ok
}

print_problem_help() {
    echo -e "There seems to be a problem with your MySQL install."
    echo -e "Please try to install it again through your package manager."
    echo -e "Try running \`sudo pacman -Syu mariadb\' and then"
    echo -e "\`sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql\'"
    echo -e "(See https://wiki.archlinux.org/index.php/MariaDB#Installation)."
}

echo -n "Checking if MySQL works... "
mysql_works=$(run_check)
if [[ $mysql_works -eq 0 ]]; then echo "yes"; else echo "no"; fi

if [[ $mysql_works -ne 0 ]]; then
    echo -e "MySQL seems to be not installed. Trying to install it now (need root privileges)."
    # See https://wiki.archlinux.org/index.php/MariaDB#Installation
    sudo pacman -Syu mariadb
    sudo mariadb-install-db --user=mysql --basedir=/usr --datadir=/var/lib/mysql
    sudo systemctl enable mariadb.service
    sudo systemctl start mariadb.service
    echo -n "Checking if MySQL works... "
    mysql_works=$(run_check)
    if [[ $mysql_works -eq 0 ]]; then
        echo -e "MySQL is now successfully installed."
    else
        echo -e "There was an error during the MySQL installation \n(or you entered wrong root password).\n"
        print_problem_help
        exit 1
    fi
elif [[ $mysql_works -eq 0 ]]; then
    echo -e "MySQL seems to be already installed and working.\nTrying to use this MySQL install."
fi

echo ""
`dirname $0`/mysql/generateDB.sh -n

exit 0

